package com.example.contentieux_security.entity;
import com.example.contentieux_security.entity.DossierContentieux;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "historique_dossier")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class HistoriqueDossier {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "date_action", nullable = false)
    private LocalDateTime dateAction = LocalDateTime.now();

    @Column(name = "type_action", nullable = false)
    private String typeAction;              // CREATION, VALIDATION, REJET, MODIFICATION...

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false)
    private String utilisateur;             // username de l'acteur

    // ── Relation ──────────────────────────────────────────────────
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "dossier_id", nullable = false)
    private DossierContentieux dossier;
}