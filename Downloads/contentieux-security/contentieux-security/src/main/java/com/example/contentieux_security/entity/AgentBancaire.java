package com.example.contentieux_security.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "agent_bancaire")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AgentBancaire {
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

    @ManyToOne
    @JoinColumn(name = "agence_id")
    private Agence agence;
}
