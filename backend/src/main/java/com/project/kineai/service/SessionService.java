package com.project.kineai.service;

import com.project.kineai.dto.request.*;
import com.project.kineai.dto.response.SessionResponse;
import com.project.kineai.exception.BusinessException;
import com.project.kineai.mapper.*;
import com.project.kineai.model.entity.*;
import com.project.kineai.model.enums.SessionStatus;
import com.project.kineai.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class SessionService {

    private final SessionRepository sessionRepository;
    private final SessionMetricsRepository metricsRepository;
    private final ExerciseRepository exerciseRepository;
    private final RehabPlanRepository planRepository;
    private final SessionMapper sessionMapper;
    private final SessionMetricsMapper metricsMapper;
    private final PatientService patientService;
    private final com.project.kineai.service.BadgeService badgeService;
    private final RehabPlanService rehabPlanService;
    private final PatientRepository patientRepository;

    // ── Démarrer séance ───────────────────────
    @Transactional
    public SessionResponse startSession(CreateSessionRequest request) {
        Patient patient = patientService.getCurrentPatient();

        Exercise exercise = exerciseRepository.findById(request.getExerciseId())
                .orElseThrow(() -> new BusinessException("Exercice introuvable"));

        RehabPlan plan = request.getPlanId() != null
                ? planRepository.findById(request.getPlanId()).orElse(null)
                : null;

        Session session = Session.builder()
                .patient(patient)
                .exercise(exercise)
                .rehabPlan(plan)
                .startTime(LocalDateTime.now())
                .sessionStatus(SessionStatus.IN_PROGRESS)
                .build();

        return sessionMapper.toResponse(sessionRepository.save(session));
    }

    // ── Sauvegarder métriques (toutes les 5s) ─
    @Transactional
    public void saveMetrics(UUID sessionId, SaveMetricsRequest request) {
        Session session = getSessionOrThrow(sessionId);

        SessionMetrics metrics = metricsMapper.toEntity(request);
        metrics.setSession(session);

        metricsRepository.save(metrics);
    }

    // ── Terminer séance ───────────────────────
    // SessionService.java — completeSession() corrigée

    @Transactional
    public SessionResponse completeSession(UUID sessionId,
                                           CompleteSessionRequest request) {
        Session session = getSessionOrThrow(sessionId);
        Patient patient = session.getPatient();

        session.setSessionStatus(SessionStatus.COMPLETED);
        session.setEndTime(LocalDateTime.now());
        session.setScore(request.getFinalScore());
        session.setRepsCompleted(request.getRepsCompleted());
        session.setJointAngles(request.getJointAngles());

        int xp = calculateXp(session);
        session.setXpEarned(xp);

        patient.setTotalXp(patient.getTotalXp() + xp);

        // ✅ RG-31 — vérifier si une séance a déjà été
        // complétée AUJOURD'HUI avant d'incrémenter le streak
        LocalDateTime todayStart = LocalDate.now().atStartOfDay();
        List<Session> todaySessions = sessionRepository
                .findByPatientIdAndStartTimeAfterAndSessionStatus(
                        patient.getId(), todayStart,
                        SessionStatus.COMPLETED);

        // todaySessions contient déjà la séance qu'on vient
        // de sauvegarder plus bas — donc on vérifie AVANT
        // le save() si une AUTRE séance complétée existe déjà
        boolean alreadyCompletedToday = !todaySessions.isEmpty();

        if (!alreadyCompletedToday) {
            patient.setStreakCount(patient.getStreakCount() + 1);
        }

        session = sessionRepository.save(session);

        badgeService.checkAndUnlockBadges(patient, session);

        rehabPlanService.checkProgression(patient.getId());

        if (session.getRehabPlan() != null) {
            rehabPlanService.checkWeekAdvancement(
                    session.getRehabPlan().getId());
        }

        log.info("Séance {} terminée — score: {}", sessionId, request.getFinalScore());
        return sessionMapper.toResponse(session);
    }

    // ── Interrompre séance ────────────────────
    // ── Interrompre séance ────────────────────
    @Transactional
    public SessionResponse interruptSession(UUID sessionId) {
        Session session = getSessionOrThrow(sessionId);
        session.setSessionStatus(SessionStatus.INTERRUPTED);
        session.setEndTime(LocalDateTime.now());

        // ✅ Casser le streak — l'abandon en cours de
        // séance rompt la série de réussites consécutives
        Patient patient = session.getPatient();
        patient.setStreakCount(0);
        patientRepository.save(patient);

        return sessionMapper.toResponse(sessionRepository.save(session));
    }

    // ── Historique ────────────────────────────
    @Transactional(readOnly = true)
    public List<SessionResponse> getMySessionHistory() {
        Patient patient = patientService.getCurrentPatient();
        return sessionMapper.toResponseList(
                sessionRepository.findByPatientIdOrderByStartTimeDesc(patient.getId()));
    }

    public List<SessionResponse> getSessionsByPatientId(UUID patientId) {
        return sessionMapper.toResponseList(
                sessionRepository.findByPatientIdOrderByStartTimeDesc(patientId));
    }

    // ── XP — RG-29 ───────────────────────────
    private int calculateXp(Session session) {
        int xp = 10; // base
        if (session.getScore() != null
                && session.getScore().doubleValue() > 80.0) {
            xp += 20; // bonus score > 80%
        }
        return xp;
    }

    private Session getSessionOrThrow(UUID sessionId) {
        return sessionRepository.findById(sessionId)
                .orElseThrow(() -> new BusinessException("Séance introuvable"));
    }


}