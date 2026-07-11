package com.neusoft.hospital.module.followup;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "follow_up_tasks")
@Getter
@Setter
@NoArgsConstructor
public class FollowUpTask {

    @Id
    @Column(length = 40)
    private String id;

    @Column(name = "plan_id", nullable = false, length = 40)
    private String planId;

    @Column(name = "patient_id", nullable = false, length = 40)
    private String patientId;

    @Column(name = "day_index", nullable = false)
    private int dayIndex;

    @Column(name = "target_date", nullable = false, length = 10)
    private String targetDate;

    @Column(name = "questions_json", columnDefinition = "TEXT", nullable = false)
    private String questionsJson;

    @Column(name = "answers_json", columnDefinition = "TEXT")
    private String answersJson;

    @Column(nullable = false)
    private boolean completed;

    @Column(name = "completed_time")
    private Long completedTime;

    @Column(name = "doctor_reply", columnDefinition = "TEXT")
    private String doctorReply;
}