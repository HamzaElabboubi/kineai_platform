package com.project.kineai.service;

import com.project.kineai.dto.response.KineResponse;
import com.project.kineai.exception.BusinessException;
import com.project.kineai.mapper.KineMapper;
import com.project.kineai.model.entity.Kinesitherapeute;
import com.project.kineai.model.entity.User;
import com.project.kineai.repository.KineRepository;
import com.project.kineai.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class KineService {

    private final KineRepository kineRepository;
    private final UserRepository userRepository;
    private final KineMapper kineMapper;

    // ── Mon profil (kiné connecté) ────────────
    @Transactional(readOnly = true)
    public KineResponse getMyProfile() {
        return kineMapper.toResponse(getCurrentKine());
    }

    // ── Liste kinés validés ───────────────────
    // Utilisé lors de l'inscription patient
    // pour afficher la liste déroulante des kinés
    @Transactional(readOnly = true)
    public List<KineResponse> getAllValidated() {
        return kineMapper.toResponseList(
                kineRepository.findByValidatedTrue());
    }



    // ── Helpers ───────────────────────────────
    public Kinesitherapeute getCurrentKine() {
        User user = getCurrentUser();
        return kineRepository.findByUserId(user.getId())
                .orElseThrow(() -> new BusinessException(
                        "Profil kiné introuvable"));
    }

    public User getCurrentUser() {
        String email = SecurityContextHolder.getContext()
                .getAuthentication().getName();
        return userRepository.findByEmailAndActiveTrue(email)
                .orElseThrow(() -> new BusinessException(
                        "Utilisateur introuvable"));
    }
    // ── Kinés en attente de validation ────────────
    @Transactional(readOnly = true)
    public List<KineResponse> getPendingKines() {
        return kineMapper.toResponseList(
                kineRepository.findByValidatedFalse());
    }
    // ── Valider un kiné (RG-38) ───────────────────
    @Transactional
    public KineResponse validateKine(UUID kineId) {
        Kinesitherapeute kine = kineRepository
                .findById(kineId)
                .orElseThrow(() ->
                        new BusinessException("Kiné introuvable"));

        // Activer le compte User associé
        kine.setValidated(true);
        kine.getUser().setActive(true);
        userRepository.save(kine.getUser());

        kineRepository.save(kine);
        log.info("Kiné validé : {}", kine.getFullName());
        return kineMapper.toResponse(kine);
    }
    // ── Rejeter un kiné ───────────────────────────
    @Transactional
    public void rejectKine(UUID kineId) {
        Kinesitherapeute kine = kineRepository
                .findById(kineId)
                .orElseThrow(() ->
                        new BusinessException("Kiné introuvable"));

        // Désactiver définitivement
        kine.getUser().setActive(false);
        userRepository.save(kine.getUser());
        kineRepository.save(kine);
        log.info("Kiné rejeté : {}", kine.getFullName());
    }
}