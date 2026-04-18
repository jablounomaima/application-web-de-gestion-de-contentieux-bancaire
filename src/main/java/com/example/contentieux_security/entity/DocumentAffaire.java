package com.example.contentieux_security.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Entité DocumentAffaire.
 *
 * ✅ @Getter + @Setter → corrige les erreurs
 *    "cannot find symbol: method setNomFichierOriginal / setNomFichierServeur /
 *     setCheminFichier / setTypeMime / setTailleFichier / setTypeDocument ..."
 *    dans AffaireJudiciaireService.
 */
@Entity
@Table(name = "document_affaire")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DocumentAffaire {

    public enum TypeDocument {
        ASSIGNATION,
        JUGEMENT,
        PV_AUDIENCE,
        TITRE_FONCIER,
        CONTRAT,
        RAPPORT_EXPERTISE,
        PV_SAISIE,
        FACTURE_HONORAIRES,
        PHOTO_BIEN,
        AUTRE
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(length = 255)
    private String nomFichierOriginal;

    @Column(length = 255)
    private String nomFichierServeur;

    @Column(length = 500)
    private String cheminFichier;

    @Column(length = 100)
    private String typeMime;

    private long tailleFichier;

    @Enumerated(EnumType.STRING)
    @Column(length = 30)
    private TypeDocument typeDocument;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(length = 100)
    private String uploadeePar;

    private LocalDateTime dateUpload;

    // ── Helpers pour Thymeleaf ──────────────────────────────

    /** Taille formatée (ex : "1.2 MB") utilisée dans les templates. */
    public String getTailleFormatee() {
        if (tailleFichier < 1024)          return tailleFichier + " B";
        if (tailleFichier < 1_048_576)     return (tailleFichier / 1024) + " KB";
        return String.format("%.1f MB", tailleFichier / 1_048_576.0);
    }

    /** Icône Bootstrap Icons selon le type MIME. */
    public String getIconeType() {
        if (typeMime == null) return "bi-file-earmark";
        if (typeMime.equals("application/pdf"))  return "bi-file-earmark-pdf";
        if (typeMime.startsWith("image/"))       return "bi-file-earmark-image";
        if (typeMime.contains("word"))           return "bi-file-earmark-word";
        if (typeMime.contains("excel")
         || typeMime.contains("spreadsheet"))   return "bi-file-earmark-excel";
        return "bi-file-earmark";
    }

    // ── Relation ───────────────────────────────────────────

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "affaire_id", nullable = false)
    private AffaireJudiciaire affaire;
}