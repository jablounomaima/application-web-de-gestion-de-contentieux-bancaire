package com.example.contentieux_security.entity;

import com.example.contentieux_security.enums.StatutMission;  // ← enums/
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "missions")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Mission {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "numero_mission", unique = true, nullable = false)
    private String numeroMission;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private StatutMission statut = StatutMission.ASSIGNEE;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "prestation_id", nullable = false)
    private Prestation prestation;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "prestataire_id", nullable = false)
    private Prestataire prestataire;

    @Column(name = "date_assignation", nullable = false)
    private LocalDateTime dateAssignation = LocalDateTime.now();

    @Column(name = "date_debut")
    private LocalDate dateDebut;

    @Column(name = "date_fin_prevue")
    private LocalDate dateFinPrevue;

    @Column(name = "date_fin_reelle")
    private LocalDate dateFinReelle;

    @Column(name = "pv_mission", columnDefinition = "TEXT")
    private String pvMission;

    @Column(name = "montant_facture")
    private Double montantFacture;

    @Column(name = "facture_ref")
    private String factureRef;

    @Column(columnDefinition = "TEXT")
    private String notes;
}