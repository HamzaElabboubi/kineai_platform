package com.project.kineai.dto.response;

import lombok.Builder;
import lombok.Data;

import java.util.Map;

@Data
@Builder
public class AdminStatsResponse {
    private long totalPatients;
    private long totalKines;
    private long validatedKines;
    private long pendingKines;
    private Map<String, Long> patientsByLevel;
    private Map<String, Long> patientsByPathology;

    // ✅ Nouveau — seulement ceci
    private Map<String, Long> kinesBySpeciality;
}