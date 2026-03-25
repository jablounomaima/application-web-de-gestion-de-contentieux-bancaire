package com.example.contentieux_security.enums;

public enum TypePrestation {
    PROCEDURE_JUDICIAIRE("Procédure judiciaire"),
    EXECUTION_FORCEE("Exécution forcée"),
    EXPERTISE("Expertise"),
    SIGNIFICATION("Signification huissier");

    private final String libelle;
    TypePrestation(String libelle) { this.libelle = libelle; }
    public String getLibelle() { return libelle; }
}