package com.example.demo.client.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import java.time.LocalDate;

@Entity
@Table(name = "client_physique")
@DiscriminatorValue("PHYSIQUE")
@Getter
@Setter
@NoArgsConstructor
public class ClientPhysique extends Client {

    @Column(nullable = false)
    private String nom;

    @Column(nullable = false)
    private String prenom;

    @Column(unique = true, nullable = false)
    private String cin;

    @Column(name = "date_naissance")
    private LocalDate dateNaissance;

    @Override
    public String getNom() {
        return nom + " " + prenom;
    }
}
