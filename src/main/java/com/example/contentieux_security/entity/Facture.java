package com.example.contentieux_security.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
/**
 * Entité représentant la facture d'honoraires soumise par un avocat
 * pour une mission judiciaire.
 */
@Entity
@Table(name = "facture")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Facture {

    // ─────────────────────────────────────────────
    //  STATUTS
    // ─────────────────────────────────────────────

    public enum StatutFacture {
        SOUMISE,
        VALIDEE,
        REJETEE
    }

    // ─────────────────────────────────────────────
    //  CLEF PRIMAIRE
    // ─────────────────────────────────────────────

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // ─────────────────────────────────────────────
    //  RELATION — Mission (1 mission = 1 facture max)
    // ─────────────────────────────────────────────

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "mission_id", nullable = false, unique = true)
    private Mission mission;

    // ─────────────────────────────────────────────
    //  MONTANTS
    // ─────────────────────────────────────────────

    /** Montant hors taxes en DT */
    @Column(nullable = false, precision = 15, scale = 3)
    private BigDecimal montantHT;

    /** Taux de TVA en % (ex : 19.00) */
    @Column(nullable = false, precision = 5, scale = 2)
    private BigDecimal tauxTva;

    /** Montant TTC calculé = montantHT × (1 + tauxTva/100) */
    @Column(nullable = false, precision = 15, scale = 3)
    private BigDecimal montantTTC;

    // ─────────────────────────────────────────────
    //  INFORMATIONS FACTURE
    // ─────────────────────────────────────────────

    /** Numéro de facture émis par l'avocat (optionnel) */
    @Column(length = 100)
    private String numeroFacture;

    /** Date d'émission de la facture */
    @Column(nullable = false)
    private LocalDate dateFacture;

    /** Commentaire ou description libre */
    @Column(columnDefinition = "TEXT")
    private String description;

    // ─────────────────────────────────────────────
    //  TRAÇABILITÉ
    // ─────────────────────────────────────────────

    /** Login de l'avocat ayant soumis la facture */
    @Column(nullable = false, length = 100)
    private String soumisePar;

    /** Date/heure de soumission */
    @Column(nullable = false)
    private LocalDateTime dateSoumission;

    /** Statut de traitement de la facture */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private StatutFacture statut;

    // ─────────────────────────────────────────────
    //  FICHIER JOINT (scan PDF de la facture)
    // ─────────────────────────────────────────────

    /** Nom du fichier tel que stocké sur le serveur (UUID + extension) */
    @Column(length = 255)
    private String nomFichierServeur;

    /** Nom original du fichier uploadé par l'avocat */
    @Column(length = 255)
    private String nomFichierOriginal;

    /** Type MIME du fichier (ex : application/pdf) */
    @Column(length = 100)
    private String typeMime;
}