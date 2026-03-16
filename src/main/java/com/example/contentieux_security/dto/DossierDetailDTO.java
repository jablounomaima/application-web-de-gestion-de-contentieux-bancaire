package com.example.contentieux_security.dto;

import com.example.contentieux_security.entity.*;
import com.example.contentieux_security.enums.DossierStatus;
import com.itextpdf.text.pdf.draw.LineSeparator;
import lombok.*;
import java.time.LocalDateTime;
import java.util.List;

/**
 * DTO de lecture pour l'affichage complet d'un dossier.
 * Évite les LazyInitializationException dans les templates Thymeleaf.
 */
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class DossierDetailDTO {

    private Long id;
    private String numeroDossier;
    private String libelle;
    private String description;         // ← ajouté
    private String statut;
    private LocalDateTime dateCreation;
    private String creePar;
    private String notes;

    // ── Client ────────────────────────────────────────────────
    private Long clientId;
    private String clientNom;
    private String clientPrenom;
    private String clientCin;
    private String clientEmail;
    private String clientTelephone;
    private String clientAdresse;    // ← ajouter
    // ── Agence ────────────────────────────────────────────────
    private String agenceNom;
    private String agenceVille;

    // ── Validation ────────────────────────────────────────────
  // ── Validation ────────────────────────────────────────────
private Boolean validationFinanciere;
private Boolean validationJuridique;
private String commentaireFinancier;
private String commentaireJuridique;
private String validateurFinancierUsername;
private String validateurJuridiqueUsername;
private String validateurFinancierChoisi;    // ← ajouter
private String validateurJuridiqueChoisi;    // ← ajouter
    // ── Risques ───────────────────────────────────────────────
    private List<RisqueDTO> risques;
    private Double montantTotalEngagement;

    // ── Historique ────────────────────────────────────────────
    private List<HistoriqueEntryDTO> historique;


   

    // ── Sous-DTO Risque ───────────────────────────────────────
    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class RisqueDTO {
        private Long id;
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
        private Long id;
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

    

    

    // ── Factory method depuis l'entité ────────────────────────
    public static DossierDetailDTO from(DossierContentieux d,
                                        List<HistoriqueDossier> historique) {
        DossierDetailDTOBuilder b = DossierDetailDTO.builder()
                .id(d.getId())
                .numeroDossier(d.getNumeroDossier())
                .libelle(d.getLibelle())
                .description(d.getDescription())         // ← ajouté
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



        if (d.getClient() != null) {
            b.clientId(d.getClient().getId())
             .clientNom(d.getClient().getNom())
             .clientPrenom(d.getClient().getPrenom())
             .clientCin(d.getClient().getCin())
             .clientEmail(d.getClient().getEmail())
             .clientTelephone(d.getClient().getTelephone());
        }

        if (d.getAgence() != null) {
            b.agenceNom(d.getAgence().getNom())
             .agenceVille(d.getAgence().getVille());
        }

        if (d.getRisques() != null) {
            double total = 0.0;
            java.util.List<RisqueDTO> risqueDTOs = new java.util.ArrayList<>();
            for (Risque r : d.getRisques()) {
                total += r.getMontantImpaye() != null ? r.getMontantImpaye() : 0;
                java.util.List<GarantieDTO> garantieDTOs = new java.util.ArrayList<>();
                if (r.getGaranties() != null) {
                    for (Garantie g : r.getGaranties()) {
                        garantieDTOs.add(GarantieDTO.builder()
                                .id(g.getId()).typeGarantie(g.getTypeGarantie())
                                .description(g.getDescription()).valeurEstimee(g.getValeurEstimee())
                                .documentRef(g.getDocumentRef()).statut(g.getStatut())
                                .build());
                    }
                }
                risqueDTOs.add(RisqueDTO.builder()
                        .id(r.getId()).type(r.getType())
                        .montantInitial(r.getMontantInitial()).montantImpaye(r.getMontantImpaye())
                        .dateEcheance(r.getDateEcheance() != null ? r.getDateEcheance().toString() : null)
                        .description(r.getDescription()).selectionne(r.isSelectionne())
                        .garanties(garantieDTOs).build());
            }
            b.risques(risqueDTOs).montantTotalEngagement(total);
        }

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