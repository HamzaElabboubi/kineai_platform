package com.project.kineai.repository;

import com.project.kineai.model.entity.PlanExercise;
import com.project.kineai.model.enums.SessionDay;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface PlanExerciseRepository
        extends JpaRepository<PlanExercise, UUID> {

    List<PlanExercise> findByRehabPlanIdOrderByWeekNumberAscOrderInSessionAsc(
            UUID rehabPlanId);

    List<PlanExercise> findByRehabPlanIdAndWeekNumber(
            UUID rehabPlanId, Integer weekNumber);

    List<PlanExercise> findByRehabPlanIdAndWeekNumberAndDayOfWeek(
            UUID rehabPlanId, Integer weekNumber,
            SessionDay dayOfWeek);


    boolean existsByRehabPlanIdAndExerciseIdAndWeekNumberAndDayOfWeek(
            UUID rehabPlanId, UUID exerciseId,
            Integer weekNumber,
            com.project.kineai.model.enums.SessionDay dayOfWeek);

}