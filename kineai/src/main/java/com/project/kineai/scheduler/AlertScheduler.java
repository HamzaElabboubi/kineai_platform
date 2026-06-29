package com.project.kineai.scheduler;

import com.project.kineai.model.entity.Patient;
import com.project.kineai.model.entity.Session;
import com.project.kineai.model.enums.AlertType;
import com.project.kineai.model.enums.SessionStatus;
import com.project.kineai.repository.PatientRepository;
import com.project.kineai.repository.SessionRepository;
import com.project.kineai.service.AlertService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
@Slf4j
public class AlertScheduler {

    private final PatientRepository patientRepository;
    private final SessionRepository sessionRepository;
    private final AlertService alertService;

    // ── RG-23 — Alerte inactivité > 3 jours ───
    @Scheduled(cron = "0 0 * * * *")
    @Transactional
    public void checkInactivity() {
        List<Patient> patients = patientRepository.findAll();
        LocalDateTime threshold = LocalDateTime.now().minusDays(3);
        int alertsCreated = 0;

        for (Patient patient : patients) {
            Optional<Session> lastSession = sessionRepository
                    .findFirstByPatientIdAndSessionStatusOrderByStartTimeDesc(
                            patient.getId(), SessionStatus.COMPLETED);

            boolean isInactive = lastSession.isEmpty()
                    || lastSession.get().getStartTime()
                    .isBefore(threshold);

            if (isInactive && patient.getKine() != null) {
                alertService.createAlert(
                        patient,
                        patient.getKine(),
                        AlertType.INACTIVITY,
                        "Aucune séance effectuée depuis plus"
                                + " de 3 jours par "
                                + patient.getFullName());
                alertsCreated++;
            }
        }

        log.info("checkInactivity() — {} patient(s) vérifié(s),"
                        + " {} alerte(s) potentielle(s) traitée(s)",
                patients.size(), alertsCreated);
    }

    // ── RG-24 — Alerte score moyen < 60% ──────
    @Scheduled(cron = "0 30 * * * *")
    @Transactional
    public void checkLowScores() {
        List<Patient> patients = patientRepository.findAll();
        int alertsCreated = 0;

        for (Patient patient : patients) {
            long completed = sessionRepository
                    .countCompletedSessions(patient.getId());

            if (completed < 3) continue;

            Double avgScore = sessionRepository
                    .getAverageScoreLastThreeSessions(
                            patient.getId());

            if (avgScore != null && avgScore < 60.0
                    && patient.getKine() != null) {
                alertService.createAlert(
                        patient,
                        patient.getKine(),
                        AlertType.SCORE,
                        "Score moyen de " + patient.getFullName()
                                + " inférieur à 60% sur ses 3"
                                + " dernières séances");
                alertsCreated++;
            }
        }

        log.info("checkLowScores() — {} patient(s) vérifié(s),"
                        + " {} alerte(s) potentielle(s) traitée(s)",
                patients.size(), alertsCreated);
    }


}