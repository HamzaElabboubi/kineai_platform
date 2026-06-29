package com.project.kineai.model.entity;


import com.project.kineai.model.enums.BodyZone;
import com.project.kineai.model.enums.Level;
import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Entity
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Table(name = "exercises")
@Builder
public class Exercise {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(updatable = false, nullable = false)
    private UUID id ;

    //------- Basic info--------

    @Column(name = "name", nullable = false, length = 100,unique = true)
    private String name;

    @Column(name = "description", length = 500,nullable = false)
    private String description;

    //------- Exercise target --------
    @Enumerated(EnumType.STRING)
    @Column(name = "body_zone",nullable = false)
    private BodyZone bodyZone;

    @Column(name = "target_angle", nullable = false,columnDefinition = "INTEGER CHECK (target_angle >= 0 AND target_angle <= 180)")
    private Integer targetAngle;

    @Column(
            name = "tolerance_degree",
            nullable = false,
            columnDefinition = "INTEGER CHECK (tolerance_degree >= 0 AND tolerance_degree <= 45)"
    )
    @Builder.Default
    private Integer toleranceDegree = 15;

    //------- SESSION CONFIG --------
    @Column(name = "recommended_duration", nullable = false,columnDefinition = "INTEGER CHECK (recommended_duration > 0)")
    private Integer recommendedDuration; // Durée recommandée en secondes

    @Column(name = "reps_target", nullable = false,columnDefinition = "INTEGER CHECK (reps_target > 0)")
    private Integer repsTarget; // Nombre de répétitions ciblé par séance

    @Enumerated(EnumType.STRING)
    @Column(name = "difficulty_level",nullable = false)
    private Level difficultyLevel;

    //------- Mediapipe config --------
    @Column(name = "mediapipe_joints", nullable = false,columnDefinition = "TEXT")
    private String mediapipeJoints; // Liste des joints utilisés par MediaPipe, séparés par des virgules

}
