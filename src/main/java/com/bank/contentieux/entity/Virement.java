package com.bank.contentieux.entity;

import jakarta.persistence.*;
import java.time.LocalDate;

@Entity
public class Virement extends Paiement {
    private String numeroVirement;
    private String banque;
}
