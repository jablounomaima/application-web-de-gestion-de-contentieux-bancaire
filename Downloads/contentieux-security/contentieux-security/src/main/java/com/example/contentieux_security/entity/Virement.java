package com.example.contentieux_security.entity;

import jakarta.persistence.Entity;
import lombok.*;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Virement extends Paiement {
    private String numeroCompte;
    private String banque;
    private String codeBIC;
    private String referenceVirement;
}
