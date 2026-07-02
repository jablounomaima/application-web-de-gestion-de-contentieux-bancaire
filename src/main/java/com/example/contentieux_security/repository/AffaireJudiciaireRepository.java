package com.example.contentieux_security.repository;


import com.example.contentieux_security.entity.AffaireJudiciaire;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.Optional;
import java.util.List;

public interface AffaireJudiciaireRepository extends JpaRepository<AffaireJudiciaire, Long> {

    boolean existsByNumeroAffaire(String numeroAffaire);

    // ============================
    // 📄 DETAIL COMPLET D'UNE AFFAIRE
    // ============================
    // ✔ Charge audiences + avocat (évite LazyInitializationException)
    // ❌ FETCH a.mission retiré : AffaireJudiciaire n'a plus de relation Mission
    @Query("""
        SELECT DISTINCT a FROM AffaireJudiciaire a
        LEFT JOIN FETCH a.audiences
        LEFT JOIN FETCH a.avocat
        WHERE a.id = :id
    """)
    Optional<AffaireJudiciaire> findByIdWithAudiences(@Param("id") Long id);


    @Query("SELECT a FROM AffaireJudiciaire a " +
           "LEFT JOIN FETCH a.documents " +
           "LEFT JOIN FETCH a.avocat " +
           "WHERE a.id = :id")
    Optional<AffaireJudiciaire> findByIdWithDocuments(@Param("id") Long id);


    // ============================
    // 📋 AFFAIRES PAR AVOCAT
    // ============================
    // ✔ Récupère les affaires liées à l'avocat
    // ❌ Ne passe plus par Mission → Prestataire : a.avocat est la relation directe
    @Query("SELECT a FROM AffaireJudiciaire a " +
           "LEFT JOIN FETCH a.dossier d " +
           "LEFT JOIN FETCH a.avocat av " +
           "WHERE av.username = :username")
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
    // ❌ findByMission_Id supprimée : AffaireJudiciaire n'a plus de mission_id

    List<AffaireJudiciaire> findByDossier_Id(Long dossierId);

    // ❌ findByMission_Prestataire_Username supprimée : doublon cassé de findByAvocat_Username ci-dessous

    // ============================
    // 📋 AFFAIRES PAR AVOCAT (version simple)
    // ============================
    // ✔ Spring génère automatiquement la requête à partir du champ direct "avocat"
    List<AffaireJudiciaire> findByAvocat_Username(String username);


    @Query("SELECT a FROM AffaireJudiciaire a " +
           "LEFT JOIN FETCH a.avocat p " +
           "LEFT JOIN FETCH a.dossier " +
           "WHERE p.username = :username")
    List<AffaireJudiciaire> findByAvocatUsernameWithDetails(@Param("username") String username);

    // ❌ findByMissionPrestataireUsernameWithDetails supprimée : doublon cassé de la méthode ci-dessus


    // ============================
    // 📋 AFFAIRES PAR AVOCAT (avec dossier + client)
    // ============================
    // ❌ FETCH a.mission retiré : la relation avocat est directe désormais
    @Query("""
        SELECT DISTINCT a FROM AffaireJudiciaire a
        LEFT JOIN FETCH a.avocat av
        LEFT JOIN FETCH a.dossier d
        LEFT JOIN FETCH d.client
        WHERE av.username = :username
        ORDER BY a.dateLancement DESC
    """)
    List<AffaireJudiciaire> findByAvocatUsernameWithMission(
            @Param("username") String username);

    // ❌ findByMissionPrestataireUsernameWithMission supprimée : doublon cassé

    // ❌ findByDossierMissionPrestataireUsername supprimée : le chemin dossier→mission→prestataire
    //     n'a jamais existé sur le plan métier (le dossier n'a pas de mission)

    // ❌ findByMissionId supprimée : doublon de l'ancienne findByMission_Id

    // ❌ findByIdWithMission supprimée : c'est elle qui provoquait le crash au démarrage
    //     (Hibernate ne pouvait plus résoudre a.mission)


    // ============================
    // 📋 AFFAIRES PAR AVOCAT (avec dossier + client, alternative)
    // ============================
    // ❌ FETCH a.mission / m.prestataire retirés : utilise directement a.avocat
    @Query("""
        SELECT DISTINCT a FROM AffaireJudiciaire a
        LEFT JOIN FETCH a.avocat av
        LEFT JOIN FETCH a.dossier d
        LEFT JOIN FETCH d.client
        WHERE av.username = :username
        ORDER BY a.dateLancement DESC
    """)
    List<AffaireJudiciaire> findAffairesByAvocat(@Param("username") String username);

    // ============================
    // 🧾 FACTURES AVOCAT EN ATTENTE DE VALIDATION
    // ============================
    @Query("""
        SELECT DISTINCT a FROM AffaireJudiciaire a
        LEFT JOIN FETCH a.avocat av
        LEFT JOIN FETCH a.dossier d
        LEFT JOIN FETCH d.client
        WHERE a.factureStatut = 'EN_ATTENTE_VALIDATION'
        ORDER BY a.dateLancement DESC
    """)
    List<AffaireJudiciaire> findFacturesEnAttenteValidation();


    @Query("""
       SELECT DISTINCT a FROM AffaireJudiciaire a
       LEFT JOIN FETCH a.avocat av
       LEFT JOIN FETCH a.dossier d
       LEFT JOIN FETCH d.client
       WHERE a.factureRef IS NOT NULL
       ORDER BY a.dateLancement DESC
   """)
   List<AffaireJudiciaire> findAllFacturesAvocatSoumises();
}