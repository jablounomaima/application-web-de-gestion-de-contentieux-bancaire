package com.example.contentieux_security.service;

import com.example.contentieux_security.entity.Agence;
import com.example.contentieux_security.entity.AgentBancaire;
import com.example.contentieux_security.entity.DossierStatus;
import com.example.contentieux_security.repository.AgenceRepository;
import com.example.contentieux_security.repository.AgentBancaireRepository;
import com.example.contentieux_security.repository.DossierRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AdminService {
    private final AgenceRepository agenceRepository;
    private final AgentBancaireRepository agentBancaireRepository;
    private final DossierRepository dossierRepository;

    // Agence Management
    public List<Agence> getAllAgences() {
        return agenceRepository.findAll();
    }

    public Agence saveAgence(Agence agence) {
        return agenceRepository.save(agence);
    }

    public void deleteAgence(Long id) {
        agenceRepository.deleteById(id);
    }

    // Agent Management
    public List<AgentBancaire> getAllAgents() {
        return agentBancaireRepository.findAll();
    }

    public AgentBancaire saveAgent(AgentBancaire agent) {
        return agentBancaireRepository.save(agent);
    }

    public void deleteAgent(Long id) {
        agentBancaireRepository.deleteById(id);
    }

    // Statistics
    public Map<String, Object> getStats() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalDossiers", dossierRepository.count());
        stats.put("totalAgences", agenceRepository.count());
        stats.put("totalAgents", agentBancaireRepository.count());

        // Count by status
        Map<String, Long> byStatus = new HashMap<>();
        for (DossierStatus status : DossierStatus.values()) {
            byStatus.put(status.name(), (long) dossierRepository.findAll().stream()
                    .filter(d -> d.getStatut() == status).count());
        }
        stats.put("byStatus", byStatus);

        return stats;
    }
}
