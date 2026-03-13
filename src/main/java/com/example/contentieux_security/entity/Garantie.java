package com.example.contentieux_security.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "garanties")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Garantie {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "type_garantie", nullable = false)
    private String typeGarantie;            // VEHICULE, IMMOBILIER, HYPOTHEQUE...

    @Column(columnDefinition = "TEXT")
    private String description;             // ex: "Renault Clio 2019 – TN-1234"

    @Column(name = "valeur_estimee")
    private Double valeurEstimee;

    @Column(name = "document_ref")
    private String documentRef;             // référence titre foncier, immatriculation...

    private String statut = "VALIDE";       // VALIDE, EXPIRE, EN_LITIGE

    // ── Relation ──────────────────────────────────────────────────
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "risque_id", nullable = false)
    private Risque risque;

    public Boolean verifierValidite() {
        return "VALIDE".equals(statut);
    }
}