package com.bank.contentieux.entity;

import jakarta.persistence.*;
import java.util.List;

@Entity
@Table(name = "client")  // Le nom doit correspondre à ta table en DB
public class Client {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long idClient;

    private String nom;
    private String prenom;
    private String email;
    private String tel;

    // Relation ManyToOne avec Agence
    @ManyToOne
    @JoinColumn(name = "agence_id") // clé étrangère dans la table client
    private Agence agence;

    // Relation OneToMany avec Dossier
    @OneToMany(mappedBy = "client", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Dossier> dossiers;

    // Constructeurs
    public Client() {}

    public Client(String nom, String prenom, String email, String tel, Agence agence) {
        this.nom = nom;
        this.prenom = prenom;
        this.email = email;
        this.tel = tel;
        this.agence = agence;
    }

    // Getters et Setters
    public Long getIdClient() { return idClient; }
    public void setIdClient(Long idClient) { this.idClient = idClient; }

    public String getNom() { return nom; }
    public void setNom(String nom) { this.nom = nom; }

    public String getPrenom() { return prenom; }
    public void setPrenom(String prenom) { this.prenom = prenom; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getTel() { return tel; }
    public void setTel(String tel) { this.tel = tel; }

    public Agence getAgence() { return agence; }
    public void setAgence(Agence agence) { this.agence = agence; }

    public List<Dossier> getDossiers() { return dossiers; }
    public void setDossiers(List<Dossier> dossiers) { this.dossiers = dossiers; }
}
