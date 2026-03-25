package com.example.contentieux_security.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import com.example.contentieux_security.enums.TypePrestataire;

import java.time.LocalDate;
@Getter
@Setter
@Entity
@Table(name = "prestataires")
@Data  // Génère getters, setters, toString, equals, hashCode
@Builder
@NoArgsConstructor
@AllArgsConstructor
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

    @ManyToOne
    @JoinColumn(name = "agent_id")
    private AgentBancaire agentResponsable;

    @ManyToOne
    @JoinColumn(name = "agence_id")
    private Agence agence;

    private String niveauValidation;
    private Double plafondValidation;


  
   


}