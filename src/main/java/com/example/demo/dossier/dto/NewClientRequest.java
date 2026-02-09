package com.example.demo.dossier.dto;

import lombok.Data;

@Data
public class NewClientRequest {
    private String type; // "PHYSIQUE" or "MORALE"
    private String telephone;
    private String email;
    private String adresse;
    private String ville;
    
    // Physique
    private String nom;
    private String prenom;
    private String cin;
    
    // Morale
    private String raisonSociale;
    private String numeroRC;
}
