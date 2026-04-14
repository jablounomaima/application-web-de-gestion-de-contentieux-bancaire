package com.example.contentieux_security.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Entité AffaireJudiciaire.
 *
 * ✅ @Getter + @Setter ajoutés → corrige toutes les erreurs
 *    "cannot find symbol: method getStatut() / getTribunal() / getDossier() / ..."
 *    dans AffaireJudiciaireService et AvocatController.
 */
@Entity
@Table(name = "affaire_judiciaire")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AffaireJudiciaire {

    // ── Enums ──────────────────────────────────────────────

    public enum StatutAffaire {
        EN_COURS,
        JUGEMENT_RENDU,
        EXECUTION_FORCEE,
        TRANSACTION,
        CLOSE
    }

    public enum TypeJugement {
        FAVORABLE,
        DEFAVORABLE,
        EN_APPEL,
        TRANSACTION,
        EN_ATTENTE
    }

    // ── Clef primaire ──────────────────────────────────────

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // ── Numéro métier ──────────────────────────────────────

    @Column(unique = true, nullable = false, length = 50)
    private String numeroAffaire;

    // ── Tribunal ───────────────────────────────────────────

    @Column(length = 200)
    private String tribunal;

    @Column(length = 100)
    private String chambre;

    @Column(length = 50)
    private String numeroRole;

    // ── Dates ──────────────────────────────────────────────

    @Column(nullable = false)
    private LocalDate dateLancement;

    private LocalDate dateProchainAudience;

    // ── Statut ─────────────────────────────────────────────

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private StatutAffaire statut;

    // ── Jugement ───────────────────────────────────────────

    @Enumerated(EnumType.STRING)
    @Column(length = 30)
    private TypeJugement typeJugement;

    private LocalDate dateJugement;

    @Column(length = 100)
    private String montantJuge;

    @Column(length = 200)
    private String delaiPaiementJuge;

    @Column(columnDefinition = "TEXT")
    private String descriptionJugement;

    // ── Relations ──────────────────────────────────────────

    /**
     * Dossier contentieux parent.
     * ✅ getDossier() requis par AvocatController et AffaireJudiciaireService.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "dossier_id", nullable = false)
    private DossierContentieux dossier;

    /**
     * Mission associée à cette affaire (1 affaire = 1 mission avocat).
     * ✅ getMission() requis par AvocatController.
     */
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "mission_id")
    private Mission mission;

    /**
     * Liste des audiences.
     * ✅ getAudiences() requis par les templates Thymeleaf.
     */
    @OneToMany(mappedBy = "affaire", cascade = CascadeType.ALL,
               orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    private List<Audience> audiences = new ArrayList<>();

    /**
     * Liste des documents uploadés.
     * ✅ getDocuments() requis par les templates Thymeleaf.
     */
    @OneToMany(mappedBy = "affaire", cascade = CascadeType.ALL,
               orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    private List<DocumentAffaire> documents = new ArrayList<>();
}