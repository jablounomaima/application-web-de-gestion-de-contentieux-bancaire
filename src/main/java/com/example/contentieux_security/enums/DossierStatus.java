package com.example.contentieux_security.enums;

public enum DossierStatus {
    OUVERT,
    EN_TRAITEMENT,
    VALIDE,
    EN_PROCEDURE,      // ← ajouté
    EN_EXECUTION,      // ← ajouté
    CLOTURE_PARTIEL,   // ← ajouté
    REJETE,
    CLOTURE
}