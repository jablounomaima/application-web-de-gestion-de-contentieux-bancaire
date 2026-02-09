package com.example.demo.procedure.entity;

import com.example.demo.common.enums.StatutAudience;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;

@Entity
@Table(name = "audience")
@Getter
@Setter
public class Audience {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "affaire_id")
    private AffaireJudiciaire affaire;

    @Column(name = "date_audience", nullable = false)
    private LocalDateTime dateAudience;

    @Column(nullable = false)
    private String type; // Référé, Fond, Commercial, etc.

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StatutAudience statut;

    @Column(columnDefinition = "TEXT")
    private String compteRendu;

    @Column(name = "decision_intermediaire")
    private String decisionIntermediaire;
}
