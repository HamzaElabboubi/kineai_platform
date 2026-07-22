package com.project.kineai.model.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Table(name = "cpi_weight_configs")
@Builder
public class CpiWeightConfig {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(updatable = false, nullable = false)
    private UUID id;

    //------- RehabProtocol Relation--------
    // Nullable = config globale par défaut, appliquée si
    // aucun protocole n'a sa propre pondération
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "protocol_id", nullable = true, unique = true)
    private RehabProtocol protocol;
    //---------------------------------------

    @Column(name = "quality_weight", nullable = false,
            precision = 5, scale = 2)
    @Builder.Default
    private BigDecimal qualityWeight = new BigDecimal("40.00");

    @Column(name = "adherence_weight", nullable = false,
            precision = 5, scale = 2)
    @Builder.Default
    private BigDecimal adherenceWeight = new BigDecimal("20.00");

    @Column(name = "success_rate_weight", nullable = false,
            precision = 5, scale = 2)
    @Builder.Default
    private BigDecimal successRateWeight = new BigDecimal("20.00");

    @Column(name = "rom_weight", nullable = false,
            precision = 5, scale = 2)
    @Builder.Default
    private BigDecimal romWeight = new BigDecimal("20.00");
}