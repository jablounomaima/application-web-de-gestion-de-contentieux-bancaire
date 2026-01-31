package com.bank.contentieux.entity;

import jakarta.persistence.*;
import java.time.LocalDate;

@Entity
public class TypeAffaire {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long idType;

    private String libelle;
}
