package com.project.kineai.dto.request;

import com.project.kineai.model.enums.Pathology;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CreatePatientRequest {
    @Email(message = "Email doit être valide")
    @NotBlank(message = "L'email est obligatoire")
    private String email;
    @NotBlank @Size(min = 8, message = "Mot de passe minimum 8 caractères")
    private String password;
    @NotBlank(message = "Le nom complet est obligatoire")
    private String fullName;
    @NotNull
    @Min(1) @Max(120)
    private Integer age;
    @NotBlank(message = "Le numéro de téléphone est obligatoire")
    private String phone;
    @NotNull
    private Pathology pathology;
    @NotNull(message = "Le kinésithérapeute est obligatoire")
    private UUID kineId;
}
