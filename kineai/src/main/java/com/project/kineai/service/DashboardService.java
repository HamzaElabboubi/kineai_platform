package com.project.kineai.service;

import com.project.kineai.dto.response.*;
import com.project.kineai.exception.BusinessException;
import com.project.kineai.mapper.*;
import com.project.kineai.model.entity.*;
import com.project.kineai.model.enums.SessionStatus;
import com.project.kineai.model.enums.Status;
import com.project.kineai.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class DashboardService {

    private final PatientService patientService;
    private final KineService kineService;
    private final AlertService alertService;
    private final SessionRepository sessionRepository;
    private final RehabPlanRepository planRepository;
    private final BadgeRepository badgeRepository;
    private final PatientRepository patientRepository;
    private final PatientMapper patientMapper;
    private final SessionMapper sessionMapper;
    private final RehabPlanMapper planMapper;
    private final BadgeMapper badgeMapper;

    // ══════════════════════════════════════════
    // Dashboard Kiné
    // ══════════════════════════════════════════
    @Transactional(readOnly = true)
    public DashboardKineResponse getKineDashboard() {
        Kinesitherapeute kine = kineService.getCurrentKine();

        // Liste patients actifs du kiné
        List<Patient> patients = kine.getPatients()
                .stream()
                .filter(p -> p.getUser().getActive())
                .toList();

        // Alertes non résolues
        List<AlertResponse> alerts = alertService.getMyAlerts();
        long pendingCount = alertService.countPendingAlerts();

        log.info("Dashboard kiné chargé — {} patients, {} alertes",
                patients.size(), pendingCount);

        return DashboardKineResponse.builder()
                .totalPatients(patients.size())
                .pendingAlerts(pendingCount)
                .patients(patientMapper.toResponseList(patients))
                .recentAlerts(alerts)
                .build();
    }

    // ══════════════════════════════════════════
    // Dashboard Patient connecté
    // ══════════════════════════════════════════
    @Transactional(readOnly = true)
    public DashboardPatientResponse getPatientDashboard() {
        Patient patient = patientService.getCurrentPatient();
        return buildPatientDashboard(patient);
    }

    // ══════════════════════════════════════════
    // Dashboard d'un patient spécifique (kiné)
    // ══════════════════════════════════════════
    public DashboardPatientResponse getPatientDashboardById(
            UUID patientId) {
        Patient patient = patientRepository.findById(patientId)
                .orElseThrow(() -> new BusinessException(
                        "Patient introuvable"));
        return buildPatientDashboard(patient);
    }

    // ══════════════════════════════════════════
    // Construction du dashboard patient
    // ══════════════════════════════════════════
    private DashboardPatientResponse buildPatientDashboard(
            Patient patient) {

        // ── Plan actif ────────────────────────
        RehabPlanResponse activePlan = planRepository
                .findByPatientIdAndStatus(patient.getId(), Status.ACTIVE)
                .map(planMapper::toResponse)
                .orElse(null); // null si pas de plan actif

        // ── Séances récentes (5 dernières) ────
        List<SessionResponse> recentSessions = sessionRepository
                .findByPatientIdOrderByStartTimeDesc(patient.getId())
                .stream()
                .limit(5)
                .map(sessionMapper::toResponse)
                .toList();

        // ── Total séances complétées ──────────
        long totalSessions = sessionRepository
                .findByPatientIdOrderByStartTimeDesc(patient.getId())
                .stream()
                .filter(s -> s.getSessionStatus() == SessionStatus.COMPLETED)
                .count();

        // ── Score moyen 3 dernières séances ───
        Double avgScoreRaw = sessionRepository
                .getAverageScoreLastThreeSessions(patient.getId());
        BigDecimal averageScore = avgScoreRaw != null
                ? BigDecimal.valueOf(avgScoreRaw)
                .setScale(2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        // ── Badges débloqués ──────────────────
        List<BadgeResponse> badges = badgeMapper.toResponseList(
                badgeRepository.findByPatientIdOrderByUnlockedAtDesc(
                        patient.getId()));

        // ── Progression vers guérison (0-100%) ─
        // Basé sur 28 séances cibles (4 semaines × 1 séance/jour)
        int progressionPct = calculateProgressionPct(
                (int) totalSessions);

        log.info("Dashboard patient {} chargé — {} séances, {}% progression",
                patient.getFullName(), totalSessions, progressionPct);

        return DashboardPatientResponse.builder()
                .profile(patientMapper.toResponse(patient))
                .activePlan(activePlan)
                .totalSessions((int) totalSessions)
                .averageScore(averageScore)
                .streakCount(patient.getStreakCount())
                .totalXp(patient.getTotalXp())
                .badges(badges)
                .recentSessions(recentSessions)
                .progressionPct(progressionPct)
                .build();
    }

    // ── Calcul progression 0-100% ─────────────
    // 28 séances = programme complet (4 semaines)
    private int calculateProgressionPct(int totalSessions) {
        int target = 28;
        if (totalSessions >= target) return 100;
        return (totalSessions * 100) / target;
    }
}