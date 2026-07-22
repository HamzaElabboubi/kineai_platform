package com.project.kineai.model.enums;

public enum KineDecision {
    VALIDEE,   // le kiné confirme la progression proposée
    MODIFIEE,  // le kiné ajuste manuellement (ex: phase différente)
    REFUSEE    // le kiné maintient le patient dans la phase actuelle
}