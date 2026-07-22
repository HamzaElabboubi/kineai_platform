package com.project.kineai.repository;

import com.project.kineai.model.entity.PhaseDefinition;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PhaseDefinitionRepository
        extends JpaRepository<PhaseDefinition, UUID> {

    List<PhaseDefinition> findByProtocolIdOrderByOrderIndexAsc(
            UUID protocolId);

    // Utile pour trouver la phase suivante après validation
    Optional<PhaseDefinition> findByProtocolIdAndOrderIndex(
            UUID protocolId, Integer orderIndex);
}