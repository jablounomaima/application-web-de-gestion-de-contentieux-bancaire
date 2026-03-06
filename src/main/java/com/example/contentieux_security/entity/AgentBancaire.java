package com.example.contentieux_security.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@Entity
@Table(name = "agents_bancaires")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AgentBancaire {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String username;

    @Column(nullable = false)
    private String password;

    @Column(nullable = false)
    private String nom;

    @Column(nullable = false)
    private String prenom;

    @Column(nullable = false)
    private String email;

    private String telephone;

    @Column(nullable = false)
    private boolean actif = true;  // boolean (pas Boolean)

    // ✅ CHAMPS MANQUANTS AJOUTÉS
    private String matricule;
    
    private LocalDate dateEmbauche;
    
    private String role;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "agence_id", nullable = false)
    @JsonIgnoreProperties({"agents", "dossiers", "clients"})
    private Agence agence;

    // ✅ MÉTHODE AJOUTÉE pour DTO
    public String getNomAgence() {
        return agence != null ? agence.getNom() : null;
    }

    public String getNomComplet() {
        return prenom + " " + nom;
    }
}