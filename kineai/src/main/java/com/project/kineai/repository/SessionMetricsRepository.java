package com.project.kineai.repository;

import com.project.kineai.model.entity.SessionMetrics;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface SessionMetricsRepository extends JpaRepository<SessionMetrics, UUID> {
    List<SessionMetrics> findBySessionIdOrderByRecordedAtAsc(UUID sessionId);
}
