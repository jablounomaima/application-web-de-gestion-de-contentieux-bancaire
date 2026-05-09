package com.example.contentieux_security.repository;

import com.example.contentieux_security.entity.Mission;
import com.example.contentieux_security.entity.ResultatMission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import com.example.contentieux_security.entity.ResultatMission;
import com.example.contentieux_security.repository.ResultatMissionRepository;
import org.springframework.web.multipart.MultipartFile;
import java.util.Optional;
import java.util.List;

/**
 * Repository ResultatMission.
 *
 * ✅ FIX erreur 3 : findByMissionOrderByDateSoumissionDesc(Mission)
 * ✅ FIX erreur 4 : findTopByMissionIdOrderByDateSoumissionDesc(Long)
 */
@Repository
public interface ResultatMissionRepository extends JpaRepository<ResultatMission, Long> {

    /**
     * Tous les résultats d'une mission, du plus récent au plus ancien.
     * Requis par ResultatMissionService ligne 141.
     */
    List<ResultatMission> findByMission_IdOrderByDateSoumissionDesc(Long missionId);

    /**
     * Le résultat le plus récent pour un missionId donné.
     * Requis par ResultatMissionService ligne 146.
     */
    Optional<ResultatMission> findTopByMissionIdOrderByDateSoumissionDesc(Long missionId);

    /**
     * Recherche par missionId simple (utilisé dans getResultat()).
     */
    Optional<ResultatMission> findByMission_Id(Long missionId);


    @Query("SELECT r FROM ResultatMission r " +
    "LEFT JOIN FETCH r.fichiers " +
    "WHERE r.mission.id = :missionId")
Optional<ResultatMission> findByMissionIdWithFichiers(@Param("missionId") Long missionId);




}