package com.example.contentieux_security.repository;

import com.example.contentieux_security.entity.Prestataire;
import com.example.contentieux_security.entity.TypePrestataire;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PrestataireRepository extends JpaRepository<Prestataire, Long> {

    Optional<Prestataire> findByUsername(String username);

    boolean existsByUsername(String username);

    boolean existsByEmail(String email);

    // Par username de l'agent responsable
    List<Prestataire> findByAgentResponsable_Username(String agentUsername);

    // Par ID de l'agent responsable (pour AgentPrestataireController)
    List<Prestataire> findByAgentResponsable_Id(Long agentId);

    // Par type + ID agent (pour AgentPrestataireController)
    List<Prestataire> findByTypeAndAgentResponsable_Id(TypePrestataire type, Long agentId);

    List<Prestataire> findByType(TypePrestataire type);

    List<Prestataire> findByActif(boolean actif);
}