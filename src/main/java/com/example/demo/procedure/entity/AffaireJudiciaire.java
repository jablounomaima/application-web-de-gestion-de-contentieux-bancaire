package com.example.demo.procedure.entity;

import com.example.demo.dossier.entity.Dossier;
import com.example.demo.user.entity.impl.Avocat;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "affaire_judiciaire")
@Getter
@Setter
public class AffaireJudiciaire {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String reference;

    @Column(nullable = false)
    private String tribunal;

    @OneToOne(optional = false)
    @JoinColumn(name = "dossier_id")
    private Dossier dossier;

    @ManyToOne(optional = false)
    @JoinColumn(name = "avocat_id")
    private Avocat avocat;

    @Column(name = "date_lancement")
    private LocalDateTime dateLancement;

    @OneToMany(mappedBy = "affaire", cascade = CascadeType.ALL)
    private List<Audience> audiences = new ArrayList<>();

    @PrePersist
    protected void onCreate() {
        dateLancement = LocalDateTime.now();
    }
}
