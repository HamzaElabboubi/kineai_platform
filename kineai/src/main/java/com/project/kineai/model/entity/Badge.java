package com.project.kineai.model.entity;

import com.project.kineai.model.enums.BadgeType;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Table(
        name = "badges",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_patient_badge_type",
                columnNames = {"patient_id", "badge_type"}
        )
)
@Builder
public class Badge {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(updatable = false, nullable = false)
    private UUID id ;

    //------- Patient Relation--------
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "patient_id", nullable = false)
    private Patient patient;
    //-------------------------------

    @Enumerated(EnumType.STRING)
    @Column(name = "badge_type", nullable = false)
    private BadgeType badgeType;

    @Column(name = "unlocked_at", nullable = false)
    @Builder.Default
    private LocalDateTime unlockedAt = LocalDateTime.now();

    @Column(name = "displayed", nullable = false)
    @Builder.Default
    private Boolean displayed = false;


}
