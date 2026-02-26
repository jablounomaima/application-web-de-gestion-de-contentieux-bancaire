package com.example.contentieux_security.service;

import com.example.contentieux_security.entity.*;
import com.example.contentieux_security.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class DossierService {

    private final DossierRepository dossierRepository;
    private final ClientRepository clientRepository;
    private final AgenceRepository agenceRepository;
    private final RisqueRepository risqueRepository;
    private final GarantieRepository garantieRepository;
    private final AgentBancaireRepository agentBancaireRepository;

    @Transactional
    public Dossier createDossier(Long clientId, Long agenceId, String agentUsername) {
        Client client = clientRepository.findById(clientId)
                .orElseThrow(() -> new RuntimeException("Client non trouvé"));

        Agence agence;
        if (agenceId != null) {
            agence = agenceRepository.findById(agenceId)
                    .orElseThrow(() -> new RuntimeException("Agence non trouvée"));
        } else {
            // Fallback to the agent's assigned agency
            AgentBancaire agent = agentBancaireRepository.findByUsername(agentUsername)
                    .orElseThrow(() -> new RuntimeException("Agent non enregistré dans une agence"));
            agence = agent.getAgence();
        }

        Dossier dossier = Dossier.builder()
                .numeroDossier("DOS-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase())
                .client(client)
                .agence(agence)
                .statut(DossierStatus.BROUILLON)
                .dateCreation(LocalDateTime.now())
                .crééPar(agentUsername)
                .build();

        return dossierRepository.save(dossier);
    }

    @Transactional
    public void addRisque(Long dossierId, Risque risque) {
        Dossier dossier = dossierRepository.findById(dossierId)
                .orElseThrow(() -> new RuntimeException("Dossier non trouvé"));
        risque.setDossier(dossier);
        risqueRepository.save(risque);
    }

    @Transactional
    public void addGarantie(Long risqueId, Garantie garantie) {
        Risque risque = risqueRepository.findById(risqueId)
                .orElseThrow(() -> new RuntimeException("Risque non trouvé"));
        garantie.setRisque(risque);
        garantieRepository.save(garantie);
    }

    @Transactional
    public void submitForValidation(Long dossierId) {
        Dossier dossier = dossierRepository.findById(dossierId)
                .orElseThrow(() -> new RuntimeException("Dossier non trouvé"));

        if (dossier.getStatut() != DossierStatus.BROUILLON) {
            throw new RuntimeException("Le dossier doit être en état BROUILLON pour être soumis");
        }

        dossier.setStatut(DossierStatus.ATTENTE_VALIDATION);
        dossierRepository.save(dossier);
    }

    @Transactional
    public void validateFinanciere(Long dossierId) {
        Dossier dossier = dossierRepository.findById(dossierId)
                .orElseThrow(() -> new RuntimeException("Dossier non trouvé"));
        ValidationFinanciere vf = ValidationFinanciere.builder()
                .dossier(dossier)
                .estValide(true)
                .dateValidation(LocalDateTime.now())
                .build();
        dossier.setValidationFinanciere(vf);
        checkFinalValidation(dossier);
    }

    @Transactional
    public void validateJuridique(Long dossierId) {
        Dossier dossier = dossierRepository.findById(dossierId)
                .orElseThrow(() -> new RuntimeException("Dossier non trouvé"));
        ValidationJuridique vj = ValidationJuridique.builder()
                .dossier(dossier)
                .estValide(true)
                .dateValidation(LocalDateTime.now())
                .build();
        dossier.setValidationJuridique(vj);
        checkFinalValidation(dossier);
    }

    private void checkFinalValidation(Dossier dossier) {
        if (dossier.getValidationFinanciere() != null
                && Boolean.TRUE.equals(dossier.getValidationFinanciere().getEstValide()) &&
                dossier.getValidationJuridique() != null
                && Boolean.TRUE.equals(dossier.getValidationJuridique().getEstValide())) {
            dossier.setStatut(DossierStatus.VALIDE);
            dossierRepository.save(dossier);
        }
    }

    public List<Dossier> getAllDossiers() {
        return dossierRepository.findAll();
    }

    private final PrestationRepository prestationRepository;
    private final MissionRepository missionRepository;

    @Transactional
    public Prestation createPrestation(Long dossierId, String type, String description) {
        Dossier dossier = dossierRepository.findById(dossierId)
                .orElseThrow(() -> new RuntimeException("Dossier non trouvé"));

        Prestation prestation = Prestation.builder()
                .type(type)
                .description(description)
                .dossier(dossier)
                .dateDebut(java.time.LocalDate.now())
                .statut(StatutPrestation.EN_COURS)
                .build();

        return prestationRepository.save(prestation);
    }

    @Transactional
    public Mission createMission(Long prestationId, String description) {
        Prestation prestation = prestationRepository.findById(prestationId)
                .orElseThrow(() -> new RuntimeException("Prestation non trouvée"));

        Mission mission = Mission.builder()
                .prestation(prestation)
                .description(description)
                .dateAssignation(java.time.LocalDate.now())
                .statut(StatutMission.EN_COURS)
                .build();

        return missionRepository.save(mission);
    }
}
