package com.example.contentieux_security.repository;

import com.example.contentieux_security.entity.ResultatMission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository; // ✅ import manquant
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;      // ✅ import manquant
import java.util.Optional;

@Repository
public interface ResultatMissionRepository extends JpaRepository<ResultatMission, Long> {
 
    @Query("SELECT r FROM ResultatMission r WHERE r.mission.id = :missionId")
    Optional<ResultatMission> findByMissionId(@Param("missionId") Long missionId);

    List<ResultatMission> findByMissionIdOrderByDateSoumissionDesc(Long missionId);
}