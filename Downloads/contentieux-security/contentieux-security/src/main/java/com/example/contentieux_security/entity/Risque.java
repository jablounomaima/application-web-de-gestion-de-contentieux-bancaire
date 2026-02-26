package com.example.contentieux_security.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.util.List;

@Entity
@Table(name = "risques")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Risque {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private BigDecimal montantInitial;
    private BigDecimal montantRestant;

    @OneToMany(mappedBy = "risque", cascade = CascadeType.ALL)
    private List<Garantie> garanties;

    @ManyToOne
    @JoinColumn(name = "dossier_id")
    private Dossier dossier;

    private boolean enTraitement; // If this specific risk is being processed
}
