package com.bank.contentieux.entity;

import jakarta.persistence.*;
import java.time.LocalDate;
@Entity
@Table(name = "resultat")
public class Resultat {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long idResultat;

    private String description;
    private LocalDate dateResultat;

    @ManyToOne
    @JoinColumn(name = "dossier_id")
    private Dossier dossier;

    // Relation vers l'agence pour l'envoi de mail
    @ManyToOne
    @JoinColumn(name = "agence_id")
    private Agence agence;

    public Resultat() {}

    public Resultat(String description, LocalDate dateResultat, Dossier dossier, Agence agence) {
        this.description = description;
        this.dateResultat = dateResultat;
        this.dossier = dossier;
        this.agence = agence;
    }

    // Getters et Setters
    public Long getIdResultat() { return idResultat; }
    public void setIdResultat(Long idResultat) { this.idResultat = idResultat; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public LocalDate getDateResultat() { return dateResultat; }
    public void setDateResultat(LocalDate dateResultat) { this.dateResultat = dateResultat; }

    public Dossier getDossier() { return dossier; }
    public void setDossier(Dossier dossier) { this.dossier = dossier; }

    public Agence getAgence() { return agence; }
    public void setAgence(Agence agence) { this.agence = agence; }
}
