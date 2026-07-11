package com.neusoft.hospital.module.followup;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "rehab_logs")
@Getter
@Setter
@NoArgsConstructor
public class RehabLog {

    @Id
    @Column(length = 40)
    private String id;

    @Column(name = "patient_id", nullable = false, length = 40)
    private String patientId;

    @Column(nullable = false, length = 64)
    private String disease;

    @Column(name = "log_date", nullable = false, length = 10)
    private String logDate;

    @Column(name = "items_json", columnDefinition = "TEXT", nullable = false)
    private String itemsJson;

    @Column(nullable = false)
    private boolean completed;

    @Column(name = "create_time", nullable = false)
    private long createTime;
}