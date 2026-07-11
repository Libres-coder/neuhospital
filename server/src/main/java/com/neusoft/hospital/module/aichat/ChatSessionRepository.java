package com.neusoft.hospital.module.aichat;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ChatSessionRepository extends JpaRepository<ChatSession, String> {
    List<ChatSession> findByPatientIdOrderByLastTimeDesc(String patientId);
}

