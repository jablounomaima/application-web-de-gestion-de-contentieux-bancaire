package com.example.demo.user.entity.impl;

import com.example.demo.user.entity.Utilisateur;
import com.example.demo.user.role.Role;
import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import lombok.Getter;
import lombok.Setter;

@Entity
@DiscriminatorValue("AVOCAT")
@Getter
@Setter
public class Avocat extends Utilisateur {
    
    @Column(name = "barreau")
    private String barreau;

    public Avocat() {
        setRole(Role.ROLE_AVOCAT);
    }
}
