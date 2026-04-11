package com.example.contentieux_security.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "resultats_mission")
@Getter @Setter @NoArgsConstructor
public class ResultatMission {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Fichier uploadé (assignation.pdf, jugement.pdf, PV...)
    private String nomFichierOriginal;
    private String nomFichierServeur;   // UUID_assignation.pdf
    private String typeMime;
    private Long tailleFichier;

    // Commentaire libre du prestataire
    @Column(columnDefinition = "TEXT", nullable = false)
    private String commentaire;

    @Column(nullable = false)
    private LocalDateTime dateSoumission = LocalDateTime.now();

    private String soumisePar;  // username du prestataire

    // ✅ CORRECT : @JoinColumn = propriétaire de la relation
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "mission_id", nullable = false)
    private Mission mission;

    public String getTailleFormatee() {
        if (tailleFichier == null) return "-";
        if (tailleFichier < 1024) return tailleFichier + " B";
        if (tailleFichier < 1024 * 1024) return String.format("%.1f KB", tailleFichier / 1024.0);
        return String.format("%.1f MB", tailleFichier / (1024.0 * 1024));
    }

    public String getIconeType() {
        if (typeMime == null) return "bi-file-earmark";
        if (typeMime.startsWith("image/")) return "bi-file-earmark-image";
        if (typeMime.equals("application/pdf")) return "bi-file-earmark-pdf";
        if (typeMime.contains("word")) return "bi-file-earmark-word";
        return "bi-file-earmark";
    }
}