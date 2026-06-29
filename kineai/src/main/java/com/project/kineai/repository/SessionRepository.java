package com.project.kineai.repository;

import com.project.kineai.model.entity.Session;
import com.project.kineai.model.enums.SessionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SessionRepository extends JpaRepository<Session, UUID> {

    List<Session> findByPatientIdOrderByStartTimeDesc(UUID patientId);

    // ✅ findFirst remplace LIMIT 1
    Optional<Session> findFirstByPatientIdOrderByStartTimeDesc(UUID patientId);

    List<Session> findByPatientIdAndStartTimeAfterAndSessionStatus(
            UUID patientId, LocalDateTime date, SessionStatus status);

    long countByRehabPlanIdAndSessionStatusAndStartTimeBetween(
            UUID rehabPlanId, SessionStatus status,
            LocalDateTime start, LocalDateTime end);


    // ✅ @Query native pour AVG sur 3 dernières séances
    @Query(value = """
        SELECT AVG(score) FROM (
            SELECT score FROM sessions
            WHERE patient_id = :patientId
            AND status = 'COMPLETED'
            ORDER BY start_time DESC
            LIMIT 3
        ) AS last3
        """, nativeQuery = true)
    Double getAverageScoreLastThreeSessions(UUID patientId);

    // ✅ Nouveau — compter le total de séances
    // complétées, pour vérifier le seuil minimum
    // avant d'évaluer une progression/régression
    @Query(value = """
        SELECT COUNT(*) FROM sessions
        WHERE patient_id = :patientId
        AND status = 'COMPLETED'
        """, nativeQuery = true)
    long countCompletedSessions(UUID patientId);


    Optional<Session> findFirstByPatientIdAndSessionStatusOrderByStartTimeDesc(
            UUID patientId, SessionStatus status);
}
