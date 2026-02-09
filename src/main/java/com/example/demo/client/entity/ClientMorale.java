package com.example.demo.client.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "client_morale")
@DiscriminatorValue("MORALE")
@Getter
@Setter
@NoArgsConstructor
public class ClientMorale extends Client {

    @Column(name = "raison_sociale", nullable = false)
    private String raisonSociale;

    @Column(name = "numero_rc", unique = true, nullable = false)
    private String numeroRC;

    @Column(name = "forme_juridique")
    private String formeJuridique;

    @Override
    public String getNom() {
        return raisonSociale;
    }
}
