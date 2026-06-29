package com.project.kineai.model.entity;

import com.project.kineai.model.enums.AlertType;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Table(name = "alerts")
@Builder
public class Alert {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(updatable = false, nullable = false)
    private UUID id ;

    //------- Patient Relation--------
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "patient_id", nullable = false)
    private Patient patient;
    //-------------------------------

    //------- Kinesitherapeute Relation--------
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "kinesitherapeute_id", nullable = false)
    private Kinesitherapeute kinesitherapeute;
    //-------------------------------

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false)
    private AlertType type;

    @Column(name = "message", nullable = false, length = 500)
    private String message;

    @Column(name = "sent_at", nullable = false)
    @Builder.Default
    private LocalDateTime sentAt = LocalDateTime.now();

    @Column(name = "resolved", nullable = false)
    @Builder.Default
    private Boolean resolved = false;
}
