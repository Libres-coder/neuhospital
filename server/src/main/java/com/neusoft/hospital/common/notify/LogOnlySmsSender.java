package com.neusoft.hospital.common.notify;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Default development sender. Logs the payload to stdout and returns success
 * with a synthetic request id. Used whenever {@code app.sms.provider=log} (the
 * default), or when {@code ALIYUN_SMS_*} env vars are absent.
 *
 * <p>Why we don't just "no-op" the call in dev: we want the operator to
 * actually see what would have been sent so they can verify templates and
 * phone numbers.</p>
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "app.sms.provider", havingValue = "log", matchIfMissing = true)
public class LogOnlySmsSender implements SmsSender {

    @Override
    public SmsSendResult send(String phone, String templateCode, String... templateArgs) {
        String reqId = "log-" + UUID.randomUUID().toString().replace("-", "").substring(0, 16);
        log.info("[SMS:DEV] would-send phone={} template={} args={} requestId={}",
                phone, templateCode, String.join("|", templateArgs), reqId);
        return SmsSendResult.ok(reqId);
    }
}
