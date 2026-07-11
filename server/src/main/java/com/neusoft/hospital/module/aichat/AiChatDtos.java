package com.neusoft.hospital.module.aichat;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

public class AiChatDtos {

    @Data
    public static class SendRequest {
        private String sessionId; // optional - if null, server creates a new session
        private String title;     // optional - title for new session
        private String content;
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class SendResponse {
        private String sessionId;
        private String reply;
        private List<HistoryItem> history;
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class HistoryItem {
        private String role;
        private String content;
        private long time;
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class SessionSummary {
        private String id;
        private String title;
        private String lastMessage;
        private long lastTime;
    }
}