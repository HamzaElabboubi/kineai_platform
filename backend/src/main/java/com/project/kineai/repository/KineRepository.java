package com.project.kineai.repository;

import com.project.kineai.model.entity.Kinesitherapeute;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface KineRepository extends JpaRepository<Kinesitherapeute, UUID> {
    Optional<Kinesitherapeute> findByUserId(UUID userId);
    List<Kinesitherapeute> findByValidatedTrue();

    List<Kinesitherapeute> findByValidatedFalse();
    // KineRepository.java — si pas déjà présentes
    long countByValidatedTrue();
    long countByValidatedFalse();
    List<Kinesitherapeute> findAll(); // déjà fourni
}
