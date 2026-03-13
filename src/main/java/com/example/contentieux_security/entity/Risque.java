package com.example.contentieux_security.entity;
import com.example.contentieux_security.entity.DossierContentieux;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;
import java.util.List;

@Entity
@Table(name = "risques")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Risque {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String type;                    // ex: "Crédit immobilier"

    @Column(name = "montant_initial", nullable = false)
    private Double montantInitial;

    @Column(name = "montant_impaye")
    private Double montantImpaye;           // capital restant dû + intérêts

    @Column(name = "date_echeance")
    private LocalDate dateEcheance;

    @Column(columnDefinition = "TEXT")
    private String description;

    // Risque sélectionné pour procédure contentieuse
    @Column(name = "selectionne")
    private boolean selectionne = false;

    // ── Relations ─────────────────────────────────────────────────
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "dossier_id", nullable = false)
    private DossierContentieux dossier;

    @OneToMany(mappedBy = "risque", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Garantie> garanties;

    // ── Méthode ───────────────────────────────────────────────────
    public Double calculerScore() {
        if (montantInitial == null || montantImpaye == null) return 0.0;
        return (montantImpaye / montantInitial) * 100;  // % impayé
    }
}