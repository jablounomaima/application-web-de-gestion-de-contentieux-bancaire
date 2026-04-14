package com.example.contentieux_security.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "fichier_resultat")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FichierResultat {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false)
    private String nomFichierOriginal;
    
    @Column(nullable = false, unique = true)
    private String nomFichierServeur;
    
    private String typeMime;
    private Long tailleFichier;
    private LocalDateTime dateUpload;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "resultat_id")
    private ResultatMission resultat;
    
    // ✅ Ajoutez cette méthode pour l'icône
    public String getIconeType() {
        if (typeMime == null) return "bi-file-earmark";
        
        if (typeMime.equals("application/pdf")) {
            return "bi-file-earmark-pdf text-danger";
        } else if (typeMime.startsWith("image/")) {
            return "bi-file-earmark-image text-info";
        } else if (typeMime.contains("word") || typeMime.contains("document")) {
            return "bi-file-earmark-word text-primary";
        } else if (typeMime.contains("sheet") || typeMime.contains("excel")) {
            return "bi-file-earmark-excel text-success";
        } else if (typeMime.contains("zip") || typeMime.contains("compressed")) {
            return "bi-file-earmark-zip text-secondary";
        } else {
            return "bi-file-earmark";
        }
    }
    
    // ✅ Ajoutez cette méthode pour la taille formatée
    public String getTailleFormatee() {
        if (tailleFichier == null) return "0 B";
        if (tailleFichier < 1024) return tailleFichier + " B";
        if (tailleFichier < 1024 * 1024) return String.format("%.1f KB", tailleFichier / 1024.0);
        return String.format("%.1f MB", tailleFichier / (1024.0 * 1024.0));
    }
}