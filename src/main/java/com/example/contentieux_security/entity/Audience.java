package com.example.contentieux_security.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})

/**
 * Entité Audience.
 *
 * ✅ @Getter + @Setter → corrige toutes les erreurs
 *    "cannot find symbol: method setDateAudience / setHeure / setSalle /
 *     setMotif / setAffaire / setResultat / setStatut / setProchaineAudience /
 *     getAffaire() ..."
 *    dans AffaireJudiciaireService.
 */
@Entity
@Table(name = "audience")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Audience {

    public enum StatutAudience {
        PLANIFIEE,
        TENUE,
        RENVOYEE,
        ANNULEE
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private LocalDate dateAudience;

    @Column(length = 10)
    private String heure;

    @Column(length = 50)
    private String salle;

    @Column(columnDefinition = "TEXT")
    private String motif;

    @Column(columnDefinition = "TEXT")
    private String resultat;

    private LocalDate prochaineAudience;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private StatutAudience statut = StatutAudience.PLANIFIEE;

    // ── Relation ───────────────────────────────────────────

    /**
     * Affaire judiciaire parente.
     * ✅ getAffaire() requis par AffaireJudiciaireService (ligne 110).
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "affaire_id", nullable = false)
    private AffaireJudiciaire affaire;
}