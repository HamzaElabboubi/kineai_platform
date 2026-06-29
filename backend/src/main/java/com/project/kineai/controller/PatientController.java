package com.project.kineai.controller;

import com.project.kineai.dto.request.UpdatePatientRequest;
import com.project.kineai.dto.response.PatientResponse;
import com.project.kineai.service.PatientService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@Tag(name = "Patients", description = "Gestion des patients")
public class PatientController {

    private final PatientService patientService;

    // ── Mon profil (patient connecté) ─────────
    @GetMapping("/patient/profile")
    @PreAuthorize("hasRole('PATIENT')")
    @Operation(summary = "Profil du patient connecté")
    public ResponseEntity<PatientResponse> getMyProfile() {
        return ResponseEntity.ok(patientService.getMyProfile());
    }

    // ── Mes patients (kiné connecté) ──────────
    @GetMapping("/kine/patients")
    @PreAuthorize("hasRole('KINE')")
    @Operation(summary = "Liste des patients du kiné connecté")
    public ResponseEntity<List<PatientResponse>> getMyPatients() {
        return ResponseEntity.ok(patientService.getMyPatients());
    }

    // ── Patient par ID ────────────────────────
    @GetMapping("/kine/patients/{patientId}")
    @PreAuthorize("hasAnyRole('KINE', 'ADMIN')")
    @Operation(summary = "Détail d'un patient")
    public ResponseEntity<PatientResponse> getPatientById(
            @PathVariable UUID patientId) {
        return ResponseEntity.ok(
                patientService.getPatientById(patientId));
    }

    // ── Mise à jour profil ────────────────────
    @PutMapping("/patient/profile")
    @PreAuthorize("hasRole('PATIENT')")
    @Operation(summary = "Mettre à jour son profil")
    public ResponseEntity<PatientResponse> updateMyProfile(
            @Valid @RequestBody UpdatePatientRequest request) {
        return ResponseEntity.ok(
                patientService.updateMyProfile(request));
    }

    // ── Archiver patient (admin) ──────────────
    @DeleteMapping("/admin/patients/{patientId}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Archiver un patient (RG-08)")
    public ResponseEntity<Void> archivePatient(
            @PathVariable UUID patientId) {
        patientService.archivePatient(patientId);
        return ResponseEntity.noContent().build();
    }
}