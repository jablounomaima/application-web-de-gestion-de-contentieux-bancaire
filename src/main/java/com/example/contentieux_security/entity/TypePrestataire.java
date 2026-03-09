package com.example.contentieux_security.entity;

public enum TypePrestataire {
    AVOCAT,
    HUISSIER,
    EXPERT,
    VALIDATEUR_JURIDIQUE,
    VALIDATEUR_FINANCIER;

    /**
     * Retourne le nom du rôle Keycloak correspondant (minuscules)
     */
    public String toKeycloakRole() {
        return this.name().toLowerCase();
    }

    /**
     * Retourne l'URL du dashboard selon le type
     */
    public String toDashboardUrl() {
        return switch (this) {
            case AVOCAT               -> "/avocat/dashboard";
            case HUISSIER             -> "/huissier/dashboard";
            case EXPERT               -> "/expert/dashboard";
            case VALIDATEUR_JURIDIQUE -> "/validateur/juridique/dashboard";
            case VALIDATEUR_FINANCIER -> "/validateur/financier/dashboard";
        };
    }

    /**
     * Libellé affiché dans l'interface
     */
    public String getLibelle() {
        return switch (this) {
            case AVOCAT               -> "Avocat";
            case HUISSIER             -> "Huissier";
            case EXPERT               -> "Expert";
            case VALIDATEUR_JURIDIQUE -> "Validateur Juridique";
            case VALIDATEUR_FINANCIER -> "Validateur Financier";
        };
    }
}