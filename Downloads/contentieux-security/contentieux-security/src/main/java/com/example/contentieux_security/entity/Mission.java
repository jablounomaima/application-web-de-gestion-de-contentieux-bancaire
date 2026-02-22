package com.example.contentieux_security.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "missions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Mission {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String titre;
    private String description;

    private String prestataireNom;
    private String prestataireRole; // AVOCAT, EXPERT, HUISSIER

    private LocalDateTime dateAssignation;

    @ManyToOne
    @JoinColumn(name = "prestation_id")
    private Prestation prestation;

    private String statut; // EN_COURS, TERMINEE
}
