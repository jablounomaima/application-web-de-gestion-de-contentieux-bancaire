package com.example.demo.dossier.entity;

import com.example.demo.agence.entity.Agence;
import com.example.demo.client.entity.Client;
import com.example.demo.common.enums.StatutDossier;
import com.example.demo.user.entity.impl.AgentBancaire;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "dossier")
@Getter
@Setter
public class Dossier {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String reference;

    @ManyToOne(optional = false)
    @JoinColumn(name = "client_id")
    private Client client;

    @ManyToOne(optional = false)
    @JoinColumn(name = "agence_id")
    private Agence agence;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StatutDossier statut;

    @Column(name = "montant_creance", nullable = false)
    private BigDecimal montantCreance;

    @Column(name = "montant_recouvre")
    private BigDecimal montantRecouvre = BigDecimal.ZERO;

    @Column(nullable = false)
    private String strategie;

    @ManyToOne
    @JoinColumn(name = "agent_createur_id")
    private AgentBancaire agentCreateur;

    @Column(name = "date_creation", updatable = false)
    private LocalDateTime dateCreation;

    @Column(name = "date_cloture")
    private LocalDateTime dateCloture;

    @PrePersist
    protected void onCreate() {
        dateCreation = LocalDateTime.now();
        if (statut == null) statut = StatutDossier.NOUVEAU;
    }

    public double getTauxRecouvrement() {
        if (montantCreance == null || montantCreance.compareTo(BigDecimal.ZERO) == 0) return 0;
        return montantRecouvre.divide(montantCreance, 4, BigDecimal.ROUND_HALF_UP).multiply(new BigDecimal(100)).doubleValue();
    }
}
