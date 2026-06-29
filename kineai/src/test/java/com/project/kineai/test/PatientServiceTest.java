package com.project.kineai.test;



import com.project.kineai.dto.request.UpdatePatientRequest;
import com.project.kineai.dto.response.PatientResponse;
import com.project.kineai.exception.BusinessException;
import com.project.kineai.service.PatientService;
import com.project.kineai.mapper.PatientMapper;
import com.project.kineai.model.entity.Kinesitherapeute;
import com.project.kineai.model.entity.Patient;
import com.project.kineai.model.entity.User;
import com.project.kineai.repository.PatientRepository;
import com.project.kineai.repository.UserRepository;
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

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("PatientService — Tests unitaires")
class PatientServiceTest {

    @Mock
    private PatientRepository patientRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private PatientMapper patientMapper;

    @InjectMocks
    private PatientService patientService;

    private User user;
    private Patient patient;
    private UUID patientId;
    private MockedStatic<SecurityContextHolder>
            securityContextHolderMock;

    @BeforeEach
    void setUp() {
        patientId = UUID.randomUUID();
        user = User.builder()
                .id(UUID.randomUUID())
                .email("patient@test.com")
                .active(true)
                .build();

        patient = Patient.builder()
                .id(patientId)
                .user(user)
                .fullName("Jean Dupont")
                .build();
    }

    private void mockSecurityContext(String email) {
        Authentication authentication =
                mock(Authentication.class);
        when(authentication.getName()).thenReturn(email);

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

    @AfterEach
    void tearDown() {
        if (securityContextHolderMock != null) {
            securityContextHolderMock.close();
        }
    }

    // ══════════════════════════════════════════
    // GET MY PROFILE
    // ══════════════════════════════════════════
    @Nested
    @DisplayName("getMyProfile()")
    class GetMyProfileTests {

        @Test
        @DisplayName("Retourne le profil du patient"
                + " connecté")
        void getMyProfile_retourneProfilDuPatientConnecte() {
            mockSecurityContext("patient@test.com");

            when(userRepository
                    .findByEmailAndActiveTrue(
                            "patient@test.com"))
                    .thenReturn(Optional.of(user));
            when(patientRepository
                    .findByUserId(user.getId()))
                    .thenReturn(Optional.of(patient));
            when(patientMapper.toResponse(patient))
                    .thenReturn(new PatientResponse());

            PatientResponse result =
                    patientService.getMyProfile();

            assertThat(result).isNotNull();
        }
    }

    // ══════════════════════════════════════════
    // GET MY PATIENTS (kiné)
    // ══════════════════════════════════════════
    @Nested
    @DisplayName("getMyPatients()")
    class GetMyPatientsTests {

        @Test
        @DisplayName("Retourne la liste des patients"
                + " du kiné connecté")
        void getMyPatients_retourneListePatientsDuKineConnecte() {
            mockSecurityContext("kine@test.com");

            Kinesitherapeute kine = Kinesitherapeute
                    .builder()
                    .id(UUID.randomUUID())
                    .fullName("Dr. Martin")
                    .build();

            User kineUser = User.builder()
                    .id(UUID.randomUUID())
                    .email("kine@test.com")
                    .kinesitherapeute(kine)
                    .build();

            when(userRepository
                    .findByEmailAndActiveTrue(
                            "kine@test.com"))
                    .thenReturn(Optional.of(kineUser));
            when(patientRepository
                    .findByKineId(kine.getId()))
                    .thenReturn(List.of(patient));
            when(patientMapper.toResponseList(
                    List.of(patient)))
                    .thenReturn(List.of(
                            new PatientResponse()));

            List<PatientResponse> result =
                    patientService.getMyPatients();

            assertThat(result).hasSize(1);
        }
    }

    // ══════════════════════════════════════════
    // GET PATIENT BY ID
    // ══════════════════════════════════════════
    @Nested
    @DisplayName("getPatientById()")
    class GetPatientByIdTests {

        @Test
        @DisplayName("Succès — retourne le patient"
                + " correspondant")
        void getPatientById_succes_retourneLePatient() {
            when(patientRepository.findById(patientId))
                    .thenReturn(Optional.of(patient));
            when(patientMapper.toResponse(patient))
                    .thenReturn(new PatientResponse());

            PatientResponse result =
                    patientService
                            .getPatientById(patientId);

            assertThat(result).isNotNull();
        }

        @Test
        @DisplayName("Introuvable — lève"
                + " BusinessException")
        void getPatientById_introuvable_leveBusinessException() {
            UUID inconnuId = UUID.randomUUID();
            when(patientRepository.findById(inconnuId))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> patientService
                    .getPatientById(inconnuId))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining(
                            "Patient introuvable");
        }
    }

    // ══════════════════════════════════════════
    // UPDATE MY PROFILE
    // ══════════════════════════════════════════
    @Nested
    @DisplayName("updateMyProfile()")
    class UpdateMyProfileTests {

        @Test
        @DisplayName("Succès — met à jour et retourne"
                + " le patient modifié")
        void updateMyProfile_succes_metAJourEtRetourneLePatient() {
            mockSecurityContext("patient@test.com");

            UpdatePatientRequest request =
                    UpdatePatientRequest.builder()
                            .fullName("Jean Dupont Modifié")
                            .age(35)
                            .build();

            when(userRepository
                    .findByEmailAndActiveTrue(
                            "patient@test.com"))
                    .thenReturn(Optional.of(user));
            when(patientRepository
                    .findByUserId(user.getId()))
                    .thenReturn(Optional.of(patient));
            when(patientRepository.save(patient))
                    .thenReturn(patient);
            when(patientMapper.toResponse(patient))
                    .thenReturn(new PatientResponse());

            PatientResponse result = patientService
                    .updateMyProfile(request);

            assertThat(result).isNotNull();
            verify(patientMapper)
                    .updateEntity(request, patient);
            verify(patientRepository).save(patient);
        }
    }

    // ══════════════════════════════════════════
    // ARCHIVE PATIENT (admin)
    // ══════════════════════════════════════════
    @Nested
    @DisplayName("archivePatient()")
    class ArchivePatientTests {

        @Test
        @DisplayName("Succès — désactive le compte"
                + " utilisateur du patient")
        void archivePatient_succes_desactiveLeCompteUtilisateur() {
            when(patientRepository.findById(patientId))
                    .thenReturn(Optional.of(patient));

            patientService.archivePatient(patientId);

            assertThat(user.getActive()).isFalse();
            verify(userRepository).save(user);
        }

        @Test
        @DisplayName("Introuvable — lève"
                + " BusinessException")
        void archivePatient_introuvable_leveBusinessException() {
            UUID inconnuId = UUID.randomUUID();
            when(patientRepository.findById(inconnuId))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> patientService
                    .archivePatient(inconnuId))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining(
                            "Patient introuvable");
        }
    }

    // ══════════════════════════════════════════
    // HELPERS — getCurrentPatient / getCurrentUser
    // ══════════════════════════════════════════
    @Nested
    @DisplayName("getCurrentPatient() / getCurrentUser()")
    class HelperTests {

        @Test
        @DisplayName("getCurrentPatient — succès")
        void getCurrentPatient_succes_retourneLePatientConnecte() {
            mockSecurityContext("patient@test.com");

            when(userRepository
                    .findByEmailAndActiveTrue(
                            "patient@test.com"))
                    .thenReturn(Optional.of(user));
            when(patientRepository
                    .findByUserId(user.getId()))
                    .thenReturn(Optional.of(patient));

            Patient result = patientService
                    .getCurrentPatient();

            assertThat(result).isEqualTo(patient);
        }

        @Test
        @DisplayName("getCurrentPatient — profil"
                + " introuvable lève BusinessException")
        void getCurrentPatient_profilIntrouvable_leveBusinessException() {
            mockSecurityContext("patient@test.com");

            when(userRepository
                    .findByEmailAndActiveTrue(
                            "patient@test.com"))
                    .thenReturn(Optional.of(user));
            when(patientRepository
                    .findByUserId(user.getId()))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> patientService
                    .getCurrentPatient())
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining(
                            "Profil patient introuvable");
        }

        @Test
        @DisplayName("getCurrentUser — introuvable lève"
                + " BusinessException")
        void getCurrentUser_introuvable_leveBusinessException() {
            mockSecurityContext("inconnu@test.com");

            when(userRepository
                    .findByEmailAndActiveTrue(
                            "inconnu@test.com"))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> patientService
                    .getCurrentUser())
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining(
                            "Utilisateur introuvable");
        }
    }
}
