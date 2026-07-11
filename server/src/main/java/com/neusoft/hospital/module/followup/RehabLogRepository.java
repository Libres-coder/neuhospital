package com.neusoft.hospital.module.followup;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RehabLogRepository extends JpaRepository<RehabLog, String> {
    List<RehabLog> findByPatientIdOrderByLogDateDesc(String patientId);
}