package com.neusoft.hospital.module.followup;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface FollowUpTaskRepository extends JpaRepository<FollowUpTask, String> {
    List<FollowUpTask> findByPlanIdOrderByDayIndexAsc(String planId);
    List<FollowUpTask> findByPatientIdAndCompletedFalseAndTargetDateLessThanEqualOrderByTargetDateAsc(String patientId, String today);
}