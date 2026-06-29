package com.project.kineai.test;



import com.project.kineai.dto.request.CreatePlanRequest;
import com.project.kineai.dto.response.RehabPlanResponse;
import com.project.kineai.exception.BusinessException;
import com.project.kineai.mapper.RehabPlanMapper;
import com.project.kineai.model.entity.Exercise;
import com.project.kineai.model.entity.Patient;
import com.project.kineai.model.entity.RehabPlan;
import com.project.kineai.model.enums.*;
import com.project.kineai.repository.*;
import com.project.kineai.service.RehabPlanService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("RehabPlanService — Tests unitaires")
class RehabPlanServiceTest {

    @Mock
    private RehabPlanRepository planRepository;
    @Mock
    private PatientRepository patientRepository;
    @Mock
    private SessionRepository sessionRepository;
    @Mock
    private ExerciseRepository exerciseRepository;
    @Mock
    private PlanExerciseRepository planExerciseRepository;
    @Mock
    private RehabPlanMapper planMapper;

    @InjectMocks
    private RehabPlanService rehabPlanService;

    private Patient patient;
    private Exercise exercise;
    private UUID patientId;

    @BeforeEach
    void setUp() {
        patientId = UUID.randomUUID();
        patient = Patient.builder()
                .id(patientId)
                .fullName("Jean Dupont")
                .age(30)
                .pathology(Pathology.GENOU)
                .level(Level.DEBUTANT)
                .build();

        exercise = Exercise.builder()
                .id(UUID.randomUUID())
                .name("Flexion genou — initiation")
                .repsTarget(10)
                .build();
    }

    // ══════════════════════════════════════════
    // GENERATE PLAN
    // ══════════════════════════════════════════
    @Nested
    @DisplayName("generatePlan()")
    class GeneratePlanTests {

        @Test
        @DisplayName("Succès — archive l'ancien plan actif"
                + " et crée le nouveau — RG-17")
        void generatePlan_succes_archiveAncienPlanEtCreeNouveau() {
            CreatePlanRequest request = CreatePlanRequest
                    .builder()
                    .patientId(patientId)
                    .startDate(LocalDate.now())
                    .build();

            RehabPlan ancienPlan = RehabPlan.builder()
                    .id(UUID.randomUUID())
                    .status(Status.ACTIVE)
                    .build();

            when(patientRepository.findById(patientId))
                    .thenReturn(Optional.of(patient));
            when(planRepository.findByPatientIdAndStatus(
                    patientId, Status.ACTIVE))
                    .thenReturn(Optional.of(ancienPlan));
            when(planRepository.save(any(RehabPlan.class)))
                    .thenAnswer(inv -> inv.getArgument(0));
            when(exerciseRepository
                    .findByBodyZoneAndDifficultyLevel(
                            BodyZone.GENOU, Level.DEBUTANT))
                    .thenReturn(List.of(exercise));
            when(planMapper.toResponse(any(RehabPlan.class)))
                    .thenReturn(new RehabPlanResponse());

            rehabPlanService.generatePlan(request);

            assertThat(ancienPlan.getStatus())
                    .isEqualTo(Status.DONE);
            verify(planRepository, times(2))
                    .save(any(RehabPlan.class));
        }

        @Test
        @DisplayName("Patient introuvable — lève une"
                + " exception")
        void generatePlan_patientIntrouvable_leveException() {
            UUID inconnuId = UUID.randomUUID();
            CreatePlanRequest request = CreatePlanRequest
                    .builder()
                    .patientId(inconnuId)
                    .build();

            when(patientRepository.findById(inconnuId))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(
                    () -> rehabPlanService.generatePlan(request))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Patient introuvable");
        }

        @Test
        @DisplayName("Patient âgé de 65 ans ou plus — niveau"
                + " forcé à DÉBUTANT — RG-39")
        void generatePlan_patientAge65OuPlus_niveauForceDebutant() {
            patient.setAge(70);
            patient.setLevel(Level.AVANCE);

            CreatePlanRequest request = CreatePlanRequest
                    .builder()
                    .patientId(patientId)
                    .startDate(LocalDate.now())
                    .build();

            when(patientRepository.findById(patientId))
                    .thenReturn(Optional.of(patient));
            when(planRepository.findByPatientIdAndStatus(
                    patientId, Status.ACTIVE))
                    .thenReturn(Optional.empty());
            when(planRepository.save(any(RehabPlan.class)))
                    .thenAnswer(inv -> inv.getArgument(0));
            when(exerciseRepository
                    .findByBodyZoneAndDifficultyLevel(
                            BodyZone.GENOU, Level.DEBUTANT))
                    .thenReturn(List.of(exercise));
            when(planMapper.toResponse(any(RehabPlan.class)))
                    .thenReturn(new RehabPlanResponse());

            rehabPlanService.generatePlan(request);

            verify(planRepository).save(argThat(p ->
                    p.getDifficultyLevel() == Level.DEBUTANT));
        }

        @Test
        @DisplayName("Aucun exercice disponible pour la"
                + " zone/niveau — lève BusinessException")
        void generatePlan_aucunExerciceDisponible_leveBusinessException() {
            CreatePlanRequest request = CreatePlanRequest
                    .builder()
                    .patientId(patientId)
                    .startDate(LocalDate.now())
                    .build();

            when(patientRepository.findById(patientId))
                    .thenReturn(Optional.of(patient));
            when(planRepository.findByPatientIdAndStatus(
                    patientId, Status.ACTIVE))
                    .thenReturn(Optional.empty());
            when(planRepository.save(any(RehabPlan.class)))
                    .thenAnswer(inv -> inv.getArgument(0));
            when(exerciseRepository
                    .findByBodyZoneAndDifficultyLevel(
                            BodyZone.GENOU, Level.DEBUTANT))
                    .thenReturn(List.of());

            assertThatThrownBy(
                    () -> rehabPlanService.generatePlan(request))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining(
                            "Aucun exercice disponible");
        }
    }

    // ══════════════════════════════════════════
    // CHECK PROGRESSION — RG-19/RG-20
    // ══════════════════════════════════════════
    @Nested
    @DisplayName("checkProgression()")
    class CheckProgressionTests {

        @Test
        @DisplayName("Moins de 3 séances complétées — ne"
                + " fait rien")
        void checkProgression_moinsDe3Seances_neFaitRienRG19() {
            when(sessionRepository
                    .countCompletedSessions(patientId))
                    .thenReturn(2L);

            rehabPlanService.checkProgression(patientId);

            verify(planRepository, never())
                    .findByPatientIdAndStatus(any(), any());
        }

        @Test
        @DisplayName("Score moyen > 85% — progresse d'un"
                + " niveau — RG-19")
        void checkProgression_scoreSuperieur85_progresseDeNiveau() {
            RehabPlan plan = RehabPlan.builder()
                    .id(UUID.randomUUID())
                    .difficultyLevel(Level.DEBUTANT)
                    .build();

            when(sessionRepository
                    .countCompletedSessions(patientId))
                    .thenReturn(3L);
            when(sessionRepository
                    .getAverageScoreLastThreeSessions(patientId))
                    .thenReturn(90.0);
            when(patientRepository.findById(patientId))
                    .thenReturn(Optional.of(patient));
            when(planRepository.findByPatientIdAndStatus(
                    patientId, Status.ACTIVE))
                    .thenReturn(Optional.of(plan));

            rehabPlanService.checkProgression(patientId);

            assertThat(plan.getDifficultyLevel())
                    .isEqualTo(Level.INTERMEDIAIRE);
            assertThat(patient.getLevel())
                    .isEqualTo(Level.INTERMEDIAIRE);
            verify(planRepository).save(plan);
            verify(patientRepository).save(patient);
        }

        @Test
        @DisplayName("Score moyen < 50% — régresse d'un"
                + " niveau — RG-20")
        void checkProgression_scoreInferieur50_regresseDeNiveau() {
            RehabPlan plan = RehabPlan.builder()
                    .id(UUID.randomUUID())
                    .difficultyLevel(Level.INTERMEDIAIRE)
                    .build();

            when(sessionRepository
                    .countCompletedSessions(patientId))
                    .thenReturn(3L);
            when(sessionRepository
                    .getAverageScoreLastThreeSessions(patientId))
                    .thenReturn(40.0);
            when(patientRepository.findById(patientId))
                    .thenReturn(Optional.of(patient));
            when(planRepository.findByPatientIdAndStatus(
                    patientId, Status.ACTIVE))
                    .thenReturn(Optional.of(plan));

            rehabPlanService.checkProgression(patientId);

            assertThat(plan.getDifficultyLevel())
                    .isEqualTo(Level.DEBUTANT);
            assertThat(patient.getLevel())
                    .isEqualTo(Level.DEBUTANT);
        }

        @Test
        @DisplayName("Score moyen entre 50% et 85% — aucun"
                + " changement de niveau")
        void checkProgression_scoreEntre50Et85_neChangeRien() {
            RehabPlan plan = RehabPlan.builder()
                    .id(UUID.randomUUID())
                    .difficultyLevel(Level.INTERMEDIAIRE)
                    .build();

            when(sessionRepository
                    .countCompletedSessions(patientId))
                    .thenReturn(3L);
            when(sessionRepository
                    .getAverageScoreLastThreeSessions(patientId))
                    .thenReturn(65.0);
            when(patientRepository.findById(patientId))
                    .thenReturn(Optional.of(patient));
            when(planRepository.findByPatientIdAndStatus(
                    patientId, Status.ACTIVE))
                    .thenReturn(Optional.of(plan));

            rehabPlanService.checkProgression(patientId);

            assertThat(plan.getDifficultyLevel())
                    .isEqualTo(Level.INTERMEDIAIRE);
            verify(planRepository, never()).save(any());
        }

        @Test
        @DisplayName("Déjà au niveau AVANCÉ — ne progresse"
                + " pas davantage — RG-21")
        void checkProgression_dejaAuNiveauMax_neProgressePasPlus() {
            RehabPlan plan = RehabPlan.builder()
                    .id(UUID.randomUUID())
                    .difficultyLevel(Level.AVANCE)
                    .build();

            when(sessionRepository
                    .countCompletedSessions(patientId))
                    .thenReturn(3L);
            when(sessionRepository
                    .getAverageScoreLastThreeSessions(patientId))
                    .thenReturn(95.0);
            when(patientRepository.findById(patientId))
                    .thenReturn(Optional.of(patient));
            when(planRepository.findByPatientIdAndStatus(
                    patientId, Status.ACTIVE))
                    .thenReturn(Optional.of(plan));

            rehabPlanService.checkProgression(patientId);

            assertThat(plan.getDifficultyLevel())
                    .isEqualTo(Level.AVANCE);
            verify(planRepository, never()).save(any());
        }
    }

    // ══════════════════════════════════════════
    // CHECK WEEK ADVANCEMENT — RG-22/RG-67
    // ══════════════════════════════════════════
    @Nested
    @DisplayName("checkWeekAdvancement()")
    class CheckWeekAdvancementTests {

        @Test
        @DisplayName("3 séances complétées cette semaine —"
                + " avance à la semaine suivante — RG-67")
        void checkWeekAdvancement_3SeancesCompletees_avanceSemaineSuivante() {
            UUID planId = UUID.randomUUID();
            RehabPlan plan = RehabPlan.builder()
                    .id(planId)
                    .patient(patient)
                    .status(Status.ACTIVE)
                    .startDate(LocalDate.now())
                    .currentWeek(1)
                    .build();

            when(planRepository.findById(planId))
                    .thenReturn(Optional.of(plan));
            when(sessionRepository
                    .countByRehabPlanIdAndSessionStatusAndStartTimeBetween(
                            eq(planId), eq(SessionStatus.COMPLETED),
                            any(), any()))
                    .thenReturn(3L);
            when(exerciseRepository
                    .findByBodyZoneAndDifficultyLevel(
                            any(), any()))
                    .thenReturn(List.of(exercise));

            rehabPlanService.checkWeekAdvancement(planId);

            assertThat(plan.getCurrentWeek()).isEqualTo(2);
            assertThat(plan.getStatus())
                    .isEqualTo(Status.ACTIVE);
        }

        @Test
        @DisplayName("4ème semaine complétée — clôture le"
                + " plan — RG-22")
        void checkWeekAdvancement_derniereSemaineComplete_clotureLePlan() {
            UUID planId = UUID.randomUUID();
            RehabPlan plan = RehabPlan.builder()
                    .id(planId)
                    .patient(patient)
                    .status(Status.ACTIVE)
                    .startDate(LocalDate.now())
                    .currentWeek(4)
                    .build();

            when(planRepository.findById(planId))
                    .thenReturn(Optional.of(plan));
            when(sessionRepository
                    .countByRehabPlanIdAndSessionStatusAndStartTimeBetween(
                            eq(planId), eq(SessionStatus.COMPLETED),
                            any(), any()))
                    .thenReturn(3L);

            rehabPlanService.checkWeekAdvancement(planId);

            assertThat(plan.getStatus())
                    .isEqualTo(Status.DONE);
            verify(exerciseRepository, never())
                    .findByBodyZoneAndDifficultyLevel(
                            any(), any());
        }

        @Test
        @DisplayName("Moins de 3 séances cette semaine —"
                + " aucun changement")
        void checkWeekAdvancement_moinsDe3Seances_neChangeRien() {
            UUID planId = UUID.randomUUID();
            RehabPlan plan = RehabPlan.builder()
                    .id(planId)
                    .status(Status.ACTIVE)
                    .startDate(LocalDate.now())
                    .currentWeek(1)
                    .build();

            when(planRepository.findById(planId))
                    .thenReturn(Optional.of(plan));
            when(sessionRepository
                    .countByRehabPlanIdAndSessionStatusAndStartTimeBetween(
                            eq(planId), eq(SessionStatus.COMPLETED),
                            any(), any()))
                    .thenReturn(2L);

            rehabPlanService.checkWeekAdvancement(planId);

            assertThat(plan.getCurrentWeek()).isEqualTo(1);
            verify(planRepository, never()).save(any());
        }

        @Test
        @DisplayName("Plan inactif (DONE) — ne fait rien")
        void checkWeekAdvancement_planInactif_neFaitRien() {
            UUID planId = UUID.randomUUID();
            RehabPlan plan = RehabPlan.builder()
                    .id(planId)
                    .status(Status.DONE)
                    .currentWeek(2)
                    .build();

            when(planRepository.findById(planId))
                    .thenReturn(Optional.of(plan));

            rehabPlanService.checkWeekAdvancement(planId);

            verify(sessionRepository, never())
                    .countByRehabPlanIdAndSessionStatusAndStartTimeBetween(
                            any(), any(), any(), any());
        }
    }
}
