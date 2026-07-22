package com.project.kineai.model.entity;

import com.project.kineai.model.enums.Pathology;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Entity
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Table(name = "rehab_protocols")
@Builder
public class RehabProtocol {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(updatable = false, nullable = false)
    private UUID id;

    @Enumerated(EnumType.STRING)
    @Column(name = "pathology", nullable = false)
    private Pathology pathology;

    @Column(name = "name", nullable = false, length = 150)
    private String name;

    @Column(name = "active", nullable = false)
    @Builder.Default
    private Boolean active = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    public void prePersist() {
        this.createdAt = LocalDateTime.now();
    }

    //------- PhaseDefinition Relation--------
    @OneToMany(mappedBy = "protocol", cascade = CascadeType.ALL,
            orphanRemoval = true)
    private List<PhaseDefinition> phaseDefinitions;
    //-----------------------------------------

    //------- CpiWeightConfig Relation--------
    // Optionnel — si absent, le service utilise la config
    // globale par défaut (protocol = null)
    @OneToOne(mappedBy = "protocol", cascade = CascadeType.ALL,
            orphanRemoval = true)
    private CpiWeightConfig cpiWeightConfig;
    //-----------------------------------------
}