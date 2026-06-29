package com.project.kineai.dto.request;

import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class SaveMetricsRequest {
    @NotBlank
    private String jointAngles; // JSON string
    @NotNull
    @DecimalMin("0.0") @DecimalMax("100.0")
    private BigDecimal conformityPct;
    @NotNull @Min(0) private Integer repsAtMoment;
}
