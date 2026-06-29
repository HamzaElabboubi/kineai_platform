package com.project.kineai.service;

import com.project.kineai.dto.response.BadgeResponse;
import com.project.kineai.mapper.BadgeMapper;
import com.project.kineai.model.entity.*;
import com.project.kineai.model.enums.BadgeType;
import com.project.kineai.repository.BadgeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class BadgeService {

    private final BadgeRepository badgeRepository;
    private final BadgeMapper badgeMapper;
    private final PatientService patientService;

    // ── Vérifier et débloquer badges ──────────
    @Transactional
    public void checkAndUnlockBadges(Patient patient, Session session) {
        // Badge 1ère séance
        unlock(patient, BadgeType.FIRST_SESSION);

        // Badge score parfait > 95%
        if (session.getScore() != null
                && session.getScore().compareTo(new BigDecimal("95")) >= 0) {
            unlock(patient, BadgeType.PERFECT_SCORE);
        }

        // Badge 7 jours consécutifs
        if (patient.getStreakCount() >= 7) {
            unlock(patient, BadgeType.SEVEN_DAYS);
        }
    }

    // ── Débloquer un badge — RG-30 ────────────
    @Transactional
    public void unlock(Patient patient, BadgeType type) {
        // Unicité garantie — RG-30
        if (badgeRepository.existsByPatientIdAndBadgeType(
                patient.getId(), type)) return;

        Badge badge = Badge.builder()
                .patient(patient)
                .badgeType(type)
                .displayed(false)
                .build();

        badgeRepository.save(badge);
        log.info("Badge {} débloqué pour patient {}", type, patient.getId());
    }

    // ── Mes badges ────────────────────────────
    @Transactional(readOnly = true)
    public List<BadgeResponse> getMyBadges() {
        Patient patient = patientService.getCurrentPatient();
        return badgeMapper.toResponseList(
                badgeRepository.findByPatientIdOrderByUnlockedAtDesc(patient.getId()));
    }

    // ── Marquer badge affiché ─────────────────
    @Transactional
    public void markDisplayed(UUID badgeId) {
        badgeRepository.findById(badgeId)
                .ifPresent(badge -> {
                    badge.setDisplayed(true);
                    badgeRepository.save(badge);
                });
    }

    // ── Compter mes badges débloqués ──────────
    @Transactional(readOnly = true)
    public long countMyBadges() {
        Patient patient = patientService.getCurrentPatient();
        return badgeRepository.countByPatientId(patient.getId());
    }
}