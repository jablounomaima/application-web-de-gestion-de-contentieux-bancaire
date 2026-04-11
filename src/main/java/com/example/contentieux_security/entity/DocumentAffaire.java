package com.example.contentieux_security.entity;



import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "documents_affaire")
@Getter @Setter @NoArgsConstructor
public class DocumentAffaire {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String nomFichierOriginal;  // nom affiché à l'utilisateur

    @Column(nullable = false)
    private String nomFichierServeur;   // UUID_nomoriginal.pdf stocké sur disque

    @Column(nullable = false)
    private String cheminFichier;       // chemin complet sur disque

    private String typeMime;            // application/pdf, image/jpeg...
    private Long tailleFichier;         // en octets

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private TypeDocument typeDocument;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false)
    private String uploadeePar;         // username du prestataire

    @Column(nullable = false)
    private LocalDateTime dateUpload = LocalDateTime.now();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "affaire_id", nullable = false)
    private AffaireJudiciaire affaire;

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

    // Taille lisible ex: "2.3 MB"
    public String getTailleFormatee() {
        if (tailleFichier == null) return "-";
        if (tailleFichier < 1024) return tailleFichier + " B";
        if (tailleFichier < 1024 * 1024) return String.format("%.1f KB", tailleFichier / 1024.0);
        return String.format("%.1f MB", tailleFichier / (1024.0 * 1024));
    }

    // Pour afficher une icône selon le type MIME
    public String getIconeType() {
        if (typeMime == null) return "bi-file-earmark";
        if (typeMime.startsWith("image/")) return "bi-file-earmark-image";
        if (typeMime.equals("application/pdf")) return "bi-file-earmark-pdf";
        if (typeMime.contains("word")) return "bi-file-earmark-word";
        if (typeMime.contains("excel") || typeMime.contains("spreadsheet")) return "bi-file-earmark-excel";
        return "bi-file-earmark";
    }
}
