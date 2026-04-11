package com.example.contentieux_security.repository;

import com.example.contentieux_security.entity.Mission;
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
    @Query("""
        SELECT m FROM Mission m
        JOIN FETCH m.prestation p
        JOIN FETCH p.dossier d
        JOIN FETCH m.prestataire pr
        WHERE d.id = :dossierId
        ORDER BY m.dateAssignation DESC
    """)
    List<Mission> findByDossierIdWithPrestataire(@Param("dossierId") Long dossierId);

    // ─────────────────────────────────────────────
    // 🔹 Missions d’un prestataire avec détails
    // ─────────────────────────────────────────────
    @Query("""
        SELECT m FROM Mission m
        JOIN FETCH m.prestation p
        JOIN FETCH p.dossier d
        WHERE m.prestataire.username = :username
        ORDER BY m.dateAssignation DESC
    """)
    List<Mission> findMissionsWithDetails(@Param("username") String username);

    // ─────────────────────────────────────────────
    // 🔹 Trouver mission par ID avec détails complets
    // ─────────────────────────────────────────────
    @Query("""
        SELECT m FROM Mission m
        JOIN FETCH m.prestation p
        JOIN FETCH p.dossier d
        LEFT JOIN FETCH d.client
        JOIN FETCH m.prestataire pr
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
        AND m.statut IN (
            com.example.contentieux_security.enums.StatutMission.ASSIGNEE,
            com.example.contentieux_security.enums.StatutMission.EN_COURS
        )
    """)
    Optional<Mission> findMissionAvocatDuDossier(@Param("dossierId") Long dossierId);

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
}