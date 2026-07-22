package com.project.kineai.service;

import com.project.kineai.model.entity.Patient;
import com.project.kineai.model.entity.RehabPlan;
import com.project.kineai.model.enums.NotificationType;
import com.project.kineai.model.enums.SessionDay;
import com.project.kineai.model.enums.SessionStatus;
import com.project.kineai.model.enums.Status;
import com.project.kineai.repository.RehabPlanRepository;
import com.project.kineai.repository.SessionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Jobs planifiés — mono-instance (1 seul backend Oracle Cloud).
 * Si passage en multi-instances plus tard, ajouter un verrou
 * distribué (ShedLock) pour éviter les doublons d'exécution.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class NotificationScheduler {

    private final RehabPlanRepository planRepository;
    private final SessionRepository sessionRepository;
    private final NotificationService notificationService;
    private final AlertService alertService;

    private static final SessionDay[] TRAINING_DAYS = {
            SessionDay.LUNDI,
            SessionDay.MERCREDI,
            SessionDay.VENDREDI
    };

    // ══════════════════════════════════════════
    // Rappel de séance — chaque jour à 18h
    // ══════════════════════════════════════════
    @Scheduled(cron = "0 0 18 * * *")
    public void sendSessionReminders() {
        log.info("Job sendSessionReminders — démarrage");

        // ⚠️ Nécessite d'ajouter à RehabPlanRepository :
        // List<RehabPlan> findByStatus(Status status);
        List<RehabPlan> activePlans =
                planRepository.findByStatus(Status.ACTIVE);

        LocalDate tomorrow = LocalDate.now().plusDays(1);
        SessionDay tomorrowDay = toSessionDay(tomorrow);

        if (!isTrainingDay(tomorrowDay)) {
            log.info("Demain n'est pas un jour d'entraînement"
                    + " — aucun rappel à envoyer");
            return;
        }

        int sentCount = 0;
        for (RehabPlan plan : activePlans) {
            Patient patient = plan.getPatient();

            int currentWeek = plan.getCurrentWeek();
            LocalDate weekStart = plan.getStartDate()
                    .plusDays((currentWeek - 1) * 7L);
            LocalDate weekEnd = weekStart.plusDays(7);

            long sessionsThisWeek = sessionRepository
                    .countByRehabPlanIdAndSessionStatusAndStartTimeBetween(
                            plan.getId(), SessionStatus.COMPLETED,
                            weekStart.atStartOfDay(),
                            weekEnd.atStartOfDay());

            if (sessionsThisWeek >= TRAINING_DAYS.length) {
                continue;
            }

            notificationService.createNotification(
                    patient,
                    NotificationType.SESSION_REMINDER,
                    "Vous avez une séance de rééducation prévue"
                            + " demain. Restez motivé(e) !",
                    LocalDate.now().atStartOfDay());
            sentCount++;
        }

        log.info("Job sendSessionReminders — terminé,"
                + " {} rappel(s) envoyé(s)", sentCount);
    }

    // ══════════════════════════════════════════
    // Détection inactivité — chaque jour à 8h
    // ══════════════════════════════════════════
    @Scheduled(cron = "0 0 8 * * *")
    public void checkMissedSessions() {
        log.info("Job checkMissedSessions — démarrage");

        List<RehabPlan> activePlans =
                planRepository.findByStatus(Status.ACTIVE);

        LocalDateTime threshold = LocalDateTime.now().minusDays(3);

        int alertCount = 0;
        for (RehabPlan plan : activePlans) {
            Patient patient = plan.getPatient();

            Optional<com.project.kineai.model.entity.Session>
                    lastCompleted = sessionRepository
                    .findFirstByPatientIdAndSessionStatusOrderByStartTimeDesc(
                            patient.getId(),
                            SessionStatus.COMPLETED);

            boolean inactive = lastCompleted
                    .map(s -> s.getStartTime().isBefore(threshold))
                    .orElse(true);

            if (!inactive) continue;

            alertService.createAlert(
                    patient, patient.getKine(),
                    com.project.kineai.model.enums.AlertType.INACTIVITY,
                    "Le patient " + patient.getFullName()
                            + " n'a pas fait de séance depuis"
                            + " plus de 3 jours.");

            notificationService.createNotification(
                    patient,
                    NotificationType.STREAK_AT_RISK,
                    "Ça fait quelques jours que vous n'avez"
                            + " pas fait de séance. Reprenez"
                            + " dès aujourd'hui pour ne pas"
                            + " perdre votre progression !",
                    LocalDate.now().atStartOfDay());

            alertCount++;
        }

        log.info("Job checkMissedSessions — terminé,"
                        + " {} patient(s) inactif(s) détecté(s)",
                alertCount);
    }

    // ── Helpers ────────────────────────────────
    private boolean isTrainingDay(SessionDay day) {
        for (SessionDay trainingDay : TRAINING_DAYS) {
            if (trainingDay == day) return true;
        }
        return false;
    }

    private SessionDay toSessionDay(LocalDate date) {
        return switch (date.getDayOfWeek()) {
            case MONDAY -> SessionDay.LUNDI;
            case TUESDAY -> SessionDay.MARDI;
            case WEDNESDAY -> SessionDay.MERCREDI;
            case THURSDAY -> SessionDay.JEUDI;
            case FRIDAY -> SessionDay.VENDREDI;
            case SATURDAY -> SessionDay.SAMEDI;
            case SUNDAY -> SessionDay.DIMANCHE;
        };
    }
}