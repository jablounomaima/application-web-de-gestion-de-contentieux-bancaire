package com.example.contentieux_security.entity;


import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "affaires_judiciaires")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder

public class AffaireJudiciaire {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String numeroAffaire; // AFF-2026-00001

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private StatutAffaire statut = StatutAffaire.EN_COURS;

    // Infos tribunal
    private String tribunal;
    private String numeroRole;       // numéro de rôle au tribunal
    private String chambre;

    // Dates clés
    private LocalDate dateLancement;
    private LocalDate dateProchainAudience;
    private LocalDate dateLimiteExecution;

    // Jugement
    @Enumerated(EnumType.STRING)
    @Column(length = 30)
    private TypeJugement typeJugement; // FAVORABLE | DEFAVORABLE | EN_APPEL | TRANSACTION

    private LocalDate dateJugement;
    private String montantJuge;        // montant ordonné par le tribunal
    private String delaiPaiementJuge;  // délai accordé par le tribunal
    private String descriptionJugement;

    // Notes
    @Column(columnDefinition = "TEXT")
    private String observations;

    @Column(nullable = false)
    private LocalDateTime dateCreation = LocalDateTime.now();

    // Relations
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "mission_id", nullable = false)
    private Mission mission; // la mission avocat liée

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "dossier_id", nullable = false)
    private DossierContentieux dossier;

    @OneToMany(mappedBy = "affaire", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("dateAudience ASC")
    private List<Audience> audiences = new ArrayList<>();

    @OneToMany(mappedBy = "affaire", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("dateUpload DESC")
    private List<DocumentAffaire> documents = new ArrayList<>();

    public enum StatutAffaire {
        EN_COURS, JUGEMENT_RENDU, EXECUTION_FORCEE, TRANSACTION, CLOSE
    }

    public enum TypeJugement {
        FAVORABLE, DEFAVORABLE, EN_APPEL, TRANSACTION, EN_ATTENTE
    }
}