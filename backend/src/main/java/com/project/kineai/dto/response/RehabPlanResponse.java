package com.project.kineai.dto.response;

import com.project.kineai.model.enums.Level;
import com.project.kineai.model.enums.Status;
import lombok.Data;

import java.time.LocalDate;
import java.util.UUID;

@Data
public class RehabPlanResponse {
    private UUID id;
    private UUID patientId;
    private LocalDate startDate;
    private LocalDate endDate;
    private Status status;
    private Level difficultyLevel;
    private Integer currentWeek;
}
