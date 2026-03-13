package com.example.contentieux_security.dto;

import com.example.contentieux_security.entity.StatutDossier;
import lombok.Getter;
import lombok.Setter;
import java.util.List;

@Getter @Setter
public class DossierCreationRequest {

    // ── Client ────────────────────────────────────────────────────
    private Long clientId;              // si client existant
    // Si nouveau client :
    private String clientNom;
    private String clientPrenom;
    private String clientCin;
    private String clientEmail;
    private String clientTelephone;
    private String clientAdresse;
    private String clientTypeClient;    // PARTICULIER | ENTREPRISE
    private String clientRaisonSociale;

    // ── Dossier ───────────────────────────────────────────────────
    private String libelle;
    private String notes;

    // ── Risques ───────────────────────────────────────────────────
    private List<RisqueRequest> risques;

    @Getter @Setter
    public static class RisqueRequest {
        private String type;
        private Double montantInitial;
        private Double montantImpaye;
        private String dateEcheance;    // format: yyyy-MM-dd
        private String description;
        private List<GarantieRequest> garanties;
    }

    @Getter @Setter
    public static class GarantieRequest {
        private String typeGarantie;
        private String description;
        private Double valeurEstimee;
        private String documentRef;
    }
}