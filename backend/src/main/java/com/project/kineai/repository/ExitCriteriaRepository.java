package com.project.kineai.repository;

import com.project.kineai.model.entity.ExitCriteria;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ExitCriteriaRepository
        extends JpaRepository<ExitCriteria, UUID> {

    List<ExitCriteria> findByPhaseDefinitionId(
            UUID phaseDefinitionId);
}