package com.example.demo.user.entity;

import com.example.demo.user.role.Role;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "utilisateur")
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "type_utilisateur", discriminatorType = DiscriminatorType.STRING)
@Getter
@Setter
public abstract class Utilisateur {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String username;

    @Column(nullable = false)
    private String password;

    private String nom;
    private String prenom;
    private String email;
    private String telephone;

    @Enumerated(EnumType.STRING)
    @Column(name = "role_enum")
    private Role role;

    public Utilisateur() {}
}
