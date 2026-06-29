package com.project.kineai.controller;

import com.project.kineai.dto.request.CompleteSessionRequest;
import com.project.kineai.dto.request.CreateSessionRequest;
import com.project.kineai.dto.request.SaveMetricsRequest;
import com.project.kineai.dto.response.SessionResponse;
import com.project.kineai.service.SessionService;
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
@RequestMapping("/api/v1/sessions")
@RequiredArgsConstructor
@Tag(name = "Séances", description = "Gestion des séances de rééducation guidées")
public class SessionController {

    private final SessionService sessionService;

    // ── Démarrer séance ───────────────────────
    @PostMapping("/start")
    @PreAuthorize("hasRole('PATIENT')")
    @Operation(summary = "Démarrer une séance guidée",
            description = "Crée une session IN_PROGRESS liée à un exercice")
    public ResponseEntity<SessionResponse> startSession(
            @Valid @RequestBody CreateSessionRequest request) {
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(sessionService.startSession(request));
    }

    // ── Sauvegarder métriques (toutes les 5s) ─
    @PostMapping("/{sessionId}/metrics")
    @PreAuthorize("hasRole('PATIENT')")
    @Operation(summary = "Sauvegarder métriques MediaPipe",
            description = "Appelé toutes les 5 secondes pendant la séance — angles + score + reps")
    public ResponseEntity<Void> saveMetrics(
            @PathVariable UUID sessionId,
            @Valid @RequestBody SaveMetricsRequest request) {
        sessionService.saveMetrics(sessionId, request);
        return ResponseEntity.ok().build();
    }

    // ── Terminer séance ───────────────────────
    @PostMapping("/{sessionId}/complete")
    @PreAuthorize("hasRole('PATIENT')")
    @Operation(summary = "Terminer et valider une séance",
            description = "Calcule XP, vérifie badges (RG-30) et progression (RG-19/RG-20)")
    public ResponseEntity<SessionResponse> completeSession(
            @PathVariable UUID sessionId,
            @Valid @RequestBody CompleteSessionRequest request) {
        return ResponseEntity.ok(
                sessionService.completeSession(sessionId, request));
    }

    // ── Interrompre séance ────────────────────
    @PostMapping("/{sessionId}/interrupt")
    @PreAuthorize("hasRole('PATIENT')")
    @Operation(summary = "Interrompre une séance en cours",
            description = "Status passe à INTERRUPTED — aucun XP ni badge attribué")
    public ResponseEntity<SessionResponse> interruptSession(
            @PathVariable UUID sessionId) {
        return ResponseEntity.ok(
                sessionService.interruptSession(sessionId));
    }

    // ── Historique patient connecté ───────────
    @GetMapping("/my")
    @PreAuthorize("hasRole('PATIENT')")
    @Operation(summary = "Historique des séances du patient connecté")
    public ResponseEntity<List<SessionResponse>> getMySessionHistory() {
        return ResponseEntity.ok(
                sessionService.getMySessionHistory());
    }

    // ── Historique d'un patient (kiné) ────────
    @GetMapping("/patient/{patientId}")
    @PreAuthorize("hasAnyRole('KINE', 'ADMIN')")
    @Operation(summary = "Historique des séances d'un patient")
    public ResponseEntity<List<SessionResponse>> getPatientSessions(
            @PathVariable UUID patientId) {
        return ResponseEntity.ok(
                sessionService.getSessionsByPatientId(patientId));
    }
}