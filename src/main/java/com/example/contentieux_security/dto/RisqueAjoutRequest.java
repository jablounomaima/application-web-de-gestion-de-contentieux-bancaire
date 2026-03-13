package com.example.contentieux_security.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.*;

/**
 * DTO pour l'ajout d'un risque (crédit) à un dossier existant.
 */
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class RisqueAjoutRequest {

    @NotBlank(message = "Le type de crédit est obligatoire")
    private String type;                  // ex: "Crédit immobilier"

    @NotNull(message = "Le montant initial est obligatoire")
    @Positive(message = "Le montant doit être positif")
    private Double montantInitial;        // ex: 10_000_000.0

    @Positive(message = "Le montant impayé doit être positif")
    private Double montantImpaye;

    private String dateEcheance;          // format yyyy-MM-dd

    private String description;           // informations complémentaires
}