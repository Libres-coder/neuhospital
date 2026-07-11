package com.neusoft.hospital.module.followup;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface FollowUpPlanRepository extends JpaRepository<FollowUpPlan, String> {
    List<FollowUpPlan> findByPatientIdOrderByCreateTimeDesc(String patientId);
}