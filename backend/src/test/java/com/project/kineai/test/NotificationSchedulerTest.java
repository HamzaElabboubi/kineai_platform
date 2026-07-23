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
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
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

    // ✅ Horloge fixe — Mardi 21 juillet 2026, 10h00.
    // "Demain" = mercredi = TOUJOURS un jour d'entraînement,
    // peu importe le jour réel d'exécution du test.
    private static final Clock FIXED_CLOCK = Clock.fixed(
            LocalDate.of(2026, 7, 21)
                    .atTime(10, 0)
                    .atZone(ZoneId.systemDefault())
                    .toInstant(),
            ZoneId.systemDefault());

    private NotificationScheduler scheduler;

    private Patient patient;
    private Kinesitherapeute kine;
    private RehabPlan plan;

    @BeforeEach
    void setUp() {
        // ✅ Construction manuelle — le Clock n'est pas un
        // @Mock, c'est une valeur fixe réelle
        scheduler = new NotificationScheduler(
                planRepository, sessionRepository,
                notificationService, alertService, FIXED_CLOCK);

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
                .startDate(LocalDate.of(2026, 7, 21).minusDays(2))
                .build();
    }

    @Nested
    @DisplayName("sendSessionReminders()")
    class SendSessionRemindersTests {

        @Test
        @DisplayName("Demain est un jour d'entraînement et"
                + " séances restantes — envoie une notification")
        void sendSessionReminders_jourEntrainementEtSeancesRestantes_envoieNotification() {
            when(planRepository.findByStatus(Status.ACTIVE))
                    .thenReturn(List.of(plan));
            when(sessionRepository
                    .countByRehabPlanIdAndSessionStatusAndStartTimeBetween(
                            eq(plan.getId()),
                            eq(SessionStatus.COMPLETED),
                            any(), any()))
                    .thenReturn(0L);

            scheduler.sendSessionReminders();

            verify(notificationService, times(1))
                    .createNotification(eq(patient),
                            eq(NotificationType.SESSION_REMINDER),
                            any(String.class),
                            any(LocalDateTime.class));
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

        @Test
        @DisplayName("Demain n'est pas un jour d'entraînement"
                + " — n'appelle même pas le repository de séances")
        void sendSessionReminders_pasJourEntrainement_nAppellePasSessionRepository() {
            // Horloge fixée sur un JEUDI — demain = vendredi...
            // non attends, on veut tester le cas négatif : on fixe
            // sur MERCREDI, donc demain = jeudi = pas un jour
            // d'entraînement
            Clock jeudiClock = Clock.fixed(
                    LocalDate.of(2026, 7, 22) // mercredi
                            .atTime(10, 0)
                            .atZone(ZoneId.systemDefault())
                            .toInstant(),
                    ZoneId.systemDefault());

            NotificationScheduler schedulerJeudi =
                    new NotificationScheduler(
                            planRepository, sessionRepository,
                            notificationService, alertService,
                            jeudiClock);

            when(planRepository.findByStatus(Status.ACTIVE))
                    .thenReturn(List.of(plan));

            schedulerJeudi.sendSessionReminders();

            // ✅ Le repository de séances ne doit JAMAIS être
            // appelé — le code s'arrête avant, donc aucun stub
            // n'est nécessaire ici (pas de UnnecessaryStubbingException)
            verify(sessionRepository, never())
                    .countByRehabPlanIdAndSessionStatusAndStartTimeBetween(
                            any(), any(), any(), any());
            verify(notificationService, never())
                    .createNotification(any(), any(), any(), any());
        }
    }

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
                    .startTime(LocalDateTime.of(2026, 7, 16, 10, 0))
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
                    .startTime(LocalDateTime.of(2026, 7, 21, 4, 0))
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