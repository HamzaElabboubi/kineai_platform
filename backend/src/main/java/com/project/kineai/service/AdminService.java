package com.project.kineai.service;

import com.project.kineai.dto.response.AdminStatsResponse;
import com.project.kineai.dto.response.KineResponse;
import com.project.kineai.dto.response.PatientResponse;
import com.project.kineai.exception.BusinessException;
import com.project.kineai.mapper.KineMapper;
import com.project.kineai.mapper.PatientMapper;
import com.project.kineai.model.entity.Kinesitherapeute;
import com.project.kineai.model.entity.Patient;
import com.project.kineai.repository.KineRepository;
import com.project.kineai.repository.PatientRepository;
import com.project.kineai.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AdminService {

    private final PatientRepository patientRepository;
    private final KineRepository kineRepository;
    private final UserRepository userRepository;
    private final PatientMapper patientMapper;
    private final KineMapper kineMapper;

    // ── Statistiques globales ─────────────────
    @Transactional(readOnly = true)
    public AdminStatsResponse getStats() {
        List<Patient> allPatients = patientRepository.findAll();

        Map<String, Long> byLevel = allPatients.stream()
                .collect(Collectors.groupingBy(
                        p -> p.getLevel().name(),
                        Collectors.counting()));

        Map<String, Long> byPathology = allPatients.stream()
                .collect(Collectors.groupingBy(
                        p -> p.getPathology().name(),
                        Collectors.counting()));

        // ✅ Kinés par spécialité
        Map<String, Long> bySpeciality =
                kineRepository.findAll().stream()
                        .collect(Collectors.groupingBy(
                                Kinesitherapeute::getSpeciality,
                                Collectors.counting()));

        return AdminStatsResponse.builder()
                .totalPatients(patientRepository.count())
                .totalKines(kineRepository.count())
                .validatedKines(
                        kineRepository.countByValidatedTrue())
                .pendingKines(
                        kineRepository.countByValidatedFalse())
                .patientsByLevel(byLevel)
                .patientsByPathology(byPathology)
                .kinesBySpeciality(bySpeciality)
                .build();
    }

    // ── Liste TOUS les patients (vue admin) ───
    @Transactional(readOnly = true)
    public List<PatientResponse> getAllPatients() {
        List<Patient> patients = patientRepository.findAll();
        return patients.stream()
                .map(patientMapper::toResponse)
                .toList();
    }

    // ── Liste TOUS les kinés (vue admin) ──────
    @Transactional(readOnly = true)
    public List<KineResponse> getAllKines() {
        List<Kinesitherapeute> kines = kineRepository.findAll();
        return kines.stream()
                .map(kine -> {
                    KineResponse response =
                            kineMapper.toResponse(kine);
                    response.setPatientCount(
                            kine.getPatients() != null
                                    ? kine.getPatients().size()
                                    : 0);
                    return response;
                })
                .toList();
    }

    // ── Supprimer définitivement un kiné ──────
    @Transactional
    public void deleteKine(UUID kineId) {
        Kinesitherapeute kine = kineRepository
                .findById(kineId)
                .orElseThrow(() ->
                        new BusinessException(
                                "Kinésithérapeute introuvable"));

        if (kine.getPatients() != null
                && !kine.getPatients().isEmpty()) {
            throw new BusinessException(
                    "Impossible de supprimer un kiné ayant"
                            + " des patients assignés. Réassignez"
                            + " d'abord ses patients.");
        }

        userRepository.delete(kine.getUser());
        log.info("Kiné supprimé définitivement : {}",
                kine.getFullName());
    }

    // ── Réactiver un patient archivé ──────────
    @Transactional
    public PatientResponse reactivatePatient(
            UUID patientId) {
        Patient patient = patientRepository
                .findById(patientId)
                .orElseThrow(() ->
                        new BusinessException(
                                "Patient introuvable"));

        patient.getUser().setActive(true);
        userRepository.save(patient.getUser());
        log.info("Patient réactivé : {}",
                patient.getFullName());
        return patientMapper.toResponse(patient);
    }


    // ── Réaffecter un patient à un autre kiné ─
    @Transactional
    public PatientResponse reassignKine(
            UUID patientId, UUID newKineId) {

        Patient patient = patientRepository
                .findById(patientId)
                .orElseThrow(() -> new BusinessException(
                        "Patient introuvable"));

        Kinesitherapeute newKine = kineRepository
                .findById(newKineId)
                .orElseThrow(() -> new BusinessException(
                        "Kinésithérapeute introuvable"));

        if (!newKine.getValidated()) {
            throw new BusinessException(
                    "Impossible d'assigner un patient à un"
                            + " kiné non validé.");
        }

        String oldKineName = patient.getKine().getFullName();
        patient.setKine(newKine);
        patientRepository.save(patient);

        log.info("Patient {} réaffecté de Dr. {} vers Dr. {}",
                patient.getFullName(), oldKineName,
                newKine.getFullName());

        return patientMapper.toResponse(patient);
    }

    // ── Désactiver un kiné (suspension) ───────
    @Transactional
    public KineResponse deactivateKine(UUID kineId) {
        Kinesitherapeute kine = kineRepository
                .findById(kineId)
                .orElseThrow(() -> new BusinessException(
                        "Kinésithérapeute introuvable"));

        if (!kine.getValidated()) {
            throw new BusinessException(
                    "Ce compte n'est pas encore validé."
                            + " Utilisez plutôt le rejet.");
        }

        kine.getUser().setActive(false);
        userRepository.save(kine.getUser());

        log.info("Kiné désactivé : {}", kine.getFullName());
        return kineMapper.toResponse(kine);
    }

    // ── Réactiver un kiné ──────────────────────
    @Transactional
    public KineResponse activateKine(UUID kineId) {
        Kinesitherapeute kine = kineRepository
                .findById(kineId)
                .orElseThrow(() -> new BusinessException(
                        "Kinésithérapeute introuvable"));

        if (!kine.getValidated()) {
            throw new BusinessException(
                    "Ce compte doit d'abord être validé"
                            + " avant réactivation.");
        }

        kine.getUser().setActive(true);
        userRepository.save(kine.getUser());

        log.info("Kiné réactivé : {}", kine.getFullName());
        return kineMapper.toResponse(kine);
    }
}