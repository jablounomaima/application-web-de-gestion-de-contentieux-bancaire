package com.example.contentieux_security.service;

import com.example.contentieux_security.entity.Agence;        // ← AJOUTER CECI
import com.example.contentieux_security.entity.Client;
import com.example.contentieux_security.repository.ClientRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ClientService {

    private final ClientRepository clientRepository;

    public List<Client> findByAgence(Agence agence) {
        return clientRepository.findByAgence(agence);
    }
    public Client save(Client client) {
        return clientRepository.save(client);
    }
    public Client findById(Long id) {
        return clientRepository.findById(id).orElse(null);
    }
}