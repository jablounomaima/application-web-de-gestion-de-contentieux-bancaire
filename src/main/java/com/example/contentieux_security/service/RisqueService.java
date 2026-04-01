package com.example.contentieux_security.service;

import com.example.contentieux_security.entity.Risque;
import com.example.contentieux_security.entity.Garantie;
import com.example.contentieux_security.repository.RisqueRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
@Service
public class RisqueService {

    private final RisqueRepository risqueRepository;

    public RisqueService(RisqueRepository risqueRepository) {
        this.risqueRepository = risqueRepository;
    }
    @Transactional
    public void ajouterGarantie(Long risqueId, Garantie garantie) {

        Risque risque = risqueRepository.findById(risqueId)
                .orElseThrow(() -> new RuntimeException("Risque introuvable"));

        if (risque.getGaranties() == null) {
            risque.setGaranties(new java.util.HashSet<>());
        }

        risque.getGaranties().add(garantie);
        garantie.setRisque(risque);

        risqueRepository.save(risque);
    }
}