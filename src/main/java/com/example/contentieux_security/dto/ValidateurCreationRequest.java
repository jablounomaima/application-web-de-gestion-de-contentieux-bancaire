package com.example.contentieux_security.dto;

import com.example.contentieux_security.enums.TypeValidateur;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ValidateurCreationRequest {

    @NotBlank(message = "Le nom d'utilisateur est obligatoire")
    private String username;

    @NotBlank(message = "Le prénom est obligatoire")
    private String prenom;

    @NotBlank(message = "Le nom est obligatoire")
    private String nom;

    @NotBlank(message = "L'email est obligatoire")
    @Email
    private String email;

    private String telephone;

    private String matricule;

    // ✅ TypeValidateur — séparé de TypePrestataire
    @NotNull(message = "Le type de validateur est obligatoire")
    private TypeValidateur type;

    // ✅ Agence assignée par l'admin
    @NotNull(message = "L'agence est obligatoire")
    private Long agenceId;

  
}