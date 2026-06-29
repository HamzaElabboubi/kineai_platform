package com.project.kineai.dto.response;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class DashboardKineResponse {
    private Integer totalPatients;
    private Long pendingAlerts;
    private List<PatientResponse> patients;
    private List<AlertResponse> recentAlerts;
}
