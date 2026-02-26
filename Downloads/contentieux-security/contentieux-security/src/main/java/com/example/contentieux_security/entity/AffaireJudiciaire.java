package com.example.contentieux_security.entity;

import jakarta.persistence.*;
import java.time.LocalDate;
import lombok.*;

@Entity
@Table(name = "affaires_judiciaires")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AffaireJudiciaire {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String numeroAffaire;
    private String juridiction;
    private String objet;
    private LocalDate dateIntroduction;
    private LocalDate dateProchaineAudience;

    @Enumerated(EnumType.STRING)
    private StatutAffaire statut;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "dossier_id")
    private Dossier dossier;

    // Add relation to Prestation as well since it was there
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "prestation_id")
    private Prestation prestation;

    private String avocatEnCharge;
    private String huissierEnCharge;
    private String expertEnCharge;

    private String notes;
}
