package com.example.contentieux_security.repository;

import com.example.contentieux_security.entity.Agence;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import com.example.contentieux_security.dto.*;

import java.util.List;
import java.util.Optional;

@Repository
public interface AgenceRepository extends JpaRepository<Agence, Long> {
// AgenceRepository
@Query("SELECT new com.example.contentieux_security.dto.AgenceDTO(" +
       "a.id, a.code, a.nom, a.adresse, a.ville, a.telephone, a.email, a.directeur, SIZE(a.agents)) " +
       "FROM Agence a GROUP BY a")
List<AgenceDTO> findAllWithAgentCount();

Optional<Agence> findByCode(String code);
    boolean existsByCode(String code);  // ← must be present

    static boolean existsByUsername(String username) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'existsByUsername'");
    }
}