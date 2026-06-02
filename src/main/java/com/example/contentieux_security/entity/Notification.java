package com.example.contentieux_security.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
@Entity
@Table(name = "notifications", indexes = {
    @Index(name = "idx_notif_destinataire", columnList = "destinataire"),
    @Index(name = "idx_notif_lue",          columnList = "destinataire, lue")
})
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Username du destinataire (= Principal.getName()) */
    @Column(nullable = false)
    private String destinataire;

    @Column(nullable = false)
    private String titre;

    @Column(nullable = false, length = 1000)
    private String message;

    // Notification.java
@Column(name = "url", length = 500)
private String url;

    /**
     * Type de notification :
     * VALIDATION_FINANCIERE, VALIDATION_JURIDIQUE,
     * VALIDATION_FINANCIERE_OK, REJET_FINANCIER,
     * VALIDATION_JURIDIQUE_OK,  REJET_JURIDIQUE,
     * NOUVELLE_MISSION, MISSION_MODIFIEE,
     * PV_SOUMIS, FACTURE_SOUMISE, RESULTATS_SOUMIS,
     * NOUVELLE_AUDIENCE, JUGEMENT_RENDU
     */
    @Column(nullable = false)
    private String type;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "dossier_id")
    @JsonIgnore
    private DossierContentieux dossier;

    @Column(nullable = false)
    private LocalDateTime dateCreation;

    @Column(nullable = false)
    private boolean lue = false;

    /** URL de redirection au clic sur la notification */
    @Column
    private String urlAction;

    @PrePersist
    public void prePersist() {
        if (this.dateCreation == null) {
            this.dateCreation = LocalDateTime.now();
        }
        this.lue = false;
    }
}