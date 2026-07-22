package com.project.kineai.test;

import com.project.kineai.dto.response.NotificationResponse;
import com.project.kineai.exception.BusinessException;
import com.project.kineai.mapper.NotificationMapper;
import com.project.kineai.model.entity.Notification;
import com.project.kineai.model.entity.Patient;
import com.project.kineai.model.enums.NotificationType;
import com.project.kineai.repository.NotificationRepository;
import com.project.kineai.service.NotificationService;
import com.project.kineai.service.PatientService;
import com.project.kineai.service.notification.NotificationSender;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("NotificationService — Tests unitaires")
class NotificationServiceTest {

    @Mock
    private NotificationRepository notificationRepository;
    @Mock
    private NotificationMapper notificationMapper;
    @Mock
    private PatientService patientService;
    @Mock
    private NotificationSender emailSender;

    // ✅ Pas d'@InjectMocks ici — le constructeur attend
    // List<NotificationSender>, que Mockito ne résout pas
    // automatiquement depuis un @Mock unique. Construction
    // manuelle nécessaire.
    private NotificationService notificationService;

    private Patient patient;
    private UUID patientId;

    @BeforeEach
    void setUp() {
        notificationService = new NotificationService(
                notificationRepository,
                notificationMapper,
                patientService,
                List.of(emailSender));

        patientId = UUID.randomUUID();
        patient = Patient.builder()
                .id(patientId)
                .fullName("Jean Dupont")
                .streakCount(0)
                .totalXp(0)
                .build();
    }

    // ══════════════════════════════════════════
    // CREATE NOTIFICATION — RG anti-doublon
    // ══════════════════════════════════════════
    @Nested
    @DisplayName("createNotification()")
    class CreateNotificationTests {

        @Test
        @DisplayName("Succès — sauvegarde et dispatch vers"
                + " les canaux externes")
        void createNotification_succes_sauvegardeEtDispatch() {
            when(notificationRepository
                    .existsByPatientIdAndTypeAndSentAtAfter(
                            eq(patientId),
                            eq(NotificationType.SESSION_REMINDER),
                            any(LocalDateTime.class)))
                    .thenReturn(false);
            when(notificationRepository.save(any(Notification.class)))
                    .thenAnswer(inv -> inv.getArgument(0));

            notificationService.createNotification(
                    patient, NotificationType.SESSION_REMINDER,
                    "Rappel de séance", LocalDateTime.now());

            verify(notificationRepository, times(1))
                    .save(any(Notification.class));
            verify(emailSender, times(1))
                    .send(any(Notification.class));
        }

        @Test
        @DisplayName("Doublon récent existant — n'envoie"
                + " rien (anti-spam)")
        void createNotification_doublonRecent_neFaitRien() {
            when(notificationRepository
                    .existsByPatientIdAndTypeAndSentAtAfter(
                            eq(patientId),
                            eq(NotificationType.SESSION_REMINDER),
                            any(LocalDateTime.class)))
                    .thenReturn(true);

            notificationService.createNotification(
                    patient, NotificationType.SESSION_REMINDER,
                    "Rappel de séance", LocalDateTime.now());

            verify(notificationRepository, never())
                    .save(any(Notification.class));
            verify(emailSender, never())
                    .send(any(Notification.class));
        }

        @Test
        @DisplayName("Échec d'un canal externe — ne bloque"
                + " pas la création in-app")
        void createNotification_echecCanalExterne_neBloquePasSauvegarde() {
            when(notificationRepository
                    .existsByPatientIdAndTypeAndSentAtAfter(
                            any(), any(), any()))
                    .thenReturn(false);

            Notification saved = Notification.builder()
                    .id(UUID.randomUUID())
                    .patient(patient)
                    .type(NotificationType.SESSION_REMINDER)
                    .message("Rappel")
                    .read(false)
                    .build();
            when(notificationRepository.save(any(Notification.class)))
                    .thenReturn(saved);

            doThrow(new RuntimeException("SMTP indisponible"))
                    .when(emailSender).send(any(Notification.class));

            notificationService.createNotification(
                    patient, NotificationType.SESSION_REMINDER,
                    "Rappel", LocalDateTime.now());

            verify(notificationRepository, times(1)).save(any());
        }
    }

    // ══════════════════════════════════════════
    // GET MY NOTIFICATIONS
    // ══════════════════════════════════════════
    @Nested
    @DisplayName("getMyNotifications() / getMyUnreadNotifications()")
    class GetNotificationsTests {

        @Test
        @DisplayName("Retourne l'historique complet mappé")
        void getMyNotifications_retourneHistoriqueMappe() {
            when(patientService.getCurrentPatient())
                    .thenReturn(patient);

            Notification notif = Notification.builder()
                    .id(UUID.randomUUID())
                    .patient(patient)
                    .type(NotificationType.BADGE_UNLOCKED)
                    .message("Bravo !")
                    .read(false)
                    .build();

            when(notificationRepository
                    .findByPatientIdOrderBySentAtDesc(patientId))
                    .thenReturn(List.of(notif));

            NotificationResponse response = NotificationResponse
                    .builder()
                    .id(notif.getId())
                    .type(NotificationType.BADGE_UNLOCKED)
                    .message("Bravo !")
                    .read(false)
                    .build();

            when(notificationMapper.toResponseList(List.of(notif)))
                    .thenReturn(List.of(response));

            List<NotificationResponse> result =
                    notificationService.getMyNotifications();

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getMessage())
                    .isEqualTo("Bravo !");
        }

        @Test
        @DisplayName("countMyUnread — retourne le compteur"
                + " du repository")
        void countMyUnread_retourneCompteurRepository() {
            when(patientService.getCurrentPatient())
                    .thenReturn(patient);
            when(notificationRepository
                    .countByPatientIdAndReadFalse(patientId))
                    .thenReturn(3L);

            long count = notificationService.countMyUnread();

            assertThat(count).isEqualTo(3L);
        }
    }

    // ══════════════════════════════════════════
    // MARK AS READ — sécurité
    // ══════════════════════════════════════════
    @Nested
    @DisplayName("markAsRead()")
    class MarkAsReadTests {

        @Test
        @DisplayName("Succès — notification appartient au"
                + " patient connecté")
        void markAsRead_succes_notificationAppartientAuPatient() {
            Notification notif = Notification.builder()
                    .id(UUID.randomUUID())
                    .patient(patient)
                    .type(NotificationType.SESSION_REMINDER)
                    .message("Rappel")
                    .read(false)
                    .build();

            when(notificationRepository.findById(notif.getId()))
                    .thenReturn(Optional.of(notif));
            when(patientService.getCurrentPatient())
                    .thenReturn(patient);

            notificationService.markAsRead(notif.getId());

            assertThat(notif.getRead()).isTrue();
            verify(notificationRepository).save(notif);
        }

        @Test
        @DisplayName("Notification d'un autre patient —"
                + " lève BusinessException")
        void markAsRead_notificationAutrePatient_leveBusinessException() {
            Patient autrePatient = Patient.builder()
                    .id(UUID.randomUUID())
                    .fullName("Autre Patient")
                    .build();

            Notification notif = Notification.builder()
                    .id(UUID.randomUUID())
                    .patient(autrePatient)
                    .type(NotificationType.SESSION_REMINDER)
                    .message("Rappel")
                    .read(false)
                    .build();

            when(notificationRepository.findById(notif.getId()))
                    .thenReturn(Optional.of(notif));
            when(patientService.getCurrentPatient())
                    .thenReturn(patient);

            assertThatThrownBy(() ->
                    notificationService.markAsRead(notif.getId()))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("non autorisé");

            verify(notificationRepository, never()).save(any());
        }

        @Test
        @DisplayName("Notification introuvable — lève"
                + " BusinessException")
        void markAsRead_notificationIntrouvable_leveBusinessException() {
            UUID idInconnu = UUID.randomUUID();
            when(notificationRepository.findById(idInconnu))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() ->
                    notificationService.markAsRead(idInconnu))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("introuvable");
        }
    }

    // ══════════════════════════════════════════
    // MARK ALL AS READ
    // ══════════════════════════════════════════
    @Nested
    @DisplayName("markAllAsRead()")
    class MarkAllAsReadTests {

        @Test
        @DisplayName("Marque toutes les notifications non"
                + " lues du patient connecté")
        void markAllAsRead_marqueToutesLesNonLues() {
            when(patientService.getCurrentPatient())
                    .thenReturn(patient);

            Notification n1 = Notification.builder()
                    .id(UUID.randomUUID()).patient(patient)
                    .type(NotificationType.SESSION_REMINDER)
                    .message("A").read(false).build();
            Notification n2 = Notification.builder()
                    .id(UUID.randomUUID()).patient(patient)
                    .type(NotificationType.STREAK_AT_RISK)
                    .message("B").read(false).build();

            when(notificationRepository
                    .findByPatientIdAndReadFalseOrderBySentAtDesc(
                            patientId))
                    .thenReturn(List.of(n1, n2));

            notificationService.markAllAsRead();

            assertThat(n1.getRead()).isTrue();
            assertThat(n2.getRead()).isTrue();
            verify(notificationRepository).saveAll(List.of(n1, n2));
        }
    }
}