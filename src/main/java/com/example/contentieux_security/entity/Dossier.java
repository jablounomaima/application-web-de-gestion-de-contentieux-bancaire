package com.example.contentieux_security.entity;

import com.example.contentieux_security.enums.DossierStatus;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Dossier {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    private String numeroDossier;
    private String libelle;
    private LocalDateTime dateCreation;
    private LocalDateTime dateValidation;
    
    @Enumerated(EnumType.STRING)
    private DossierStatus statut;
    
    private String creePar;
    private Boolean validationFinanciere;
    private Boolean validationJuridique;
    private String remarquesValidation;
    
    // Champs manquants ajoutés
    private String notes;
    private String commentaireFinancier;
    private String commentaireJuridique;
    private String validateurFinancierUsername;
    private String validateurJuridiqueUsername;
    
    @ManyToOne
    @JoinColumn(name = "client_id")
    private Client client;
    
    @ManyToOne
    @JoinColumn(name = "agence_id")
    private Agence agence;
    
    @ManyToOne
    @JoinColumn(name = "agent_id")
    private AgentBancaire agentCreateur;
    
    // Relation avec Risque
    @OneToMany(mappedBy = "dossier")
    private List<Risque> risques;
}