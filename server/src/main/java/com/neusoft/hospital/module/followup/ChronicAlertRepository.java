package com.neusoft.hospital.module.followup;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ChronicAlertRepository extends JpaRepository<ChronicAlert, String> {
    List<ChronicAlert> findByPatientIdAndAcknowledgedFalseOrderByCreateTimeDesc(String patientId);
}