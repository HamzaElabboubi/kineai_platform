package com.project.kineai.dto.response;

import com.project.kineai.model.enums.BodyZone;
import com.project.kineai.model.enums.Level;
import lombok.Data;

import java.util.UUID;

@Data
public class ExerciseResponse {
    private UUID id;
    private String name;
    private String description;    // ← ajouter
    private BodyZone bodyZone;
    private Integer targetAngle;
    private Integer toleranceDeg;
    private Integer recommendedDuration; // ← ajouter
    private Integer repsTarget;
    private Level difficultyLevel;
    private String mediapipeJoints;
}
