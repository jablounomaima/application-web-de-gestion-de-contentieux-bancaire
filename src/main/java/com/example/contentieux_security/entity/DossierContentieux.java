package com.example.contentieux_security.entity;

import com.example.contentieux_security.enums.DossierStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "dossier")
public class DossierContentieux {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // ── Validateurs choisis ─────────────────────────────
    @Column
    private String validateurFinancierChoisi;

    @Column
    private String validateurJuridiqueChoisi;

    // ── Infos dossier ────────────────────────────────────
    @Column(name = "numero_dossier", unique = true, nullable = false)
    private String numeroDossier;

    @Column(nullable = false)
    private String libelle;

    @Column(name = "date_creation", nullable = false)
    private LocalDateTime dateCreation = LocalDateTime.now();

    @Column(name = "date_validation")
    private LocalDateTime dateValidation;

    // ── Statut ───────────────────────────────────────────
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private DossierStatus statut = DossierStatus.OUVERT;

    @Column(name = "cree_par")
    private String creePar;

    // ── Validations ──────────────────────────────────────
    @Column(name = "validation_financiere")
    private Boolean validationFinanciere;

    @Column(name = "validation_juridique")
    private Boolean validationJuridique;

    @Column(name = "commentaire_financier", columnDefinition = "TEXT")
    private String commentaireFinancier;

    @Column(name = "commentaire_juridique", columnDefinition = "TEXT")
    private String commentaireJuridique;

    @Column(name = "validateur_financier_username")
    private String validateurFinancierUsername;

    @Column(name = "validateur_juridique_username")
    private String validateurJuridiqueUsername;

    // ── Description ──────────────────────────────────────
    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(columnDefinition = "TEXT")
    private String notes;

    // ── Montant ──────────────────────────────────────────
    @Column(name = "montant_recupere")
    private Double montantRecupere = 0.0;

    // ── Relations ────────────────────────────────────────
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "client_id", nullable = false)
    private Client client;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "agence_id", nullable = false)
    private Agence agence;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "agent_id", nullable = false)
    private AgentBancaire agentCreateur;

    @OneToMany(mappedBy = "dossier", cascade = CascadeType.ALL, fetch = FetchType.LAZY ,orphanRemoval = true)
    private Set<Risque> risques;

    @OneToMany(mappedBy = "dossier", cascade = CascadeType.ALL, orphanRemoval = true)
private List<HistoriqueDossier> historiques;

@OneToMany(mappedBy = "dossier", cascade = CascadeType.ALL, orphanRemoval = true)
private List<Notification> notifications;


    // ── Méthodes utilitaires ─────────────────────────────

    /** Calcul du solde total des risques */
    public Double calculerSolde() {
        if (risques == null) return 0.0;

        return risques.stream()
                .mapToDouble(r -> r.getMontantImpaye() != null ? r.getMontantImpaye() : 0)
                .sum();
    }

    /** Vérifie si le dossier est entièrement validé */
    public boolean isEntierementValide() {
        return Boolean.TRUE.equals(validationFinanciere)
                && Boolean.TRUE.equals(validationJuridique);
    }

    /** Mise à jour automatique du statut */
    @PreUpdate
    public void preUpdate() {
        if (isEntierementValide() && DossierStatus.EN_TRAITEMENT.equals(statut)) {
            this.statut = DossierStatus.VALIDE;
            this.dateValidation = LocalDateTime.now();
        }
    }
}