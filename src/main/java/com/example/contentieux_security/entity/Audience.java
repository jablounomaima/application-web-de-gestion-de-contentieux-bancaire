

package com.example.contentieux_security.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "audiences")
@Getter @Setter @NoArgsConstructor
public class Audience {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private LocalDate dateAudience;

    private String heure;          // ex: "09:30"
    private String salle;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private StatutAudience statut = StatutAudience.PLANIFIEE;

    @Column(columnDefinition = "TEXT")
    private String motif;          // objet de l'audience

    @Column(columnDefinition = "TEXT")
    private String resultat;       // ce qui s'est passé pendant l'audience

    private LocalDate prochaineAudience; // si renvoyée

    @Column(nullable = false)
    private LocalDateTime dateCreation = LocalDateTime.now();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "affaire_id", nullable = false)
    private AffaireJudiciaire affaire;

    public enum StatutAudience {
        PLANIFIEE, TENUE, RENVOYEE, ANNULEE
    }
}