package com.bank.contentieux.entity;

import jakarta.persistence.*;
import java.time.LocalDate;

@Entity
public class Audience {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long idAudience;

    private LocalDate dateAudience;
    private String decision;

    @ManyToOne
    private Affaire affaire;
}
