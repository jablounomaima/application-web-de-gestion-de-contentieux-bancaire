package com.example.contentieux_security.service;

import com.example.contentieux_security.dto.AgenceDTO;
import com.example.contentieux_security.entity.Agence;
import com.example.contentieux_security.repository.AgenceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AgenceService {

    private final AgenceRepository agenceRepository;

    public List<AgenceDTO> getAllAgences() {
        return agenceRepository.findAll().stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    public AgenceDTO getAgenceById(Long id) {
        Agence agence = agenceRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Agence non trouvée"));
        return convertToDTO(agence);
    }

    @Transactional
    public AgenceDTO createAgence(AgenceDTO dto) {
        if (agenceRepository.existsByCode(dto.getCode())) {
            throw new RuntimeException("Code agence déjà existant");
        }

        Agence agence = Agence.builder()
                .code(dto.getCode())
                .nom(dto.getNom())
                .adresse(dto.getAdresse())
                .ville(dto.getVille())
                .telephone(dto.getTelephone())
                .email(dto.getEmail())
                .build();

        return convertToDTO(agenceRepository.save(agence));
    }

    @Transactional
    public AgenceDTO updateAgence(Long id, AgenceDTO dto) {
        Agence agence = agenceRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Agence non trouvée"));

        // Vérifier si le nouveau code n'est pas déjà utilisé par une autre agence
        if (!agence.getCode().equals(dto.getCode()) && agenceRepository.existsByCode(dto.getCode())) {
            throw new RuntimeException("Code agence déjà existant");
        }

        agence.setCode(dto.getCode());
        agence.setNom(dto.getNom());
        agence.setAdresse(dto.getAdresse());
        agence.setVille(dto.getVille());
        agence.setTelephone(dto.getTelephone());
        agence.setEmail(dto.getEmail());

        return convertToDTO(agenceRepository.save(agence));
    }

    @Transactional
    public void deleteAgence(Long id) {
        Agence agence = agenceRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Agence non trouvée"));

        // Vérifier si l'agence a des agents
        if (!agence.getAgents().isEmpty()) {
            throw new RuntimeException("Impossible de supprimer : l'agence contient des agents");
        }

        agenceRepository.deleteById(id);
    }

    private AgenceDTO convertToDTO(Agence agence) {
        AgenceDTO dto = new AgenceDTO();
        dto.setId(agence.getId());
        dto.setCode(agence.getCode());
        dto.setNom(agence.getNom());
        dto.setAdresse(agence.getAdresse());
        dto.setVille(agence.getVille());
        dto.setTelephone(agence.getTelephone());
        dto.setEmail(agence.getEmail());
        dto.setNombreAgents(agence.getNombreAgents());
        return dto;
    }
}