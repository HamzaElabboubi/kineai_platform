package com.project.kineai.dto.request;


import com.project.kineai.model.enums.SessionDay;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.UUID;

@Data

public class CreatePlanExerciseRequest {

    @NotNull(message = "L'exercice est obligatoire")
    private UUID exerciseId;

    @NotNull(message = "La semaine est obligatoire")
    @Min(value = 1, message = "La semaine doit être entre 1 et 4")
    @Max(value = 4, message = "La semaine doit être entre 1 et 4")
    private Integer weekNumber;

    @NotNull(message = "Le jour est obligatoire")
    private SessionDay dayOfWeek;

    @NotNull(message = "Le nombre de répétitions est obligatoire")
    @Min(value = 1, message = "Les répétitions doivent être supérieures à 0")
    private Integer repsPrescribed;

    @Min(value = 1, message = "L'ordre doit être supérieur à 0")
    private Integer orderInSession = 1; // défaut = 1
}
