package com.example.contentieux_security.repository;
import com.example.contentieux_security.entity.Agence;        // ← VÉRIFIER

import com.example.contentieux_security.entity.Client;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface ClientRepository extends JpaRepository<Client, Long> {
    Optional<Client> findByCin(String cin);
    Optional<Client> findByEmail(String email);
    boolean existsByCin(String cin);
    boolean existsByEmail(String email);
    List<Client> findByAgence(Agence agence);
    List<Client> findByNomContainingIgnoreCaseOrPrenomContainingIgnoreCase(String nom, String prenom);
}