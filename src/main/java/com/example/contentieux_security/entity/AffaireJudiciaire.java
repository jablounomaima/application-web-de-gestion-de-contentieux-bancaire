package com.example.contentieux_security.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})

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

    // ==============================
    // 📌 ENUMS (états métier)
    // ==============================

    // ✔ Statut global de l'affaire
    public enum StatutAffaire {
        EN_COURS,
        JUGEMENT_RENDU,
        EXECUTION_FORCEE,
        TRANSACTION,
        CLOSE
    }

    // ✔ Type de jugement rendu
    public enum TypeJugement {
        FAVORABLE,
        DEFAVORABLE,
        EN_APPEL,
        TRANSACTION,
        EN_ATTENTE
    }

    // ==============================
    // 🆔 IDENTIFIANT
    // ==============================

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // ✔ Numéro unique de l'affaire (important métier)
    @Column(unique = true, nullable = false, length = 50)
    private String numeroAffaire;

    // ==============================
    // ⚖️ INFORMATIONS TRIBUNAL
    // ==============================

    // ✔ Nom du tribunal
    @Column(length = 200)
    private String tribunal;

    // ✔ Chambre (ex: civile, pénale…)
    @Column(length = 100)
    private String chambre;

    // ✔ Numéro de rôle
    @Column(length = 50)
    private String numeroRole;

    // ==============================
    // 📅 DATES IMPORTANTES
    // ==============================

    // ✔ Date de lancement de l’affaire
    @Column(nullable = false)
    private LocalDate dateLancement;

    // ✔ Date de la prochaine audience
    private LocalDate dateProchainAudience;

    // ==============================
    // 📊 STATUT
    // ==============================

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private StatutAffaire statut;

    // ==============================
    // 📜 INFORMATIONS JUGEMENT
    // ==============================

    @Enumerated(EnumType.STRING)
    private TypeJugement typeJugement;

    private LocalDate dateJugement;

    private String montantJuge;

    private String delaiPaiementJuge;

    @Column(columnDefinition = "TEXT")
    private String descriptionJugement;

    // ==============================
    // 🔗 RELATIONS
    // ==============================

    // ✔ Relation avec le dossier contentieux
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "dossier_id", nullable = false)
    private DossierContentieux dossier;

    // ✔ Relation avec mission (1 affaire = 1 mission)
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "mission_id")
    private Mission mission;

    // ✔ Liste des audiences
    @OneToMany(mappedBy = "affaire", cascade = CascadeType.ALL,
               orphanRemoval = true, fetch = FetchType.EAGER)
    @Builder.Default
    private List<Audience> audiences = new ArrayList<>();

    // ✔ Liste des documents
    @OneToMany(mappedBy = "affaire", cascade = CascadeType.ALL,
               orphanRemoval = true, fetch = FetchType.EAGER)
    @Builder.Default
    private List<DocumentAffaire> documents = new ArrayList<>();


    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "avocat_id")
    private Prestataire avocat;

}