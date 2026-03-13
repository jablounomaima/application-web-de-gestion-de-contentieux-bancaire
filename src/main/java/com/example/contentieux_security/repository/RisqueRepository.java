package com.example.contentieux_security.repository;

import com.example.contentieux_security.entity.Risque;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface RisqueRepository extends JpaRepository<Risque, Long> {
    List<Risque> findByDossier_Id(Long dossierId);
    List<Risque> findByDossier_IdAndSelectionne(Long dossierId, boolean selectionne);
}