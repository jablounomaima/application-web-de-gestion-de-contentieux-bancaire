package com.example.demo.dossier.dto;

import com.example.demo.common.enums.StatutDossier;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class DossierResponse {
    private Long id;
    private String reference;
    private String clientNom;
    private String agenceNom;
    private BigDecimal montantCreance;
    private BigDecimal montantRecouvre;
    private double tauxRecouvrement;
    private StatutDossier statut;
    private String strategie;
    private LocalDateTime dateCreation;
    private String agentCreateurUsername;
}
