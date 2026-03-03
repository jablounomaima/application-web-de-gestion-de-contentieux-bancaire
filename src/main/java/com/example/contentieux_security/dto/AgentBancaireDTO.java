package com.example.contentieux_security.dto;

import lombok.Data;
import java.time.LocalDate;

@Data
public class AgentBancaireDTO {
    private Long id;
    private String username;
    private String nom;
    private String prenom;
    private String email;
    private String telephone;
    private String matricule;
    private LocalDate dateEmbauche;
    private String role;
    private Boolean actif;
    private Long agenceId;
    private String nomAgence;
}