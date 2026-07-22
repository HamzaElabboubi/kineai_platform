package com.project.kineai.model.entity;

import com.project.kineai.model.enums.BodyZone;
import com.project.kineai.model.enums.Level;
import jakarta.persistence.*;
import lombok.*;

import java.util.List;
import java.util.UUID;

@Entity
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Table(
        name = "phase_definitions",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_protocol_order",
                columnNames = {"protocol_id", "order_index"}
        )
)
@Builder
public class PhaseDefinition {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(updatable = false, nullable = false)
    private UUID id;

    //------- RehabProtocol Relation--------
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "protocol_id", nullable = false)
    private RehabProtocol protocol;
    //---------------------------------------

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Column(
            name = "order_index",
            nullable = false,
            columnDefinition = "INTEGER CHECK (order_index > 0)"
    )
    private Integer orderIndex;

    @Column(name = "description", length = 500)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "body_zone", nullable = false)
    private BodyZone bodyZone;

    // ✅ Nécessaire pour interroger ExerciseRepository
    // .findByBodyZoneAndDifficultyLevel() — sans ce champ,
    // aucun lien possible avec le catalogue d'exercices existant
    @Enumerated(EnumType.STRING)
    @Column(name = "target_difficulty_level", nullable = false)
    private Level targetDifficultyLevel;

    @Column(
            name = "base_reps_target",
            nullable = false,
            columnDefinition = "INTEGER CHECK (base_reps_target > 0)"
    )
    private Integer baseRepsTarget;

    @Column(
            name = "min_sessions_required",
            nullable = false,
            columnDefinition = "INTEGER CHECK (min_sessions_required >= 1)"
    )
    @Builder.Default
    private Integer minSessionsRequired = 3;

    //------- ExitCriteria Relation--------
    @OneToMany(mappedBy = "phaseDefinition", cascade = CascadeType.ALL,
            orphanRemoval = true)
    private List<ExitCriteria> exitCriteria;
    //--------------------------------------
}