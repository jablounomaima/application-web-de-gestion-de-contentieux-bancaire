package com.example.contentieux_security.entity;

import jakarta.persistence.*;
import java.time.LocalDate;
import lombok.*;
import java.time.LocalDate;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Frais {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String typeFrais; // e.g., Timbres, Huissier, Expert
    private Double montant;
    private LocalDate dateEngagement;
    private String description;

    // Using reference instead of creating a full FactureFrais entity for now, as it
    // links to Dossier/Affaire.
    // Usually, Frais is directly linked to AffaireJudiciaire or Dossier depending
    // on the scope.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "affaire_judiciaire_id")
    private AffaireJudiciaire affaireJudiciaire;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "dossier_id")
    private Dossier dossier;
}
