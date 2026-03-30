package com.example.contentieux_security.repository;

import com.example.contentieux_security.entity.Validateur;
import com.example.contentieux_security.enums.TypeValidateur;
import org.springframework.data.jpa.repository.JpaRepository;
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

    List<Validateur> findByActifTrue();

    List<Validateur> findByAgenceId(Long agenceId);

    List<Validateur> findByTypeValidateur(TypeValidateur typeValidateur);

    List<Validateur> findByTypeValidateurAndActifTrue(TypeValidateur typeValidateur);
}