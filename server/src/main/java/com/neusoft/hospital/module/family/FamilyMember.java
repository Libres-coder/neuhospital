package com.neusoft.hospital.module.family;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * A patient's relative (or self) that they can book appointments for.
 * One User can own many FamilyMembers; default one is the booking patient
 * selected in the app. All operations are scoped by {@code ownerId}, so
 * token-based authorization is enough — no separate role needed.
 */
@Entity
@Table(name = "family_members")
@Getter
@Setter
@NoArgsConstructor
public class FamilyMember {

    @Id
    @Column(length = 40)
    private String id;

    /** Owner's user id (FK to users.id). */
    @Column(name = "owner_id", nullable = false, length = 40)
    private String ownerId;

    @Column(nullable = false, length = 64)
    private String name;

    @Column(nullable = false, length = 20)
    private String phone;

    @Column(name = "id_card", nullable = false, length = 32)
    private String idCard;

    /** 父母 / 子女 / 配偶 / 本人 / 其他 */
    @Column(nullable = false, length = 16)
    private String relation;

    @Column(length = 255)
    private String avatar;

    @Column(name = "is_default", nullable = false)
    private boolean isDefault;

    @Column(name = "create_time", nullable = false)
    private long createTime;

    @Column(name = "update_time")
    private Long updateTime;
}
