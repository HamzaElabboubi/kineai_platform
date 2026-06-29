package com.project.kineai.controller;

import com.project.kineai.dto.response.KineResponse;
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
@RequestMapping("/api/v1/kine")
@RequiredArgsConstructor
@Tag(name = "Kinésithérapeutes", description = "Gestion des kinés")
public class KineController {

    private final KineService kineService;

    // ── Mon profil (kiné connecté) ────────────
    @GetMapping("/profile")
    @PreAuthorize("hasRole('KINE')")
    @Operation(summary = "Profil du kiné connecté")
    public ResponseEntity<KineResponse> getMyProfile() {
        return ResponseEntity.ok(kineService.getMyProfile());
    }

    // ── Liste kinés validés ───────────────────
    // Public — pour l'inscription patient (choisir son kiné)
    @GetMapping("/validated")
    @Operation(summary = "Liste des kinés validés",
            description = "Utilisé lors de l'inscription patient pour choisir son kiné")
    public ResponseEntity<List<KineResponse>> getAllValidated() {
        return ResponseEntity.ok(kineService.getAllValidated());
    }

    // ── Valider un kiné (admin) ───────────────
    @PutMapping("/admin/validate/{kineId}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Valider un kiné (RG-38)",
            description = "L'admin doit valider le kiné avant qu'il puisse se connecter")
    public ResponseEntity<KineResponse> validateKine(
            @PathVariable UUID kineId) {
        return ResponseEntity.ok(kineService.validateKine(kineId));
    }
}