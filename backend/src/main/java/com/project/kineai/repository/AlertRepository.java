package com.project.kineai.repository;

import com.project.kineai.model.entity.Alert;
import com.project.kineai.model.enums.AlertType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface AlertRepository
        extends JpaRepository<Alert, UUID> {

    boolean existsByPatientIdAndTypeAndResolvedFalse(
            UUID patientId, AlertType type);

    List<Alert> findByKinesitherapeuteIdAndResolvedFalse(
            UUID kineId);

    long countByKinesitherapeuteIdAndResolvedFalse(
            UUID kineId);

    // ✅ Nouveau — toutes les alertes du patient
    // (résolues ET non résolues, triées récent → ancien)
    List<Alert> findByPatientIdOrderBySentAtDesc(
            UUID patientId);

    // ✅ Nouveau — toutes les alertes du kiné connecté
// (résolues ET non résolues, triées récent → ancien)
    List<Alert> findByKinesitherapeuteIdOrderBySentAtDesc(
            UUID kineId);
}