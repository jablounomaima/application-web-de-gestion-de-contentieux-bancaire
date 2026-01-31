package com.bank.contentieux.entity;

import jakarta.persistence.*;
import java.time.LocalDate;

@Entity
public class Cheque extends Paiement {
    private String numeroCheque;
    private String banque;
}
