package com.example.contentieux_security.repository;

import com.example.contentieux_security.entity.Prestataire;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import com.example.contentieux_security.entity.TypePrestataire;
import java.util.List;
import java.util.Optional;

@Repository
public interface PrestataireRepository extends JpaRepository<Prestataire, Long> {
    
    // Trouver par username (Keycloak)
    Optional<Prestataire> findByUsername(String username);
    
    // Tous les prestataires gérés par un agent
    List<Prestataire> findByAgentResponsableId(Long agentId);
    
    // Par type et agent responsable
    List<Prestataire> findByTypeAndAgentResponsableId(TypePrestataire type, Long agentId);
    
    // Par type et agence
    List<Prestataire> findByTypeAndAgenceId(TypePrestataire type, Long agenceId);
    
    // Tous les prestataires actifs d'un agent
    List<Prestataire> findByAgentResponsableIdAndActifTrue(Long agentId);
    
    boolean existsByUsername(String username);
}