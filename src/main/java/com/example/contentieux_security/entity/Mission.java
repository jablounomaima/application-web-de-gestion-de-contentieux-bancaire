package com.example.contentieux_security.entity;

import com.example.contentieux_security.enums.StatutMission;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Entité Mission.
 *
 * ✅ @Getter + @Setter → corrige toutes les erreurs
 *    "cannot find symbol: method getStatut() / getPrestataire() /
 *     getPrestation() / getNumeroMission() / setPvMission() / setStatut() / ..."
 *    dans PrestationService et AvocatController.
 */
@Entity
@Table(name = "mission")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Mission {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false, length = 50)
    private String numeroMission;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private StatutMission statut;

    private LocalDate dateAssignation;
    private LocalDate dateFinPrevue;
    private LocalDate dateRealisation;

    // PV de mission (commentaire texte)
    @Column(columnDefinition = "TEXT")
    private String pvMission;

    // Facture (champs legacy conservés pour compatibilité PrestationService)
    private Double  montantFacture;

    @Column(length = 100)
    private String  factureRef;
    private Boolean pvValide = false;
private Boolean factureValide = false;


    private LocalDateTime dateValidationAgent;
    private LocalDateTime dateValidationPv;
private LocalDateTime dateValidationFacture;
private String valideParAgent;
@Column(columnDefinition = "TEXT")

private String commentaireAgent;
private String resultat; // "VALIDE" ou "REJETE"

    // ── Relations ──────────────────────────────────────────

    /**
     * Prestation parente.
     * ✅ getPrestation() requis par PrestationService.
     */
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "prestation_id")
    private Prestation prestation;

    /**
     * Prestataire assigné (avocat, huissier, expert...).
     * ✅ getPrestataire() requis par PrestationService.
     */
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "prestataire_id")
    private Prestataire prestataire;

    /**
     * Affaire judiciaire liée (si mission de type judiciaire).
     * ✅ getAffaire() utilisé dans les templates.
     */
    @OneToOne(mappedBy = "mission", fetch = FetchType.EAGER)
    private AffaireJudiciaire affaire;


    public boolean isModifiable() {
        return dateValidationAgent == null;
    }


    @OneToOne(mappedBy = "mission", cascade = CascadeType.ALL, 
          orphanRemoval = true, fetch = FetchType.EAGER)
private ResultatMission resultatMission;
}