package com.example.demo.client.controller;

import com.example.demo.client.dto.ClientResponse;
import com.example.demo.client.entity.Client;
import com.example.demo.client.entity.ClientMorale;
import com.example.demo.client.entity.ClientPhysique;
import com.example.demo.client.repository.ClientRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/clients")
@RequiredArgsConstructor
public class ClientController {

    private final ClientRepository clientRepository;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'AGENT_BANCAIRE')")
    public ResponseEntity<List<ClientResponse>> getAll() {
        List<ClientResponse> responses = clientRepository.findAll().stream()
                .map(client -> {
                    ClientResponse res = new ClientResponse();
                    res.setId(client.getId());
                    res.setNom(client.getNom());
                    res.setVille(client.getVille());
                    res.setTelephone(client.getTelephone());
                    res.setType(client instanceof ClientPhysique ? "Physique" : "Morale");
                    return res;
                })
                .collect(Collectors.toList());
        return ResponseEntity.ok(responses);
    }
}
