package com.neusoft.hospital.module.appointment;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "appointments")
@Getter
@Setter
@NoArgsConstructor
public class Appointment {

    @Id
    @Column(length = 40)
    private String id;

    @Column(name = "patient_id", nullable = false, length = 40)
    private String patientId;

    @Column(name = "patient_name", nullable = false, length = 64)
    private String patientName;

    @Column(name = "doctor_id", nullable = false, length = 40)
    private String doctorId;

    @Column(name = "doctor_name", nullable = false, length = 64)
    private String doctorName;

    @Column(name = "department_id", nullable = false, length = 40)
    private String departmentId;

    @Column(name = "department_name", nullable = false, length = 64)
    private String departmentName;

    @Column(name = "appointment_date", nullable = false, length = 10)
    private String appointmentDate;

    @Column(name = "time_slot", nullable = false, length = 16)
    private String timeSlot;

    @Column(nullable = false)
    private int duration;

    /** pending / payed / confirmed / completed / cancelled / no_show */
    @Column(nullable = false, length = 16)
    private String status;

    @Column(name = "reminder_set", nullable = false)
    private boolean reminderSet;

    /** Wall-clock epoch ms when the SMS reminder was sent; null until sent. */
    @Column(name = "reminder_sent_at")
    private Long reminderSentAt;

    @Column(name = "create_time", nullable = false)
    private long createTime;

    @Column(name = "update_time", nullable = false)
    private long updateTime;

    /** Optimistic-lock counter, bumped on every UPDATE. */
    @Version
    @Column(nullable = false)
    private int version;
}
