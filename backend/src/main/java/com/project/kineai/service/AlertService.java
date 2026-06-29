package com.project.kineai.service;

import com.project.kineai.dto.response.AlertResponse;
import com.project.kineai.exception.BusinessException;
import com.project.kineai.mapper.AlertMapper;
import com.project.kineai.model.entity.*;
import com.project.kineai.model.enums.AlertType;
import com.project.kineai.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class AlertService {

    private final AlertRepository alertRepository;
    private final AlertMapper alertMapper;
    private final PatientService patientService;
    private final KineService kineService;

    // ── Créer alerte ──────────────────────────
    @Transactional
    public void createAlert(Patient patient,
                            Kinesitherapeute kine,
                            AlertType type,
                            String message) {
        // Éviter les doublons — RG-26
        boolean exists = alertRepository
                .existsByPatientIdAndTypeAndResolvedFalse(patient.getId(), type);
        if (exists) return;

        Alert alert = Alert.builder()
                .patient(patient)
                .kinesitherapeute(kine)
                .type(type)
                .message(message)
                .resolved(false)
                .build();

        alertRepository.save(alert);
        log.info("Alerte {} créée pour patient {}", type, patient.getId());
    }

    // ── Alertes du kiné connecté ──────────────
    @Transactional(readOnly = true)
    public List<AlertResponse> getMyAlerts() {
        Kinesitherapeute kine = kineService.getCurrentKine();
        return alertMapper.toResponseList(
                alertRepository.findByKinesitherapeuteIdAndResolvedFalse(kine.getId()));
    }

    // ── Résoudre alerte ───────────────────────
    @Transactional
    public AlertResponse resolveAlert(UUID alertId) {
        Alert alert = alertRepository.findById(alertId)
                .orElseThrow(() -> new BusinessException("Alerte introuvable"));
        alert.setResolved(true);
        return alertMapper.toResponse(alertRepository.save(alert));
    }

    // ── Compter alertes en attente ────────────
    @Transactional(readOnly = true)
    public long countPendingAlerts() {
        Kinesitherapeute kine = kineService
                .getCurrentKine();
        return alertRepository
                .countByKinesitherapeuteIdAndResolvedFalse(kine.getId());
    }

    // ── Mes alertes (patient connecté) ────────
    public List<AlertResponse> getMyAlertsAsPatient() {
        Patient patient = patientService.getCurrentPatient();
        return alertMapper.toResponseList(
                alertRepository
                        .findByPatientIdOrderBySentAtDesc(
                                patient.getId()));
    }

    // ── Toutes mes alertes (résolues + non résolues) ──
    @Transactional(readOnly = true)
    public List<AlertResponse> getAllMyAlerts() {
        Kinesitherapeute kine = kineService.getCurrentKine();
        return alertMapper.toResponseList(
                alertRepository
                        .findByKinesitherapeuteIdOrderBySentAtDesc(
                                kine.getId()));
    }
}
