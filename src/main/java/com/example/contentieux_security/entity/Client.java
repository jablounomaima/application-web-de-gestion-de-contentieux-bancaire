package com.example.contentieux_security.entity;
import com.example.contentieux_security.entity.DossierContentieux;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;
import java.util.List;

@Entity
@Table(name = "clients")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Client {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String nom;

    @Column(nullable = false)
    private String prenom;

    @Column(unique = true, nullable = false)
    private String cin;

    @Column(unique = true)
    private String email;

    private String telephone;
    private String adresse;

    @Column(name = "date_inscription")
    private LocalDate dateInscription = LocalDate.now();

    private boolean actif = true;

    // ── Relations ─────────────────────────────────────────────────
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "agence_id")
    private Agence agence;                  // rattaché à une agence

    @OneToMany(mappedBy = "client", fetch = FetchType.LAZY)
    private List<DossierContentieux> dossiers;

    // ── Utilitaire ────────────────────────────────────────────────
    public String getNomComplet() {
        return prenom + " " + nom;
    }
}