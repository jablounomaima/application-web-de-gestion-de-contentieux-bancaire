package com.bank.contentieux.entity;

import jakarta.persistence.*;
import java.util.List;

@Entity
public class Risque {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long idRisque;

    private String type;
    private String niveau;

    @ManyToOne
    private Dossier dossier;

    @OneToMany(mappedBy = "risque")
    private List<Garantie> garanties;
}
