package com.project.kineai.model.entity;

import com.project.kineai.model.enums.Level;
import com.project.kineai.model.enums.Status;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Entity
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Table(name = "rehab_plans")
@Builder
public class RehabPlan {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, unique = true, nullable = false)
    private UUID id;

    //------- Patient Relation--------
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "patient_id", nullable = false)
    private Patient patient;
    //-----------------------------

    //------- PlanExercise relation--------
    @OneToMany(mappedBy = "rehabPlan", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<PlanExercise> planExercises;

    //------- Plan info--------
    @Column(name = "start_date",nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date",nullable = false)
    private LocalDate endDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    @Builder.Default
    private Status status = Status.ACTIVE;

    @Enumerated(EnumType.STRING)
    @Column(name = "difficulty_level", nullable = false)
    @Builder.Default
    private Level difficultyLevel = Level.DEBUTANT;

    @Column(name = "current_week", nullable = false, columnDefinition = "INTEGER CHECK (current_week > 0 AND current_week <= 4)")
    @Builder.Default
    private Integer currentWeek = 1;
}
