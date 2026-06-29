package com.project.kineai.model.entity;

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
@Table(name = "kinesitherapeutes")
@Builder
public class Kinesitherapeute {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, unique = true, nullable = false)
    private UUID id ;

    //------- User Relation--------
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;
    //-----------------------------

    //------- Kinesitherapeute Details--------
    @Column(name = "full_name", nullable = false, length = 100)
    private String fullName;

    @Column(name = "speciality", nullable = false, length = 100)
    private String speciality;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    @Builder.Default
    private Boolean validated = false;

    //--------------------------------------


    //created_at automatique => Évite de remplir la date manuellement "AUTO TIMESTAMP".
    @PrePersist
    public void prePersist() {
        this.createdAt = LocalDateTime.now();
    }

    //------- Patient Relation--------
    @OneToMany(mappedBy = "kine",fetch = FetchType.LAZY, orphanRemoval = true)
    private List<Patient> patients;
    //-----------------------------

}
