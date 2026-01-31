package com.bank.contentieux.entity;

import jakarta.persistence.*;
import java.util.List;
import java.time.LocalDate;


@Entity
public class Facture {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long idFacture;

    private String numeroFacture;
    private LocalDate dateEmission;
    private Double montant;
    private String statut;

    @ManyToOne
    private Dossier dossier;

    @ManyToOne
    private Utilisateur emisePar;

    @OneToOne
    private Paiement paiement;
}
