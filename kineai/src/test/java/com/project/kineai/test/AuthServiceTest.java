package com.project.kineai.test;
import com.project.kineai.dto.request.CreateKineRequest;
import com.project.kineai.dto.request.CreatePatientRequest;
import com.project.kineai.dto.request.LoginRequest;
import com.project.kineai.dto.response.AuthResponse;
import com.project.kineai.exception.BusinessException;
import com.project.kineai.mapper.UserMapper;
import com.project.kineai.model.entity.Kinesitherapeute;
import com.project.kineai.model.entity.Patient;
import com.project.kineai.model.entity.User;
import com.project.kineai.model.enums.Pathology;
import com.project.kineai.model.enums.Role;
import com.project.kineai.repository.KineRepository;
import com.project.kineai.repository.PatientRepository;
import com.project.kineai.repository.UserRepository;
import com.project.kineai.security.Jwt.JwtUtils;
import com.project.kineai.service.AuthService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuthService — Tests unitaires")
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private PatientRepository patientRepository;
    @Mock
    private KineRepository kineRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private JwtUtils jwtUtils;
    @Mock
    private AuthenticationManager authenticationManager;
    @Mock
    private UserMapper userMapper;

    @InjectMocks
    private AuthService authService;

    private User patientUser;
    private User kineUser;
    private Kinesitherapeute kine;

    @BeforeEach
    void setUp() {
        patientUser = User.builder()
                .id(UUID.randomUUID())
                .email("patient@test.com")
                .password("encoded")
                .role(Role.PATIENT)
                .active(true)
                .build();

        kine = Kinesitherapeute.builder()
                .id(UUID.randomUUID())
                .fullName("Dr. Test")
                .speciality("Orthopédie")
                .validated(true)
                .build();

        kineUser = User.builder()
                .id(UUID.randomUUID())
                .email("kine@test.com")
                .password("encoded")
                .role(Role.KINE)
                .active(true)
                .kinesitherapeute(kine)
                .build();
    }

    // ══════════════════════════════════════════
    // LOGIN
    // ══════════════════════════════════════════
    @Nested
    @DisplayName("Login()")
    class LoginTests {

        @Test
        @DisplayName("Connexion réussie avec credentials"
                + " valides — retourne AuthResponse avec tokens")
        void login_credentialsValides_retourneAuthResponse() {
            LoginRequest request = LoginRequest.builder()
                    .email("patient@test.com")
                    .password("password123")
                    .build();

            Patient patient = Patient.builder()
                    .id(UUID.randomUUID())
                    .fullName("Jean Dupont")
                    .build();
            patientUser.setPatient(patient);

            when(userRepository
                    .findByEmailAndActiveTrue("patient@test.com"))
                    .thenReturn(Optional.of(patientUser));
            when(userMapper.toAuthResponse(patientUser, patient))
                    .thenReturn(AuthResponse.builder()
                            .userId(patientUser.getId())
                            .email("patient@test.com")
                            .role("PATIENT")
                            .fullName("Jean Dupont")
                            .build());
            when(jwtUtils.generateAccessToken(
                    anyString(), anyString()))
                    .thenReturn("access-token");
            when(jwtUtils.generateRefreshToken(anyString()))
                    .thenReturn("refresh-token");

            AuthResponse response = authService.Login(request);

            assertThat(response).isNotNull();
            assertThat(response.getEmail())
                    .isEqualTo("patient@test.com");
            assertThat(response.getAccessToken())
                    .isEqualTo("access-token");
            verify(authenticationManager)
                    .authenticate(any());
        }

        @Test
        @DisplayName("Connexion avec compte inexistant ou"
                + " inactif — lève une exception")
        void login_compteInexistant_leveException() {
            LoginRequest request = LoginRequest.builder()
                    .email("inconnu@test.com")
                    .password("password123")
                    .build();

            when(userRepository
                    .findByEmailAndActiveTrue("inconnu@test.com"))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> authService.Login(request))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining(
                            "Compte inactif ou introuvable");
        }

        @Test
        @DisplayName("Connexion d'un kiné non validé —"
                + " lève une exception explicite")
        void login_kineNonValide_leveException() {
            kine.setValidated(false);
            LoginRequest request = LoginRequest.builder()
                    .email("kine@test.com")
                    .password("password123")
                    .build();

            when(userRepository
                    .findByEmailAndActiveTrue("kine@test.com"))
                    .thenReturn(Optional.of(kineUser));

            assertThatThrownBy(() -> authService.Login(request))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining(
                            "en attente de validation");
        }
    }

    // ══════════════════════════════════════════
    // REGISTER PATIENT
    // ══════════════════════════════════════════
    @Nested
    @DisplayName("registerPatient()")
    class RegisterPatientTests {

        @Test
        @DisplayName("Email déjà utilisé — lève"
                + " BusinessException")
        void registerPatient_emailExistant_leveBusinessException() {
            CreatePatientRequest request = CreatePatientRequest
                    .builder()
                    .email("existe@test.com")
                    .password("password123")
                    .fullName("Jean Dupont")
                    .age(30)
                    .phone("0600000000")
                    .pathology(Pathology.GENOU)
                    .kineId(UUID.randomUUID())
                    .build();

            when(userRepository
                    .existsByEmail("existe@test.com"))
                    .thenReturn(true);

            assertThatThrownBy(
                    () -> authService.registerPatient(request))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("Email déjà utilisé");

            verify(userRepository, never()).save(any());
        }

        @Test
        @DisplayName("Kinésithérapeute introuvable —"
                + " lève BusinessException")
        void registerPatient_kineIntrouvable_leveBusinessException() {
            UUID kineId = UUID.randomUUID();
            CreatePatientRequest request = CreatePatientRequest
                    .builder()
                    .email("nouveau@test.com")
                    .password("password123")
                    .fullName("Jean Dupont")
                    .age(30)
                    .phone("0600000000")
                    .pathology(Pathology.GENOU)
                    .kineId(kineId)
                    .build();

            when(userRepository
                    .existsByEmail("nouveau@test.com"))
                    .thenReturn(false);
            when(kineRepository.findById(kineId))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(
                    () -> authService.registerPatient(request))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining(
                            "Kinésithérapeute introuvable");
        }

        @Test
        @DisplayName("Inscription réussie — crée User et"
                + " Patient avec niveau DEBUTANT")
        void registerPatient_succes_creeUserEtPatient() {
            UUID kineId = UUID.randomUUID();
            CreatePatientRequest request = CreatePatientRequest
                    .builder()
                    .email("nouveau@test.com")
                    .password("password123")
                    .fullName("Jean Dupont")
                    .age(30)
                    .phone("0600000000")
                    .pathology(Pathology.GENOU)
                    .kineId(kineId)
                    .build();

            when(userRepository
                    .existsByEmail("nouveau@test.com"))
                    .thenReturn(false);
            when(kineRepository.findById(kineId))
                    .thenReturn(Optional.of(kine));
            when(passwordEncoder.encode("password123"))
                    .thenReturn("encoded-password");
            when(userRepository.save(any(User.class)))
                    .thenAnswer(invocation ->
                            invocation.getArgument(0));
            when(jwtUtils.generateAccessToken(
                    anyString(), anyString()))
                    .thenReturn("access-token");
            when(jwtUtils.generateRefreshToken(anyString()))
                    .thenReturn("refresh-token");

            AuthResponse response =
                    authService.registerPatient(request);

            assertThat(response).isNotNull();
            assertThat(response.getFullName())
                    .isEqualTo("Jean Dupont");
            assertThat(response.getRole())
                    .isEqualTo("PATIENT");

            verify(patientRepository).save(any(Patient.class));
        }
    }

    // ══════════════════════════════════════════
    // REGISTER KINE
    // ══════════════════════════════════════════
    @Nested
    @DisplayName("registerKine()")
    class RegisterKineTests {

        @Test
        @DisplayName("Email déjà utilisé — lève"
                + " BusinessException")
        void registerKine_emailExistant_leveBusinessException() {
            CreateKineRequest request = CreateKineRequest
                    .builder()
                    .email("existe@test.com")
                    .password("password123")
                    .fullName("Dr. Martin")
                    .speciality("Orthopédie")
                    .build();

            when(userRepository
                    .existsByEmail("existe@test.com"))
                    .thenReturn(true);

            assertThatThrownBy(
                    () -> authService.registerKine(request))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("Email déjà utilisé");

            verify(kineRepository, never()).save(any());
        }

        @Test
        @DisplayName("Inscription réussie — kiné créé avec"
                + " validated = false (RG-38)")
        void registerKine_succes_creeAvecValidatedFalse() {
            CreateKineRequest request = CreateKineRequest
                    .builder()
                    .email("nouveau-kine@test.com")
                    .password("password123")
                    .fullName("Dr. Martin")
                    .speciality("Orthopédie")
                    .build();

            when(userRepository
                    .existsByEmail("nouveau-kine@test.com"))
                    .thenReturn(false);
            when(passwordEncoder.encode("password123"))
                    .thenReturn("encoded-password");
            when(userRepository.save(any(User.class)))
                    .thenAnswer(invocation ->
                            invocation.getArgument(0));
            when(jwtUtils.generateAccessToken(
                    anyString(), anyString()))
                    .thenReturn("access-token");
            when(jwtUtils.generateRefreshToken(anyString()))
                    .thenReturn("refresh-token");

            AuthResponse response =
                    authService.registerKine(request);

            assertThat(response).isNotNull();
            assertThat(response.getFullName())
                    .isEqualTo("Dr. Martin");

            verify(kineRepository).save(argThat(k ->
                    k.getValidated() == false));
        }
    }
}