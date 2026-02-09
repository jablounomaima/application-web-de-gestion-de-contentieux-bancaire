package com.example.demo.user.entity.impl;

import com.example.demo.user.entity.Utilisateur;
import com.example.demo.user.role.Role;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;

@Entity
@DiscriminatorValue("VALIDATEUR_FINANCIER")
public class ValidateurFinancier extends Utilisateur {
    public ValidateurFinancier() {
        setRole(Role.ROLE_VALIDATEUR_FINANCIER);
    }
}
