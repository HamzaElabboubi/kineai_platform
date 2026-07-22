package com.project.kineai.test;

import com.project.kineai.model.entity.Kinesitherapeute;
import com.project.kineai.model.entity.Patient;
import com.project.kineai.model.entity.RehabPlan;
import com.project.kineai.model.entity.Session;
import com.project.kineai.model.enums.AlertType;
import com.project.kineai.model.enums.NotificationType;
import com.project.kineai.model.enums.SessionStatus;
import com.project.kineai.model.enums.Status;
import com.project.kineai.repository.RehabPlanRepository;
import com.project.kineai.repository.SessionRepository;
import com.project.kineai.service.AlertService;
import com.project.kineai.service.NotificationScheduler;
import com.project.kineai.service.NotificationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("NotificationScheduler — Tests unitaires")
class NotificationSchedulerTest {

    @Mock
    private RehabPlanRepository planRepository;
    @Mock
    private SessionRepository sessionRepository;
    @Mock
    private NotificationService notificationService;
    @Mock
    private AlertService alertService;

    @InjectMocks
    private NotificationScheduler scheduler;

    private Patient patient;
    private Kinesitherapeute kine;
    private RehabPlan plan;

    @BeforeEach
    void setUp() {
        kine = Kinesitherapeute.builder()
                .id(UUID.randomUUID())
                .fullName("Dr. Martin")
                .build();

        patient = Patient.builder()
                .id(UUID.randomUUID())
                .fullName("Jean Dupont")
                .kine(kine)
                .build();

        plan = RehabPlan.builder()
                .id(UUID.randomUUID())
                .patient(patient)
                .status(Status.ACTIVE)
                .currentWeek(1)
                .startDate(LocalDate.now().minusDays(2))
                .build();
    }

    // ══════════════════════════════════════════
    // SEND SESSION REMINDERS
    // ══════════════════════════════════════════
    @Nested
    @DisplayName("sendSessionReminders()")
    class SendSessionRemindersTests {

        @Test
        @DisplayName("Séances restantes cette semaine —"
                + " notifie uniquement si demain est un"
                + " jour d'entraînement")
        void sendSessionReminders_seancesRestantes_notifieSiJourEntrainement() {
            when(planRepository.findByStatus(Status.ACTIVE))
                    .thenReturn(List.of(plan));
            when(sessionRepository
                    .countByRehabPlanIdAndSessionStatusAndStartTimeBetween(
                            eq(plan.getId()),
                            eq(SessionStatus.COMPLETED),
                            any(), any()))
                    .thenReturn(0L);

            scheduler.sendSessionReminders();

            DayOfWeek tomorrow = LocalDate.now()
                    .plusDays(1).getDayOfWeek();
            boolean isTrainingDay =
                    tomorrow == DayOfWeek.MONDAY
                            || tomorrow == DayOfWeek.WEDNESDAY
                            || tomorrow == DayOfWeek.FRIDAY;

            if (isTrainingDay) {
                verify(notificationService, times(1))
                        .createNotification(eq(patient),
                                eq(NotificationType.SESSION_REMINDER),
                                any(String.class),
                                any(LocalDateTime.class));
            } else {
                verify(notificationService, never())
                        .createNotification(any(), any(),
                                any(), any());
            }
        }

        @Test
        @DisplayName("Toutes les séances de la semaine déjà"
                + " complétées — n'envoie aucun rappel")
        void sendSessionReminders_seancesCompletes_nEnvoieRien() {
            when(planRepository.findByStatus(Status.ACTIVE))
                    .thenReturn(List.of(plan));
            when(sessionRepository
                    .countByRehabPlanIdAndSessionStatusAndStartTimeBetween(
                            eq(plan.getId()),
                            eq(SessionStatus.COMPLETED),
                            any(), any()))
                    .thenReturn(3L);

            scheduler.sendSessionReminders();

            verify(notificationService, never())
                    .createNotification(any(), any(), any(), any());
        }
    }

    // ══════════════════════════════════════════
    // CHECK MISSED SESSIONS
    // ══════════════════════════════════════════
    @Nested
    @DisplayName("checkMissedSessions()")
    class CheckMissedSessionsTests {

        @Test
        @DisplayName("Inactif depuis plus de 3 jours —"
                + " crée alerte kiné et notification patient")
        void checkMissedSessions_inactifPlusDe3Jours_creeAlerteEtNotification() {
            when(planRepository.findByStatus(Status.ACTIVE))
                    .thenReturn(List.of(plan));

            Session ancienneSeance = Session.builder()
                    .id(UUID.randomUUID())
                    .patient(patient)
                    .startTime(LocalDateTime.now().minusDays(5))
                    .sessionStatus(SessionStatus.COMPLETED)
                    .build();

            when(sessionRepository
                    .findFirstByPatientIdAndSessionStatusOrderByStartTimeDesc(
                            patient.getId(),
                            SessionStatus.COMPLETED))
                    .thenReturn(Optional.of(ancienneSeance));

            scheduler.checkMissedSessions();

            verify(alertService, times(1)).createAlert(
                    eq(patient), eq(kine),
                    eq(AlertType.INACTIVITY), any(String.class));

            verify(notificationService, times(1))
                    .createNotification(eq(patient),
                            eq(NotificationType.STREAK_AT_RISK),
                            any(String.class),
                            any(LocalDateTime.class));
        }

        @Test
        @DisplayName("Activité récente (< 3 jours) — ne"
                + " déclenche rien")
        void checkMissedSessions_activiteRecente_neDeclencheRien() {
            when(planRepository.findByStatus(Status.ACTIVE))
                    .thenReturn(List.of(plan));

            Session seanceRecente = Session.builder()
                    .id(UUID.randomUUID())
                    .patient(patient)
                    .startTime(LocalDateTime.now().minusHours(6))
                    .sessionStatus(SessionStatus.COMPLETED)
                    .build();

            when(sessionRepository
                    .findFirstByPatientIdAndSessionStatusOrderByStartTimeDesc(
                            patient.getId(),
                            SessionStatus.COMPLETED))
                    .thenReturn(Optional.of(seanceRecente));

            scheduler.checkMissedSessions();

            verify(alertService, never())
                    .createAlert(any(), any(), any(), any());
            verify(notificationService, never())
                    .createNotification(any(), any(), any(), any());
        }

        @Test
        @DisplayName("Patient n'a jamais complété de séance"
                + " — considéré comme inactif")
        void checkMissedSessions_jamaisDeSeanceCompletee_considereInactif() {
            when(planRepository.findByStatus(Status.ACTIVE))
                    .thenReturn(List.of(plan));

            when(sessionRepository
                    .findFirstByPatientIdAndSessionStatusOrderByStartTimeDesc(
                            patient.getId(),
                            SessionStatus.COMPLETED))
                    .thenReturn(Optional.empty());

            scheduler.checkMissedSessions();

            verify(alertService, times(1))
                    .createAlert(eq(patient), eq(kine),
                            eq(AlertType.INACTIVITY), any());
        }
    }
}