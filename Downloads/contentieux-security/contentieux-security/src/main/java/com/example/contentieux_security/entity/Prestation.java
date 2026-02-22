package com.example.contentieux_security.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "prestations")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Prestation {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String type; // Procédure judiciaire, Exécution
    private LocalDateTime dateCreation;

    @ManyToOne
    @JoinColumn(name = "dossier_id")
    private Dossier dossier;
}
