package com.neusoft.hospital.common;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

@Data
@JsonInclude(JsonInclude.Include.ALWAYS)
public class Result<T> {

    private int code;
    private String message;
    private T data;
    private String traceId;

    public static <T> Result<T> ok(T data) {
        Result<T> r = new Result<>();
        r.code = 0;
        r.message = "ok";
        r.data = data;
        r.traceId = currentTraceId();
        return r;
    }

    public static <T> Result<T> fail(int code, String message) {
        Result<T> r = new Result<>();
        r.code = code;
        r.message = message;
        r.data = null;
        r.traceId = currentTraceId();
        return r;
    }

    public static <T> Result<T> fail(String message) {
        return fail(500, message);
    }

    private static String currentTraceId() {
        try {
            return org.slf4j.MDC.get(TraceIdFilter.MDC_KEY);
        } catch (Exception ignored) {
            return null;
        }
    }
}