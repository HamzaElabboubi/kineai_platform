package com.project.kineai.test;


import com.project.kineai.dto.response.ExerciseResponse;
import com.project.kineai.exception.BusinessException;
import com.project.kineai.mapper.ExerciseMapper;
import com.project.kineai.model.entity.*;
import com.project.kineai.model.enums.*;
import com.project.kineai.repository.*;
import com.project.kineai.service.ExerciseService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ExerciseService — Tests unitaires")
class ExerciseServiceTest {

    @Mock
    private ExerciseRepository exerciseRepository;
    @Mock
    private PatientRepository patientRepository;
    @Mock
    private RehabPlanRepository rehabPlanRepository;
    @Mock
    private PlanExerciseRepository planExerciseRepository;
    @Mock
    private SessionRepository sessionRepository;
    @Mock
    private ExerciseMapper exerciseMapper;

    @InjectMocks
    private ExerciseService exerciseService;

    private Patient patient;
    private Exercise exercise;
    private RehabPlan activePlan;
    private MockedStatic<SecurityContextHolder>
            securityContextHolderMock;

    @BeforeEach
    void setUp() {
        patient = Patient.builder()
                .id(UUID.randomUUID())
                .fullName("Jean Dupont")
                .level(Level.DEBUTANT)
                .build();

        exercise = Exercise.builder()
                .id(UUID.randomUUID())
                .name("Flexion genou")
                .toleranceDegree(15)
                .repsTarget(10)
                .build();

        activePlan = RehabPlan.builder()
                .id(UUID.randomUUID())
                .patient(patient)
                .startDate(LocalDate.now().minusDays(2))
                .currentWeek(1)
                .status(Status.ACTIVE)
                .build();



    }

    @AfterEach
    void tearDown() {
        if (securityContextHolderMock != null) {
            securityContextHolderMock.close();
        }
    }

    // ══════════════════════════════════════════
    // GET ALL EXERCISES / GET BY BODY ZONE
    // ══════════════════════════════════════════
    @Nested
    @DisplayName("getAllExercises() / getByBodyZone()")
    class SimpleQueriesTests {

        @Test
        @DisplayName("getAllExercises — retourne tous"
                + " les exercices")
        void getAllExercises_retourneTousLesExercices() {
            when(exerciseRepository.findAll())
                    .thenReturn(List.of(exercise));
            when(exerciseMapper.toResponse(exercise))
                    .thenReturn(new ExerciseResponse());

            List<ExerciseResponse> result =
                    exerciseService.getAllExercises();

            assertThat(result).hasSize(1);
        }

        @Test
        @DisplayName("getByBodyZone — retourne les"
                + " exercices filtrés par zone")
        void getByBodyZone_retourneExercicesFiltresParZone() {
            when(exerciseRepository
                    .findByBodyZone(BodyZone.GENOU))
                    .thenReturn(List.of(exercise));
            when(exerciseMapper.toResponse(exercise))
                    .thenReturn(new ExerciseResponse());

            List<ExerciseResponse> result =
                    exerciseService
                            .getByBodyZone(BodyZone.GENOU);

            assertThat(result).hasSize(1);
        }
    }

    // ══════════════════════════════════════════
    // GET MY EXERCISES — système expert
    // ══════════════════════════════════════════
    @Nested
    @DisplayName("getMyExercises()")
    class GetMyExercisesTests {
        @BeforeEach
        void setUpSecurityContext() {
            Authentication authentication =
                    mock(Authentication.class);
            when(authentication.getName())
                    .thenReturn("patient@test.com");

            SecurityContext securityContext =
                    mock(SecurityContext.class);
            when(securityContext.getAuthentication())
                    .thenReturn(authentication);

            securityContextHolderMock =
                    mockStatic(SecurityContextHolder.class);
            securityContextHolderMock
                    .when(SecurityContextHolder::getContext)
                    .thenReturn(securityContext);
        }

        @Test
        @DisplayName("Patient introuvable — lève"
                + " BusinessException")
        void getMyExercises_patientIntrouvable_leveBusinessException() {
            when(patientRepository
                    .findByUser_Email("patient@test.com"))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(
                    () -> exerciseService.getMyExercises())
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining(
                            "Patient introuvable");
        }

        @Test
        @DisplayName("Aucun plan actif — lève"
                + " BusinessException")
        void getMyExercises_aucunPlanActif_leveBusinessException() {
            when(patientRepository
                    .findByUser_Email("patient@test.com"))
                    .thenReturn(Optional.of(patient));
            when(rehabPlanRepository
                    .findByPatientIdAndStatus(
                            patient.getId(), Status.ACTIVE))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(
                    () -> exerciseService.getMyExercises())
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("Aucun plan actif");
        }

        @Test
        @DisplayName("3 séances déjà complétées cette"
                + " semaine — lève BusinessException"
                + " — RG-66")
        void getMyExercises_3SeancesDejaCompleteesCetteSemaine_leveBusinessException() {
            when(patientRepository
                    .findByUser_Email("patient@test.com"))
                    .thenReturn(Optional.of(patient));
            when(rehabPlanRepository
                    .findByPatientIdAndStatus(
                            patient.getId(), Status.ACTIVE))
                    .thenReturn(Optional.of(activePlan));
            when(sessionRepository
                    .countByRehabPlanIdAndSessionStatusAndStartTimeBetween(
                            eq(activePlan.getId()),
                            eq(SessionStatus.COMPLETED),
                            any(), any()))
                    .thenReturn(3L);

            assertThatThrownBy(
                    () -> exerciseService.getMyExercises())
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining(
                            "terminé toutes les séances");
        }

        @Test
        @DisplayName("Moins de 20h depuis la dernière"
                + " séance — lève BusinessException"
                + " — RG-65")
        void getMyExercises_moinsDe20hDepuisDerniereSeance_leveBusinessException() {
            Session recentSession = Session.builder()
                    .id(UUID.randomUUID())
                    .startTime(LocalDateTime.now()
                            .minusHours(5))
                    .sessionStatus(SessionStatus.COMPLETED)
                    .build();

            when(patientRepository
                    .findByUser_Email("patient@test.com"))
                    .thenReturn(Optional.of(patient));
            when(rehabPlanRepository
                    .findByPatientIdAndStatus(
                            patient.getId(), Status.ACTIVE))
                    .thenReturn(Optional.of(activePlan));
            when(sessionRepository
                    .countByRehabPlanIdAndSessionStatusAndStartTimeBetween(
                            eq(activePlan.getId()),
                            eq(SessionStatus.COMPLETED),
                            any(), any()))
                    .thenReturn(0L);
            when(sessionRepository
                    .findFirstByPatientIdAndSessionStatusOrderByStartTimeDesc(
                            patient.getId(),
                            SessionStatus.COMPLETED))
                    .thenReturn(Optional.of(recentSession));

            assertThatThrownBy(
                    () -> exerciseService.getMyExercises())
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining(
                            "trop récente");
        }

        @Test
        @DisplayName("Aucun exercice prévu pour ce"
                + " jour — lève BusinessException")
        void getMyExercises_aucunExercicePrevuPourCeJour_leveBusinessException() {
            when(patientRepository
                    .findByUser_Email("patient@test.com"))
                    .thenReturn(Optional.of(patient));
            when(rehabPlanRepository
                    .findByPatientIdAndStatus(
                            patient.getId(), Status.ACTIVE))
                    .thenReturn(Optional.of(activePlan));
            when(sessionRepository
                    .countByRehabPlanIdAndSessionStatusAndStartTimeBetween(
                            eq(activePlan.getId()),
                            eq(SessionStatus.COMPLETED),
                            any(), any()))
                    .thenReturn(0L);
            when(sessionRepository
                    .findFirstByPatientIdAndSessionStatusOrderByStartTimeDesc(
                            patient.getId(),
                            SessionStatus.COMPLETED))
                    .thenReturn(Optional.empty());
            when(planExerciseRepository
                    .findByRehabPlanIdAndWeekNumberAndDayOfWeek(
                            eq(activePlan.getId()), eq(1),
                            eq(SessionDay.LUNDI)))
                    .thenReturn(Collections.emptyList());

            assertThatThrownBy(
                    () -> exerciseService.getMyExercises())
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining(
                            "Aucun exercice prévu");
        }

        @Test
        @DisplayName("Succès niveau DÉBUTANT — reps et"
                + " tolérance de base — RG-69")
        void getMyExercises_succes_niveauDebutant() {
            PlanExercise planExercise = PlanExercise.builder()
                    .id(UUID.randomUUID())
                    .exercise(exercise)
                    .repsPrescribed(12)
                    .build();

            ExerciseResponse baseResponse =
                    new ExerciseResponse();

            when(patientRepository
                    .findByUser_Email("patient@test.com"))
                    .thenReturn(Optional.of(patient));
            when(rehabPlanRepository
                    .findByPatientIdAndStatus(
                            patient.getId(), Status.ACTIVE))
                    .thenReturn(Optional.of(activePlan));
            when(sessionRepository
                    .countByRehabPlanIdAndSessionStatusAndStartTimeBetween(
                            eq(activePlan.getId()),
                            eq(SessionStatus.COMPLETED),
                            any(), any()))
                    .thenReturn(0L);
            when(sessionRepository
                    .findFirstByPatientIdAndSessionStatusOrderByStartTimeDesc(
                            patient.getId(),
                            SessionStatus.COMPLETED))
                    .thenReturn(Optional.empty());
            when(planExerciseRepository
                    .findByRehabPlanIdAndWeekNumberAndDayOfWeek(
                            eq(activePlan.getId()), eq(1),
                            eq(SessionDay.LUNDI)))
                    .thenReturn(List.of(planExercise));
            when(exerciseMapper.toResponse(exercise))
                    .thenReturn(baseResponse);

            List<ExerciseResponse> result =
                    exerciseService.getMyExercises();

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getRepsTarget())
                    .isEqualTo(12);
            assertThat(result.get(0).getToleranceDeg())
                    .isEqualTo(15);
        }

        @Test
        @DisplayName("Succès niveau INTERMÉDIAIRE — reps"
                + " +5, tolérance -5 (min 8) — RG-69")
        void getMyExercises_succes_niveauIntermediaire() {
            patient.setLevel(Level.INTERMEDIAIRE);

            PlanExercise planExercise = PlanExercise.builder()
                    .id(UUID.randomUUID())
                    .exercise(exercise)
                    .repsPrescribed(12)
                    .build();

            when(patientRepository
                    .findByUser_Email("patient@test.com"))
                    .thenReturn(Optional.of(patient));
            when(rehabPlanRepository
                    .findByPatientIdAndStatus(
                            patient.getId(), Status.ACTIVE))
                    .thenReturn(Optional.of(activePlan));
            when(sessionRepository
                    .countByRehabPlanIdAndSessionStatusAndStartTimeBetween(
                            eq(activePlan.getId()),
                            eq(SessionStatus.COMPLETED),
                            any(), any()))
                    .thenReturn(0L);
            when(sessionRepository
                    .findFirstByPatientIdAndSessionStatusOrderByStartTimeDesc(
                            patient.getId(),
                            SessionStatus.COMPLETED))
                    .thenReturn(Optional.empty());
            when(planExerciseRepository
                    .findByRehabPlanIdAndWeekNumberAndDayOfWeek(
                            eq(activePlan.getId()), eq(1),
                            eq(SessionDay.LUNDI)))
                    .thenReturn(List.of(planExercise));
            when(exerciseMapper.toResponse(exercise))
                    .thenReturn(new ExerciseResponse());

            List<ExerciseResponse> result =
                    exerciseService.getMyExercises();

            assertThat(result.get(0).getRepsTarget())
                    .isEqualTo(17); // 12 + 5
            assertThat(result.get(0).getToleranceDeg())
                    .isEqualTo(10); // 15 - 5
        }

        @Test
        @DisplayName("Succès niveau AVANCÉ — reps +10,"
                + " tolérance -9 (min 5) — RG-69")
        void getMyExercises_succes_niveauAvance() {
            patient.setLevel(Level.AVANCE);

            PlanExercise planExercise = PlanExercise.builder()
                    .id(UUID.randomUUID())
                    .exercise(exercise)
                    .repsPrescribed(12)
                    .build();

            when(patientRepository
                    .findByUser_Email("patient@test.com"))
                    .thenReturn(Optional.of(patient));
            when(rehabPlanRepository
                    .findByPatientIdAndStatus(
                            patient.getId(), Status.ACTIVE))
                    .thenReturn(Optional.of(activePlan));
            when(sessionRepository
                    .countByRehabPlanIdAndSessionStatusAndStartTimeBetween(
                            eq(activePlan.getId()),
                            eq(SessionStatus.COMPLETED),
                            any(), any()))
                    .thenReturn(0L);
            when(sessionRepository
                    .findFirstByPatientIdAndSessionStatusOrderByStartTimeDesc(
                            patient.getId(),
                            SessionStatus.COMPLETED))
                    .thenReturn(Optional.empty());
            when(planExerciseRepository
                    .findByRehabPlanIdAndWeekNumberAndDayOfWeek(
                            eq(activePlan.getId()), eq(1),
                            eq(SessionDay.LUNDI)))
                    .thenReturn(List.of(planExercise));
            when(exerciseMapper.toResponse(exercise))
                    .thenReturn(new ExerciseResponse());

            List<ExerciseResponse> result =
                    exerciseService.getMyExercises();

            assertThat(result.get(0).getRepsTarget())
                    .isEqualTo(22); // 12 + 10
            assertThat(result.get(0).getToleranceDeg())
                    .isEqualTo(6); // 15 - 9
        }

        @Test
        @DisplayName("Première séance jamais effectuée —"
                + " aucune vérification d'espacement"
                + " requise")
        void getMyExercises_premiereSeance_aucuneVerificationEspacementRequise() {
            PlanExercise planExercise = PlanExercise.builder()
                    .id(UUID.randomUUID())
                    .exercise(exercise)
                    .repsPrescribed(10)
                    .build();

            when(patientRepository
                    .findByUser_Email("patient@test.com"))
                    .thenReturn(Optional.of(patient));
            when(rehabPlanRepository
                    .findByPatientIdAndStatus(
                            patient.getId(), Status.ACTIVE))
                    .thenReturn(Optional.of(activePlan));
            when(sessionRepository
                    .countByRehabPlanIdAndSessionStatusAndStartTimeBetween(
                            eq(activePlan.getId()),
                            eq(SessionStatus.COMPLETED),
                            any(), any()))
                    .thenReturn(0L);
            when(sessionRepository
                    .findFirstByPatientIdAndSessionStatusOrderByStartTimeDesc(
                            patient.getId(),
                            SessionStatus.COMPLETED))
                    .thenReturn(Optional.empty());
            when(planExerciseRepository
                    .findByRehabPlanIdAndWeekNumberAndDayOfWeek(
                            eq(activePlan.getId()), eq(1),
                            eq(SessionDay.LUNDI)))
                    .thenReturn(List.of(planExercise));
            when(exerciseMapper.toResponse(exercise))
                    .thenReturn(new ExerciseResponse());

            List<ExerciseResponse> result =
                    exerciseService.getMyExercises();

            assertThat(result).hasSize(1);
        }
    }
}