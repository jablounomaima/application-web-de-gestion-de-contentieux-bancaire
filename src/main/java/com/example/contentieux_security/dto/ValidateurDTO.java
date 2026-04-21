package com.example.contentieux_security.dto;

import com.example.contentieux_security.enums.TypeValidateur;
import lombok.Data;

@Data
public class ValidateurDTO {
    private Long id;
    private String matricule;
    private String username;
    
    private String nom;
    private String prenom;
    private String email;
    private String password;
    private String telephone;
    private TypeValidateur typeValidateur;
    private Long agenceId;
    private String agenceNom;
    private boolean actif;
}