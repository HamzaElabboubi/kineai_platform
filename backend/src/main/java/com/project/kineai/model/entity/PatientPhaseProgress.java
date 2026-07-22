package com.project.kineai.model.entity;

import com.project.kineai.model.enums.PhaseProgressStatus;
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
@Table(name = "patient_phase_progress")
@Builder
public class PatientPhaseProgress {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(updatable = false, nullable = false)
    private UUID id;

    //------- Patient Relation--------
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "patient_id", nullable = false)
    private Patient patient;
    //---------------------------------

    //------- RehabProtocol Relation--------
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "protocol_id", nullable = false)
    private RehabProtocol protocol;
    //---------------------------------------

    //------- PhaseDefinition Relation (phase actuelle)--------
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "current_phase_id", nullable = false)
    private PhaseDefinition currentPhase;
    //-----------------------------------------------------------

    @Column(name = "entered_phase_at", nullable = false)
    @Builder.Default
    private LocalDateTime enteredPhaseAt = LocalDateTime.now();

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    @Builder.Default
    private PhaseProgressStatus status = PhaseProgressStatus.EN_COURS;

    // Null tant qu'aucune séance n'a été complétée dans la phase
    @Column(name = "current_cpi_score", precision = 5, scale = 2)
    private BigDecimal currentCpiScore;
}