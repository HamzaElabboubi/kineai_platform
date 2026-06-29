package com.project.kineai.dto.response;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Data
public class KineResponse {
    private UUID id;
    private String fullName;
    private String speciality;
    private Boolean validated;
    private Integer patientCount;
    private String email;
    private Boolean active;// ← ajouter si manquant
}
