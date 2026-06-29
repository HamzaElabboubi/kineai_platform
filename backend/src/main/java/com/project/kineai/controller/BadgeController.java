package com.project.kineai.controller;

import com.project.kineai.dto.response.BadgeResponse;
import com.project.kineai.service.BadgeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/badges")
@RequiredArgsConstructor
@Tag(name = "Gamification", description = "Badges XP et streak")
public class BadgeController {

    private final BadgeService badgeService;

    // ── Mes badges ────────────────────────────
    @GetMapping("/my")
    @PreAuthorize("hasRole('PATIENT')")
    @Operation(summary = "Badges du patient connecté",
            description = "Retourne tous les badges débloqués avec displayed=false en premier")
    public ResponseEntity<List<BadgeResponse>> getMyBadges() {
        return ResponseEntity.ok(badgeService.getMyBadges());
    }

    // ── Marquer badge affiché ─────────────────
    @PutMapping("/{badgeId}/display")
    @PreAuthorize("hasRole('PATIENT')")
    @Operation(summary = "Marquer un badge comme affiché",
            description = "Appelé après l'animation de déblocage côté Angular")
    public ResponseEntity<Void> markDisplayed(
            @PathVariable UUID badgeId) {
        badgeService.markDisplayed(badgeId);
        return ResponseEntity.ok().build();
    }

    // ── Compteur de badges débloqués ──────────
    @GetMapping("/my/count")
    @PreAuthorize("hasRole('PATIENT')")
    @Operation(summary = "Nombre total de badges débloqués"
            + " par le patient connecté")
    public ResponseEntity<Long> countMyBadges() {
        return ResponseEntity.ok(
                badgeService.countMyBadges());
    }
}