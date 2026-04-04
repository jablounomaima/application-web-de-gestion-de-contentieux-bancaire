package com.example.contentieux_security.entity;

import com.example.contentieux_security.entity.DossierContentieux;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;
import java.util.List;
import com.example.contentieux_security.enums.*;
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

    // ✅ NULL autorisé - soit CIN soit RNE selon le type
    @Column(unique = true, nullable = true)
    private String cin;

    @Column(unique = true, nullable = true)
    private String rne;

    private String raisonSociale;

    @Column(unique = true)
    private String email;

    private String telephone;
    private String adresse;

    @Column(name = "date_inscription")
    private LocalDate dateInscription = LocalDate.now();

    private boolean actif = true;

    @Enumerated(EnumType.STRING)
    @Column(name = "type_client")
    private TypeClient typeClient;

    // Relations...
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "agence_id")
    private Agence agence;

    @OneToMany(mappedBy = "client", fetch = FetchType.LAZY)
    private List<DossierContentieux> dossiers;

    // Méthode utilitaire
    public String getNomComplet() {
        return prenom + " " + nom;
    }
}