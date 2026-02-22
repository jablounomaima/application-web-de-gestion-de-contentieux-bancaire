package com.example.contentieux_security.entity;

import jakarta.persistence.*;
import lombok.*;
import java.util.List;

@Entity
@Table(name = "agences")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Agence {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String nom;

    private String code;
    private String ville;

    @OneToMany(mappedBy = "agence")
    private List<Dossier> dossiers;
}
