package com.example.contentieux_security.enums;

public enum TypePrestation {

    PROCEDURE_JUDICIAIRE("Procédure judiciaire"),
    EXPERTISE("Expertise"),
    SIGNIFICATION("Signification"),
    RECOUVREMENT("Recouvrement"),
    ENQUETE("Enquête");

    private final String libelle;

    TypePrestation(String libelle) {
        this.libelle = libelle;
    }

    public String getLibelle() {
        return libelle;
    }
}