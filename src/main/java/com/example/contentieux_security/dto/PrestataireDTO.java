package com.example.contentieux_security.dto;

import com.example.contentieux_security.entity.TypePrestataire;
import lombok.Data;

@Data
public class PrestataireDTO {
    private Long id;
    private String username;
    private String prenom;
    private String nom;
    private String email;
    private String telephone;
    private String adresse;
    private TypePrestataire type;
    private String specialite;
    private String numeroCartePro;
    private String niveauValidation;
    private Double plafondValidation;
    private boolean actif;
}