package com.example.contentieux_security.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "garanties")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Garantie {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String type; // Maison, Voiture, Terrain, etc.
    private String valeurEstimée;
    private String description;

    @ManyToOne
    @JoinColumn(name = "dossier_id")
    private Dossier dossier;
}
