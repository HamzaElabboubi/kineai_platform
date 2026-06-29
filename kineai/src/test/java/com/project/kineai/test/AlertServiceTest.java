package com.project.kineai.test;


import com.project.kineai.dto.response.AlertResponse;
import com.project.kineai.exception.BusinessException;
import com.project.kineai.mapper.AlertMapper;
import com.project.kineai.model.entity.Alert;
import com.project.kineai.model.entity.Kinesitherapeute;
import com.project.kineai.model.entity.Patient;
import com.project.kineai.model.enums.AlertType;
import com.project.kineai.repository.AlertRepository;
import com.project.kineai.service.AlertService;
import com.project.kineai.service.KineService;
import com.project.kineai.service.PatientService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AlertService — Tests unitaires")
class AlertServiceTest {

    @Mock
    private AlertRepository alertRepository;
    @Mock
    private AlertMapper alertMapper;
    @Mock
    private PatientService patientService;
    @Mock
    private KineService kineService;

    @InjectMocks
    private AlertService alertService;

    private Patient patient;
    private Kinesitherapeute kine;
    private Alert alert;
    private UUID alertId;

    @BeforeEach
    void setUp() {
        patient = Patient.builder()
                .id(UUID.randomUUID())
                .fullName("Jean Dupont")
                .build();

        kine = Kinesitherapeute.builder()
                .id(UUID.randomUUID())
                .fullName("Dr. Martin")
                .build();

        alertId = UUID.randomUUID();
        alert = Alert.builder()
                .id(alertId)
                .patient(patient)
                .kinesitherapeute(kine)
                .type(AlertType.INACTIVITY)
                .message("Aucune séance depuis 3 jours")
                .resolved(false)
                .build();
    }

    // ══════════════════════════════════════════
    // CREATE ALERT — RG-26
    // ══════════════════════════════════════════
    @Nested
    @DisplayName("createAlert()")
    class CreateAlertTests {

        @Test
        @DisplayName("Alerte du même type déjà en attente —"
                + " ignore la création — RG-26")
        void createAlert_doublonExistant_neCreeRien() {
            when(alertRepository
                    .existsByPatientIdAndTypeAndResolvedFalse(
                            patient.getId(),
                            AlertType.INACTIVITY))
                    .thenReturn(true);

            alertService.createAlert(patient, kine,
                    AlertType.INACTIVITY,
                    "Aucune séance depuis 3 jours");

            verify(alertRepository, never()).save(any());
        }

        @Test
        @DisplayName("Nouvelle alerte — sauvegardée"
                + " correctement")
        void createAlert_nouvelleAlerte_sauvegardeCorrectement() {
            when(alertRepository
                    .existsByPatientIdAndTypeAndResolvedFalse(
                            patient.getId(),
                            AlertType.SCORE))
                    .thenReturn(false);

            alertService.createAlert(patient, kine,
                    AlertType.SCORE,
                    "Score moyen inférieur à 60%");

            verify(alertRepository).save(argThat(a ->
                    a.getType() == AlertType.SCORE
                            && a.getPatient().equals(patient)
                            && a.getKinesitherapeute()
                            .equals(kine)
                            && !a.getResolved()));
        }
    }

    // ══════════════════════════════════════════
    // GET MY ALERTS (kiné)
    // ══════════════════════════════════════════
    @Nested
    @DisplayName("getMyAlerts()")
    class GetMyAlertsTests {

        @Test
        @DisplayName("Retourne les alertes non résolues"
                + " du kiné connecté")
        void getMyAlerts_retourneAlertesNonResoluesDuKineConnecte() {
            when(kineService.getCurrentKine())
                    .thenReturn(kine);
            when(alertRepository
                    .findByKinesitherapeuteIdAndResolvedFalse(
                            kine.getId()))
                    .thenReturn(List.of(alert));
            when(alertMapper.toResponseList(List.of(alert)))
                    .thenReturn(List.of(new AlertResponse()));

            List<AlertResponse> result =
                    alertService.getMyAlerts();

            assertThat(result).hasSize(1);
        }
    }

    // ══════════════════════════════════════════
    // RESOLVE ALERT — RG-28
    // ══════════════════════════════════════════
    @Nested
    @DisplayName("resolveAlert()")
    class ResolveAlertTests {

        @Test
        @DisplayName("Résolution réussie — passe"
                + " resolved à true — RG-28")
        void resolveAlert_succes_passeResolvedATrue() {
            when(alertRepository.findById(alertId))
                    .thenReturn(Optional.of(alert));
            when(alertRepository.save(any(Alert.class)))
                    .thenAnswer(inv -> inv.getArgument(0));
            when(alertMapper.toResponse(any(Alert.class)))
                    .thenReturn(new AlertResponse());

            alertService.resolveAlert(alertId);

            assertThat(alert.getResolved()).isTrue();
            verify(alertRepository).save(alert);
        }

        @Test
        @DisplayName("Alerte introuvable — lève"
                + " BusinessException")
        void resolveAlert_alerteIntrouvable_leveBusinessException() {
            UUID inconnuId = UUID.randomUUID();
            when(alertRepository.findById(inconnuId))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(
                    () -> alertService.resolveAlert(inconnuId))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("Alerte introuvable");
        }
    }

    // ══════════════════════════════════════════
    // COUNT PENDING ALERTS
    // ══════════════════════════════════════════
    @Nested
    @DisplayName("countPendingAlerts()")
    class CountPendingAlertsTests {

        @Test
        @DisplayName("Retourne le comptage exact des"
                + " alertes non résolues")
        void countPendingAlerts_retourneLeComptageCorrect() {
            when(kineService.getCurrentKine())
                    .thenReturn(kine);
            when(alertRepository
                    .countByKinesitherapeuteIdAndResolvedFalse(
                            kine.getId()))
                    .thenReturn(5L);

            long count = alertService.countPendingAlerts();

            assertThat(count).isEqualTo(5);
        }
    }

    // ══════════════════════════════════════════
    // GET MY ALERTS AS PATIENT
    // ══════════════════════════════════════════
    @Nested
    @DisplayName("getMyAlertsAsPatient()")
    class GetMyAlertsAsPatientTests {

        @Test
        @DisplayName("Retourne toutes les alertes du"
                + " patient connecté — RG-53")
        void getMyAlertsAsPatient_retourneToutesLesAlertesDuPatient() {
            when(patientService.getCurrentPatient())
                    .thenReturn(patient);
            when(alertRepository
                    .findByPatientIdOrderBySentAtDesc(
                            patient.getId()))
                    .thenReturn(List.of(alert));
            when(alertMapper.toResponseList(List.of(alert)))
                    .thenReturn(List.of(new AlertResponse()));

            List<AlertResponse> result =
                    alertService.getMyAlertsAsPatient();

            assertThat(result).hasSize(1);
        }
    }

    // ══════════════════════════════════════════
    // GET ALL MY ALERTS (historique complet kiné)
    // ══════════════════════════════════════════
    @Nested
    @DisplayName("getAllMyAlerts()")
    class GetAllMyAlertsTests {

        @Test
        @DisplayName("Retourne l'historique complet des"
                + " alertes du kiné — résolues et"
                + " non résolues")
        void getAllMyAlerts_retourneHistoriqueCompletDuKine() {
            Alert alerteResolue = Alert.builder()
                    .id(UUID.randomUUID())
                    .patient(patient)
                    .kinesitherapeute(kine)
                    .type(AlertType.SCORE)
                    .message("Score faible")
                    .resolved(true)
                    .build();

            when(kineService.getCurrentKine())
                    .thenReturn(kine);
            when(alertRepository
                    .findByKinesitherapeuteIdOrderBySentAtDesc(
                            kine.getId()))
                    .thenReturn(List.of(alert,
                            alerteResolue));
            when(alertMapper.toResponseList(
                    List.of(alert, alerteResolue)))
                    .thenReturn(List.of(
                            new AlertResponse(),
                            new AlertResponse()));

            List<AlertResponse> result =
                    alertService.getAllMyAlerts();

            assertThat(result).hasSize(2);
        }
    }
}