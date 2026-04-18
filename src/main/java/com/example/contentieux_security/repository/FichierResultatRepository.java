package com.example.contentieux_security.repository;

import com.example.contentieux_security.entity.FichierResultat;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FichierResultatRepository extends JpaRepository<FichierResultat, Long> {
    
    List<FichierResultat> findByResultatId(Long resultatId);
    
    Optional<FichierResultat> findByNomFichierServeur(String nomFichierServeur);


    void deleteByResultatId(Long resultatId);
}