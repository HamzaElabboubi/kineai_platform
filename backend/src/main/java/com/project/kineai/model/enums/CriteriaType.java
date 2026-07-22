package com.project.kineai.model.enums;

public enum CriteriaType {
    MIN_SUCCESS_RATE,        // % de répétitions correctes minimum
    MAX_PAIN_LEVEL,          // douleur maximale tolérée (0-10)
    MIN_SESSIONS_COMPLETED,  // nombre minimum de séances réussies
    MIN_CONFORMITY_PCT       // conformité MediaPipe minimale (SessionMetrics)
}