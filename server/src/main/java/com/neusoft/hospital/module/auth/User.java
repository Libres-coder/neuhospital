package com.neusoft.hospital.module.auth;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
public class User {

    @Id
    @Column(length = 40)
    private String id;

    @Column(nullable = false, length = 20)
    private String phone;

    @Column(nullable = false, length = 64)
    private String name;

    @Column(name = "is_verified", nullable = false)
    private boolean verified;

    @Column(name = "ehs_card_no", length = 64)
    private String ehsCardNo;

    @Column(name = "ehs_bound", nullable = false)
    private boolean ehsBound;

    @Column(name = "create_time", nullable = false)
    private long createTime;

    @Column(name = "update_time", nullable = false)
    private long updateTime;

    public User(String id, String phone, String name) {
        this.id = id;
        this.phone = phone;
        this.name = name;
        long now = System.currentTimeMillis();
        this.createTime = now;
        this.updateTime = now;
    }
}