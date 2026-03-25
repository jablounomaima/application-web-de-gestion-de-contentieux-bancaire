package com.example.contentieux_security.entity;

import com.example.contentieux_security.enums.StatutPrestation;  // ← enums/
import com.example.contentieux_security.enums.TypePrestation;    // ← enums/
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "prestations")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Prestation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "numero_prestation", unique = true, nullable = false)
    private String numeroPrestation;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private TypePrestation type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private StatutPrestation statut = StatutPrestation.EN_COURS;

    @Column(columnDefinition = "TEXT")
    private String description;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "dossier_id", nullable = false)
    private DossierContentieux dossier;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "agent_id", nullable = false)
    private AgentBancaire agentCreateur;

    @OneToMany(mappedBy = "prestation", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Mission> missions;

    @Column(name = "date_creation", nullable = false)
    private LocalDateTime dateCreation = LocalDateTime.now();

    @Column(name = "date_cloture")
    private LocalDate dateCloture;
}