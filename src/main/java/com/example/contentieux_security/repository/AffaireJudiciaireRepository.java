package com.example.contentieux_security.repository;


import com.example.contentieux_security.entity.AffaireJudiciaire;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.Optional;
import java.util.List;

public interface AffaireJudiciaireRepository extends JpaRepository<AffaireJudiciaire, Long> {

    @Query("SELECT a FROM AffaireJudiciaire a " +
           "LEFT JOIN FETCH a.dossier d " +
           "LEFT JOIN FETCH a.mission m " +
           "LEFT JOIN FETCH m.prestataire " +
           "WHERE a.id = :id")
    Optional<AffaireJudiciaire> findByIdWithDetails(@Param("id") Long id);

    @Query("SELECT a FROM AffaireJudiciaire a " +
           "LEFT JOIN FETCH a.dossier d " +
           "LEFT JOIN FETCH a.mission m " +
           "LEFT JOIN FETCH m.prestataire p " +
           "WHERE p.username = :username")
    List<AffaireJudiciaire> findByAvocatUsername(@Param("username") String username);

    @Query("SELECT a FROM AffaireJudiciaire a " +
           "LEFT JOIN FETCH a.dossier " +
           "WHERE a.dossier.id = :dossierId")
    Optional<AffaireJudiciaire> findByDossierId(@Param("dossierId") Long dossierId);

    Optional<AffaireJudiciaire> findByNumeroAffaire(String numeroAffaire);

    @Query("SELECT COALESCE(MAX(CAST(SUBSTRING(a.numeroAffaire, 10) AS int)), 0) " +
           "FROM AffaireJudiciaire a " +
           "WHERE a.numeroAffaire LIKE :prefix%")
    Integer findMaxSequence(@Param("prefix") String prefix);
}
