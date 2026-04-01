package com.example.contentieux_security.enums;

public enum DossierStatus {
    OUVERT,
    EN_TRAITEMENT,

    EN_CORRECTION,   // ✅ AJOUT IMPORTANT

    VALIDE,
    EN_PROCEDURE,
    EN_EXECUTION,
    CLOTURE_PARTIEL,

    REJETE,          // ⚠ rejet total uniquement (rare)
    CLOTURE
}