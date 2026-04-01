package com.example.contentieux_security.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GarantieAjoutRequest {

    @NotBlank(message = "Le type de garantie est obligatoire")
    private String typeGarantie;

    private String description;

    @Positive(message = "La valeur estimée doit être positive")
    private Double valeurEstimee;

    private String documentRef;

    private Long dossierId;
}