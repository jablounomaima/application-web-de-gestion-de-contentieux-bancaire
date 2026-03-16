package com.example.contentieux_security.repository;

import com.example.contentieux_security.entity.Notification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {

    // ❌ Avant : findByDestinataire_OrderByDateCreationDesc  ← _ parasite
    // ✅ Après : findByDestinataireOrderByDateCreationDesc   ← sans _
    List<Notification> findByDestinataireOrderByDateCreationDesc(String destinataire);

    List<Notification> findByDestinataireAndLueFalseOrderByDateCreationDesc(String destinataire);

    long countByDestinataireAndLueFalse(String destinataire);

    @Modifying
    @Query("UPDATE Notification n SET n.lue = true WHERE n.destinataire = :dest")
    void marquerToutesLues(@Param("dest") String destinataire);

    @Modifying
@Query("DELETE FROM Notification n WHERE n.dossier.id = :id")
void deleteByDossierId(@Param("id") Long id);
}