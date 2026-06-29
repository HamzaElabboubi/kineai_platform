package com.project.kineai.test;

import com.project.kineai.dto.request.CompleteSessionRequest;
import com.project.kineai.dto.request.CreateSessionRequest;
import com.project.kineai.dto.response.SessionResponse;
import com.project.kineai.exception.BusinessException;
import com.project.kineai.mapper.SessionMapper;
import com.project.kineai.mapper.SessionMetricsMapper;
import com.project.kineai.model.entity.*;
import com.project.kineai.model.enums.SessionStatus;
import com.project.kineai.repository.*;
import com.project.kineai.service.BadgeService;
import com.project.kineai.service.PatientService;
import com.project.kineai.service.RehabPlanService;
import com.project.kineai.service.SessionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("SessionService — Tests unitaires")
class SessionServiceTest {

    @Mock
    private SessionRepository sessionRepository;
    @Mock
    private SessionMetricsRepository metricsRepository;
    @Mock
    private ExerciseRepository exerciseRepository;
    @Mock
    private RehabPlanRepository planRepository;
    @Mock
    private SessionMapper sessionMapper;
    @Mock
    private SessionMetricsMapper metricsMapper;
    @Mock
    private PatientService patientService;
    @Mock
    private BadgeService badgeService;
    @Mock
    private RehabPlanService rehabPlanService;
    @Mock
    private PatientRepository patientRepository;

    @InjectMocks
    private SessionService sessionService;

    private Patient patient;
    private Exercise exercise;
    private Session session;
    private UUID sessionId;

    @BeforeEach
    void setUp() {
        patient = Patient.builder()
                .id(UUID.randomUUID())
                .fullName("Jean Dupont")
                .totalXp(0)
                .streakCount(0)
                .build();

        exercise = Exercise.builder()
                .id(UUID.randomUUID())
                .name("Flexion genou — initiation")
                .build();

        sessionId = UUID.randomUUID();

        session = Session.builder()
                .id(sessionId)
                .patient(patient)
                .exercise(exercise)
                .sessionStatus(SessionStatus.IN_PROGRESS)
                .startTime(LocalDateTime.now())
                .repsCompleted(0)
                .xpEarned(0)
                .build();
    }

    // ══════════════════════════════════════════
    // START SESSION
    // ══════════════════════════════════════════
    @Nested
    @DisplayName("startSession()")
    class StartSessionTests {

        @Test
        @DisplayName("Démarrage réussi — crée une session"
                + " IN_PROGRESS")
        void startSession_succes_creeSessionInProgress() {
            CreateSessionRequest request =
                    CreateSessionRequest.builder()
                            .exerciseId(exercise.getId())
                            .build();

            when(patientService.getCurrentPatient())
                    .thenReturn(patient);
            when(exerciseRepository.findById(exercise.getId()))
                    .thenReturn(Optional.of(exercise));
            when(sessionRepository.save(any(Session.class)))
                    .thenAnswer(inv -> inv.getArgument(0));
            when(sessionMapper.toResponse(any(Session.class)))
                    .thenReturn(new SessionResponse());

            SessionResponse response =
                    sessionService.startSession(request);

            assertThat(response).isNotNull();

            verify(sessionRepository).save(argThat(s ->
                    s.getSessionStatus() == SessionStatus.IN_PROGRESS
                            && s.getPatient().equals(patient)
                            && s.getExercise().equals(exercise)));
        }

        @Test
        @DisplayName("Exercice introuvable — lève"
                + " BusinessException")
        void startSession_exerciceIntrouvable_leveBusinessException() {
            UUID exerciseId = UUID.randomUUID();
            CreateSessionRequest request =
                    CreateSessionRequest.builder()
                            .exerciseId(exerciseId)
                            .build();

            when(patientService.getCurrentPatient())
                    .thenReturn(patient);
            when(exerciseRepository.findById(exerciseId))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(
                    () -> sessionService.startSession(request))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("Exercice introuvable");

            verify(sessionRepository, never()).save(any());
        }
    }

    // ══════════════════════════════════════════
    // COMPLETE SESSION
    // ══════════════════════════════════════════
    @Nested
    @DisplayName("completeSession()")
    class CompleteSessionTests {

        private CompleteSessionRequest buildRequest(
                BigDecimal score) {
            return CompleteSessionRequest.builder()
                    .finalScore(score)
                    .repsCompleted(10)
                    .jointAngles("{\"main_angle\":90}")
                    .build();
        }

        @Test
        @DisplayName("Score > 80% — XP avec bonus"
                + " (10 + 20 = 30) — RG-29")
        void completeSession_scoreSuperieur80_xpAvecBonus() {
            CompleteSessionRequest request =
                    buildRequest(new BigDecimal("90.0"));

            when(sessionRepository.findById(sessionId))
                    .thenReturn(Optional.of(session));
            when(sessionRepository
                    .findByPatientIdAndStartTimeAfterAndSessionStatus(
                            any(), any(), any()))
                    .thenReturn(Collections.emptyList());
            when(sessionRepository.save(any(Session.class)))
                    .thenAnswer(inv -> inv.getArgument(0));
            when(sessionMapper.toResponse(any(Session.class)))
                    .thenReturn(new SessionResponse());

            sessionService.completeSession(sessionId, request);

            assertThat(session.getXpEarned()).isEqualTo(30);
            assertThat(patient.getTotalXp()).isEqualTo(30);
        }

        @Test
        @DisplayName("Score <= 80% — XP sans bonus"
                + " (10 uniquement) — RG-29")
        void completeSession_scoreInferieurOuEgal80_xpSansBonus() {
            CompleteSessionRequest request =
                    buildRequest(new BigDecimal("75.0"));

            when(sessionRepository.findById(sessionId))
                    .thenReturn(Optional.of(session));
            when(sessionRepository
                    .findByPatientIdAndStartTimeAfterAndSessionStatus(
                            any(), any(), any()))
                    .thenReturn(Collections.emptyList());
            when(sessionRepository.save(any(Session.class)))
                    .thenAnswer(inv -> inv.getArgument(0));
            when(sessionMapper.toResponse(any(Session.class)))
                    .thenReturn(new SessionResponse());

            sessionService.completeSession(sessionId, request);

            assertThat(session.getXpEarned()).isEqualTo(10);
        }

        @Test
        @DisplayName("Première séance du jour — incrémente"
                + " le streak — RG-31")
        void completeSession_premiereSeanceDuJour_incrementeStreak() {
            CompleteSessionRequest request =
                    buildRequest(new BigDecimal("85.0"));
            patient.setStreakCount(2);

            when(sessionRepository.findById(sessionId))
                    .thenReturn(Optional.of(session));
            when(sessionRepository
                    .findByPatientIdAndStartTimeAfterAndSessionStatus(
                            any(), any(), any()))
                    .thenReturn(Collections.emptyList());
            when(sessionRepository.save(any(Session.class)))
                    .thenAnswer(inv -> inv.getArgument(0));
            when(sessionMapper.toResponse(any(Session.class)))
                    .thenReturn(new SessionResponse());

            sessionService.completeSession(sessionId, request);

            assertThat(patient.getStreakCount()).isEqualTo(3);
        }

        @Test
        @DisplayName("Deuxième séance le même jour — le"
                + " streak ne change pas — RG-31")
        void completeSession_deuxiemeSeanceMemeJour_streakInchange() {
            CompleteSessionRequest request =
                    buildRequest(new BigDecimal("85.0"));
            patient.setStreakCount(2);

            Session sessionDejaCompleteeAujourdhui =
                    Session.builder()
                            .id(UUID.randomUUID())
                            .sessionStatus(SessionStatus.COMPLETED)
                            .build();

            when(sessionRepository.findById(sessionId))
                    .thenReturn(Optional.of(session));
            when(sessionRepository
                    .findByPatientIdAndStartTimeAfterAndSessionStatus(
                            any(), any(), any()))
                    .thenReturn(List.of(
                            sessionDejaCompleteeAujourdhui));
            when(sessionRepository.save(any(Session.class)))
                    .thenAnswer(inv -> inv.getArgument(0));
            when(sessionMapper.toResponse(any(Session.class)))
                    .thenReturn(new SessionResponse());

            sessionService.completeSession(sessionId, request);

            assertThat(patient.getStreakCount()).isEqualTo(2);
        }

        @Test
        @DisplayName("Séance liée à un plan — déclenche"
                + " checkWeekAdvancement()")
        void completeSession_avecRehabPlan_appelleCheckWeekAdvancement() {
            CompleteSessionRequest request =
                    buildRequest(new BigDecimal("85.0"));

            UUID planId = UUID.randomUUID();
            RehabPlan plan = RehabPlan.builder()
                    .id(planId)
                    .build();
            session.setRehabPlan(plan);

            when(sessionRepository.findById(sessionId))
                    .thenReturn(Optional.of(session));
            when(sessionRepository
                    .findByPatientIdAndStartTimeAfterAndSessionStatus(
                            any(), any(), any()))
                    .thenReturn(Collections.emptyList());
            when(sessionRepository.save(any(Session.class)))
                    .thenAnswer(inv -> inv.getArgument(0));
            when(sessionMapper.toResponse(any(Session.class)))
                    .thenReturn(new SessionResponse());

            sessionService.completeSession(sessionId, request);

            verify(rehabPlanService).checkWeekAdvancement(planId);
        }

        @Test
        @DisplayName("Séance sans plan — n'appelle jamais"
                + " checkWeekAdvancement()")
        void completeSession_sansRehabPlan_nAppellePasCheckWeekAdvancement() {
            CompleteSessionRequest request =
                    buildRequest(new BigDecimal("85.0"));
            // session.getRehabPlan() reste null (cf setUp())

            when(sessionRepository.findById(sessionId))
                    .thenReturn(Optional.of(session));
            when(sessionRepository
                    .findByPatientIdAndStartTimeAfterAndSessionStatus(
                            any(), any(), any()))
                    .thenReturn(Collections.emptyList());
            when(sessionRepository.save(any(Session.class)))
                    .thenAnswer(inv -> inv.getArgument(0));
            when(sessionMapper.toResponse(any(Session.class)))
                    .thenReturn(new SessionResponse());

            sessionService.completeSession(sessionId, request);

            verify(rehabPlanService, never())
                    .checkWeekAdvancement(any());
        }
    }

    // ══════════════════════════════════════════
    // INTERRUPT SESSION
    // ══════════════════════════════════════════
    @Nested
    @DisplayName("interruptSession()")
    class InterruptSessionTests {

        @Test
        @DisplayName("Interruption réussie — réinitialise"
                + " le streak à 0 — RG-32")
        void interruptSession_succes_reinitialiseStreakAZero() {
            patient.setStreakCount(5);

            when(sessionRepository.findById(sessionId))
                    .thenReturn(Optional.of(session));
            when(sessionRepository.save(any(Session.class)))
                    .thenAnswer(inv -> inv.getArgument(0));
            when(sessionMapper.toResponse(any(Session.class)))
                    .thenReturn(new SessionResponse());

            sessionService.interruptSession(sessionId);

            assertThat(patient.getStreakCount()).isEqualTo(0);
            assertThat(session.getSessionStatus())
                    .isEqualTo(SessionStatus.INTERRUPTED);
            verify(patientRepository).save(patient);
        }

        @Test
        @DisplayName("Séance introuvable — lève"
                + " BusinessException")
        void interruptSession_seanceIntrouvable_leveBusinessException() {
            UUID inconnuId = UUID.randomUUID();
            when(sessionRepository.findById(inconnuId))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(
                    () -> sessionService
                            .interruptSession(inconnuId))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("Séance introuvable");
        }
    }
}