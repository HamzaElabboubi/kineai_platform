package com.project.kineai.dto.response;

import com.project.kineai.model.enums.SessionDay;
import lombok.Builder;
import lombok.Data;

import java.util.UUID;

@Data
@Builder
public class PlanExerciseResponse {
    private UUID id;
    private UUID exerciseId;
    private String exerciseName;
    private String bodyZone;
    private Integer weekNumber;
    private SessionDay dayOfWeek;
    private Integer repsPrescribed;
    private Integer orderInSession;
}