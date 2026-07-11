package com.neusoft.hospital.module.appointment;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "departments")
@Getter
@Setter
@NoArgsConstructor
public class Department {

    @Id
    @Column(length = 40)
    private String id;

    @Column(name = "parent_id", length = 40)
    private String parentId;

    @Column(nullable = false, length = 64)
    private String name;

    @Column(name = "name_py", length = 64)
    private String namePy;

    @Column(length = 255)
    private String description;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder;

    /** icon URL ? currently unused, kept nullable for future. */
    @Transient
    private String iconUrl;
}