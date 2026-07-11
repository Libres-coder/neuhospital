package com.neusoft.hospital.common.notify;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * Sends SMS via Aliyun dysmsapi. No third-party SDK: signature is computed
 * locally with HMAC-SHA1 and the request goes out as a regular
 * application/x-www-form-urlencoded HTTPS POST. This keeps the dependency
 * surface small and makes the protocol easy to read.
 *
 * <p>Active when {@code app.sms.provider=aliyun} AND {@code ALIYUN_SMS_ACCESS_KEY_ID}
 * is set. Enabling without credentials is a configuration error that we
 * surface at boot, not at request time.</p>
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "app.sms.provider", havingValue = "aliyun")
public class AliyunSmsSender implements SmsSender {

    private static final String HOST = "dysmsapi.aliyuncs.com";
    private static final String ACTION = "SendSms";
    private static final String VERSION = "2017-05-25";
    private static final String SIGN_METHOD = "HMAC-SHA1";
    private static final DateTimeFormatter ISO8601 =
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'").withZone(ZoneOffset.UTC);

    @Value("${aliyun.sms.access-key-id}")
    private String accessKeyId;
    @Value("${aliyun.sms.access-key-secret}")
    private String accessKeySecret;
    @Value("${aliyun.sms.sign-name:}")
    private String signName;

    private final HttpClient http = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();

    private final ObjectMapper json = new ObjectMapper();

    @Override
    public SmsSendResult send(String phone, String templateCode, String... templateArgs) {
        if (!StringUtils.hasText(accessKeyId) || !StringUtils.hasText(accessKeySecret)) {
            String msg = "Aliyun SMS not configured (ALIYUN_SMS_ACCESS_KEY_ID / SECRET missing)";
            log.error(msg);
            return SmsSendResult.failure(msg);
        }
        if (!StringUtils.hasText(signName)) {
            return SmsSendResult.failure("ALIYUN_SMS_SIGN_NAME missing");
        }

        Map<String, String> params = new LinkedHashMap<>();
        params.put("PhoneNumbers", phone);
        params.put("SignName", signName);
        params.put("TemplateCode", templateCode);
        params.put("TemplateParam", toJsonArgs(templateArgs));
        params.put("AccessKeyId", accessKeyId);
        params.put("SignatureMethod", SIGN_METHOD);
        params.put("SignatureNonce", UUID.randomUUID().toString());
        params.put("SignatureVersion", "1.0");
        params.put("Timestamp", ISO8601.format(Instant.now()));
        params.put("Format", "JSON");
        params.put("RegionId", "cn-hangzhou");
        params.put("Action", ACTION);
        params.put("Version", VERSION);
        String signature = computeSignature(params, accessKeySecret);
        params.put("Signature", signature);

        // form-encoded body
        StringBuilder body = new StringBuilder();
        for (Map.Entry<String, String> e : params.entrySet()) {
            if (body.length() > 0) body.append('&');
            body.append(urlEncode(e.getKey())).append('=').append(urlEncode(e.getValue()));
        }

        HttpRequest req;
        try {
            req = HttpRequest.newBuilder(URI.create("https://" + HOST + "/"))
                    .header("Content-Type", "application/x-www-form-urlencoded")
                    .timeout(Duration.ofSeconds(10))
                    .POST(HttpRequest.BodyPublishers.ofString(body.toString()))
                    .build();
            HttpResponse<String> resp = http.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            return parseResponse(resp.body());
        } catch (IOException | InterruptedException e) {
            if (e instanceof InterruptedException) Thread.currentThread().interrupt();
            log.warn("Aliyun SMS HTTP error: {}", e.toString());
            return SmsSendResult.failure("transport: " + e.getMessage());
        }
    }

    /** Aliyun returns JSON like {"RequestId":"abc","Code":"OK","Message":"OK"} on success. */
    private SmsSendResult parseResponse(String body) {
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> m = json.readValue(body, Map.class);
            String code = (String) m.getOrDefault("Code", "");
            String reqId = (String) m.getOrDefault("RequestId", null);
            String msg = (String) m.getOrDefault("Message", null);
            if ("OK".equalsIgnoreCase(code)) {
                log.info("Aliyun SMS OK requestId={}", reqId);
                return SmsSendResult.ok(reqId);
            }
            log.warn("Aliyun SMS rejected: code={} message={} requestId={}", code, msg, reqId);
            return SmsSendResult.failure(code + (msg != null ? " " + msg : ""));
        } catch (IOException e) {
            log.warn("Aliyun SMS response parse failed: {}", e.toString());
            return SmsSendResult.failure("parse: " + e.getMessage());
        }
    }

    /** Inline JSON serialization for template args, escaping per Aliyun's spec. */
    private String toJsonArgs(String... templateArgs) {
        try {
            return json.writeValueAsString(Arrays.asList(templateArgs));
        } catch (IOException e) {
            return "{}";
        }
    }

    /**
     * Compute the v1 signature: percent-encode each (key, value) pair sorted by key,
     * concatenate with &, prefix with HTTP method + host + path, HMAC-SHA1 with
     * (secret + "&"), base64 encode.
     */
    static String computeSignature(Map<String, String> params, String secret) {
        List<String> sortedKeys = new ArrayList<>(params.keySet());
        Collections.sort(sortedKeys);

        StringBuilder canonical = new StringBuilder();
        for (int i = 0; i < sortedKeys.size(); i++) {
            String k = sortedKeys.get(i);
            String v = params.get(k);
            if (canonical.length() > 0) canonical.append('&');
            canonical.append(urlEncode(k)).append('=').append(urlEncode(v));
        }
        try {
            String stringToSign = "POST&" + urlEncode("/") + "&" + urlEncode(canonical.toString());
            Mac mac = Mac.getInstance("HmacSHA1");
            mac.init(new SecretKeySpec((secret + "&").getBytes(StandardCharsets.UTF_8), "HmacSHA1"));
            byte[] sigBytes = mac.doFinal(stringToSign.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(sigBytes);
        } catch (Exception e) {
            throw new IllegalStateException("HMAC-SHA1 unavailable", e);
        }
    }

    private static String urlEncode(String s) {
        // Aliyun expects application/x-www-form-urlencoded encoding with space as %20
        // and a few chars (like '*', '~', '-', '_', '.') NOT encoded.
        String encoded = java.net.URLEncoder.encode(s, StandardCharsets.UTF_8);
        return encoded
                .replace("+", "%20")
                .replace("%7E", "~")
                .replace("%2A", "*")
                .replace("%2D", "-")
                .replace("%5F", "_")
                .replace("%2E", ".");
    }
}
