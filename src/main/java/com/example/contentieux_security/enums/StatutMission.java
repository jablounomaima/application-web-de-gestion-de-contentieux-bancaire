package com.example.contentieux_security.enums;

public enum StatutMission {
    ASSIGNEE("Assignée"),
    EN_COURS("En cours"),
    PV_SOUMIS("PV soumis"),
    FACTURE_SOUMISE("Facture soumise"),
    REALISEE("Réalisée"),
    VALIDEE_AGENT("Validée par l'agent"), // ✅ AJOUT IMPORTANT
    TERMINEE("Terminée"),
    ANNULEE("Annulée"),
    // ✅ Ajouter ces deux
    FACTURE_VALIDEE("Facture validée"),
    FACTURE_PAYEE("Facture payée"),
    FACTURE_REJETEE("Facture rejetée"),
    REJETEE("Rejetée par l'agent"); // ✅ AJOUT


    private final String libelle;
    
    StatutMission(String libelle) { 
        this.libelle = libelle; 
    }
    
    public String getLibelle() { 
        return libelle; 
    }
}