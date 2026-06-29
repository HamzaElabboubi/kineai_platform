package com.project.kineai.controller;

import com.project.kineai.dto.response.AdminStatsResponse;
import com.project.kineai.dto.response.KineResponse;
import com.project.kineai.dto.response.PatientResponse;
import com.project.kineai.service.AdminService;
import com.project.kineai.service.KineService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
@Tag(name = "Administration",
        description = "Endpoints réservés à l'administrateur")
public class AdminController {

    private final AdminService adminService;
    private final KineService kineService;

    // ── Statistiques globales ─────────────────
    @GetMapping("/stats")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Statistiques globales plateforme")
    public ResponseEntity<AdminStatsResponse> getStats() {
        return ResponseEntity.ok(adminService.getStats());
    }

    // ── Kinés en attente de validation ────────
    @GetMapping("/kine/pending")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Liste des kinés en attente")
    public ResponseEntity<List<KineResponse>>
    getPendingKines() {
        return ResponseEntity.ok(
                kineService.getPendingKines());
    }

    // ── Tous les kinés (validés + en attente) ─
    @GetMapping("/kines")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Liste de tous les kinés")
    public ResponseEntity<List<KineResponse>>
    getAllKines() {
        return ResponseEntity.ok(
                adminService.getAllKines());
    }

    // ── Valider un compte kiné ────────────────
    @PutMapping("/kine/{id}/validate")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Valider un compte kiné")
    public ResponseEntity<KineResponse>
    validateKine(@PathVariable UUID id) {
        return ResponseEntity.ok(
                kineService.validateKine(id));
    }

    // ── Rejeter un compte kiné ────────────────
    @PutMapping("/kine/{id}/reject")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Rejeter un compte kiné")
    public ResponseEntity<Void>
    rejectKine(@PathVariable UUID id) {
        kineService.rejectKine(id);
        return ResponseEntity.noContent().build();
    }

    // ── Supprimer définitivement un kiné ──────
    @DeleteMapping("/kine/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Supprimer un kiné définitivement")
    public ResponseEntity<Void>
    deleteKine(@PathVariable UUID id) {
        adminService.deleteKine(id);
        return ResponseEntity.noContent().build();
    }

    // ── Tous les patients de la plateforme ────
    @GetMapping("/patients")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Liste de tous les patients")
    public ResponseEntity<List<PatientResponse>>
    getAllPatients() {
        return ResponseEntity.ok(
                adminService.getAllPatients());
    }

    // ── Archiver un patient ───────────────────
    // (déjà présent dans PatientController, on le
    // laisse là-bas pour ne pas dupliquer)

    // ── Réactiver un patient archivé ──────────
    @PutMapping("/patients/{id}/reactivate")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Réactiver un patient archivé")
    public ResponseEntity<PatientResponse>
    reactivatePatient(@PathVariable UUID id) {
        return ResponseEntity.ok(
                adminService.reactivatePatient(id));
    }

    // ── Réaffecter un patient à un autre kiné ─
    @PutMapping("/patients/{patientId}/reassign")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Réaffecter un patient à un autre kiné")
    public ResponseEntity<PatientResponse> reassignKine(
            @PathVariable UUID patientId,
            @RequestParam UUID newKineId) {
        return ResponseEntity.ok(
                adminService.reassignKine(patientId, newKineId));
    }


    @PutMapping("/kine/{id}/deactivate")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Désactiver un compte kiné"
            + " (suspension, réversible)")
    public ResponseEntity<KineResponse>
    deactivateKine(@PathVariable UUID id) {
        return ResponseEntity.ok(
                adminService.deactivateKine(id));
    }

    @PutMapping("/kine/{id}/activate")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Réactiver un compte kiné"
            + " précédemment désactivé")
    public ResponseEntity<KineResponse>
    activateKine(@PathVariable UUID id) {
        return ResponseEntity.ok(
                adminService.activateKine(id));
    }
}