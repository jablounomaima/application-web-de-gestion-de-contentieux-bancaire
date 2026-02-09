package com.example.demo.dossier.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "risque")
@Getter
@Setter
public class Risque {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String description;

    @Column(nullable = false)
    private String niveau; // FAIBLE, MOYEN, ELEVE, CRITIQUE

    @Column(name = "plan_action")
    private String planAction;

    @ManyToOne
    @JoinColumn(name = "dossier_id")
    private Dossier dossier;
}
