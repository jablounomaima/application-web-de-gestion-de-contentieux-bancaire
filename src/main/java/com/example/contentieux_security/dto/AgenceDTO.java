package com.example.contentieux_security.dto;

import lombok.Data;

@Data
public class AgenceDTO {
    private Long id;
    private String code;
    private String nom;
    private String adresse;
    private String ville;
    private String telephone;
    private String email;
    private int nombreAgents;
      // ✅ AJOUTÉ : Ce champ manquait !
    private String directeur;
}