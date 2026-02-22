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

    @Transactional
    public Dossier createDossier(Long clientId, Long agenceId, String agentUsername) {
        Client client = clientRepository.findById(clientId)
                .orElseThrow(() -> new RuntimeException("Client non trouvé"));
        Agence agence = agenceRepository.findById(agenceId)
                .orElseThrow(() -> new RuntimeException("Agence non trouvée"));

        Dossier dossier = Dossier.builder()
                .numeroDossier("DOS-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase())
                .client(client)
                .agence(agence)
                .statut(DossierStatus.BROUILLON)
                .dateCreation(LocalDateTime.now())
                .crééPar(agentUsername)
                .validationFinanciere(false)
                .validationJuridique(false)
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
        dossier.setValidationFinanciere(true);
        checkFinalValidation(dossier);
    }

    @Transactional
    public void validateJuridique(Long dossierId) {
        Dossier dossier = dossierRepository.findById(dossierId)
                .orElseThrow(() -> new RuntimeException("Dossier non trouvé"));
        dossier.setValidationJuridique(true);
        checkFinalValidation(dossier);
    }

    private void checkFinalValidation(Dossier dossier) {
        if (dossier.isValidationFinanciere() && dossier.isValidationJuridique()) {
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
    public Prestation createPrestation(Long dossierId, String type) {
        Dossier dossier = dossierRepository.findById(dossierId)
                .orElseThrow(() -> new RuntimeException("Dossier non trouvé"));

        Prestation prestation = Prestation.builder()
                .type(type)
                .dossier(dossier)
                .dateCreation(LocalDateTime.now())
                .build();

        return prestationRepository.save(prestation);
    }

    @Transactional
    public Mission createMission(Long prestationId, String titre, String prestataireNom, String prestataireRole) {
        Prestation prestation = prestationRepository.findById(prestationId)
                .orElseThrow(() -> new RuntimeException("Prestation non trouvée"));

        Mission mission = Mission.builder()
                .prestation(prestation)
                .titre(titre)
                .prestataireNom(prestataireNom)
                .prestataireRole(prestataireRole)
                .dateAssignation(LocalDateTime.now())
                .statut("EN_COURS")
                .build();

        return missionRepository.save(mission);
    }
}
