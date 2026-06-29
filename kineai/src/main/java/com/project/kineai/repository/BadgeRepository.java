package com.project.kineai.repository;

import com.project.kineai.model.entity.Badge;
import com.project.kineai.model.enums.BadgeType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface BadgeRepository extends JpaRepository<Badge, UUID> {
    List<Badge> findByPatientIdOrderByUnlockedAtDesc(UUID patientId);
    Optional<Badge> findByPatientIdAndBadgeType(UUID patientId, BadgeType type);
    boolean existsByPatientIdAndBadgeType(UUID patientId, BadgeType type);
    long countByPatientId(UUID patientId);
}
