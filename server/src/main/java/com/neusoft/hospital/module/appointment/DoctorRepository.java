package com.neusoft.hospital.module.appointment;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DoctorRepository extends JpaRepository<Doctor, String> {
    List<Doctor> findByDepartmentId(String departmentId);
}

