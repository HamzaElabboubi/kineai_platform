package com.project.kineai.model.entity;

import com.project.kineai.model.enums.KineDecision;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Table(name = "phase_progress_history")
@Builder
public class PhaseProgressHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(updatable = false, nullable = false)
    private UUID id;

    //------- Patient Relation--------
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "patient_id", nullable = false)
    private Patient patient;
    //---------------------------------

    //------- PhaseDefinition Relation (phase quittée)--------
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "phase_definition_id", nullable = false)
    private PhaseDefinition phaseDefinition;
    //-----------------------------------------------------------

    @Column(name = "entered_at", nullable = false)
    private LocalDateTime enteredAt;

    @Column(name = "exited_at", nullable = false)
    private LocalDateTime exitedAt;

    @Column(name = "final_cpi_score", nullable = false,
            precision = 5, scale = 2)
    private BigDecimal finalCpiScore;

    //------- Kinesitherapeute Relation--------
    // Jamais null — c'est la preuve de validation humaine
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "validated_by_kine_id", nullable = false)
    private Kinesitherapeute validatedByKine;
    //-------------------------------------------

    @Enumerated(EnumType.STRING)
    @Column(name = "kine_decision", nullable = false)
    private KineDecision kineDecision;
}