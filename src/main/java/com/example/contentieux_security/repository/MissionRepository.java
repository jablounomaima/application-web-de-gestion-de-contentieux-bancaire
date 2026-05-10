package com.example.contentieux_security.repository;

import com.example.contentieux_security.entity.DossierContentieux;
import com.example.contentieux_security.entity.Mission;
import com.example.contentieux_security.entity.ResultatMission;
import com.example.contentieux_security.enums.StatutMission;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface MissionRepository extends JpaRepository<Mission, Long> {
    boolean existsByNumeroMission(String numeroMission);

    // ─────────────────────────────────────────────
    // 🔹 Méthodes simples
    // ─────────────────────────────────────────────
    List<Mission> findByPrestation_Id(Long prestationId);

    List<Mission> findByPrestataire_Username(String username);

    List<Mission> findByPrestataire_UsernameAndStatut(String username, StatutMission statut);

    List<Mission> findByPrestataire_UsernameOrderByDateAssignationDesc(String username);


    List<Mission> findByPrestation_Dossier_IdOrderByDateAssignationDesc(Long dossierId);

    // ─────────────────────────────────────────────
    // 🔹 Charger missions avec détails (prestataire + dossier)
    // ─────────────────────────────────────────────
    @Query("SELECT m FROM Mission m " +
    "LEFT JOIN FETCH m.prestataire " +
    "LEFT JOIN FETCH m.prestation p " +
    "LEFT JOIN FETCH p.dossier " +
    "WHERE p.dossier.id = :dossierId " +
    "ORDER BY m.dateAssignation DESC")
    List<Mission> findByDossierIdWithPrestataire(@Param("dossierId") Long dossierId);

    // ─────────────────────────────────────────────
    // 🔹 Missions d’un prestataire avec détails
    // ─────────────────────────────────────────────
    @Query("""
        SELECT m FROM Mission m
        LEFT JOIN FETCH m.prestation p
        LEFT JOIN FETCH p.dossier d
        LEFT JOIN FETCH d.client
        LEFT JOIN FETCH m.prestataire pr
        WHERE pr.username = :username
        ORDER BY m.dateAssignation DESC
    """)
    List<Mission> findMissionsAvecDetailsParPrestataire(@Param("username") String username);
    // ─────────────────────────────────────────────
    // 🔹 Trouver mission par ID avec détails complets
    // ─────────────────────────────────────────────
    @Query("""
        SELECT DISTINCT m FROM Mission m
        LEFT JOIN FETCH m.prestation p
        LEFT JOIN FETCH p.dossier d
        LEFT JOIN FETCH d.client
        LEFT JOIN FETCH d.agence
        LEFT JOIN FETCH d.agentCreateur
        LEFT JOIN FETCH d.risques r
        LEFT JOIN FETCH r.garanties
        LEFT JOIN FETCH m.prestataire pr
        WHERE m.id = :id
    """)
    Optional<Mission> findByIdWithDetails(@Param("id") Long id);


// ─────────────────────────────────────────────
    // 🔹 Missions créées par agent
    // ─────────────────────────────────────────────
    @Query("""
        SELECT m FROM Mission m
        JOIN FETCH m.prestation p
        JOIN FETCH p.dossier d
        JOIN FETCH d.agentCreateur a
        WHERE a.username = :username
        ORDER BY m.dateAssignation DESC
    """)
    List<Mission> findMissionsByAgentUsername(@Param("username") String username);

    // ─────────────────────────────────────────────
    // 🔹 Mission avocat active dans un dossier
    // ─────────────────────────────────────────────
    @Query("""
        SELECT m FROM Mission m
        JOIN FETCH m.prestation p
        JOIN FETCH p.dossier d
        JOIN FETCH m.prestataire pr
        WHERE d.id = :dossierId
        AND pr.type = com.example.contentieux_security.enums.TypePrestataire.AVOCAT
        ORDER BY m.id DESC
    """)
    List<Mission> findMissionsAvocatDuDossier(@Param("dossierId") Long dossierId);
    // ─────────────────────────────────────────────
    // 🔹 Statistiques
    // ─────────────────────────────────────────────
    @Query("""
        SELECT COUNT(m) FROM Mission m
        WHERE FUNCTION('YEAR', m.dateAssignation) = :annee
    """)
    long countByAnnee(@Param("annee") int annee);

  

    @Query("SELECT m FROM Mission m " +
       "LEFT JOIN FETCH m.prestataire " +
       "LEFT JOIN FETCH m.prestation p " +
       "LEFT JOIN FETCH p.dossier d " +
       "LEFT JOIN FETCH d.client " +
       "ORDER BY m.dateAssignation DESC")
List<Mission> findAllWithDetails();

// MissionRepository.java
@Query("SELECT m FROM Mission m JOIN FETCH m.prestataire WHERE m.id = :id")
Optional<Mission> findByIdWithPrestataire(@Param("id") Long id);




@Query("""
    SELECT p.dossier FROM Mission m
    JOIN m.prestation p
    WHERE m.id = :missionId
""")
DossierContentieux findDossierByMissionId(@Param("missionId") Long missionId);



// Et corriger findLastNumero si elle existe déjà :
@Query("SELECT m.numeroMission FROM Mission m " +
       "WHERE m.numeroMission LIKE CONCAT(:prefix, '%') " +
       "ORDER BY m.numeroMission DESC LIMIT 1")
Optional<String> findLastNumero(@Param("prefix") String prefix);




// MissionRepository.java
@Query("""
    SELECT m FROM Mission m
    LEFT JOIN FETCH m.prestataire
    LEFT JOIN FETCH m.prestation p
    LEFT JOIN FETCH p.dossier d
    LEFT JOIN FETCH d.client
    LEFT JOIN FETCH d.agentCreateur
    WHERE d.creePar = :username
""")
List<Mission> findByPrestation_Dossier_CreeParWithDetails(@Param("username") String username);



// MissionRepository.java

// For dashboard (already done)
@Query("""
    SELECT m FROM Mission m
    LEFT JOIN FETCH m.prestation p
    LEFT JOIN FETCH p.dossier d
    LEFT JOIN FETCH d.client
    LEFT JOIN FETCH m.prestataire
    WHERE m.prestataire.username = :username
""")
List<Mission> findByPrestataireUsernameWithDetails(@Param("username") String username);


@Query("""
    SELECT DISTINCT m FROM Mission m
    LEFT JOIN FETCH m.prestataire
    LEFT JOIN FETCH m.prestation p
    LEFT JOIN FETCH p.dossier d
    LEFT JOIN FETCH d.client
    WHERE d.id = :dossierId
    ORDER BY m.dateAssignation DESC
""")
List<Mission> findByDossierIdWithFullDetails(@Param("dossierId") Long dossierId);


// MissionRepository.java — query de secours
@Query("""
    SELECT DISTINCT m FROM Mission m
    LEFT JOIN FETCH m.prestataire
    LEFT JOIN FETCH m.prestation p
    LEFT JOIN FETCH p.dossier d
    LEFT JOIN FETCH d.client
    WHERE p.dossier.id = :dossierId
""")
List<Mission> findMissionsByDossierId(@Param("dossierId") Long dossierId);

// Toutes les missions avec facture pour un dossier
@Query("""
    SELECT m FROM Mission m
    JOIN FETCH m.prestataire p
    JOIN m.prestation pr
    WHERE pr.dossier.id = :dossierId
    AND m.factureRef IS NOT NULL
""")
List<Mission> findFacturesParDossier(@Param("dossierId") Long dossierId);

// Toutes les missions avec facture pour tous les dossiers d'une agence
@Query("""
    SELECT m FROM Mission m
    JOIN FETCH m.prestataire p
    JOIN m.prestation pr
    JOIN pr.dossier d
    JOIN d.agence a
    WHERE a.id = :agenceId
    AND m.factureRef IS NOT NULL
""")
List<Mission> findFacturesParAgence(@Param("agenceId") Long agenceId);




}