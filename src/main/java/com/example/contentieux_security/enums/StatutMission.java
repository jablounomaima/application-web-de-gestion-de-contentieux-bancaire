package com.example.contentieux_security.enums;

public enum StatutMission {
    ASSIGNEE("Assignée"),
    EN_COURS("En cours"),
    PV_SOUMIS("PV soumis"),
    FACTURE_SOUMISE("Facture soumise"),
    TERMINEE("Terminée"),
    ANNULEE("Annulée");

    private final String libelle;
    StatutMission(String libelle) { this.libelle = libelle; }
    public String getLibelle() { return libelle; }
}