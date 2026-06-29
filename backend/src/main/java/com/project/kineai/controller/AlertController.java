package com.project.kineai.controller;

import com.project.kineai.dto.response.AlertResponse;
import com.project.kineai.service.AlertService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/alerts")
@RequiredArgsConstructor
@Tag(name = "Alertes", description = "Gestion des alertes kiné")
public class AlertController {

    private final AlertService alertService;

    // ── Mes alertes en attente (kiné connecté) ─
    @GetMapping("/my")
    @PreAuthorize("hasRole('KINE')")
    @Operation(summary = "Alertes non résolues du kiné connecté")
    public ResponseEntity<List<AlertResponse>> getMyAlerts() {
        return ResponseEntity.ok(alertService.getMyAlerts());
    }

    // ── Résoudre une alerte ────────────────────
    @PutMapping("/{alertId}/resolve")
    @PreAuthorize("hasRole('KINE')")
    @Operation(summary = "Marquer une alerte comme résolue")
    public ResponseEntity<AlertResponse> resolveAlert(
            @PathVariable UUID alertId) {
        return ResponseEntity.ok(
                alertService.resolveAlert(alertId));
    }

    // ── Compteur alertes en attente ────────────
    @GetMapping("/pending/count")
    @PreAuthorize("hasRole('KINE')")
    @Operation(summary = "Nombre d'alertes en attente")
    public ResponseEntity<Long> countPendingAlerts() {
        return ResponseEntity.ok(
                alertService.countPendingAlerts());
    }

    // ── Mes alertes (patient connecté) ────────
    @GetMapping("/my-patient")
    @PreAuthorize("hasRole('PATIENT')")
    @Operation(summary = "Alertes du patient connecté"
            + " (résolues et non résolues)")
    public ResponseEntity<List<AlertResponse>>
    getMyAlertsAsPatient() {
        return ResponseEntity.ok(
                alertService.getMyAlertsAsPatient());
    }

    // ── Toutes mes alertes — historique complet ───
    @GetMapping("/my/all")
    @PreAuthorize("hasRole('KINE')")
    @Operation(summary = "Historique complet des alertes"
            + " du kiné connecté (résolues et non résolues)")
    public ResponseEntity<List<AlertResponse>>
    getAllMyAlerts() {
        return ResponseEntity.ok(
                alertService.getAllMyAlerts());
    }
}