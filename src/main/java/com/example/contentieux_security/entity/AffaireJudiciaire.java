package com.example.contentieux_security.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;
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
        CONDAMNATION, REJET, PARTIEL, MIXTE
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

    @Enumerated(EnumType.STRING)   // ← doit être STRING, pas ORDINAL
    @Column(name = "type_jugement", length = 20)
    private TypeJugement typeJugement;

    private LocalDate dateJugement;

    private String montantJuge;

    private String delaiPaiementJuge;

    @Column(columnDefinition = "TEXT")
    private String descriptionJugement;

    private LocalDate dateLimiteAppel; 

    // ❌ Supprimer ces deux lignes (pas d'entités PVSoumis/FactureSoumise)
// private List<PVSoumis> pvList;
// private List<FactureSoumise> factureList;

// ✅ Ajouter ces champs directement dans l'entité
// ❌ Supprimer ces 2 lignes (entités inexistantes)
// private List<PVSoumis> pvList;
// private List<FactureSoumise> factureList;

// ✅ Ajouter les champs directs PV
@Column(columnDefinition = "TEXT")
private String pvTexte;

@Enumerated(EnumType.STRING)
@Column(length = 20)
private StatutPV pvStatut;

// ✅ Ajouter les champs directs Facture  
@Column(length = 100)
private String factureRef;

private Double montantFacture;

@Enumerated(EnumType.STRING)
@Column(length = 20)
private StatutFacture factureStatut;


// ✅ Ajouter ce champ dans AffaireJudiciaire.java
@ElementCollection(fetch = FetchType.EAGER)
@CollectionTable(name = "affaire_pv_fichiers",
                 joinColumns = @JoinColumn(name = "affaire_id"))
@Column(name = "fichier_data", columnDefinition = "LONGTEXT")
@Builder.Default
private List<String> pvFichiers = new ArrayList<>();

// ✅ Enums à ajouter dans la classe
public enum StatutPV {
    EN_ATTENTE, VALIDE, REFUSE
}

public enum StatutFacture {
    EN_ATTENTE, PAYEE, REJETEE
}





    // ==============================
    // 🔗 RELATIONS
    // ==============================

    // ✔ Relation avec le dossier contentieux
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "dossier_id", nullable = false)
    @JsonIgnore
    private DossierContentieux dossier;

    // ✔ Relation avec mission (1 affaire = 1 mission)
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "mission_id")
    @JsonIgnore
    private Mission mission;

    // ✔ Liste des audiences
    @OneToMany(mappedBy = "affaire", cascade = CascadeType.ALL,
               orphanRemoval = true, fetch = FetchType.EAGER)
    @Builder.Default
    @JsonIgnore
    private List<Audience> audiences = new ArrayList<>();

    // ✔ Liste des documents
    @OneToMany(mappedBy = "affaire", cascade = CascadeType.ALL,
               orphanRemoval = true, fetch = FetchType.EAGER)
    @Builder.Default
    @JsonIgnore
    private List<DocumentAffaire> documents = new ArrayList<>();


    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "avocat_id")
    @JsonIgnore
    private Prestataire avocat;

}