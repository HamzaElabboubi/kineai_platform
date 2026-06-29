package com.project.kineai.test;


import com.project.kineai.dto.response.KineResponse;
import com.project.kineai.exception.BusinessException;
import com.project.kineai.mapper.KineMapper;
import com.project.kineai.model.entity.Kinesitherapeute;
import com.project.kineai.model.entity.User;
import com.project.kineai.repository.KineRepository;
import com.project.kineai.repository.UserRepository;
import com.project.kineai.service.KineService;
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
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("KineService — Tests unitaires")
class KineServiceTest {

    @Mock
    private KineRepository kineRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private KineMapper kineMapper;

    @InjectMocks
    private KineService kineService;

    private User user;
    private Kinesitherapeute kine;
    private UUID kineId;
    private MockedStatic<SecurityContextHolder>
            securityContextHolderMock;

    @BeforeEach
    void setUp() {
        kineId = UUID.randomUUID();
        user = User.builder()
                .id(UUID.randomUUID())
                .email("kine@test.com")
                .active(false)
                .build();

        kine = Kinesitherapeute.builder()
                .id(kineId)
                .user(user)
                .fullName("Dr. Martin")
                .speciality("Orthopédie")
                .validated(false)
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
        @DisplayName("Retourne le profil du kiné"
                + " connecté")
        void getMyProfile_retourneProfilDuKineConnecte() {
            mockSecurityContext("kine@test.com");

            when(userRepository
                    .findByEmailAndActiveTrue(
                            "kine@test.com"))
                    .thenReturn(Optional.of(user));
            when(kineRepository
                    .findByUserId(user.getId()))
                    .thenReturn(Optional.of(kine));
            when(kineMapper.toResponse(kine))
                    .thenReturn(new KineResponse());

            KineResponse result =
                    kineService.getMyProfile();

            assertThat(result).isNotNull();
        }
    }

    // ══════════════════════════════════════════
    // GET ALL VALIDATED
    // ══════════════════════════════════════════
    @Nested
    @DisplayName("getAllValidated()")
    class GetAllValidatedTests {

        @Test
        @DisplayName("Retourne la liste des kinés"
                + " validés")
        void getAllValidated_retourneListeKinesValides() {
            when(kineRepository.findByValidatedTrue())
                    .thenReturn(List.of(kine));
            when(kineMapper.toResponseList(
                    List.of(kine)))
                    .thenReturn(List.of(
                            new KineResponse()));

            List<KineResponse> result =
                    kineService.getAllValidated();

            assertThat(result).hasSize(1);
        }
    }

    // ══════════════════════════════════════════
    // GET PENDING KINES
    // ══════════════════════════════════════════
    @Nested
    @DisplayName("getPendingKines()")
    class GetPendingKinesTests {

        @Test
        @DisplayName("Retourne la liste des kinés non"
                + " validés")
        void getPendingKines_retourneKinesNonValides() {
            when(kineRepository.findByValidatedFalse())
                    .thenReturn(List.of(kine));
            when(kineMapper.toResponseList(
                    List.of(kine)))
                    .thenReturn(List.of(
                            new KineResponse()));

            List<KineResponse> result =
                    kineService.getPendingKines();

            assertThat(result).hasSize(1);
        }
    }

    // ══════════════════════════════════════════
    // VALIDATE KINE — RG-38
    // ══════════════════════════════════════════
    @Nested
    @DisplayName("validateKine()")
    class ValidateKineTests {

        @Test
        @DisplayName("Succès — active le kiné et son"
                + " compte utilisateur — RG-38")
        void validateKine_succes_activeLeKineEtSonCompte() {
            when(kineRepository.findById(kineId))
                    .thenReturn(Optional.of(kine));
            when(kineMapper.toResponse(kine))
                    .thenReturn(new KineResponse());

            kineService.validateKine(kineId);

            assertThat(kine.getValidated()).isTrue();
            assertThat(user.getActive()).isTrue();
            verify(userRepository).save(user);
            verify(kineRepository).save(kine);
        }

        @Test
        @DisplayName("Introuvable — lève"
                + " BusinessException")
        void validateKine_introuvable_leveBusinessException() {
            UUID inconnuId = UUID.randomUUID();
            when(kineRepository.findById(inconnuId))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> kineService
                    .validateKine(inconnuId))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining(
                            "Kiné introuvable");
        }
    }

    // ══════════════════════════════════════════
    // REJECT KINE
    // ══════════════════════════════════════════
    @Nested
    @DisplayName("rejectKine()")
    class RejectKineTests {

        @Test
        @DisplayName("Succès — désactive définitivement"
                + " le compte")
        void rejectKine_succes_desactiveDefinitivementLeCompte() {
            when(kineRepository.findById(kineId))
                    .thenReturn(Optional.of(kine));

            kineService.rejectKine(kineId);

            assertThat(user.getActive()).isFalse();
            verify(userRepository).save(user);
            verify(kineRepository).save(kine);
        }

        @Test
        @DisplayName("Introuvable — lève"
                + " BusinessException")
        void rejectKine_introuvable_leveBusinessException() {
            UUID inconnuId = UUID.randomUUID();
            when(kineRepository.findById(inconnuId))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> kineService
                    .rejectKine(inconnuId))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining(
                            "Kiné introuvable");
        }
    }

    // ══════════════════════════════════════════
    // HELPERS — getCurrentKine / getCurrentUser
    // ══════════════════════════════════════════
    @Nested
    @DisplayName("getCurrentKine() / getCurrentUser()")
    class HelperTests {

        @Test
        @DisplayName("getCurrentKine — succès")
        void getCurrentKine_succes_retourneLeKineConnecte() {
            mockSecurityContext("kine@test.com");

            when(userRepository
                    .findByEmailAndActiveTrue(
                            "kine@test.com"))
                    .thenReturn(Optional.of(user));
            when(kineRepository
                    .findByUserId(user.getId()))
                    .thenReturn(Optional.of(kine));

            Kinesitherapeute result =
                    kineService.getCurrentKine();

            assertThat(result).isEqualTo(kine);
        }

        @Test
        @DisplayName("getCurrentKine — profil"
                + " introuvable lève BusinessException")
        void getCurrentKine_profilIntrouvable_leveBusinessException() {
            mockSecurityContext("kine@test.com");

            when(userRepository
                    .findByEmailAndActiveTrue(
                            "kine@test.com"))
                    .thenReturn(Optional.of(user));
            when(kineRepository
                    .findByUserId(user.getId()))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> kineService
                    .getCurrentKine())
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining(
                            "Profil kiné introuvable");
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

            assertThatThrownBy(() -> kineService
                    .getCurrentUser())
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining(
                            "Utilisateur introuvable");
        }
    }
}
