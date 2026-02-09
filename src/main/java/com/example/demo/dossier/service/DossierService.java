package com.example.demo.dossier.service;

import com.example.demo.agence.repository.AgenceRepository;
import com.example.demo.client.entity.Client;
import com.example.demo.client.entity.ClientMorale;
import com.example.demo.client.entity.ClientPhysique;
import com.example.demo.client.repository.ClientRepository;
import com.example.demo.dossier.dto.DossierRequest;
import com.example.demo.dossier.dto.DossierResponse;
import com.example.demo.dossier.dto.NewClientRequest;
import com.example.demo.dossier.entity.Dossier;
import com.example.demo.dossier.repository.DossierRepository;
import com.example.demo.user.entity.impl.AgentBancaire;
import com.example.demo.user.repository.UtilisateurRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Year;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DossierService {

    private final DossierRepository dossierRepository;
    private final ClientRepository clientRepository;
    private final AgenceRepository agenceRepository;
    private final UtilisateurRepository utilisateurRepository;

    @Transactional
    public DossierResponse createDossier(DossierRequest request, String agentUsername) {
        Dossier dossier = new Dossier();
        dossier.setReference(generateUniqueReference());

        // Gestion du Client (Existant ou Nouveau)
        Client client;
        if (request.getClientId() != null) {
            client = clientRepository.findById(request.getClientId())
                    .orElseThrow(() -> new RuntimeException("Client non trouvé"));
        } else if (request.getNewClient() != null) {
            NewClientRequest nc = request.getNewClient();
            if ("PHYSIQUE".equals(nc.getType())) {
                ClientPhysique cp = new ClientPhysique();
                cp.setNom(nc.getNom());
                cp.setPrenom(nc.getPrenom());
                cp.setCin(nc.getCin());
                cp.setTelephone(nc.getTelephone());
                cp.setEmail(nc.getEmail());
                cp.setAdresse(nc.getAdresse());
                cp.setVille(nc.getVille());
                client = clientRepository.save(cp);
            } else {
                ClientMorale cm = new ClientMorale();
                cm.setRaisonSociale(nc.getRaisonSociale());
                cm.setNumeroRC(nc.getNumeroRC());
                cm.setTelephone(nc.getTelephone());
                cm.setEmail(nc.getEmail());
                cm.setAdresse(nc.getAdresse());
                cm.setVille(nc.getVille());
                client = clientRepository.save(cm);
            }
        } else {
            throw new RuntimeException("Client manquant (ID ou Nouveau Client requis)");
        }
        
        dossier.setClient(client);
        dossier.setAgence(agenceRepository.findById(request.getAgenceId())
                .orElseThrow(() -> new RuntimeException("Agence non trouvée")));
        
        dossier.setMontantCreance(request.getMontantCreance());
        dossier.setStrategie(request.getStrategie());
        dossier.setStatut(request.getStatut());

        AgentBancaire agent = (AgentBancaire) utilisateurRepository.findByUsername(agentUsername)
                .orElseThrow(() -> new RuntimeException("Agent non trouvé"));
        dossier.setAgentCreateur(agent);

        return mapToResponse(dossierRepository.save(dossier));
    }

    @Transactional(readOnly = true)
    public List<DossierResponse> getAllDossiers() {
        return dossierRepository.findAll().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    private String generateUniqueReference() {
        String ref;
        do {
            int num = new Random().nextInt(9000) + 1000;
            ref = "DOS-" + Year.now().getValue() + "-" + num;
        } while (dossierRepository.existsByReference(ref));
        return ref;
    }

    private DossierResponse mapToResponse(Dossier dossier) {
        DossierResponse res = new DossierResponse();
        res.setId(dossier.getId());
        res.setReference(dossier.getReference());
        res.setClientNom(dossier.getClient().getNom()); 
        res.setAgenceNom(dossier.getAgence().getNom());
        res.setMontantCreance(dossier.getMontantCreance());
        res.setMontantRecouvre(dossier.getMontantRecouvre());
        res.setTauxRecouvrement(dossier.getTauxRecouvrement());
        res.setStatut(dossier.getStatut());
        res.setStrategie(dossier.getStrategie());
        res.setDateCreation(dossier.getDateCreation());
        if (dossier.getAgentCreateur() != null) {
            res.setAgentCreateurUsername(dossier.getAgentCreateur().getUsername());
        }
        return res;
    }
}
