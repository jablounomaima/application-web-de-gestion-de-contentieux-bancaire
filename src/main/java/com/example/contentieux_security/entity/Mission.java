package com.example.contentieux_security.entity;

import com.example.contentieux_security.enums.StatutMission;
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

    @Column(name = "numero_mission", unique = true, nullable = false, length = 100)
    private String numeroMission;

    @Column(columnDefinition = "TEXT")
    private String description;

    // ✅ CORRECTION IMPORTANTE
    @Enumerated(EnumType.STRING)
    @Column(name = "statut", nullable = false, length = 50)
    private StatutMission statut;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "prestation_id", nullable = false)
    private Prestation prestation;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "prestataire_id", nullable = false)
    private Prestataire prestataire;

    @Column(name = "date_assignation", nullable = false)
    private LocalDateTime dateAssignation;

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

    @Column(name = "facture_ref", length = 100)
    private String factureRef;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @Column(name = "date_creation", nullable = false)
    private LocalDateTime dateCreation;

    // ✅ CORRECT
    @OneToOne(mappedBy = "mission", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private ResultatMission resultatMission;
    // ✅ CORRECTION: initialisation propre
    @PrePersist
    public void prePersist() {
        if (this.dateCreation == null) {
            this.dateCreation = LocalDateTime.now();
        }
        if (this.dateAssignation == null) {
            this.dateAssignation = LocalDateTime.now();
        }
        if (this.statut == null) {
            this.statut = StatutMission.ASSIGNEE;
        }
    }

    public Mission getMissionAvocatDuDossier(Long dossierId) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getMissionAvocatDuDossier'");
    }
}