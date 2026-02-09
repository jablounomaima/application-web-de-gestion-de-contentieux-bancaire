package com.example.demo.finance.entity;

import com.example.demo.common.enums.StatutFacture;
import com.example.demo.dossier.entity.Dossier;
import com.example.demo.user.entity.Utilisateur;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "facture")
@Getter
@Setter
public class Facture {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String reference;

    @ManyToOne(optional = false)
    @JoinColumn(name = "dossier_id")
    private Dossier dossier;

    @ManyToOne(optional = false)
    @JoinColumn(name = "emetteur_id")
    private Utilisateur emetteur; // Avocat, Huissier ou Expert

    @Column(name = "montant_ht", nullable = false)
    private BigDecimal montantHT;

    @Column(nullable = false)
    private BigDecimal tva; // Taux de TVA (ex: 19.0)

    @Column(name = "montant_ttc", nullable = false)
    private BigDecimal montantTTC;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StatutFacture statut;

    @Column(name = "date_emission", updatable = false)
    private LocalDateTime dateEmission;

    @OneToMany(mappedBy = "facture", cascade = CascadeType.ALL)
    private List<Paiement> paiements = new ArrayList<>();

    @PrePersist
    protected void onCreate() {
        dateEmission = LocalDateTime.now();
        if (statut == null) statut = StatutFacture.EMISE;
        // Calcul automatique TTC si non fourni
        if (montantTTC == null && montantHT != null) {
            BigDecimal tvaAmount = montantHT.multiply(tva.divide(new BigDecimal(100)));
            montantTTC = montantHT.add(tvaAmount);
        }
    }
}
