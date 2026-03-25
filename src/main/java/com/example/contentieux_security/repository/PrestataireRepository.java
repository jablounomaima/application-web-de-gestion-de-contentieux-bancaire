package com.example.contentieux_security.repository;

import com.example.contentieux_security.entity.Prestataire;
import com.example.contentieux_security.enums.TypePrestataire;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PrestataireRepository extends JpaRepository<Prestataire, Long> {

    Optional<Prestataire> findByUsername(String username);

    boolean existsByUsername(String username);
    boolean existsByEmail(String email);

    // ✅ agentResponsable (pas agent)
    List<Prestataire> findByAgentResponsable_Username(String agentUsername);
    List<Prestataire> findByAgentResponsable_Id(Long agentId);

    // ✅ type + agentResponsable (pas agent)
    List<Prestataire> findByTypeAndAgentResponsable_Id(TypePrestataire type, Long agentId);

    List<Prestataire> findByType(TypePrestataire type);
    List<Prestataire> findByActif(boolean actif);

    // ── Pour PrestationController ──────────────────────
    List<Prestataire> findByTypeAndActifTrue(TypePrestataire type);
    List<Prestataire> findByTypeInAndActifTrue(List<TypePrestataire> types);
}