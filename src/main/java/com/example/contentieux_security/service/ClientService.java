package com.example.contentieux_security.service;

import com.example.contentieux_security.entity.Agence;
import com.example.contentieux_security.entity.Client;
import com.example.contentieux_security.enums.TypeClient;
import com.example.contentieux_security.repository.ClientRepository;
import com.example.contentieux_security.repository.DossierRepository;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ClientService {

    private final ClientRepository clientRepository;
    private final DossierRepository dossierRepository;

    // ✅ ICI tu ajoutes la méthode
    private String clean(String value) {
        if (value == null)
            return null;
        value = value.trim();
        return value.isEmpty() ? null : value;
    }

    public List<Client> findByAgence(Agence agence) {
        return clientRepository.findByAgence(agence);
    }

    @Transactional
    public Client save(Client client) {

        // 🔥 nettoyage de TOUS les champs String
        client.setCin(clean(client.getCin()));
        client.setRne(clean(client.getRne()));
        client.setEmail(clean(client.getEmail()));
        client.setRaisonSociale(clean(client.getRaisonSociale()));
        client.setTelephone(clean(client.getTelephone()));
        client.setAdresse(clean(client.getAdresse()));
        client.setNom(clean(client.getNom()));
        client.setPrenom(clean(client.getPrenom()));

        // 🔥 cohérence métier
        if (client.getTypeClient() == TypeClient.ENTREPRISE) {
            client.setCin(null);
            client.setPrenom(null); // pas de prénom pour une entreprise
        } else {
            client.setRne(null);
            client.setRaisonSociale(null);
        }

        return clientRepository.save(client);
    }

    public Client findById(Long id) {
        return clientRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Client introuvable avec id = " + id));
    }

    // ClientService.java
    // ClientService.java
    @Transactional
    public void deleteById(Long id) {
        // Supprimer d'abord les dossiers liés
        dossierRepository.deleteByClient_Id(id);
        // Puis supprimer le client
        clientRepository.deleteById(id);
    }
}