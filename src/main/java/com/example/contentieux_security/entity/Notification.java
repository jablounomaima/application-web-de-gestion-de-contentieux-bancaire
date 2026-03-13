package com.example.contentieux_security.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "notifications")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String destinataire;        // username du validateur

    @Column(nullable = false)
    private String titre;

    @Column(nullable = false)
    private String message;

    @Column(nullable = false)
    private String type;                // "VALIDATION_FINANCIERE" ou "VALIDATION_JURIDIQUE"

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "dossier_id")
    private DossierContentieux dossier;

    @Column(nullable = false)
    private LocalDateTime dateCreation;

    @Column(nullable = false)
    private boolean lue = false;

    @Column
    private String urlAction;           // lien vers le dossier à valider
}