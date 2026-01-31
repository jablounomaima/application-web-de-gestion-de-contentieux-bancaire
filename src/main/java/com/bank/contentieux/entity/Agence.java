
package com.bank.contentieux.entity;

import jakarta.persistence.*;
import java.util.List;
@Entity
public class Agence {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long idAgence;

    private String nom;
    private String adresse;

    @OneToMany(mappedBy = "agence")
    private List<Utilisateur> utilisateurs;
}
