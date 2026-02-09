package com.example.demo.user.entity.impl;

import com.example.demo.user.entity.Utilisateur;
import com.example.demo.user.role.Role;
import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import lombok.Getter;
import lombok.Setter;

@Entity
@DiscriminatorValue("EXPERT")
@Getter
@Setter
public class Expert extends Utilisateur {
    
    @Column(name = "domaine_expertise")
    private String domaineExpertise;

    public Expert() {
        setRole(Role.ROLE_EXPERT);
    }
}
