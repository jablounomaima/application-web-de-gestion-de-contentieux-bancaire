package com.example.contentieux_security.enums;

public enum TypePrestataire {

    AVOCAT("Avocat"),
    EXPERT("Expert judiciaire"),
    HUISSIER("Huissier"),
    VALIDATEUR_FINANCIER("Validateur financier"),
    VALIDATEUR_JURIDIQUE("Validateur juridique");

    private final String libelle;

    // Constructeur
    TypePrestataire(String libelle) {
        this.libelle = libelle;
    }

    // Getter pour le libellé affiché à l'utilisateur
    public String getLibelle() {
        return libelle;
    }

    // Méthode existante (conservée)
    public String toKeycloakRole() {
        return switch (this) {
            case AVOCAT -> "ROLE_AVOCAT";
            case EXPERT -> "ROLE_EXPERT";
            case HUISSIER -> "ROLE_HUISSIER";
            case VALIDATEUR_FINANCIER -> "ROLE_VALIDATEUR_FINANCIER";
            case VALIDATEUR_JURIDIQUE -> "ROLE_VALIDATEUR_JURIDIQUE";
        };
    }

    // Bonus : permet d'afficher directement ${p.type} dans Thymeleaf sans .getLibelle()
    @Override
    public String toString() {
        return libelle;
    }
}