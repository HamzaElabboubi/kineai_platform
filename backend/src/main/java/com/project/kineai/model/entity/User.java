package com.project.kineai.model.entity;

import com.project.kineai.model.enums.Role;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "users")
@Getter
@Setter
@Builder
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id ;

    @Column(name = "email",unique = true,length = 150,nullable = false)
    private String email ;

    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    @Column(name = "password",nullable = false, length = 255)
    private String password ;

    @Enumerated( EnumType.STRING)
    @Column(name ="role",nullable = false, updatable = false)
    private Role role;

    @Column(name ="active",nullable = false)
    @Builder.Default
    private Boolean active = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void prePersist() {
        this.createdAt = LocalDateTime.now();
    }

    //------- Patient Relation---------
    @OneToOne(mappedBy = "user", fetch = FetchType.LAZY)
    private Patient patient;
    //---------------------------------

    //------- Kinesitherapeute Relation---------
    @OneToOne(mappedBy = "user", fetch = FetchType.LAZY)
    private Kinesitherapeute kinesitherapeute;
    //------------------------------------------

}
