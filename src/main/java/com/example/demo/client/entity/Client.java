package com.example.demo.client.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;

@Entity
@Table(name = "client")
@Inheritance(strategy = InheritanceType.JOINED)
@DiscriminatorColumn(name = "type_client", discriminatorType = DiscriminatorType.STRING)
@Getter
@Setter
@lombok.NoArgsConstructor
public abstract class Client {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String telephone;

    private String email;

    @Column(nullable = false)
    private String adresse;

    private String ville;

    private String codePostal;

    @Column(name = "date_creation", updatable = false)
    private LocalDateTime dateCreation;

    @PrePersist
    protected void onCreate() {
        dateCreation = LocalDateTime.now();
    }

    public abstract String getNom();
}
