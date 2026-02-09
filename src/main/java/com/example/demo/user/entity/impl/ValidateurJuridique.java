package com.example.demo.user.entity.impl;

import com.example.demo.user.entity.Utilisateur;
import com.example.demo.user.role.Role;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;

@Entity
@DiscriminatorValue("VALIDATEUR_JURIDIQUE")
public class ValidateurJuridique extends Utilisateur {
    public ValidateurJuridique() {
        setRole(Role.ROLE_VALIDATEUR_JURIDIQUE);
    }
}
