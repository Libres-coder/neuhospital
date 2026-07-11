package com.neusoft.hospital.module.aichat;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "chat_sessions")
@Getter
@Setter
@NoArgsConstructor
public class ChatSession {

    @Id
    @Column(length = 40)
    private String id;

    @Column(name = "patient_id", nullable = false, length = 40)
    private String patientId;

    @Column(nullable = false, length = 128)
    private String title;

    @Column(name = "last_message", length = 255)
    private String lastMessage;

    @Column(name = "create_time", nullable = false)
    private long createTime;

    @Column(name = "last_time", nullable = false)
    private long lastTime;
}
