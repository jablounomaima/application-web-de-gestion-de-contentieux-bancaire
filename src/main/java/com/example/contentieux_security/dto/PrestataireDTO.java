package com.example.contentieux_security.dto;

import lombok.Data;

@Data  // Génère tous les getters/setters
public class PrestataireDTO {
    private Long id;
    private String username;
    private String nom;
    private String prenom;
    private String email;
    private String telephone;
    private String adresse;
    private String type;
    private String specialite;
    private String numeroCartePro;
    private boolean actif;
    private String niveauValidation;
    private Double plafondValidation;
    private String agentResponsableNom;
    private String agenceNom;
}