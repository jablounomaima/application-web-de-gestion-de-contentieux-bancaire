package com.example.contentieux_security.entity;

import jakarta.persistence.*;
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

    private String tribunal;
    private String numeroAffaire;
    private String statut;

    @OneToOne
    @JoinColumn(name = "prestation_id")
    private Prestation prestation;
}
