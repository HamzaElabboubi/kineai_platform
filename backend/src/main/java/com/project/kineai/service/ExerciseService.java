package com.project.kineai.service;

import com.project.kineai.dto.response.ExerciseResponse;
import com.project.kineai.exception.BusinessException;
import com.project.kineai.mapper.ExerciseMapper;
import com.project.kineai.model.entity.*;
import com.project.kineai.model.enums.Level;
import com.project.kineai.model.enums.SessionDay;
import com.project.kineai.model.enums.SessionStatus;
import com.project.kineai.model.enums.Status;
import com.project.kineai.repository.ExerciseRepository;
import com.project.kineai.repository.PatientRepository;
import com.project.kineai.repository.PlanExerciseRepository;
import com.project.kineai.repository.RehabPlanRepository;
import com.project.kineai.repository.SessionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static com.project.kineai.model.enums.Level.INTERMEDIAIRE;

@Service
@RequiredArgsConstructor
public class ExerciseService {

    private final ExerciseRepository exerciseRepository;
    private final PatientRepository patientRepository;
    private final RehabPlanRepository rehabPlanRepository;
    private final PlanExerciseRepository planExerciseRepository;
    private final SessionRepository sessionRepository;
    private final ExerciseMapper exerciseMapper;

    // Mêmes jours que dans RehabPlanService —
    // ordre cohérent avec l'assignation initiale
    private static final SessionDay[] TRAINING_DAYS = {
            SessionDay.LUNDI,
            SessionDay.MERCREDI,
            SessionDay.VENDREDI
    };

    @Transactional(readOnly = true)
    public List<ExerciseResponse> getAllExercises() {
        return exerciseRepository.findAll()
                .stream()
                .map(exerciseMapper::toResponse)
                .toList();
    }

    // ✅ Exercices du PROCHAIN jour d'entraînement
    // dans la semaine courante du plan actif
    @Transactional(readOnly = true)
    public List<ExerciseResponse> getMyExercises() {
        String email = SecurityContextHolder
                .getContext()
                .getAuthentication()
                .getName();

        Patient patient = patientRepository
                .findByUser_Email(email)
                .orElseThrow(() -> new BusinessException(
                        "Patient introuvable"));

        RehabPlan activePlan = rehabPlanRepository
                .findByPatientIdAndStatus(
                        patient.getId(), Status.ACTIVE)
                .orElseThrow(() -> new BusinessException(
                        "Aucun plan actif. Contactez"
                                + " votre kinésithérapeute."));

        int currentWeek = activePlan.getCurrentWeek();

        LocalDate weekStart = activePlan.getStartDate()
                .plusDays((currentWeek - 1) * 7L);
        LocalDate weekEnd = weekStart.plusDays(7);

        long sessionsThisWeek = sessionRepository
                .countByRehabPlanIdAndSessionStatusAndStartTimeBetween(
                        activePlan.getId(),
                        SessionStatus.COMPLETED,
                        weekStart.atStartOfDay(),
                        weekEnd.atStartOfDay());

        if (sessionsThisWeek >= TRAINING_DAYS.length) {
            throw new BusinessException(
                    "Vous avez terminé toutes les séances"
                            + " prévues cette semaine."
                            + " Revenez la semaine prochaine !");
        }

        // ✅ NOUVEAU — vérifier l'espacement minimum
        // depuis la dernière séance complétée
        Optional<Session> lastCompleted = sessionRepository
                .findFirstByPatientIdAndSessionStatusOrderByStartTimeDesc(
                        patient.getId(), SessionStatus.COMPLETED);

        if (lastCompleted.isPresent()) {
            LocalDateTime lastSessionTime =
                    lastCompleted.get().getStartTime();
            LocalDateTime now = LocalDateTime.now();

            long hoursSinceLastSession =
                    Duration.between(lastSessionTime, now)
                            .toHours();

            if (hoursSinceLastSession < 20) {
                long hoursRemaining = 20 - hoursSinceLastSession;
                throw new BusinessException(
                        "Votre dernière séance était trop"
                                + " récente. Pour laisser à votre"
                                + " corps le temps de récupérer,"
                                + " revenez dans environ "
                                + hoursRemaining + "h.");
            }
        }

        SessionDay nextDay =
                TRAINING_DAYS[(int) sessionsThisWeek];

        List<PlanExercise> planExercises =
                planExerciseRepository
                        .findByRehabPlanIdAndWeekNumberAndDayOfWeek(
                                activePlan.getId(),
                                currentWeek, nextDay);

        if (planExercises.isEmpty()) {
            throw new BusinessException(
                    "Aucun exercice prévu pour votre"
                            + " prochaine séance. Contactez"
                            + " votre kinésithérapeute.");
        }

        // ✅ APRÈS — on garde le PlanExercise complet,
// pas juste l'Exercise, pour accéder à repsPrescribed
        return planExercises.stream()
                .map(pe -> buildAdjustedResponse(
                        pe, patient.getLevel()))
                .collect(Collectors.toList());
    }

    // ── Ajustement dynamique difficulté ───────
// Le système expert rend l'exercice plus exigeant
// (plus de reps, tolérance plus stricte) selon
// le niveau réel du patient, sans jamais toucher
// aux données stockées en base
    // ✅ APRÈS — prend PlanExercise, base sur
// repsPrescribed (progressif) au lieu de
// repsTarget (fixe)
    private ExerciseResponse buildAdjustedResponse(
            PlanExercise planExercise, Level patientLevel) {

        Exercise exercise = planExercise.getExercise();
        ExerciseResponse response =
                exerciseMapper.toResponse(exercise);

        // ✅ Base = reps PROGRESSIVES de cette séance
        // précise (déjà ajustées par semaine dans
        // RehabPlanService), pas la valeur fixe du
        // catalogue
        int baseReps = planExercise.getRepsPrescribed() != null
                ? planExercise.getRepsPrescribed()
                : (exercise.getRepsTarget() != null
                ? exercise.getRepsTarget() : 10);

        int baseTolerance =
                exercise.getToleranceDegree() != null
                        ? exercise.getToleranceDegree() : 15;

        switch (patientLevel) {
            case DEBUTANT -> {
                response.setRepsTarget(baseReps);
                response.setToleranceDeg(baseTolerance);
            }
            case INTERMEDIAIRE -> {
                response.setRepsTarget(baseReps + 5);
                response.setToleranceDeg(
                        Math.max(8, baseTolerance - 5));
            }
            case AVANCE -> {
                response.setRepsTarget(baseReps + 10);
                response.setToleranceDeg(
                        Math.max(5, baseTolerance - 9));
            }
        }

        return response;
    }

    @Transactional(readOnly = true)
    public List<ExerciseResponse> getByBodyZone(
            com.project.kineai.model.enums.BodyZone bodyZone) {
        return exerciseRepository
                .findByBodyZone(bodyZone)
                .stream()
                .map(exerciseMapper::toResponse)
                .toList();
    }
}