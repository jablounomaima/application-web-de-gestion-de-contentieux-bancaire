package com.example.demo.client.dto;

import lombok.Data;

@Data
public class ClientResponse {
    private Long id;
    private String nom;
    private String type; // Physique ou Morale
    private String ville;
    private String telephone;
}
