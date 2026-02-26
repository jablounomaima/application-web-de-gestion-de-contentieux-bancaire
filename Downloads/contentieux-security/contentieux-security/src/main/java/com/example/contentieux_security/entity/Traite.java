package com.example.contentieux_security.entity;

import jakarta.persistence.Entity;
import lombok.*;
import java.time.LocalDate;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Traite extends Paiement {
    private String numero;
    private LocalDate dateEcheance;
    private String tirreur;
}
