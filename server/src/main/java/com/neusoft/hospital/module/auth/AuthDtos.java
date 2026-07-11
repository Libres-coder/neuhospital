package com.neusoft.hospital.module.auth;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

public class AuthDtos {

    @Data
    public static class SmsRequest {
        private String phone;
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class SmsResponse {
        private String traceId;
        private int ttlSeconds;
    }

    @Data
    public static class LoginRequest {
        private String phone;
        private String code;
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class LoginResponse {
        @com.fasterxml.jackson.annotation.JsonProperty("token")
        private String accessToken;
        @com.fasterxml.jackson.annotation.JsonProperty("refreshToken")
        private String refreshToken;
        private String userId;
        private String phone;
        private String name;
        @com.fasterxml.jackson.annotation.JsonProperty("isVerified")
        private boolean verified;
        @com.fasterxml.jackson.annotation.JsonProperty("hasEhsCard")
        private boolean hasEhsCard;
        @com.fasterxml.jackson.annotation.JsonProperty("accessTtlSeconds")
        private long accessTtlSeconds;
        @com.fasterxml.jackson.annotation.JsonProperty("refreshTtlSeconds")
        private long refreshTtlSeconds;
    }

    @Data
    public static class RefreshRequest {
        private String refreshToken;
    }

    @Data
    public static class IdCardRequest {
        private String name;
        private String idCard;
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class VerifyIdCardResponse {
        private boolean verified;
    }

    @Data
    public static class BindEhsRequest {
        private String ehsCardNo;
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class BindEhsResponse {
        private String ehsCardNo;
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class MeResponse {
        private String userId;
        private String phone;
        private String name;
        @com.fasterxml.jackson.annotation.JsonProperty("isVerified")
        private boolean verified;
        @com.fasterxml.jackson.annotation.JsonProperty("hasEhsCard")
        private boolean hasEhsCard;
    }
}