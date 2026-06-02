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

    /** Toutes les notifications d'un utilisateur, les plus récentes en premier */
    List<Notification> findByDestinataireOrderByDateCreationDesc(String destinataire);

    /** Notifications non lues uniquement */
    List<Notification> findByDestinataireAndLueFalseOrderByDateCreationDesc(String destinataire);

    /** Compteur non lues — utilisé par la navbar */
    long countByDestinataireAndLueFalse(String destinataire);

    /** Marquer toutes les notifications d'un utilisateur comme lues */
    @Modifying
    @Query("UPDATE Notification n SET n.lue = true WHERE n.destinataire = :dest")
    void marquerToutesLues(@Param("dest") String destinataire);

    /** Suppression en cascade lors de la suppression d'un dossier */
    @Modifying
    @Query("DELETE FROM Notification n WHERE n.dossier.id = :id")
    void deleteByDossierId(@Param("id") Long id);

    /** Toutes les notifications avec dossier chargé (pour le dossierId) */
    @Query("SELECT n FROM Notification n LEFT JOIN FETCH n.dossier WHERE n.destinataire = :dest ORDER BY n.dateCreation DESC")
    List<Notification> findByDestinataireWithDossier(@Param("dest") String destinataire);

    /** Notifications non lues avec dossier chargé */
    @Query("SELECT n FROM Notification n LEFT JOIN FETCH n.dossier WHERE n.destinataire = :dest AND n.lue = false ORDER BY n.dateCreation DESC")
    List<Notification> findNonLuesWithDossier(@Param("dest") String destinataire);
}