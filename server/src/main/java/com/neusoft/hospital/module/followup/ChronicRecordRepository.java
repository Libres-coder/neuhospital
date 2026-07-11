package com.neusoft.hospital.module.followup;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ChronicRecordRepository extends JpaRepository<ChronicRecord, String> {
    List<ChronicRecord> findByPatientIdAndTypeOrderByRecordDateDesc(String patientId, String type);
}