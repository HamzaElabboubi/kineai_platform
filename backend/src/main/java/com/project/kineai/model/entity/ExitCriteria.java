package com.project.kineai.model.entity;

import com.project.kineai.model.enums.CriteriaType;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Table(
        name = "exit_criteria",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_phase_criteria_type",
                columnNames = {"phase_definition_id", "criteria_type"}
        )
)
@Builder
public class ExitCriteria {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(updatable = false, nullable = false)
    private UUID id;

    //------- PhaseDefinition Relation--------
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "phase_definition_id", nullable = false)
    private PhaseDefinition phaseDefinition;
    //-----------------------------------------

    @Enumerated(EnumType.STRING)
    @Column(name = "criteria_type", nullable = false)
    private CriteriaType criteriaType;

    @Column(name = "threshold_value", nullable = false,
            precision = 5, scale = 2)
    private BigDecimal thresholdValue;
}