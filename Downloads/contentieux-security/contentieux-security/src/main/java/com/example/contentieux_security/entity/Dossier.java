package com.example.contentieux_security.entity;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.util.List;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "dossiers")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Dossier {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String numeroDossier;

    @Enumerated(EnumType.STRING)
    private DossierStatus statut;

    private LocalDateTime dateCreation;

    @OneToOne
    @JoinColumn(name = "client_id", unique = true)
    private Client client;

    @ManyToOne
    @JoinColumn(name = "agence_id")
    private Agence agence;

    private String crééPar; // Username of the agent

    @OneToMany(mappedBy = "dossier", cascade = CascadeType.ALL)
    private List<Risque> risques;

    @OneToMany(mappedBy = "dossier", cascade = CascadeType.ALL)
    private List<Echeance> echeances;

    @OneToMany(mappedBy = "dossier", cascade = CascadeType.ALL)
    private List<Prestation> prestations;

    @OneToOne(mappedBy = "dossier", cascade = CascadeType.ALL)
    private AffaireJudiciaire affaireJudiciaire;

    @OneToMany(mappedBy = "dossier", cascade = CascadeType.ALL)
    private List<HistoriqueDossier> historiques;

    @OneToOne(mappedBy = "dossier", cascade = CascadeType.ALL)
    private ValidationFinanciere validationFinanciere;

    @OneToOne(mappedBy = "dossier", cascade = CascadeType.ALL)
    private ValidationJuridique validationJuridique;

    @OneToMany(mappedBy = "dossier", cascade = CascadeType.ALL)
    private List<Document> documents;

    @OneToMany(mappedBy = "dossier", cascade = CascadeType.ALL)
    private List<Facture> factures;

    @OneToMany(mappedBy = "dossier", cascade = CascadeType.ALL)
    private List<Notification> notifications;

    @OneToMany(mappedBy = "dossier", cascade = CascadeType.ALL)
    private List<ResultatMission> resultatsMission;
}
