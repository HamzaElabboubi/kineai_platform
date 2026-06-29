package com.project.kineai.model.entity;


import com.project.kineai.model.enums.Level;
import com.project.kineai.model.enums.Pathology;
import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Entity
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Table(name = "patients")
@Builder
public class Patient {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false,unique = true, nullable = false)
    private UUID id ;

    //------- User Relation--------
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;
    //-----------------------------





    @Column(name = "full_name", nullable = false, length = 100)
    private String fullName;

    @Column(name = "age", nullable = false, columnDefinition = "INTEGER CHECK (age > 0 AND age < 120)")
    private Integer age;

    @Column(name = "phone", nullable = true, length = 20)
    private String phone;

    @Enumerated(EnumType.STRING)
    @Column(name = "pathology", nullable = false)
    private Pathology pathology;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    @Column(name = "level", nullable = false)
    private Level level =Level.DEBUTANT;

    //------- Kinesitherapeute Relation---------
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "kine_id", nullable = false)
    private Kinesitherapeute kine;
    //------------------------------------------

    //------- Gamifaction---------
    @Builder.Default
    @Column(name = "streak_count", nullable = false)
    private Integer streakCount = 0;

    @Builder.Default
    @Column(name = "total_xp", nullable = false)
    private Integer totalXp = 0;
}
