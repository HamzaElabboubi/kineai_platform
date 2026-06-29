package com.project.kineai.model.entity;

import com.project.kineai.model.enums.SessionDay;
import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Entity
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Table(
        name = "plan_exercises",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_plan_exercise_day",
                columnNames = {"rehab_plan_id", "exercise_id", "week_number", "day_of_week"}
        )
)
@Builder
public class PlanExercise {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(updatable = false, nullable = false)
    private UUID id ;

    //------- RehabPlan Relation--------
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "rehab_plan_id", nullable = false)
    private RehabPlan rehabPlan;
    //-----------------------------

    //------- Exercise Relation--------
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "exercise_id", nullable = false)
    private Exercise exercise;
    //-----------------------------

    //------- Plaining--------
    @Column(name = "week_number", nullable = false, columnDefinition = "INTEGER CHECK (week_number > 0 AND week_number <= 4)")
    private Integer weekNumber;

    @Enumerated(EnumType.STRING)
    @Column(name = "day_of_week", nullable = false)
    private SessionDay dayOfWeek;

    //----------------------------

    //------- Exercise Config--------
    @Column(
            name = "reps_prescribed",
            nullable = false,
            columnDefinition = "INTEGER CHECK (reps_prescribed > 0)"
    )
    private Integer repsPrescribed;

    @Builder.Default
    @Column(
            name = "order_in_session",
            nullable = false,
            columnDefinition = "INTEGER CHECK (order_in_session > 0)"
    )
    private Integer orderInSession = 1;
}
