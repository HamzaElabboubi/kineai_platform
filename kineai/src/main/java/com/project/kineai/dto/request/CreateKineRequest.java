package com.project.kineai.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CreateKineRequest {
    @Email(message = "Email doit être valide")
    @NotBlank
    private String email;
    @NotBlank @Size(min = 8,message = "Le mot de passe doit contenir au moins 8 caractères")
    private String password;
    @NotBlank(message = "Le nom complet est obligatoire")
    private String fullName;
    @NotBlank(message = "Spécialité est obligatoire")
    private String speciality;
}
