package com.example.contentieux_security.repository;

import com.example.contentieux_security.entity.AgentBancaire;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AgentBancaireRepository extends JpaRepository<AgentBancaire, Long> {
    
    // Retourne une liste pour gérer les doublons temporairement
    List<AgentBancaire> findByUsername(String username);
    
    Optional<AgentBancaire> findById(Long id);
    
    List<AgentBancaire> findByAgenceId(Long agenceId);
    
    boolean existsByUsername(String username);
}