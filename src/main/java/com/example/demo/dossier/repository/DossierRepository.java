package com.example.demo.dossier.repository;

import com.example.demo.dossier.entity.Dossier;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface DossierRepository extends JpaRepository<Dossier, Long> {
    List<Dossier> findByAgenceId(Long agenceId);
    List<Dossier> findByAgentCreateurId(Long agentId);
    boolean existsByReference(String reference);
}
