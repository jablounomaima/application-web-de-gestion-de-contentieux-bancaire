package com.example.contentieux_security.entity;

import jakarta.persistence.Entity;
import lombok.*;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Espece extends Paiement {
    private String recu;
    private String cin;
}
