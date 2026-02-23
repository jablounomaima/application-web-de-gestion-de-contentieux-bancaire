package com.example.contentieux_security.entity;

import jakarta.persistence.*;
import lombok.*;
import java.util.List;

@Entity
@Table(name = "clients")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Client {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Common fields
    private String type; // PHYSIQUE or MORALE

    // PHYSIQUE fields
    private String cin;
    private String passeport;
    private String carteSejour;
    private String nom;
    private String prenom;
    private String dateNaissance;
    private String adresse;

    // MORALE fields
    private String raisonSociale;
    private String rne; // or Registre de commerce
    private String matriculeFiscal;
    private String adresseSiege;
    private String representantLegal;

    @OneToMany(mappedBy = "client")
    private List<Dossier> dossiers;
}
