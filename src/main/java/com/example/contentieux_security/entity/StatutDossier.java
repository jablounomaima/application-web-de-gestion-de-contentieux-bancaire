package com.example.contentieux_security.entity;

public enum StatutDossier {
    OUVERT("Ouvert"),
    EN_TRAITEMENT("En traitement"),
    EN_ATTENTE_VALIDATION("En attente de validation"),
    VALIDE("Validé"),
    EN_PROCEDURE("En procédure judiciaire"),
    EN_EXECUTION("En exécution"),
    REJETE("Rejeté"),
    CLOTURE("Clôturé");

    private final String libelle;
    StatutDossier(String libelle) { this.libelle = libelle; }
    public String getLibelle() { return libelle; }
}