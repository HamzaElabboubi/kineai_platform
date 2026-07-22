package com.project.kineai.repository;

import com.project.kineai.model.entity.RehabProtocol;
import com.project.kineai.model.enums.Pathology;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface RehabProtocolRepository
        extends JpaRepository<RehabProtocol, UUID> {

    // ⚠️ RG à faire respecter en service : un seul protocole
    // ACTIF par pathologie à la fois (non garanti par la BDD)
    Optional<RehabProtocol> findByPathologyAndActiveTrue(
            Pathology pathology);
}