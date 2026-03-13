package com.example.contentieux_security.repository;

import com.example.contentieux_security.entity.Garantie;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface GarantieRepository extends JpaRepository<Garantie, Long> {
    @Query("""
        SELECT g FROM Garantie g
        LEFT JOIN FETCH g.risque r
        LEFT JOIN FETCH r.dossier
        WHERE g.id = :id
    """)
    Optional<Garantie> findByIdWithRisqueAndDossier(@Param("id") Long id);
}