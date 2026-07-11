package com.neusoft.hospital.module.appointment;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

public class AppointmentDtos {

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class DepartmentDto {
        private String id;
        private String parentId;
        private String name;
        private String namePy;
        private String desc;
        private String iconUrl;
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class DoctorDto {
        private String id;
        private String name;
        private String departmentId;
        private String departmentName;
        private String title;
        private String expertise;
        private String profile;
        private Float rating;
        private String avatarUrl;
        private List<ScheduleDto> schedule;
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class ScheduleDto {
        private String date;
        private String dayOfWeek;
        private List<TimeSlotDto> slots;
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class TimeSlotDto {
        private String id;
        private String startTime;
        private String endTime;
        private int available;
        private int total;
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class AppointmentDto {
        private String id;
        private String patientId;
        private String patientName;
        private String doctorId;
        private String doctorName;
        private String departmentId;
        private String departmentName;
        private String date;
        private String timeSlot;
        private int duration;
        private String status;
        private boolean reminderSet;
        private long createTime;
    }

    @Data
    public static class BookRequest {
        private String doctorId;
        private String date;
        private String timeSlot;
        private int duration;
        private String patientId;
        private String patientName;
    }

    @Data
    public static class StatusRequest {
        private String id;
    }
}
