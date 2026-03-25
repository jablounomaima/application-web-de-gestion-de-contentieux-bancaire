package com.example.contentieux_security.repository;

import com.example.contentieux_security.entity.Prestation;
import com.example.contentieux_security.enums.StatutPrestation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PrestationRepository extends JpaRepository<Prestation, Long> {

    List<Prestation> findByDossier_Id(Long dossierId);
    List<Prestation> findByDossier_IdAndStatut(Long dossierId, StatutPrestation statut);

    @Query("SELECT p FROM Prestation p WHERE p.dossier.id = :dossierId ORDER BY p.dateCreation DESC")
    List<Prestation> findByDossierIdOrderByDateDesc(Long dossierId);

    @Query("SELECT MAX(p.numeroPrestation) FROM Prestation p WHERE p.numeroPrestation LIKE :prefix%")
    Optional<String> findLastNumero(String prefix);
}