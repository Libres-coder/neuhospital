package com.neusoft.hospital.common.notify;

/**
 * Strategy for sending SMS. Two implementations live in this project:
 *
 * <ul>
 *   <li>{@link LogOnlySmsSender} (default) — logs to console. Used when
 *       {@code app.sms.provider=log} or when {@code ALIYUN_SMS_* } env vars
 *       are missing.</li>
 *   <li>{@link AliyunSmsSender} — talks to dysmsapi.aliyuncs.com with
 *       HMAC-SHA1 signed requests.</li>
 * </ul>
 *
 * <p>Implementations MUST be safe to call from a scheduled thread (i.e.
 * stateless and idempotent at the gateway level).</p>
 */
public interface SmsSender {
    /**
     * Send a templated SMS to one phone number.
     *
     * @param phone        E.164-shaped Chinese mobile, e.g. {@code 13800000000}
     * @param templateCode Vendor template id, e.g. {@code SMS_123456}
     * @param templateArgs Template placeholder values (must match template's
     *                     positional ordering).
     * @return gateway result; never null.
     */
    SmsSendResult send(String phone, String templateCode, String... templateArgs);
}
