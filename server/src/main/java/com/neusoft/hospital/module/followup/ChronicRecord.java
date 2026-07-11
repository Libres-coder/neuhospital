package com.neusoft.hospital.module.followup;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "chronic_records")
@Getter
@Setter
@NoArgsConstructor
public class ChronicRecord {

    @Id
    @Column(length = 40)
    private String id;

    @Column(name = "patient_id", nullable = false, length = 40)
    private String patientId;

    @Column(nullable = false, length = 16)
    private String type;

    @Column(name = "record_date", nullable = false, length = 10)
    private String recordDate;

    private Integer systolic;
    private Integer diastolic;
    @Column(name = "heart_rate")
    private Integer heartRate;
    @Column(name = "fasting_glucose")
    private Double fastingGlucose;
    @Column(name = "postprandial_glucose")
    private Double postprandialGlucose;
    private Double hba1c;
    @Column(length = 255)
    private String note;

    @Column(name = "alert_level", nullable = false)
    private int alertLevel;

    @Column(name = "create_time", nullable = false)
    private long createTime;
}