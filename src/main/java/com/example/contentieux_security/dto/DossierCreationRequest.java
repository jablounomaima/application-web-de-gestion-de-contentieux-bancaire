package com.example.contentieux_security.dto;

import lombok.Getter;
import lombok.Setter;
import java.util.List;

/**
 * DTO de création d'un dossier contentieux.
 * Utilisé par DossierController (POST /agent/dossiers/creer)
 * et DossierService.creerDossier().
 */
@Getter @Setter
public class DossierCreationRequest {

    // ── Client ────────────────────────────────────────────────────
    private Long   clientId;            // renseigné si client existant → les champs ci-dessous ignorés

    // Si nouveau client (clientId == null) :
    private String clientNom;
    private String clientPrenom;
    private String clientCin;
    private String clientEmail;
    private String clientTelephone;
    private String clientAdresse;
    private String clientTypeClient;    // PARTICULIER | ENTREPRISE  (optionnel)
    private String clientRaisonSociale; // pour les entreprises        (optionnel)

    // ── Dossier ───────────────────────────────────────────────────
    private String libelle;
    private String description;         // description longue du dossier
    private String notes;               // notes internes de l'agent

    // ── Risques ───────────────────────────────────────────────────
    private List<RisqueRequest> risques;

    // ─────────────────────────────────────────────────────────────
    @Getter @Setter
    public static class RisqueRequest {
        private String  type;
        private Double  montantInitial;
        private Double  montantImpaye;
        private String  dateEcheance;   // format : yyyy-MM-dd
        private String  description;
        private List<GarantieRequest> garanties;
    }

    // ─────────────────────────────────────────────────────────────
    @Getter @Setter
    public static class GarantieRequest {
        private String typeGarantie;    // VEHICULE, IMMOBILIER, HYPOTHEQUE...
        private String description;
        private Double valeurEstimee;
        private String documentRef;
    }
}