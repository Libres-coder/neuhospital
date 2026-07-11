package com.neusoft.hospital.module.followup;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "follow_up_plans")
@Getter
@Setter
@NoArgsConstructor
public class FollowUpPlan {

    @Id
    @Column(length = 40)
    private String id;

    @Column(name = "patient_id", nullable = false, length = 40)
    private String patientId;

    @Column(nullable = false, length = 64)
    private String disease;

    @Column(name = "surgery_date", nullable = false, length = 10)
    private String surgeryDate;

    @Column(name = "total_days", nullable = false)
    private int totalDays;

    @Column(name = "create_time", nullable = false)
    private long createTime;
}