package com.example.contentieux_security.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CompteBancaireAgenceDTO {
    private Long id;
    private String banque;
    private String rib;
    private String titulaireCompte;
    private boolean actif;
    private Long agenceId;
    private String agenceNom;
}
