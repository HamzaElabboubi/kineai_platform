package com.project.kineai.service;

import com.project.kineai.dto.response.NotificationResponse;
import com.project.kineai.exception.BusinessException;
import com.project.kineai.mapper.NotificationMapper;
import com.project.kineai.model.entity.Notification;
import com.project.kineai.model.entity.Patient;
import com.project.kineai.model.enums.NotificationType;
import com.project.kineai.repository.NotificationRepository;
import com.project.kineai.service.notification.NotificationSender;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final NotificationMapper notificationMapper;
    private final PatientService patientService;

    // Spring injecte automatiquement toutes les implémentations
    // de NotificationSender (ex: EmailNotificationSender)
    private final List<NotificationSender> senders;

    // ── Créer une notification — anti-doublon ─────────
    @Transactional
    public void createNotification(Patient patient,
                                   NotificationType type,
                                   String message,
                                   LocalDateTime dedupeSince) {

        boolean alreadySentRecently = notificationRepository
                .existsByPatientIdAndTypeAndSentAtAfter(
                        patient.getId(), type, dedupeSince);

        if (alreadySentRecently) {
            log.debug("Notification {} déjà envoyée récemment"
                            + " au patient {} — ignorée",
                    type, patient.getId());
            return;
        }

        Notification notification = Notification.builder()
                .patient(patient)
                .type(type)
                .message(message)
                .read(false)
                .build();

        notification = notificationRepository.save(notification);
        log.info("Notification {} créée pour patient {}",
                type, patient.getId());

        dispatchToExternalChannels(notification);
    }

    // ── Dispatch vers les canaux externes ──────────────
    private void dispatchToExternalChannels(
            Notification notification) {
        for (NotificationSender sender : senders) {
            try {
                sender.send(notification);
            } catch (Exception e) {
                log.warn("Échec envoi notification {} via"
                                + " canal {} — {}",
                        notification.getId(),
                        sender.channelName(), e.getMessage());
            }
        }
    }

    // ── Mes notifications (patient connecté) ───────────
    @Transactional(readOnly = true)
    public List<NotificationResponse> getMyNotifications() {
        Patient patient = patientService.getCurrentPatient();
        return notificationMapper.toResponseList(
                notificationRepository
                        .findByPatientIdOrderBySentAtDesc(
                                patient.getId()));
    }

    // ── Mes notifications non lues ──────────────────────
    @Transactional(readOnly = true)
    public List<NotificationResponse> getMyUnreadNotifications() {
        Patient patient = patientService.getCurrentPatient();
        return notificationMapper.toResponseList(
                notificationRepository
                        .findByPatientIdAndReadFalseOrderBySentAtDesc(
                                patient.getId()));
    }

    // ── Compteur non lues (badge frontend) ──────────────
    @Transactional(readOnly = true)
    public long countMyUnread() {
        Patient patient = patientService.getCurrentPatient();
        return notificationRepository
                .countByPatientIdAndReadFalse(patient.getId());
    }

    // ── Marquer comme lue ────────────────────────────────
    @Transactional
    public void markAsRead(UUID notificationId) {
        Notification notification = notificationRepository
                .findById(notificationId)
                .orElseThrow(() -> new BusinessException(
                        "Notification introuvable"));

        Patient current = patientService.getCurrentPatient();
        if (!notification.getPatient().getId()
                .equals(current.getId())) {
            throw new BusinessException(
                    "Accès non autorisé à cette notification");
        }

        notification.setRead(true);
        notificationRepository.save(notification);
    }

    // ── Marquer toutes comme lues ───────────────────────
    @Transactional
    public void markAllAsRead() {
        Patient patient = patientService.getCurrentPatient();
        List<Notification> unread = notificationRepository
                .findByPatientIdAndReadFalseOrderBySentAtDesc(
                        patient.getId());
        unread.forEach(n -> n.setRead(true));
        notificationRepository.saveAll(unread);
        log.info("{} notification(s) marquée(s) lues pour"
                + " patient {}", unread.size(), patient.getId());
    }
}