package com.example.demo.dossier.dto;

import com.example.demo.common.enums.StatutDossier;
import lombok.Data;
import java.math.BigDecimal;

@Data
public class DossierRequest {
    private Long clientId;
    private Long agenceId;
    private BigDecimal montantCreance;
    private String strategie;
    private StatutDossier statut;
    private NewClientRequest newClient;
}
