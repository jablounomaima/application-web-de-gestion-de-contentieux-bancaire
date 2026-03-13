package com.example.contentieux_security.repository;

import com.example.contentieux_security.entity.Dossier;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DossierRepository extends JpaRepository<Dossier, Long> {

    List<Dossier> findByAgentCreateur_Username(String username);
    
    List<Dossier> findByAgentCreateur_UsernameAndStatut(String username, String statut);
    
    List<Dossier> findByAgence_Id(Long agenceId);
    
    List<Dossier> findByStatut(String statut);
    
    Optional<Dossier> findByNumeroDossier(String numeroDossier);
    
    List<Dossier> findByClient_Id(Long clientId);
    
    long countByAgentCreateur_UsernameAndStatut(String username, String statut);

    @Query("SELECT MAX(d.numeroDossier) FROM Dossier d WHERE d.numeroDossier LIKE :prefix%")
    Optional<String> findLastNumero(@Param("prefix") String prefix);

    @Query("SELECT d FROM Dossier d WHERE d.statut = 'EN_ATTENTE_VALIDATION' AND d.validationFinanciere IS NULL")
    List<Dossier> findEnAttenteValidationFinanciere();

    @Query("SELECT d FROM Dossier d WHERE d.statut = 'EN_ATTENTE_VALIDATION' AND d.validationJuridique IS NULL")
    List<Dossier> findEnAttenteValidationJuridique();
}