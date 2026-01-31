package com.bank.contentieux.entity;

import jakarta.persistence.*;
import java.util.*;
import java.time.LocalDate;

@Entity
@Inheritance(strategy = InheritanceType.JOINED)
public abstract class Utilisateur {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long idUser;

    private String nom;
    private String prenom;
    private String login;
    private String role;

   

    // getters & setters
}
