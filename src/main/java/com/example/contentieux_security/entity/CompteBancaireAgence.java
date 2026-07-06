// src/main/java/com/example/contentieux_security/entity/CompteBancaireAgence.java
package com.example.contentieux_security.entity;

import jakarta.persistence.*;
import lombok.*;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@Entity
@Table(name = "compte_bancaire_agence")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class CompteBancaireAgence {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 150)
    private String banque;              // ex: "BIAT", "STB"...

    @Column(nullable = false, length = 30, unique = true)
    private String rib;                 // RIB du compte de l'agence

    @Column(nullable = false, length = 200)
    private String titulaireCompte;     // Raison sociale / nom sur le compte

    @Column(nullable = false)
    @Builder.Default
    private boolean actif = true;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "agence_id", nullable = false)
    private Agence agence;
}