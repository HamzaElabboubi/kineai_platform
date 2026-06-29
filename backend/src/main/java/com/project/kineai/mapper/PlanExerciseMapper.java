package com.project.kineai.mapper;

import com.project.kineai.dto.response.PlanExerciseResponse;
import com.project.kineai.model.entity.PlanExercise;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class PlanExerciseMapper {

    public PlanExerciseResponse toResponse(
            PlanExercise pe) {
        return PlanExerciseResponse.builder()
                .id(pe.getId())
                .exerciseId(pe.getExercise().getId())
                .exerciseName(pe.getExercise().getName())
                .bodyZone(pe.getExercise()
                        .getBodyZone().name())
                .weekNumber(pe.getWeekNumber())
                .dayOfWeek(pe.getDayOfWeek())
                .repsPrescribed(pe.getRepsPrescribed())
                .orderInSession(pe.getOrderInSession())
                .build();
    }

    public List<PlanExerciseResponse> toResponseList(
            List<PlanExercise> list) {
        return list.stream()
                .map(this::toResponse)
                .toList();
    }
}