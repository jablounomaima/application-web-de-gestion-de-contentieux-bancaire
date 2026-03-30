package com.example.contentieux_security.dto;

import com.example.contentieux_security.enums.TypePrestataire;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class PrestataireCreationRequest {

    private Long id;

    @NotBlank(message = "Le nom d'utilisateur est obligatoire")
    private String username;

    private String motDePasse;

    @NotBlank(message = "Le prénom est obligatoire")
    private String prenom;

    @NotBlank(message = "Le nom est obligatoire")
    private String nom;

    @NotBlank(message = "L'email est obligatoire")
    @Email
    private String email;

    private String telephone;

    private String specialite;

    private String numeroCartePro;

    private String adresse;

    @NotNull(message = "Le type de prestataire est obligatoire")
    private TypePrestataire typePrestataire; // ✅ TypePrestataire depuis entity

    // ✅ Agence assignée par l'admin — obligatoire pour les validateurs
    private Long agenceId;

    // Champs spécifiques aux validateurs (optionnels)
    private String niveauValidation;
    private Double plafondValidation;

    // ✅ Méthode utilitaire — retourne le type (compatibilité avec l'ancien code)
    public TypePrestataire getType() {
        return typePrestataire;
    }

    public void setType(TypePrestataire type) {
        this.typePrestataire = type;
    }
}