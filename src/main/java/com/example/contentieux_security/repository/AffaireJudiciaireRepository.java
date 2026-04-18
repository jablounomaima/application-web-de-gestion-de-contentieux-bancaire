package com.example.contentieux_security.repository;


import com.example.contentieux_security.entity.AffaireJudiciaire;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.Optional;
import java.util.List;

public interface AffaireJudiciaireRepository extends JpaRepository<AffaireJudiciaire, Long> {

       // ============================
       // 📄 DETAIL COMPLET D’UNE AFFAIRE
       // ============================
       // ✔ Charge audiences + documents (évite LazyInitializationException)
    // PAR ces deux requêtes séparées :
    @Query("SELECT a FROM AffaireJudiciaire a " +
    "LEFT JOIN FETCH a.audiences " +
    "LEFT JOIN FETCH a.avocat " +
    "LEFT JOIN FETCH a.dossier d " +
    "LEFT JOIN FETCH d.client " +
    "WHERE a.id = :id")
Optional<AffaireJudiciaire> findByIdWithAudiences(@Param("id") Long id);
@Query("SELECT a FROM AffaireJudiciaire a " +
       "LEFT JOIN FETCH a.documents " +
       "LEFT JOIN FETCH a.avocat " +
       "WHERE a.id = :id")
Optional<AffaireJudiciaire> findByIdWithDocuments(@Param("id") Long id);     // ============================
       // 📋 AFFAIRES PAR AVOCAT
       // ============================
       // ✔ Récupère les affaires liées au prestataire (avocat)
       // ⚠️ passe par Mission → Prestataire
       @Query("SELECT a FROM AffaireJudiciaire a " +
              "LEFT JOIN FETCH a.dossier d " +
              "LEFT JOIN FETCH a.mission m " +
              "LEFT JOIN FETCH m.prestataire p " +
              "WHERE p.username = :username")
       List<AffaireJudiciaire> findByAvocatUsername(@Param("username") String username);
   
   
       // ============================
       // 📂 AFFAIRE PAR DOSSIER
       // ============================
       @Query("SELECT a FROM AffaireJudiciaire a " +
              "LEFT JOIN FETCH a.dossier " +
              "WHERE a.dossier.id = :dossierId")
       Optional<AffaireJudiciaire> findByDossierId(@Param("dossierId") Long dossierId);
   
   
       // ============================
       // 🔍 RECHERCHE PAR NUMERO
       // ============================
       Optional<AffaireJudiciaire> findByNumeroAffaire(String numeroAffaire);
   
   
       // ============================
       // 🔢 GENERATION NUMERO AFFAIRE
       // ============================
       // ✔ récupère le dernier numéro (pour incrémentation)
       @Query("SELECT MAX(CAST(SUBSTRING(a.numeroAffaire, :prefixLength + 1) AS integer)) " +
              "FROM AffaireJudiciaire a WHERE a.numeroAffaire LIKE :prefix%")
       Integer findMaxSequence(@Param("prefix") String prefix);
   
   
       // ============================
       // 🔗 RELATIONS DIRECTES
       // ============================
       Optional<AffaireJudiciaire> findByMission_Id(Long missionId);
   
       Optional<AffaireJudiciaire> findByDossier_Id(Long dossierId);
   
   
       // ============================
       // 📋 AFFAIRES PAR AVOCAT (version simple)
       // ============================
       // ✔ Spring génère automatiquement la requête
       List<AffaireJudiciaire> findByMission_Prestataire_Username(String username);
   
   
       // ============================
       // 📄 DETAIL AVEC AUDIENCES
       // ============================
     

      


          List<AffaireJudiciaire> findByAvocat_Username(String username);






          @Query("SELECT a FROM AffaireJudiciaire a " +
       "LEFT JOIN FETCH a.avocat p " +
       "LEFT JOIN FETCH a.dossier " +
       "WHERE p.username = :username")
List<AffaireJudiciaire> findByAvocatUsernameWithDetails(@Param("username") String username);

@Query("SELECT a FROM AffaireJudiciaire a " +
       "LEFT JOIN FETCH a.avocat p " +
       "LEFT JOIN FETCH a.dossier " +
       "LEFT JOIN FETCH a.mission m " +
       "LEFT JOIN FETCH m.prestataire pr " +
       "WHERE pr.username = :username")
List<AffaireJudiciaire> findByMissionPrestataireUsernameWithDetails(@Param("username") String username);
}