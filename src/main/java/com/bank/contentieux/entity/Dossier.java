package com.bank.contentieux.entity;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.util.List;

@Entity
public class Dossier {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long idDossier;

    private String statut;
    private Double montantCredit;
    private LocalDate dateCreation;

    @OneToMany(mappedBy = "dossier")
    private List<Risque> risques;
}
