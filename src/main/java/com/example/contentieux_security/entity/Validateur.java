package com.example.contentieux_security.entity;

import com.example.contentieux_security.enums.TypeValidateur;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "validateurs")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Validateur {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false)
    private String username;

    @Column(nullable = false, unique = true)
    private String matricule;

    @Column(nullable = false)
    private String nom;

    @Column(nullable = false)
    private String prenom;

    @Column(nullable = false, unique = true)
    private String email;

    private String telephone;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TypeValidateur typeValidateur;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "agence_id")
    private Agence agence;

    @Column(nullable = false)
    @Builder.Default
    private boolean actif = true;

   
}