package com.example.contentieux_security.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
@Entity
@Table(name = "resultat_mission")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ResultatMission {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(length = 2000)
    private String commentaire;
    
    private LocalDateTime dateSoumission;
    private String soumisePar;
    private LocalDateTime dateCreation;
    private LocalDateTime dateModification;
    
    
    @ManyToOne(fetch = FetchType.LAZY, optional = false)  // ✅ mission obligatoire
    @JoinColumn(name = "mission_id", nullable = false , foreignKey = @ForeignKey(name = "FK_resultat_mission_mission"))  // ✅ force le nom de la FK
    private Mission mission;
    
    @OneToMany(mappedBy = "resultat", cascade = CascadeType.ALL, fetch = FetchType.EAGER, orphanRemoval = true)
    // ← SUPPRIMER @Builder.Default et initialiser dans le constructeur ou utiliser cette syntaxe :
    private List<FichierResultat> fichiers = new ArrayList<>();
    
    // Méthode helper
    public void addFichier(FichierResultat fichier) {
        if (this.fichiers == null) {
            this.fichiers = new ArrayList<>();
        }
        this.fichiers.add(fichier);
        fichier.setResultat(this);
    }
}