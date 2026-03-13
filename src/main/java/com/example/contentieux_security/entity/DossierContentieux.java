package com.example.contentieux_security.entity;

import com.example.contentieux_security.enums.DossierStatus;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.HashSet; 

@Entity
@Table(name = "dossier")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class DossierContentieux {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;


    // Validateurs choisis par l'agent
    @Column
    private String validateurFinancierChoisi;   // username du validateur financier choisi

    @Column
    private String validateurJuridiqueChoisi;   // username du validateur juridique choisi

    @Column(name = "numero_dossier", unique = true, nullable = false)
    private String numeroDossier;           // ex: DOS-2026-0001

    @Column(nullable = false)
    private String libelle;

    @Column(name = "date_creation", nullable = false)
    private LocalDateTime dateCreation = LocalDateTime.now();

    @Column(name = "date_validation")
    private LocalDateTime dateValidation;

    // ── Statut ────────────────────────────────────────────────────
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DossierStatus statut = DossierStatus.OUVERT;

    @Column(name = "cree_par")
    private String creePar;                 // username de l'agent créateur

    // ── Validation ────────────────────────────────────────────────
    @Column(name = "validation_financiere")
    private Boolean validationFinanciere;   // null=en attente, true=accepté, false=rejeté

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

    @Column(columnDefinition = "TEXT")
    private String description;         // description détaillée du dossier

    @Column(columnDefinition = "TEXT")
    private String notes;               // notes internes de l'agent

    // ── Récupération ──────────────────────────────────────────────
    @Column(name = "montant_recupere")
    private Double montantRecupere = 0.0;

    // ── Relations ─────────────────────────────────────────────────
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "client_id", nullable = false)
    private Client client;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "agence_id", nullable = false)
    private Agence agence;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "agent_id", nullable = false)
    private AgentBancaire agentCreateur;

    @OneToMany(mappedBy = "dossier", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private Set<Risque> risques;

    // ── Méthodes utilitaires ──────────────────────────────────────

    /** Somme des montants impayés de tous les risques */
    public Double calculerSolde() {
        if (risques == null) return 0.0;
        return risques.stream()
                .mapToDouble(r -> r.getMontantImpaye() != null ? r.getMontantImpaye() : 0)
                .sum();
    }

    /** Vrai si les deux validations sont accordées */
    public boolean isEntierementValide() {
        return Boolean.TRUE.equals(validationFinanciere)
            && Boolean.TRUE.equals(validationJuridique);
    }

    /** Passage automatique au statut VALIDE dès les 2 validations obtenues */
    @PreUpdate
    public void preUpdate() {
        if (isEntierementValide() && DossierStatus.EN_TRAITEMENT.equals(statut)) {
            this.statut = DossierStatus.VALIDE;
            this.dateValidation = LocalDateTime.now();
        }
    }
}