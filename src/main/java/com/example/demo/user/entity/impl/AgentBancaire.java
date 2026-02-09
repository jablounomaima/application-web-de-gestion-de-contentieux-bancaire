package com.example.demo.user.entity.impl;

import com.example.demo.agence.entity.Agence;
import com.example.demo.user.entity.Utilisateur;
import com.example.demo.user.role.Role;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import lombok.Getter;
import lombok.Setter;

@Entity
@DiscriminatorValue("AGENT_BANCAIRE")
@Getter
@Setter
public class AgentBancaire extends Utilisateur {
    
    @ManyToOne
    @JoinColumn(name = "agence_id")
    private Agence agence;

    public AgentBancaire() {
        setRole(Role.ROLE_AGENT_BANCAIRE);
    }
}
