package com.project.kineai.repository;

import com.project.kineai.model.entity.Notification;
import com.project.kineai.model.enums.NotificationType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public interface NotificationRepository
        extends JpaRepository<Notification, UUID> {

    // Toutes les notifications du patient, récentes → anciennes
    List<Notification> findByPatientIdOrderBySentAtDesc(
            UUID patientId);

    // Notifications non lues du patient
    List<Notification> findByPatientIdAndReadFalseOrderBySentAtDesc(
            UUID patientId);

    // Compteur pour badge "non lu" côté frontend
    long countByPatientIdAndReadFalse(UUID patientId);

    // Anti-doublon — RG inspirée de AlertService (RG-26)
    // Évite d'envoyer 2x le même type de notif le même jour
    boolean existsByPatientIdAndTypeAndSentAtAfter(
            UUID patientId, NotificationType type,
            LocalDateTime since);
}