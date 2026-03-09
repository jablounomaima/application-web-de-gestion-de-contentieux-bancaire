package com.example.contentieux_security.dto;

import com.example.contentieux_security.entity.TypePrestataire;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class PrestataireCreationRequest {

    @NotBlank(message = "Le prénom est requis")
    private String prenom;

    @NotBlank(message = "Le nom est requis")
    private String nom;

    @NotBlank(message = "L'email est requis")
    @Email(message = "Email invalide")
    private String email;

    @NotBlank(message = "Le nom d'utilisateur est requis")
    private String username;

    private String telephone;
    private String adresse;
    private String specialite;
    private String numeroCartePro;

    @NotNull(message = "Le type de prestataire est requis")
    private TypePrestataire type;

    private String niveauValidation;
    private Double plafondValidation;

    // Mot de passe temporaire généré ou saisi
    private String motDePasse;
}