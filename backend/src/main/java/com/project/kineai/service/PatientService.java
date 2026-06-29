package com.project.kineai.service;

import com.project.kineai.dto.request.UpdatePatientRequest;
import com.project.kineai.dto.response.PatientResponse;
import com.project.kineai.exception.BusinessException;
import com.project.kineai.mapper.PatientMapper;
import com.project.kineai.model.entity.Patient;
import com.project.kineai.model.entity.User;
import com.project.kineai.repository.PatientRepository;
import com.project.kineai.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PatientService {

    private final PatientRepository patientRepository;
    private final UserRepository userRepository;
    private final PatientMapper patientMapper;

    //------- Mon Profile -------------
    @Transactional(readOnly = true)
    public PatientResponse getMyProfile() {
        return patientMapper.toResponse(getCurrentPatient());
    }

    // ── Mes patients (kiné) ───────────────────
    @Transactional(readOnly = true)
    public List<PatientResponse> getMyPatients() {
         User user = getCurrentUser();
        UUID kineId = user.getKinesitherapeute().getId();
        return patientMapper.toResponseList(
                patientRepository.findByKineId(kineId));
    }

    // ── Patient par ID ────────────────────────
    @Transactional(readOnly = true)
    public PatientResponse getPatientById(UUID patientId) {
        Patient patient = patientRepository.findById(patientId)
                .orElseThrow(()-> new BusinessException("Patient introuvable"));
        return  patientMapper.toResponse(patient);
    }

    // ── Mise à jour profil ────────────────────
    @Transactional
    public PatientResponse updateMyProfile(UpdatePatientRequest request) {
        Patient patient = getCurrentPatient();
        patientMapper.updateEntity(request, patient);
        return patientMapper.toResponse(patientRepository.save(patient));
    }

    // ── Archiver patient (admin) ──────────────
    @Transactional
    public void archivePatient(UUID patientId) {
        Patient patient = patientRepository.findById(patientId)
                .orElseThrow(() -> new BusinessException("Patient introuvable"));
        patient.getUser().setActive(false);
        userRepository.save(patient.getUser());
    }

    // ── Helpers ───────────────────────────────
    public Patient getCurrentPatient() {
        User user = getCurrentUser();
        return patientRepository.findByUserId(user.getId())
                .orElseThrow(() -> new BusinessException("Profil patient introuvable"));
    }

    public User getCurrentUser() {
        String email = SecurityContextHolder.getContext()
                .getAuthentication().getName();
        return userRepository.findByEmailAndActiveTrue(email)
                .orElseThrow(() -> new BusinessException("Utilisateur introuvable"));
    }

    }

