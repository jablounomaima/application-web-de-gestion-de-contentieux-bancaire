package com.example.contentieux_security.entity;

import jakarta.persistence.*;
import java.time.LocalDate;
import lombok.*;
import java.time.LocalDate;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Echeance {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String libelle;
    private LocalDate dateEcheance;

    @Enumerated(EnumType.STRING)
    private TypeEcheance type;

    @Enumerated(EnumType.STRING)
    private PrioriteEcheance priorite;

    private String description;
    private Boolean estTraitee;
    private LocalDate dateTraitement;
    private String commentaire;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "dossier_id")
    private Dossier dossier;
}
