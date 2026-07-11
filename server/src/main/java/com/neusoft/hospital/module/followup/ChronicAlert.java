package com.neusoft.hospital.module.followup;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "chronic_alerts")
@Getter
@Setter
@NoArgsConstructor
public class ChronicAlert {

    @Id
    @Column(length = 40)
    private String id;

    @Column(name = "patient_id", nullable = false, length = 40)
    private String patientId;

    @Column(name = "record_id", nullable = false, length = 40)
    private String recordId;

    @Column(nullable = false, length = 16)
    private String type;

    @Column(nullable = false)
    private int level;

    @Column(nullable = false, length = 255)
    private String message;

    @Column(name = "create_time", nullable = false)
    private long createTime;

    @Column(nullable = false)
    private boolean acknowledged;
}