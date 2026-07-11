package com.neusoft.hospital.common.notify;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Result of one SMS attempt. Mirrors what Aliyun dysmsapi returns so the
 * scheduler can decide whether to retry next minute or treat as final failure.
 */
@Getter
@AllArgsConstructor
public class SmsSendResult {
    /** Whether the gateway accepted the request (NOT whether the user got it). */
    private final boolean accepted;
    /** Vendor-side request id, useful for tracing in the operator console. */
    private final String gatewayRequestId;
    /** Human-readable message; null on success. */
    private final String error;

    public static SmsSendResult ok(String requestId) {
        return new SmsSendResult(true, requestId, null);
    }

    public static SmsSendResult failure(String error) {
        return new SmsSendResult(false, null, error);
    }
}
