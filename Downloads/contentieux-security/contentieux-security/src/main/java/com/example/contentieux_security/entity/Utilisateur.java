package com.example.contentieux_security.entity;

import java.util.Map;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "utilisateur")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Utilisateur {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String username;

    private String nom;
    private String prenom;
    private String email;
    private String telephone;
    private String password;

    @Enumerated(EnumType.STRING)
    @Column(name = "role_utilisateur", nullable = false)
    private RoleUtilisateur role;

    // Additional fields can be added if specific to some roles
    private String specialite; // useful for experts or avocats
}
