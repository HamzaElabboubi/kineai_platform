package com.project.kineai.model.enums;

public enum PhaseProgressStatus {
    EN_COURS,                                  // phase active, critères non atteints
    CRITERES_ATTEINTS_EN_ATTENTE_VALIDATION,   // moteur propose, kiné doit valider
    VALIDE_PAR_KINE                            // état transitoire — bascule
                                                // immédiatement vers la phase
                                                 // suivante avec un nouveau EN_COURS
}