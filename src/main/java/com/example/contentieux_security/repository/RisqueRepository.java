package com.example.contentieux_security.repository;

import com.example.contentieux_security.entity.Risque;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface RisqueRepository extends JpaRepository<Risque, Long> {
    List<Risque> findByDossier_Id(Long dossierId);
    List<Risque> findByDossier_IdAndSelectionne(Long dossierId, boolean selectionne);

    @Query("""
    SELECT DISTINCT r FROM Risque r
    LEFT JOIN FETCH r.garanties
    WHERE r.dossier.id = :dossierId
""")
List<Risque> findByDossierIdWithGaranties(@Param("dossierId") Long dossierId);
}