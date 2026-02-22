package com.example.contentieux_security.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.List;

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

    @ManyToOne
    @JoinColumn(name = "client_id")
    private Client client;

    @ManyToOne
    @JoinColumn(name = "agence_id")
    private Agence agence;

    private String crééPar; // Username of the agent

    @OneToMany(mappedBy = "dossier", cascade = CascadeType.ALL)
    private List<Risque> risques;

    private boolean validationFinanciere;
    private boolean validationJuridique;
}
