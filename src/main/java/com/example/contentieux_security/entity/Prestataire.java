package com.example.contentieux_security.entity;

import com.example.contentieux_security.enums.TypePrestataire;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

/**
 * Entité Prestataire.
 *
 * ✅ FIX : @Data supprimé — il était en conflit avec @Getter @Setter
 *          (@Data génère aussi equals/hashCode/toString sur entités JPA
 *           ce qui cause des boucles infinies sur les relations lazy).
 *          @Getter + @Setter seuls suffisent.
 */
@Entity
@Table(name = "prestataires")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Prestataire {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String username;

    private String nom;
    private String prenom;
    private String email;
    private String telephone;
    private String adresse;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TypePrestataire type;

    private String specialite;
    private String numeroCartePro;
    private LocalDate dateDebutCollaboration;

    private boolean actif = true;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "agent_id")
    private AgentBancaire agentResponsable;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "agence_id")
    private Agence agence;

    private String niveauValidation;
    private Double plafondValidation;
}