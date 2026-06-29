package com.project.kineai.controller;

import com.project.kineai.dto.request.CreateKineRequest;
import com.project.kineai.dto.request.CreatePatientRequest;
import com.project.kineai.dto.request.LoginRequest;
import com.project.kineai.dto.response.AuthResponse;
import com.project.kineai.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Tag(name = "Authentification", description = "Inscription et connexion")
public class AuthController {

    private final AuthService authService;

    // ── Login ─────────────────────────────────
    @PostMapping("/login")
    @Operation(summary = "Connexion utilisateur",
            description = "Retourne JWT access + refresh token")
    public ResponseEntity<AuthResponse> login(
            @Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.Login(request));
    }

    // ── Inscription Patient ───────────────────
    @PostMapping("/register/patient")
    @Operation(summary = "Inscription patient")
    public ResponseEntity<AuthResponse> registerPatient(
            @Valid @RequestBody CreatePatientRequest request) {
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(authService.registerPatient(request));
    }

    // ── Inscription Kiné ──────────────────────
    @PostMapping("/register/kine")
    @Operation(summary = "Inscription kinésithérapeute",
            description = "Compte en attente de validation admin (RG-38)")
    public ResponseEntity<AuthResponse> registerKine(
            @Valid @RequestBody CreateKineRequest request) {
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(authService.registerKine(request));
    }

    // ── Refresh Token ─────────────────────────
    @PostMapping("/refresh")
    @Operation(summary = "Renouvellement du token JWT")
    public ResponseEntity<AuthResponse> refresh(
            @RequestHeader("X-Refresh-Token") String refreshToken) {
        return ResponseEntity.ok(authService.refresh(refreshToken));
    }
}