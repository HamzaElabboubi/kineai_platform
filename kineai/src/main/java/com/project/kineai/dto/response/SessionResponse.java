package com.project.kineai.dto.response;

import com.project.kineai.model.enums.SessionStatus;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
public class SessionResponse {
    private UUID id;
    private UUID patientId;
    private UUID exerciseId;
    private String exerciseName;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private BigDecimal score;
    private Integer repsCompleted;
    private Integer xpEarned;
    private SessionStatus status;
}
