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

    // ════════════════════════════════════════════════════════════════════
// À AJOUTER dans ClientService.java
// Placer après la méthode findById()
// ════════════════════════════════════════════════════════════════════

@Transactional
public Client modifierClient(Long id, Client nouvellesDonnees) {

    // 1. Récupération du client existant (lève une exception si absent)
    Client existing = clientRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Client introuvable : id=" + id));

    // 2. Mise à jour uniquement des champs non-null fournis
    if (nouvellesDonnees.getNom() != null)
        existing.setNom(clean(nouvellesDonnees.getNom()));

    if (nouvellesDonnees.getPrenom() != null)
        existing.setPrenom(clean(nouvellesDonnees.getPrenom()));

    if (nouvellesDonnees.getRaisonSociale() != null)
        existing.setRaisonSociale(clean(nouvellesDonnees.getRaisonSociale()));

    if (nouvellesDonnees.getTypeClient() != null)
        existing.setTypeClient(nouvellesDonnees.getTypeClient());

    if (nouvellesDonnees.getCin() != null)
        existing.setCin(clean(nouvellesDonnees.getCin()));

    if (nouvellesDonnees.getRne() != null)
        existing.setRne(clean(nouvellesDonnees.getRne()));

    if (nouvellesDonnees.getEmail() != null)
        existing.setEmail(clean(nouvellesDonnees.getEmail()));

    if (nouvellesDonnees.getTelephone() != null)
        existing.setTelephone(clean(nouvellesDonnees.getTelephone()));

    if (nouvellesDonnees.getAdresse() != null)
        existing.setAdresse(clean(nouvellesDonnees.getAdresse()));

    // 3. Cohérence métier (même logique que save())
    if (existing.getTypeClient() == TypeClient.ENTREPRISE) {
        existing.setCin(null);
        existing.setPrenom(null);
    } else {
        existing.setRne(null);
        existing.setRaisonSociale(null);
    }

    return clientRepository.save(existing);
}
}