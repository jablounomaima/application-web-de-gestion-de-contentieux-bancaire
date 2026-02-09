package com.example.demo.client.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "client_morale")
@Getter
@Setter
public class ClientMorale extends Client {

    @Column(name = "raison_sociale", nullable = false)
    private String raisonSociale;

    @Column(name = "numero_rc", unique = true, nullable = false)
    private String numeroRC;

    @Column(name = "forme_juridique")
    private String formeJuridique;
}
