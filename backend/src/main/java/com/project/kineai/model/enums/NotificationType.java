package com.project.kineai.model.enums;

public enum NotificationType {
    SESSION_REMINDER,        // Rappel de séance à venir (veille)
    MISSED_SESSION_WARNING,  // Inactivité détectée — patient averti
    STREAK_AT_RISK,          // Série de jours en train de se rompre
    BADGE_UNLOCKED           // Nouveau badge débloqué
}