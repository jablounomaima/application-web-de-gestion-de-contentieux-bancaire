package com.example.contentieux_security.repository;

import com.example.contentieux_security.entity.Mission;
import com.example.contentieux_security.entity.ResultatMission;
import com.example.contentieux_security.enums.StatutMission;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface MissionRepository extends JpaRepository<Mission, Long> {

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
    @Query("SELECT DISTINCT m FROM Mission m " +
    "LEFT JOIN FETCH m.prestation p " +
    "LEFT JOIN FETCH p.dossier d " +
    "LEFT JOIN FETCH d.client " +
    "LEFT JOIN FETCH d.agence " +
    "LEFT JOIN FETCH d.agentCreateur " +
    "LEFT JOIN FETCH d.risques r " +
    "LEFT JOIN FETCH r.garanties " +
    "LEFT JOIN FETCH m.prestataire " +
    "WHERE m.id = :id")
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

    // ─────────────────────────────────────────────
    // 🔹 Génération numéro mission
    // ─────────────────────────────────────────────
    @Query(value = """
        SELECT m.numero_mission
        FROM missions m
        WHERE m.numero_mission LIKE CONCAT(:prefix, '%')
        ORDER BY m.numero_mission DESC
        LIMIT 1
    """, nativeQuery = true)
    Optional<String> findLastNumero(@Param("prefix") String prefix);



    @Query("SELECT m FROM Mission m " +
       "LEFT JOIN FETCH m.prestataire " +
       "LEFT JOIN FETCH m.prestation p " +
       "LEFT JOIN FETCH p.dossier d " +
       "LEFT JOIN FETCH d.client " +
       "ORDER BY m.dateAssignation DESC")
List<Mission> findAllWithDetails();

@Query("""
    SELECT m FROM Mission m
    LEFT JOIN FETCH m.prestataire
    LEFT JOIN FETCH m.prestation p
    LEFT JOIN FETCH p.dossier
    WHERE m.id = :id
""")
Optional<Mission> findByIdWithPrestataire(@Param("id") Long id);

}