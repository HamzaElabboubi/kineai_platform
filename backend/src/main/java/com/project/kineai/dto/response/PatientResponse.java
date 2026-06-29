package com.project.kineai.dto.response;

import com.project.kineai.model.enums.Level;
import com.project.kineai.model.enums.Pathology;
import lombok.Data;

import java.util.UUID;

@Data
public class PatientResponse {
    private UUID id;
    private String fullName;
    private Integer age;
    private Pathology pathology;
    private Level level;
    private Integer streakCount;
    private Integer totalXp;
    private String kineName;
    private UUID kineId;
    private Boolean isActive;
}
