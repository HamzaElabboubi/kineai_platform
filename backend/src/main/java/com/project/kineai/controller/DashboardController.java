package com.project.kineai.controller;

import com.project.kineai.dto.response.DashboardKineResponse;
import com.project.kineai.dto.response.DashboardPatientResponse;
import com.project.kineai.service.DashboardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/dashboard")
@RequiredArgsConstructor
@Tag(name = "Dashboard", description = "Tableaux de bord kiné et patient")
public class DashboardController {

    private final DashboardService dashboardService;

    // ── Dashboard kiné ────────────────────────
    @GetMapping("/kine")
    @PreAuthorize("hasRole('KINE')")
    @Operation(summary = "Dashboard complet du kiné connecté",
            description = "Retourne patients + alertes actives + statistiques globales")
    public ResponseEntity<DashboardKineResponse> getKineDashboard() {
        return ResponseEntity.ok(
                dashboardService.getKineDashboard());
    }

    // ── Dashboard patient ─────────────────────
    @GetMapping("/patient")
    @PreAuthorize("hasRole('PATIENT')")
    @Operation(summary = "Dashboard du patient connecté",
            description = "Retourne profil + plan actif + progression + badges + séances récentes")
    public ResponseEntity<DashboardPatientResponse> getPatientDashboard() {
        return ResponseEntity.ok(
                dashboardService.getPatientDashboard());
    }

    // ── Détail patient dans dashboard kiné ────
    @GetMapping("/kine/patients/{patientId}")
    @PreAuthorize("hasRole('KINE')")
    @Operation(summary = "Dashboard détaillé d'un patient",
            description = "Vue complète d'un patient depuis le dashboard kiné")
    public ResponseEntity<DashboardPatientResponse> getPatientDetail(
            @PathVariable UUID patientId) {
        return ResponseEntity.ok(
                dashboardService.getPatientDashboardById(patientId));
    }
}