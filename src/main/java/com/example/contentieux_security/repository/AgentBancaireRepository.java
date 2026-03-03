package com.example.contentieux_security.repository;

import com.example.contentieux_security.entity.AgentBancaire;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;   // ✅ IMPORTANT

@Repository
public interface AgentBancaireRepository extends JpaRepository<AgentBancaire, Long> {
    
    @Query("SELECT a FROM AgentBancaire a JOIN FETCH a.agence")
Optional<AgentBancaire> findByUsername(String username);
boolean existsByUsername(String username);
List<AgentBancaire> findByAgenceId(Long agenceId);
}