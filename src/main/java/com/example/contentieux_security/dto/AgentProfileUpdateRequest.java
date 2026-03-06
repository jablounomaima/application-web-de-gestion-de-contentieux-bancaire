package com.example.contentieux_security.dto;

import lombok.Data;

@Data
public class AgentProfileUpdateRequest {
    private String nom;
    private String prenom;
    private String email;
    private String telephone;
    // Note: On ne permet pas de changer le username, le matricule, l'agence
}