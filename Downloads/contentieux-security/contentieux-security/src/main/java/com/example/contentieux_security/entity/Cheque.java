package com.example.contentieux_security.entity;

import jakarta.persistence.Entity;
import lombok.*;
import java.time.LocalDate;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Cheque extends Paiement {
    private String numero;
    private String banque;
    private LocalDate dateEmission;
    private String tirreur;
}
