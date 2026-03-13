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

    List<Notification> findByDestinataire_OrderByDateCreationDesc(String destinataire);

    List<Notification> findByDestinataire_AndLueFalseOrderByDateCreationDesc(String destinataire);

    long countByDestinataire_AndLueFalse(String destinataire);

    @Modifying
    @Query("UPDATE Notification n SET n.lue = true WHERE n.destinataire = :dest")
    void marquerToutesLues(@Param("dest") String destinataire);
}