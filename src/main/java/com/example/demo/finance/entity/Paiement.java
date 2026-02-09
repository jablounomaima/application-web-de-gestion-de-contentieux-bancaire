package com.example.demo.finance.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "paiement")
@Getter
@Setter
public class Paiement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "facture_id")
    private Facture facture;

    @Column(nullable = false)
    private BigDecimal montant;

    @Column(name = "mode_paiement")
    private String modePaiement; // VIREMENT, CHEQUE, ESPECES

    @Column(name = "reference_paiement")
    private String referencePaiement; // Numéro de virement ou chèque

    @Column(name = "date_paiement")
    private LocalDateTime datePaiement;

    @PrePersist
    protected void onCreate() {
        if (datePaiement == null) datePaiement = LocalDateTime.now();
    }
}
