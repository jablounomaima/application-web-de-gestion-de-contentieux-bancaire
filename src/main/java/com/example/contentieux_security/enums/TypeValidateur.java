package com.example.contentieux_security.enums;

public enum TypeValidateur {
    VALIDATEUR_FINANCIER,
    VALIDATEUR_JURIDIQUE;

    public String getLibelle() {
        return switch (this) {
            case VALIDATEUR_FINANCIER -> "Validateur Financier";
            case VALIDATEUR_JURIDIQUE -> "Validateur Juridique";
        };
    }

    public String toDashboardUrl() {
        return switch (this) {
            case VALIDATEUR_FINANCIER -> "/validateur/financier/dashboard";
            case VALIDATEUR_JURIDIQUE -> "/validateur/juridique/dashboard";
        };
    }
}