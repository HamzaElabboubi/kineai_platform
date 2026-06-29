package com.project.kineai.dto.response;

import com.project.kineai.model.enums.AlertType;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
public class AlertResponse {
    private UUID id;
    private UUID patientId;
    private String patientName;
    private AlertType type;
    private String message;
    private LocalDateTime sentAt;
    private Boolean resolved;
}

