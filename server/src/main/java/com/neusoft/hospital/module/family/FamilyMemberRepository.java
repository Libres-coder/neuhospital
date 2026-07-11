package com.neusoft.hospital.module.family;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface FamilyMemberRepository extends JpaRepository<FamilyMember, String> {

    List<FamilyMember> findByOwnerIdOrderByIsDefaultDescNameAsc(String ownerId);

    long countByOwnerId(String ownerId);

    @Modifying
    @Query("UPDATE FamilyMember f SET f.isDefault = false, f.updateTime = :now WHERE f.ownerId = :ownerId")
    int clearDefault(@Param("ownerId") String ownerId, @Param("now") long now);

    @Modifying
    @Query("UPDATE FamilyMember f SET f.isDefault = true, f.updateTime = :now WHERE f.id = :id")
    int markDefault(@Param("id") String id, @Param("now") long now);
}
