package com.example.demo.dossier.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "garantie")
@Getter
@Setter
public class Garantie {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String type; // HYPOTHEQUE, CAUTION, NANTISSEMENT, etc.

    @Column(nullable = false)
    private BigDecimal valeur;

    @Column(name = "date_expiration")
    private LocalDate dateExpiration;

    @ManyToOne
    @JoinColumn(name = "dossier_id")
    private Dossier dossier;
}
