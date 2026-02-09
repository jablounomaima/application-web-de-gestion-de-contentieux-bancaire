package com.example.demo.user.entity.impl;

import com.example.demo.user.entity.Utilisateur;
import com.example.demo.user.role.Role;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;

@Entity
@DiscriminatorValue("ADMIN")
public class Admin extends Utilisateur {
    public Admin() {
        setRole(Role.ROLE_ADMIN);
    }
}
