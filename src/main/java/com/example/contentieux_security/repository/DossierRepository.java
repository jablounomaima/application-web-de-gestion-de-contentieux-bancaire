package com.example.contentieux_security.repository;

import com.example.contentieux_security.entity.DossierContentieux;
import com.example.contentieux_security.enums.DossierStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;

public interface DossierRepository extends JpaRepository<DossierContentieux, Long> {

    // ── Requêtes par agent ────────────────────────────────────────
    List<DossierContentieux> findByAgentCreateur_Username(String username);

    List<DossierContentieux> findByAgentCreateur_UsernameAndStatut(
            String username, DossierStatus statut);

    // ── Requêtes par agence ───────────────────────────────────────
    List<DossierContentieux> findByAgence_Id(Long agenceId);

    List<DossierContentieux> findByAgence_IdAndStatut(Long agenceId, DossierStatus statut);

    // ── Requêtes par statut ───────────────────────────────────────
    List<DossierContentieux> findByStatut(DossierStatus statut);

    long countByStatut(DossierStatus statut);

    long countByAgentCreateur_UsernameAndStatut(String username, DossierStatus statut);

    // ── Requêtes par client ───────────────────────────────────────
    List<DossierContentieux> findByClient_Id(Long clientId);

    // ── Numéro dossier ────────────────────────────────────────────
    Optional<DossierContentieux> findByNumeroDossier(String numeroDossier);

    @Query("SELECT MAX(d.numeroDossier) FROM DossierContentieux d WHERE d.numeroDossier LIKE :prefix%")
    Optional<String> findLastNumero(@Param("prefix") String prefix);

    // ── Validation — dossiers en attente pour chaque validateur ───

    @Query("SELECT d FROM DossierContentieux d " +
           "WHERE d.statut = 'EN_TRAITEMENT' AND d.validationFinanciere IS NULL")
    List<DossierContentieux> findEnAttenteValidationFinanciere();

    @Query("SELECT d FROM DossierContentieux d " +
           "WHERE d.statut = 'EN_TRAITEMENT' AND d.validationJuridique IS NULL")
    List<DossierContentieux> findEnAttenteValidationJuridique();

    // ── Requête pour l'agence du validateur ───────────────────────

    @Query("SELECT d FROM DossierContentieux d " +
           "WHERE d.agence.id = :agenceId " +
           "AND d.statut = 'EN_TRAITEMENT' AND d.validationFinanciere IS NULL")
    List<DossierContentieux> findEnAttenteValidationFinanciereParAgence(@Param("agenceId") Long agenceId);

    @Query("SELECT d FROM DossierContentieux d " +
           "WHERE d.agence.id = :agenceId " +
           "AND d.statut = 'EN_TRAITEMENT' AND d.validationJuridique IS NULL")
    List<DossierContentieux> findEnAttenteValidationJuridiqueParAgence(@Param("agenceId") Long agenceId);

    // ── Statistiques ──────────────────────────────────────────────

    @Query("SELECT d.statut, COUNT(d) FROM DossierContentieux d GROUP BY d.statut")
    List<Object[]> countByStatutGrouped();

    @Query("SELECT d.agence.nom, COUNT(d) FROM DossierContentieux d GROUP BY d.agence.nom")
    List<Object[]> countByAgenceGrouped();

    // Ajouter cette méthode
    @Query("""
        SELECT DISTINCT d FROM DossierContentieux d
        LEFT JOIN FETCH d.client
        LEFT JOIN FETCH d.agence
        LEFT JOIN FETCH d.risques r
        LEFT JOIN FETCH r.garanties
        WHERE d.id = :id
    """)
    Optional<DossierContentieux> findByIdWithDetails(@Param("id") Long id);

}