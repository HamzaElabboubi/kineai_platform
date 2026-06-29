package com.project.kineai.dto.request;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CompleteSessionRequest {
    @NotNull
    @DecimalMin("0.0") @DecimalMax("100.0")
    private BigDecimal finalScore;
    @NotNull @Min(0) private Integer repsCompleted;
    @NotBlank(message = "Les angles articulaires sont obligatoires")
    private String jointAngles; // JSON résumé final
}
