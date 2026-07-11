package com.neusoft.hospital.module.appointment;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "doctors")
@Getter
@Setter
@NoArgsConstructor
public class Doctor {

    @Id
    @Column(length = 40)
    private String id;

    @Column(nullable = false, length = 64)
    private String name;

    @Column(name = "department_id", nullable = false, length = 40)
    private String departmentId;

    @Column(name = "department_name", nullable = false, length = 64)
    private String departmentName;

    @Column(nullable = false, length = 32)
    private String title;

    @Column(length = 255)
    private String expertise;

    @Column(columnDefinition = "TEXT")
    private String profile;

    @Column(nullable = false)
    private Double rating;

    @Column(name = "avatar_url", length = 255)
    private String avatarUrl;
}
