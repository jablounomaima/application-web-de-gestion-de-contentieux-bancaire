package com.example.contentieux_security.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import lombok.*;

/**
 * DTO pour l'ajout d'une garantie à un risque existant.
 */
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class GarantieAjoutRequest {

    @NotBlank(message = "Le type de garantie est obligatoire")
    private String typeGarantie;          // VEHICULE, IMMOBILIER, HYPOTHEQUE...

    private String description;           // ex: "Renault Clio 2019 – immat TN-1234"

    @Positive(message = "La valeur estimée doit être positive")
    private Double valeurEstimee;

    private String documentRef;           // immatriculation, titre foncier...
}