package com.example.contentieux_security.repository;

import com.example.contentieux_security.entity.AffaireJudiciaire;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AffaireJudiciaireRepository extends JpaRepository<AffaireJudiciaire, Long> {
}
