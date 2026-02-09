package com.example.demo.user.entity.impl;

import com.example.demo.user.entity.Utilisateur;
import com.example.demo.user.role.Role;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;

@Entity
@DiscriminatorValue("HUISSIER")
public class Huissier extends Utilisateur {
    public Huissier() {
        setRole(Role.ROLE_HUISSIER);
    }
}
