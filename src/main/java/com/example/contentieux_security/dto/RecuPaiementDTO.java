package com.example.contentieux_security.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class RecuPaiementDTO {
    private String beneficiaireType;   // "Prestataire" ou "Avocat"
    private String beneficiaireNom;
    private String beneficiaireEmail;

    private String numeroDossier;
    private String reference;          // numéro de mission ou d'affaire
    private String factureRef;
    private Double montantHT;
    private Double montantTTC;

    private String paiementMode;       // "VIREMENT" ou "CHEQUE"
    private String paiementReference;  // numéro de virement ou de chèque
    private LocalDate paiementDate;
    private String paiementBeneficiaireRib;
    private String paiementCompteAgenceBanque;
    private String paiementCompteAgenceRib;
    private String paiementEffectuePar;
}
