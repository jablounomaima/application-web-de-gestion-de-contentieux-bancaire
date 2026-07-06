package com.example.contentieux_security.repository;

import com.example.contentieux_security.entity.CompteBancaireAgence;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;

public interface CompteBancaireAgenceRepository extends JpaRepository<CompteBancaireAgence, Long> {

    @Query("SELECT c FROM CompteBancaireAgence c JOIN FETCH c.agence WHERE c.agence.id = :agenceId AND c.actif = true")
    List<CompteBancaireAgence> findByAgence_IdAndActifTrue(@Param("agenceId") Long agenceId);

    Optional<CompteBancaireAgence> findByRib(String rib);
}