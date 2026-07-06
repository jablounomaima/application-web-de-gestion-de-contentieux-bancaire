package com.example.contentieux_security.service;

import com.example.contentieux_security.dto.CompteBancaireAgenceDTO;
import com.example.contentieux_security.entity.Agence;
import com.example.contentieux_security.entity.CompteBancaireAgence;
import com.example.contentieux_security.repository.AgenceRepository;
import com.example.contentieux_security.repository.CompteBancaireAgenceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CompteBancaireAgenceService {

    private final CompteBancaireAgenceRepository compteRepository;
    private final AgenceRepository agenceRepository;

    @Transactional(readOnly = true)
    public List<CompteBancaireAgenceDTO> getAllComptes() {
        return compteRepository.findAll().stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    @Transactional
    public CompteBancaireAgenceDTO createCompte(CompteBancaireAgenceDTO dto) {
        if (compteRepository.findByRib(dto.getRib()).isPresent()) {
            throw new RuntimeException("Ce RIB est déjà utilisé par un autre compte");
        }
        Agence agence = agenceRepository.findById(dto.getAgenceId())
                .orElseThrow(() -> new RuntimeException("Agence non trouvée"));

        CompteBancaireAgence compte = CompteBancaireAgence.builder()
                .banque(dto.getBanque())
                .rib(dto.getRib())
                .titulaireCompte(dto.getTitulaireCompte())
                .actif(true)
                .agence(agence)
                .build();

        return convertToDTO(compteRepository.save(compte));
    }

    @Transactional
    public CompteBancaireAgenceDTO updateCompte(Long id, CompteBancaireAgenceDTO dto) {
        CompteBancaireAgence compte = compteRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Compte bancaire non trouvé"));

        if (!compte.getRib().equals(dto.getRib()) && compteRepository.findByRib(dto.getRib()).isPresent()) {
            throw new RuntimeException("Ce RIB est déjà utilisé par un autre compte");
        }

        Agence agence = agenceRepository.findById(dto.getAgenceId())
                .orElseThrow(() -> new RuntimeException("Agence non trouvée"));

        compte.setBanque(dto.getBanque());
        compte.setRib(dto.getRib());
        compte.setTitulaireCompte(dto.getTitulaireCompte());
        compte.setActif(dto.isActif());
        compte.setAgence(agence);

        return convertToDTO(compteRepository.save(compte));
    }

    @Transactional
    public void deleteCompte(Long id) {
        if (!compteRepository.existsById(id)) {
            throw new RuntimeException("Compte bancaire non trouvé");
        }
        compteRepository.deleteById(id);
    }

    @Transactional
    public void toggleActif(Long id) {
        CompteBancaireAgence compte = compteRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Compte bancaire non trouvé"));
        compte.setActif(!compte.isActif());
        compteRepository.save(compte);
    }

    private CompteBancaireAgenceDTO convertToDTO(CompteBancaireAgence compte) {
        return new CompteBancaireAgenceDTO(
                compte.getId(),
                compte.getBanque(),
                compte.getRib(),
                compte.getTitulaireCompte(),
                compte.isActif(),
                compte.getAgence() != null ? compte.getAgence().getId() : null,
                compte.getAgence() != null ? compte.getAgence().getNom() : null
        );
    }
}
