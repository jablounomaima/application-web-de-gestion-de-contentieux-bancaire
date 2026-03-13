package com.example.contentieux_security.service;

import com.example.contentieux_security.dto.DossierCreationRequest;
import com.example.contentieux_security.entity.Dossier;
import com.example.contentieux_security.repository.DossierRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class DossierService {

    private final DossierRepository dossierRepository;

    public List<Dossier> findAll() {
        return dossierRepository.findAll();
    }

    public Dossier findById(Long id) {
        return dossierRepository.findById(id).orElse(null);
    }

    public Dossier save(Dossier dossier) {
        return dossierRepository.save(dossier);
    }

    public void delete(Long id) {
        dossierRepository.deleteById(id);
    }

    // Méthodes manquantes ajoutées
    public List<Dossier> getDossiersAgent(String username) {
        return dossierRepository.findByAgentCreateur_Username(username);
    }

    public Dossier creerDossier(DossierCreationRequest request, String username) {
        Dossier dossier = new Dossier();
        dossier.setLibelle(request.getLibelle());
        dossier.setCreePar(username);
        // Mapper les autres champs...
        return dossierRepository.save(dossier);
    }

    public Dossier getDossierByIdAndAgent(Long id, String username) {
        return dossierRepository.findById(id)
            .filter(d -> d.getAgentCreateur() != null 
                && d.getAgentCreateur().getUsername().equals(username))
            .orElse(null);
    }

    public void soumettreAValidation(Long id, String username) {
        Dossier dossier = getDossierByIdAndAgent(id, username);
        if (dossier != null) {
            dossier.setStatut(com.example.contentieux_security.enums.DossierStatus.EN_TRAITEMENT);
            dossierRepository.save(dossier);
        }
    }

    public void selectionnerRisque(Long dossierId, String risqueId) {
        // Implémentation selon votre logique métier
    }


    public List<Dossier> findByClientId(Long clientId) {
        return dossierRepository.findByClient_Id(clientId);
    }
}