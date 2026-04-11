package com.example.contentieux_security.repository;

import com.example.contentieux_security.entity.DocumentAffaire;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
 
public interface DocumentAffaireRepository extends JpaRepository<DocumentAffaire, Long> {
 
    @Query("SELECT d FROM DocumentAffaire d WHERE d.affaire.id = :affaireId ORDER BY d.dateUpload DESC")
    List<DocumentAffaire> findByAffaireId(@Param("affaireId") Long affaireId);
 
    @Query("SELECT d FROM DocumentAffaire d WHERE d.affaire.id = :affaireId AND d.typeDocument = :type")
    List<DocumentAffaire> findByAffaireIdAndType(
        @Param("affaireId") Long affaireId,
        @Param("type") DocumentAffaire.TypeDocument type);
}