package com.project.kineai.repository;

import com.project.kineai.model.entity.PhaseProgressHistory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface PhaseProgressHistoryRepository
        extends JpaRepository<PhaseProgressHistory, UUID> {

    List<PhaseProgressHistory> findByPatientIdOrderByExitedAtDesc(
            UUID patientId);
}