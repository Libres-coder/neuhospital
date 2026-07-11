package com.neusoft.hospital.module.followup;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.util.List;
import java.util.Map;

public class FollowUpDtos {

    @Data
    public static class CreatePlanRequest {
        private String disease;
        private String surgeryDate;
        private int totalDays = 90;
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class PlanDto {
        private String id;
        private String patientId;
        private String disease;
        private String surgeryDate;
        private int totalDays;
        private long createTime;
        private List<TaskDto> tasks;
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class TaskDto {
        private String id;
        private String planId;
        private int dayIndex;
        private String targetDate;
        private List<String> questions;
        private Map<String, String> answers;
        private boolean completed;
        private Long completedTime;
        private String doctorReply;
    }

    @Data
    public static class CompleteTaskRequest {
        private String taskId;
        private Map<String, String> answers;
    }

    @Data
    public static class CreateRehabRequest {
        private String disease;
        private List<String> items;
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class RehabLogDto {
        private String id;
        private String disease;
        private String logDate;
        private List<String> items;
        private boolean completed;
    }

    @Data
    public static class ChronicRecordRequest {
        private String type;
        private String date;
        private Integer systolic;
        private Integer diastolic;
        private Integer heartRate;
        private Double fastingGlucose;
        private Double postprandialGlucose;
        private Double hba1c;
        private String note;
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class ChronicRecordDto {
        private String id;
        private String type;
        private String date;
        private Integer systolic;
        private Integer diastolic;
        private Integer heartRate;
        private Double fastingGlucose;
        private Double postprandialGlucose;
        private Double hba1c;
        private String note;
        private int alertLevel;
        private long createTime;
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class ChronicAlertDto {
        private String id;
        private String recordId;
        private String type;
        private int level;
        private String message;
        private long createTime;
        private boolean acknowledged;
    }
}