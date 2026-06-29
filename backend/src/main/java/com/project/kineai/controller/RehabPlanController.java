package com.project.kineai.controller;

import com.project.kineai.dto.request.CreatePlanRequest;
import com.project.kineai.dto.response.RehabPlanResponse;
import com.project.kineai.service.RehabPlanService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/plans")
@RequiredArgsConstructor
@Tag(name = "Plans de rééducation", description = "Système expert de génération des plans")
public class RehabPlanController {

    private final RehabPlanService rehabPlanService;

    // ── Générer plan (kiné) ───────────────────
    @PostMapping
    @PreAuthorize("hasRole('KINE')")
    @Operation(summary = "Générer un plan automatique",
            description = "Le système expert Java génère un plan 4 semaines selon pathologie + âge + niveau")
    public ResponseEntity<RehabPlanResponse> generatePlan(
            @Valid @RequestBody CreatePlanRequest request) {
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(rehabPlanService.generatePlan(request));
    }

    // ── Plan actif du patient connecté ────────
        @GetMapping("/my/active")
    @PreAuthorize("hasRole('PATIENT')")
    @Operation(summary = "Plan actif du patient connecté")
    public ResponseEntity<RehabPlanResponse> getMyActivePlan() {
        // Récupère l'ID du patient depuis le contexte Security
        return ResponseEntity.ok(
                rehabPlanService.getActivePlanForCurrentPatient());
    }

    // ── Plan actif d'un patient (kiné) ────────
    @GetMapping("/patient/{patientId}/active")
    @PreAuthorize("hasAnyRole('KINE', 'ADMIN')")
    @Operation(summary = "Plan actif d'un patient")
    public ResponseEntity<RehabPlanResponse> getActivePlan(
            @PathVariable UUID patientId) {
        return ResponseEntity.ok(
                rehabPlanService.getActivePlan(patientId));
    }

    // ── Historique des plans d'un patient ─────
    @GetMapping("/patient/{patientId}/history")
    @PreAuthorize("hasAnyRole('KINE', 'ADMIN')")
    @Operation(summary = "Historique des plans d'un patient")
    public ResponseEntity<List<RehabPlanResponse>> getAllPlans(
            @PathVariable UUID patientId) {
        return ResponseEntity.ok(
                rehabPlanService.getAllPlans(patientId));
    }
}