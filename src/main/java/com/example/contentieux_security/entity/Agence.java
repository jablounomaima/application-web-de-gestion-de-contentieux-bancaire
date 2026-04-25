package com.example.contentieux_security.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler", "agents", "dossiers", "clients"})
@Entity
@Table(name = "agences")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Agence {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(unique = true, nullable = false)
    private String code;
    
    @Column(nullable = false)
    private String nom;
    
    private String adresse;
    
    private String ville;
    
    private String telephone;
    
    private String email;

    @Column(name = "directeur")
    private String directeur; // ✅ AJOUTÉ

    @OneToMany(mappedBy = "agence", cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    @JsonIgnoreProperties
    private List<AgentBancaire> agents = new ArrayList<>();

    public int getNombreAgents() {
        return agents != null ? agents.size() : 0;
    }
}