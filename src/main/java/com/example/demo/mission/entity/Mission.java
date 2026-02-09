package com.example.demo.mission.entity;

import com.example.demo.common.enums.StatutPrestation;
import com.example.demo.user.entity.Utilisateur;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;

@Entity
@Table(name = "mission")
@Getter
@Setter
public class Mission {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "prestation_id")
    private Prestation prestation;

    @ManyToOne(optional = false)
    @JoinColumn(name = "intervenant_id")
    private Utilisateur intervenant; // Huissier ou Expert

    @Column(columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StatutPrestation statut;

    @Column(name = "date_execution")
    private LocalDateTime dateExecution;

    @Column(name = "proces_verbal", columnDefinition = "TEXT")
    private String procesVerbal;

    @Column(name = "date_reception_pv")
    private LocalDateTime dateReceptionPV;
}
