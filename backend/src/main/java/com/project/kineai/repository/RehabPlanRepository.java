package com.project.kineai.repository;

import com.project.kineai.model.entity.RehabPlan;
import com.project.kineai.model.enums.Status;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RehabPlanRepository extends JpaRepository<RehabPlan, UUID> {
    Optional<RehabPlan> findByPatientIdAndStatus(UUID patientId, Status status);
    List<RehabPlan> findByPatientIdOrderByStartDateDesc(UUID patientId);
    boolean existsByPatientIdAndStatus(
            UUID patientId, Status status);
}
