package com.example.demo.mission.entity;

import com.example.demo.common.enums.StatutPrestation;
import com.example.demo.dossier.entity.Dossier;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "prestation")
@Getter
@Setter
public class Prestation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "dossier_id")
    private Dossier dossier;

    @Column(nullable = false)
    private String type; // CONSTAT, SIGNIFICATION, EXPERTISE_IMMOBILIERE, etc.

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StatutPrestation statut;

    @Column(name = "date_demande")
    private LocalDateTime dateDemande;

    @OneToMany(mappedBy = "prestation", cascade = CascadeType.ALL)
    private List<Mission> missions = new ArrayList<>();

    @PrePersist
    protected void onCreate() {
        dateDemande = LocalDateTime.now();
        if (statut == null) statut = StatutPrestation.DEMANDEE;
    }
}
