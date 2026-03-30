package com.example.contentieux_security.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AgenceDTO {
    private Long id;
    private String code;
    private String nom;
    private String adresse;
    private String ville;
    private String telephone;
    private String email;
    private String directeur;
    private int nombreAgents;
}