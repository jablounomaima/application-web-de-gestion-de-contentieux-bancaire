package com.example.contentieux_security.dto;

import lombok.Data;
import java.time.LocalDate;

@Data
public class AgentCreationRequest {
    private String nom;
    private String prenom;
    private String username;
    private String password;
    private String email;
    private String telephone;
    private String matricule;
    private LocalDate dateEmbauche;
    private String role;
    private Long agenceId;
    // getters/setters
}