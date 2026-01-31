package com.bank.contentieux.entity;

import jakarta.persistence.*;
import java.util.List;
import java.time.LocalDate;

@Entity
public class Affaire {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long idAffaire;

    private String statut;

    @ManyToOne
    private TypeAffaire typeAffaire;

    @ManyToOne
    private Tribunal tribunal;

    @ManyToOne
    private Avocat avocat;

    @OneToMany(mappedBy = "affaire")
    private List<Audience> audiences;

    @OneToMany(mappedBy = "affaire")
    private List<Mission> missions;
}
