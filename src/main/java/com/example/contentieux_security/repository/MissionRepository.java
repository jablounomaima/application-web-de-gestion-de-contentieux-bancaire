package com.example.contentieux_security.repository;

import com.example.contentieux_security.entity.Mission;
import com.example.contentieux_security.enums.StatutMission;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MissionRepository extends JpaRepository<Mission, Long> {

    List<Mission> findByPrestation_Id(Long prestationId);

    // Missions d'un prestataire (pour son dashboard)
    List<Mission> findByPrestataire_Username(String username);

    List<Mission> findByPrestataire_UsernameAndStatut(String username, StatutMission statut);

    @Query("SELECT m FROM Mission m WHERE m.prestation.dossier.id = :dossierId")
    List<Mission> findByDossierId(Long dossierId);

    @Query("SELECT MAX(m.numeroMission) FROM Mission m WHERE m.numeroMission LIKE :prefix%")
    Optional<String> findLastNumero(String prefix);
}
