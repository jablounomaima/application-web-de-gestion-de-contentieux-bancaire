package com.example.demo.agence.service;

import com.example.demo.agence.dto.AgenceRequest;
import com.example.demo.agence.dto.AgenceResponse;
import com.example.demo.agence.entity.Agence;
import com.example.demo.agence.repository.AgenceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AgenceService {

    private final AgenceRepository repository;

    @Transactional
    public AgenceResponse createAgence(AgenceRequest request) {
        if (repository.findByCode(request.getCode()).isPresent()) {
            throw new RuntimeException("Une agence avec ce code existe déjà");
        }
        Agence agence = new Agence();
        mapToEntity(request, agence);
        return mapToResponse(repository.save(agence));
    }

    @Transactional(readOnly = true)
    public List<AgenceResponse> getAllAgences() {
        return repository.findAll().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public AgenceResponse updateAgence(Long id, AgenceRequest request) {
        Agence agence = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Agence non trouvée"));
        mapToEntity(request, agence);
        return mapToResponse(repository.save(agence));
    }

    @Transactional
    public void deleteAgence(Long id) {
        repository.deleteById(id);
    }

    private void mapToEntity(AgenceRequest request, Agence agence) {
        agence.setCode(request.getCode());
        agence.setNom(request.getNom());
        agence.setAdresse(request.getAdresse());
        agence.setVille(request.getVille());
        agence.setTelephoneContact(request.getTelephoneContact());
    }

    private AgenceResponse mapToResponse(Agence agence) {
        AgenceResponse response = new AgenceResponse();
        response.setId(agence.getId());
        response.setCode(agence.getCode());
        response.setNom(agence.getNom());
        response.setAdresse(agence.getAdresse());
        response.setVille(agence.getVille());
        response.setTelephoneContact(agence.getTelephoneContact());
        return response;
    }
}
