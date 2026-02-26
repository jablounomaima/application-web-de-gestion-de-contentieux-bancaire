package com.example.contentieux_security.entity;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.util.List;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "prestations")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Prestation {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String type;
    private String description;
    private LocalDate dateDebut;
    private LocalDate dateFin;

    @Enumerated(EnumType.STRING)
    private StatutPrestation statut;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "dossier_id")
    private Dossier dossier;

    @OneToMany(mappedBy = "prestation", cascade = CascadeType.ALL)
    private List<Facture> factures;

    @OneToMany(mappedBy = "prestation", cascade = CascadeType.ALL)
    private List<Mission> missions;

    @OneToMany(mappedBy = "prestation", cascade = CascadeType.ALL)
    private List<EtapePrestation> etapes;
}
