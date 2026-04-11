package com.example.contentieux_security.repository;

import com.example.contentieux_security.entity.Audience;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
 
public interface AudienceRepository extends JpaRepository<Audience, Long> {
 
    @Query("SELECT au FROM Audience au WHERE au.affaire.id = :affaireId ORDER BY au.dateAudience ASC")
    List<Audience> findByAffaireIdOrderByDate(@Param("affaireId") Long affaireId);
}
