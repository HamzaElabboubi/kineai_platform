package com.project.kineai.repository;

import com.project.kineai.model.entity.CpiWeightConfig;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface CpiWeightConfigRepository
        extends JpaRepository<CpiWeightConfig, UUID> {

    Optional<CpiWeightConfig> findByProtocolId(UUID protocolId);

    // Config globale par défaut (protocol = null)
    Optional<CpiWeightConfig> findFirstByProtocolIdIsNull();
}