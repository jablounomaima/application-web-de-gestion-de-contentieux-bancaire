package com.bank.contentieux.entity;

import jakarta.persistence.*;

@Entity
public class Garantie {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long idGarantie;

    private String type;
    private Double valeur;

    @ManyToOne
    private Risque risque;
}
