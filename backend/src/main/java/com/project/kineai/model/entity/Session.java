package com.project.kineai.model.entity;

import jakarta.persistence.*;
import lombok.*;
import com.project.kineai.model.enums.SessionStatus;
import org.hibernate.annotations.Check;
import org.hibernate.annotations.DialectOverride;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Table(name = "sessions")
@Check(constraints = "score >= 0 AND score <= 100")
@Builder
public class Session {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(updatable = false, nullable = false)
    private UUID id ;

    //------- RehabPlan Relation--------
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "rehab_plan_id", nullable = true)
    private RehabPlan rehabPlan;
    //----------------------------------

    //------- Patient Relation--------
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "patient_id", nullable = false)
    private Patient patient;
    //--------------------------------

    //------- Exercise Relation--------
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "exercise_id", nullable = false)
    private Exercise exercise;
    //--------------------------------

    @Column(name = "start_time",nullable = false)
    private LocalDateTime startTime;

    @Column(name = "end_time",nullable = true)
    private LocalDateTime endTime;

    @Column(name = "score", precision = 5, scale = 2)
    private BigDecimal score;

    @Column(name = "reps_completed",nullable = false,columnDefinition = "INTEGER CHECK (reps_completed >= 0) ")
    @Builder.Default
    private Integer repsCompleted=0;

    @Column(name = "xp_earned",nullable = false,columnDefinition = "INTEGER CHECK (xp_earned >= 0 )")
    @Builder.Default
    private Integer xpEarned = 0;

    @Column(name = "pain_level", nullable = true,
            columnDefinition = "INTEGER CHECK (pain_level >= 0 AND pain_level <= 10)")
    private Integer painLevel; // Auto-déclaré par le patient — optionnel

    @Column(name = "joint_angles", nullable = true,columnDefinition = "TEXT")
    private String jointAngles;



    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    @Builder.Default
    private SessionStatus sessionStatus = SessionStatus.IN_PROGRESS;

    //---------- SessionMetrics -----------------------
    @OneToMany(mappedBy = "session",
            cascade = CascadeType.ALL,
            fetch = FetchType.LAZY)
    @Builder.Default
    private List<SessionMetrics> metrics = new ArrayList<>();

    //------- PatientPhaseProgress Relation--------
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "patient_phase_progress_id", nullable = true)
    private PatientPhaseProgress patientPhaseProgress;
//-----------------------------------------------




}
