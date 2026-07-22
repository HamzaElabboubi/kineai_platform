package com.project.kineai.repository;

import com.project.kineai.model.entity.PatientPhaseProgress;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface PatientPhaseProgressRepository
        extends JpaRepository<PatientPhaseProgress, UUID> {

    // ⚠️ Suppose UN SEUL parcours de protocole actif par
    // patient à la fois — RG à valider/faire respecter en
    // service (voir section "règles non protégées" ci-dessous)
    Optional<PatientPhaseProgress> findByPatientId(UUID patientId);
}