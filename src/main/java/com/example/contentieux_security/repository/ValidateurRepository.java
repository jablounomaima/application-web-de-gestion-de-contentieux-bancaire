package com.example.contentieux_security.repository;

import com.example.contentieux_security.entity.DossierContentieux;
import com.example.contentieux_security.entity.Validateur;
import com.example.contentieux_security.enums.TypeValidateur;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ValidateurRepository extends JpaRepository<Validateur, Long> {

    boolean existsByMatricule(String matricule);
    boolean existsByEmail(String email);
    boolean existsByMatriculeAndIdNot(String matricule, Long id);
    boolean existsByEmailAndIdNot(String email, Long id);

    Optional<Validateur> findByMatricule(String matricule);

    // ✅ AJOUTER — utilisé par ValidateurProfileController et ValidateurActifGuard
    Optional<Validateur> findByUsername(String username);

    List<Validateur> findByActifTrue();
    List<Validateur> findByAgenceId(Long agenceId);
    List<Validateur> findByTypeValidateur(TypeValidateur typeValidateur);
    List<Validateur> findByTypeValidateurAndActifTrue(TypeValidateur typeValidateur);
    List<Validateur> findByTypeValidateurAndActifTrueAndAgence_Id(
        TypeValidateur type, Long agenceId);

    @Query("SELECT DISTINCT d FROM DossierContentieux d " +
           "LEFT JOIN FETCH d.client " +
           "LEFT JOIN FETCH d.agence " +
           "LEFT JOIN FETCH d.agentCreateur " +
           "LEFT JOIN FETCH d.risques " +
           "WHERE d.id = :id")
    Optional<DossierContentieux> findByIdWithDetails(@Param("id") Long id);


}