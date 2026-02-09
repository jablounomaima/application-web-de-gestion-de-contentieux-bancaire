package com.example.demo.agence.dto;

import lombok.Data;

@Data
public class AgenceRequest {
    private String code;
    private String nom;
    private String adresse;
    private String ville;
    private String telephoneContact;
}
