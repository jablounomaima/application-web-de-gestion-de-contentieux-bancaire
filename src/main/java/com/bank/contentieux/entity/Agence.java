package com.bank.contentieux.entity;

import jakarta.persistence.*;
import java.util.List;

@Entity
@Table(name = "agence")
public class Agence {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String nom;
    private String adresse;
    private String email; // <- à ajouter

    // Relation OneToMany avec Client
    @OneToMany(mappedBy = "agence", cascade = CascadeType.ALL)
    private List<Client> clients;

    // Constructeurs
    public Agence() {}
    
    public Agence(String nom, String adresse) {
        this.nom = nom;
        this.adresse = adresse;
    }

    // Getters et Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getNom() { return nom; }
    public void setNom(String nom) { this.nom = nom; }

    public String getAdresse() { return adresse; }
    public void setAdresse(String adresse) { this.adresse = adresse; }

    public List<Client> getClients() { return clients; }
    public void setClients(List<Client> clients) { this.clients = clients; }

     public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

}
