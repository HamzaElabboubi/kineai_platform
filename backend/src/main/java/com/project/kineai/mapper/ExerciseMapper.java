package com.project.kineai.mapper;

import com.project.kineai.dto.response.ExerciseResponse;
import com.project.kineai.model.entity.Exercise;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface ExerciseMapper {

    // ── Entity → Response DTO ─────────────────
    // Tous les champs ont le même nom — MapStruct mappe automatiquement
    // sauf toleranceDeg qui diffère de toleranceDegree dans l'entité
        @Mapping(source = "toleranceDegree", target = "toleranceDeg")
        ExerciseResponse toResponse(Exercise exercise);

    // ── List<Entity> → List<DTO> ──────────────
    List<ExerciseResponse> toResponseList(List<Exercise> exercises);
}