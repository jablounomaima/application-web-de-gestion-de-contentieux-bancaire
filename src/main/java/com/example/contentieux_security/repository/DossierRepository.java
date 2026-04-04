package com.example.contentieux_security.repository;

import com.example.contentieux_security.entity.DossierContentieux;
import com.example.contentieux_security.enums.DossierStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface DossierRepository extends JpaRepository<DossierContentieux, Long> {



    

    // =====================================================
    // 📌 DOSSIERS PAR AGENT (avec optimisation FETCH)
    // =====================================================
    @Query("""
        SELECT DISTINCT d FROM DossierContentieux d
        LEFT JOIN FETCH d.client
        LEFT JOIN FETCH d.agence
        WHERE d.agentCreateur.username = :username
    """)
    List<DossierContentieux> findByAgentCreateurUsername(@Param("username") String username);
   
     //✔️ Version simple (sans fetch)
   List<DossierContentieux> findByAgentCreateur_UsernameAndStatut(
           String username, DossierStatus statut);

    // =====================================================
    // 📌 PAR AGENCE
    // =====================================================
    List<DossierContentieux> findByAgence_Id(Long agenceId);

    List<DossierContentieux> findByAgence_IdAndStatut(Long agenceId, DossierStatus statut);

    // =====================================================
    // 📌 PAR STATUT
    // =====================================================
    List<DossierContentieux> findByStatut(DossierStatus statut);

    long countByStatut(DossierStatus statut);

    long countByAgentCreateur_UsernameAndStatut(String username, DossierStatus statut);

    // =====================================================
    // 📌 PAR CLIENT
    // =====================================================
    List<DossierContentieux> findByClient_Id(Long clientId);

    // =====================================================
    // 📌 NUMÉRO DOSSIER
    // =====================================================
    Optional<DossierContentieux> findByNumeroDossier(String numeroDossier);

    // ⚠️ CORRIGÉ (LIKE + CONCAT)
    @Query("""
        SELECT MAX(d.numeroDossier)
        FROM DossierContentieux d
        WHERE d.numeroDossier LIKE CONCAT(:prefix, '%')
    """)
    Optional<String> findLastNumero(@Param("prefix") String prefix);

    // =====================================================
    // 📌 VALIDATION FINANCIÈRE (par validateur)
    // =====================================================
    @Query("""
        SELECT DISTINCT d FROM DossierContentieux d
        LEFT JOIN FETCH d.client
        LEFT JOIN FETCH d.agence
        WHERE d.statut = 'EN_TRAITEMENT'
        AND d.validateurFinancierChoisi = :username
        AND (
            d.validationFinanciere IS NULL
            OR d.validationFinanciere = false
        )
    """)
    List<DossierContentieux> findEnAttenteValidationFinanciereParValidateur(
            @Param("username") String username);
    // =====================================================
    // 📌 VALIDATION JURIDIQUE (par validateur)
    // =====================================================
    @Query("""
        SELECT DISTINCT d FROM DossierContentieux d
        LEFT JOIN FETCH d.client
        LEFT JOIN FETCH d.agence
        WHERE d.statut = 'EN_TRAITEMENT'
        AND d.validateurJuridiqueChoisi = :username
        AND (
            d.validationJuridique IS NULL
            OR d.validationJuridique = false
        )
    """)
    List<DossierContentieux> findEnAttenteValidationJuridiqueParValidateur(
            @Param("username") String username);
    // =====================================================
    // 📌 VALIDATION GLOBALE
    // =====================================================
    @Query("""
        SELECT d FROM DossierContentieux d
        WHERE d.statut = 'EN_TRAITEMENT'
        AND d.validationFinanciere IS NULL
    """)
    List<DossierContentieux> findEnAttenteValidationFinanciere();

    @Query("""
        SELECT d FROM DossierContentieux d
        WHERE d.statut = 'EN_TRAITEMENT'
        AND d.validationJuridique IS NULL
    """)
    List<DossierContentieux> findEnAttenteValidationJuridique();

    // =====================================================
    // 📌 VALIDATION PAR AGENCE
    // =====================================================
    @Query("""
        SELECT d FROM DossierContentieux d
        WHERE d.agence.id = :agenceId
        AND d.statut = 'EN_TRAITEMENT'
        AND d.validationFinanciere IS NULL
    """)
    List<DossierContentieux> findEnAttenteValidationFinanciereParAgence(
            @Param("agenceId") Long agenceId);

    @Query("""
        SELECT d FROM DossierContentieux d
        WHERE d.agence.id = :agenceId
        AND d.statut = 'EN_TRAITEMENT'
        AND d.validationJuridique IS NULL
    """)
    List<DossierContentieux> findEnAttenteValidationJuridiqueParAgence(
            @Param("agenceId") Long agenceId);

    // =====================================================
    // 📊 STATISTIQUES
    // =====================================================
    @Query("SELECT d.statut, COUNT(d) FROM DossierContentieux d GROUP BY d.statut")
    List<Object[]> countByStatutGrouped();

    @Query("SELECT d.agence.nom, COUNT(d) FROM DossierContentieux d GROUP BY d.agence.nom")
    List<Object[]> countByAgenceGrouped();

    // =====================================================
    // 📌 DÉTAIL DOSSIER (optimisé)
    // =====================================================
    @Query("""
    SELECT DISTINCT d FROM DossierContentieux d
    LEFT JOIN FETCH d.client
    LEFT JOIN FETCH d.agence
    LEFT JOIN FETCH d.risques r
    LEFT JOIN FETCH r.garanties
    WHERE d.id = :id
""")
Optional<DossierContentieux> findByIdWithDetails(@Param("id") Long id);
   
    // =====================================================
    // 📌 VALIDATION FINANCIÈRE AVEC FETCH
    // =====================================================
    @Query("""
        SELECT d FROM DossierContentieux d
        LEFT JOIN FETCH d.client
        LEFT JOIN FETCH d.agence
        WHERE d.validateurFinancierChoisi = :username
        AND d.statut = 'EN_TRAITEMENT'
    """)
    List<DossierContentieux> findEnAttenteValidationFinanciereAvecRelations(
            @Param("username") String username);




            // ──  la méthode de recherche de dossier dans l'interface agentbancaire ──

            @Query("""
                SELECT DISTINCT d FROM DossierContentieux d
                LEFT JOIN FETCH d.client
                LEFT JOIN FETCH d.agence
                WHERE d.agentCreateur.username = :username
                AND (
                    LOWER(d.numeroDossier) LIKE LOWER(CONCAT('%', :keyword, '%'))
                    
                    OR (
                        d.client IS NOT NULL AND (
                            LOWER(d.client.cin) LIKE LOWER(CONCAT('%', :keyword, '%'))
                            OR LOWER(d.client.nom) LIKE LOWER(CONCAT('%', :keyword, '%'))
                            OR LOWER(d.client.prenom) LIKE LOWER(CONCAT('%', :keyword, '%'))
                            OR LOWER(d.client.rne) LIKE LOWER(CONCAT('%', :keyword, '%'))  
                            OR LOWER(d.client.email) LIKE LOWER(CONCAT('%', :keyword, '%'))
                            OR d.client.telephone LIKE CONCAT('%', :keyword, '%')           
                        )
                    )
                )
                ORDER BY d.dateCreation DESC
            """)
            List<DossierContentieux> rechercherParAgent(
                    @Param("username") String username,
                    @Param("keyword") String keyword);
}