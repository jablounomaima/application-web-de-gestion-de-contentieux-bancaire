package com.example.contentieux_security.repository;

import com.example.contentieux_security.entity.HistoriqueDossier;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface HistoriqueDossierRepository extends JpaRepository<HistoriqueDossier, Long> {

    // Tout l'historique d'un dossier, du plus récent au plus ancien
    List<HistoriqueDossier> findByDossier_IdOrderByDateActionDesc(Long dossierId);

    // Historique par type d'action
    List<HistoriqueDossier> findByDossier_IdAndTypeAction(Long dossierId, String typeAction);

    // Actions d'un utilisateur
    List<HistoriqueDossier> findByUtilisateurOrderByDateActionDesc(String utilisateur);

    @Modifying
    @Query("DELETE FROM HistoriqueDossier h WHERE h.dossier.id = :id")
    void deleteByDossierId(@Param("id") Long id);

}

