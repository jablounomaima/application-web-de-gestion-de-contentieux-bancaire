package com.example.contentieux_security.dto;

import com.example.contentieux_security.entity.*;
import com.example.contentieux_security.enums.TypeClient;
import lombok.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class DossierDetailDTO {

    private Long   id;
    private String numeroDossier;
    private String libelle;
    private String description;
    private String statut;
    private LocalDateTime dateCreation;
    private String creePar;
    private String notes;

    // ── Client (objet imbriqué — lu par le template Angular) ──
    private ClientDTO client;

    // ── Champs plats conservés pour rétrocompatibilité ────────
    private Long   clientId;
    private String clientType;
    private String clientTypeClient;
    private String clientNom;
    private String clientPrenom;
    private String clientCin;
    private String clientEmail;
    private String clientTelephone;
    private String clientAdresse;
    private String clientRaisonSociale;
    private String clientRne;

    // ── Agence ────────────────────────────────────────────────
    private String agenceNom;
    private String agenceVille;

    // ── Validation ────────────────────────────────────────────
    private Boolean validationFinanciere;
    private Boolean validationJuridique;
    private String  commentaireFinancier;
    private String  commentaireJuridique;
    private String  validateurFinancierUsername;
    private String  validateurJuridiqueUsername;
    private String  validateurFinancierChoisi;
    private String  validateurJuridiqueChoisi;

    // ── Risques ───────────────────────────────────────────────
    private List<RisqueDTO> risques;
    private Double montantTotalEngagement;

    // ── Historique ────────────────────────────────────────────
    private List<HistoriqueEntryDTO> historique;

    // ══════════════════════════════════════════════════════════
    //  Sous-DTO Client (objet imbriqué)
    // ══════════════════════════════════════════════════════════
    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class ClientDTO {
        private Long   id;
        private String typeClient;       // "PARTICULIER" | "ENTREPRISE"
        private String nom;
        private String prenom;
        private String cin;
        private String raisonSociale;
        private String rne;
        private String email;
        private String telephone;
        private String adresse;
    }

    // ── Sous-DTO Risque ───────────────────────────────────────
    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class RisqueDTO {
        private Long   id;
        private String type;
        private Double montantInitial;
        private Double montantImpaye;
        private String dateEcheance;
        private String description;
        private boolean selectionne;
        private List<GarantieDTO> garanties;
    }

    // ── Sous-DTO Garantie ─────────────────────────────────────
    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class GarantieDTO {
        private Long   id;
        private String typeGarantie;
        private String description;
        private Double valeurEstimee;
        private String documentRef;
        private String statut;
    }

    // ── Sous-DTO Historique ───────────────────────────────────
    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class HistoriqueEntryDTO {
        private String dateAction;
        private String typeAction;
        private String description;
        private String utilisateur;
    }

    // ══════════════════════════════════════════════════════════
    //  Factory
    // ══════════════════════════════════════════════════════════
    public static DossierDetailDTO from(DossierContentieux d,
                                        List<HistoriqueDossier> historique) {

        DossierDetailDTOBuilder b = DossierDetailDTO.builder()
                .id(d.getId())
                .numeroDossier(d.getNumeroDossier())
                .libelle(d.getLibelle())
                .description(d.getDescription())
                .statut(d.getStatut() != null ? d.getStatut().name() : null)
                .dateCreation(d.getDateCreation())
                .creePar(d.getCreePar())
                .notes(d.getNotes())
                .validationFinanciere(d.getValidationFinanciere())
                .validationJuridique(d.getValidationJuridique())
                .commentaireFinancier(d.getCommentaireFinancier())
                .commentaireJuridique(d.getCommentaireJuridique())
                .validateurFinancierUsername(d.getValidateurFinancierUsername())
                .validateurJuridiqueUsername(d.getValidateurJuridiqueUsername())
                .validateurFinancierChoisi(d.getValidateurFinancierChoisi())
                .validateurJuridiqueChoisi(d.getValidateurJuridiqueChoisi());

        // ── Client ────────────────────────────────────────────
        if (d.getClient() != null) {
            Client c = d.getClient();

            String typeStr = c.getTypeClient() != null
                    ? c.getTypeClient().name()
                    : (c.getRaisonSociale() != null
                            && !c.getRaisonSociale().isBlank()
                            ? "ENTREPRISE"
                            : "PARTICULIER");

            // Objet imbriqué — utilisé par le template Angular
            ClientDTO clientDTO = ClientDTO.builder()
                    .id(c.getId())
                    .typeClient(typeStr)
                    .nom(c.getNom())
                    .prenom(c.getPrenom())
                    .cin(c.getCin())
                    .raisonSociale(c.getRaisonSociale())
                    .rne(c.getRne())
                    .email(c.getEmail())
                    .telephone(c.getTelephone())
                    .adresse(c.getAdresse())
                    .build();

            // Champs plats — rétrocompatibilité
            b.client(clientDTO)
             .clientId(c.getId())
             .clientType(typeStr)
             .clientTypeClient(typeStr)
             .clientNom(c.getNom())
             .clientPrenom(c.getPrenom())
             .clientCin(c.getCin())
             .clientRaisonSociale(c.getRaisonSociale())
             .clientRne(c.getRne())
             .clientEmail(c.getEmail())
             .clientTelephone(c.getTelephone())
             .clientAdresse(c.getAdresse());
        }

        // ── Agence ────────────────────────────────────────────
        if (d.getAgence() != null) {
            b.agenceNom(d.getAgence().getNom())
             .agenceVille(d.getAgence().getVille());
        }

        // ── Risques & Garanties ───────────────────────────────
        if (d.getRisques() != null) {
            double total = 0.0;
            List<RisqueDTO> risqueDTOs = new ArrayList<>();
            for (Risque r : d.getRisques()) {
                total += r.getMontantImpaye() != null ? r.getMontantImpaye() : 0;

                List<GarantieDTO> garantieDTOs = new ArrayList<>();
                if (r.getGaranties() != null) {
                    for (Garantie g : r.getGaranties()) {
                        garantieDTOs.add(GarantieDTO.builder()
                                .id(g.getId())
                                .typeGarantie(g.getTypeGarantie())
                                .description(g.getDescription())
                                .valeurEstimee(g.getValeurEstimee())
                                .documentRef(g.getDocumentRef())
                                .statut(g.getStatut())
                                .build());
                    }
                }
                risqueDTOs.add(RisqueDTO.builder()
                        .id(r.getId())
                        .type(r.getType())
                        .montantInitial(r.getMontantInitial())
                        .montantImpaye(r.getMontantImpaye())
                        .dateEcheance(r.getDateEcheance() != null
                                ? r.getDateEcheance().toString() : null)
                        .description(r.getDescription())
                        .selectionne(r.isSelectionne())
                        .garanties(garantieDTOs)
                        .build());
            }
            b.risques(risqueDTOs).montantTotalEngagement(total);
        }

        // ── Historique ────────────────────────────────────────
        if (historique != null) {
            b.historique(historique.stream()
                    .map(h -> HistoriqueEntryDTO.builder()
                            .dateAction(h.getDateAction().toString())
                            .typeAction(h.getTypeAction())
                            .description(h.getDescription())
                            .utilisateur(h.getUtilisateur())
                            .build())
                    .toList());
        }

        return b.build();
    }
}