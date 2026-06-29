package com.project.kineai.test;

import com.project.kineai.mapper.BadgeMapper;
import com.project.kineai.model.entity.Badge;
import com.project.kineai.model.entity.Patient;
import com.project.kineai.model.entity.Session;
import com.project.kineai.model.enums.BadgeType;
import com.project.kineai.repository.BadgeRepository;
import com.project.kineai.service.BadgeService;
import com.project.kineai.service.PatientService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("BadgeService — Tests unitaires")
class BadgeServiceTest {

    @Mock
    private BadgeRepository badgeRepository;
    @Mock
    private BadgeMapper badgeMapper;
    @Mock
    private PatientService patientService;

    @InjectMocks
    private BadgeService badgeService;

    private Patient patient;
    private Session session;

    @BeforeEach
    void setUp() {
        patient = Patient.builder()
                .id(UUID.randomUUID())
                .fullName("Jean Dupont")
                .streakCount(0)
                .build();

        session = Session.builder()
                .id(UUID.randomUUID())
                .patient(patient)
                .build();
    }

    // ══════════════════════════════════════════
    // CHECK AND UNLOCK BADGES
    // ══════════════════════════════════════════
    @Nested
    @DisplayName("checkAndUnlockBadges()")
    class CheckAndUnlockBadgesTests {

        @Test
        @DisplayName("Toute séance complétée tente de"
                + " débloquer FIRST_SESSION")
        void checkAndUnlockBadges_premiereSession_debloqueFirstSession() {
            session.setScore(new BigDecimal("70.0"));

            when(badgeRepository
                    .existsByPatientIdAndBadgeType(
                            eq(patient.getId()), any()))
                    .thenReturn(false);

            badgeService.checkAndUnlockBadges(patient, session);

            verify(badgeRepository).save(argThat(b ->
                    b.getBadgeType() == BadgeType.FIRST_SESSION));
        }

        @Test
        @DisplayName("Score >= 95% — débloque PERFECT_SCORE"
                + " — RG-40")
        void checkAndUnlockBadges_scoreSuperieurOuEgal95_debloquePerfectScore() {
            session.setScore(new BigDecimal("95.0"));
            patient.setStreakCount(0);

            when(badgeRepository
                    .existsByPatientIdAndBadgeType(
                            eq(patient.getId()), any()))
                    .thenReturn(false);

            badgeService.checkAndUnlockBadges(patient, session);

            verify(badgeRepository).save(argThat(b ->
                    b.getBadgeType() == BadgeType.PERFECT_SCORE));
        }

        @Test
        @DisplayName("Score < 95% — ne débloque PAS"
                + " PERFECT_SCORE")
        void checkAndUnlockBadges_scoreInferieur95_neDebloquePasPerfectScore() {
            session.setScore(new BigDecimal("94.9"));
            patient.setStreakCount(0);

            when(badgeRepository
                    .existsByPatientIdAndBadgeType(
                            eq(patient.getId()), any()))
                    .thenReturn(false);

            badgeService.checkAndUnlockBadges(patient, session);

            verify(badgeRepository, never()).save(argThat(b ->
                    b.getBadgeType() == BadgeType.PERFECT_SCORE));
        }

        @Test
        @DisplayName("Streak >= 7 — débloque SEVEN_DAYS"
                + " — RG-40")
        void checkAndUnlockBadges_streakSuperieurOuEgal7_debloqueSevenDays() {
            session.setScore(new BigDecimal("60.0"));
            patient.setStreakCount(7);

            when(badgeRepository
                    .existsByPatientIdAndBadgeType(
                            eq(patient.getId()), any()))
                    .thenReturn(false);

            badgeService.checkAndUnlockBadges(patient, session);

            verify(badgeRepository).save(argThat(b ->
                    b.getBadgeType() == BadgeType.SEVEN_DAYS));
        }

        @Test
        @DisplayName("Streak < 7 — ne débloque PAS"
                + " SEVEN_DAYS")
        void checkAndUnlockBadges_streakInferieur7_neDebloquePasSevenDays() {
            session.setScore(new BigDecimal("60.0"));
            patient.setStreakCount(6);

            when(badgeRepository
                    .existsByPatientIdAndBadgeType(
                            eq(patient.getId()), any()))
                    .thenReturn(false);

            badgeService.checkAndUnlockBadges(patient, session);

            verify(badgeRepository, never()).save(argThat(b ->
                    b.getBadgeType() == BadgeType.SEVEN_DAYS));
        }
    }

    // ══════════════════════════════════════════
    // UNLOCK
    // ══════════════════════════════════════════
    @Nested
    @DisplayName("unlock()")
    class UnlockTests {

        @Test
        @DisplayName("Badge déjà débloqué — ignore la"
                + " création — RG-30")
        void unlock_badgeDejaExistant_neCreeAucunDoublon() {
            when(badgeRepository
                    .existsByPatientIdAndBadgeType(
                            patient.getId(),
                            BadgeType.FIRST_SESSION))
                    .thenReturn(true);

            badgeService.unlock(patient, BadgeType.FIRST_SESSION);

            verify(badgeRepository, never()).save(any());
        }

        @Test
        @DisplayName("Nouveau badge — sauvegardé avec"
                + " displayed = false")
        void unlock_nouveauBadge_sauvegardeAvecDisplayedFalse() {
            when(badgeRepository
                    .existsByPatientIdAndBadgeType(
                            patient.getId(),
                            BadgeType.FIRST_SESSION))
                    .thenReturn(false);

            badgeService.unlock(patient, BadgeType.FIRST_SESSION);

            verify(badgeRepository).save(argThat(b ->
                    b.getBadgeType() == BadgeType.FIRST_SESSION
                            && b.getPatient().equals(patient)
                            && !b.getDisplayed()));
        }
    }

    // ══════════════════════════════════════════
    // COUNT MY BADGES
    // ══════════════════════════════════════════
    @Nested
    @DisplayName("countMyBadges()")
    class CountMyBadgesTests {

        @Test
        @DisplayName("Retourne le comptage exact du"
                + " patient connecté")
        void countMyBadges_retourneLeComptageDuPatientConnecte() {
            when(patientService.getCurrentPatient())
                    .thenReturn(patient);
            when(badgeRepository
                    .countByPatientId(patient.getId()))
                    .thenReturn(3L);

            long count = badgeService.countMyBadges();

            assertThat(count).isEqualTo(3);
        }
    }
}