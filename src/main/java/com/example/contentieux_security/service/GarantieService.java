package com.example.contentieux_security.service;

import com.example.contentieux_security.entity.Garantie;
import com.example.contentieux_security.repository.GarantieRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class GarantieService {

    private final GarantieRepository garantieRepository;

    public GarantieService(GarantieRepository garantieRepository) {
        this.garantieRepository = garantieRepository;
    }

    // 🔹 Récupérer toutes les garanties
    public List<Garantie> findAll() {
        return garantieRepository.findAll();
    }

    // 🔹 Récupérer une garantie par ID
    public Garantie getById(Long id) {
        return garantieRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Garantie introuvable avec id : " + id));
    }

    // 🔹 Sauvegarder / créer une garantie
    public Garantie save(Garantie garantie) {
        return garantieRepository.save(garantie);
    }

    // 🔹 Mettre à jour une garantie
    public Garantie update(Long id, Garantie updated) {
        Garantie existing = getById(id);

        existing.setTypeGarantie(updated.getTypeGarantie());
        existing.setValeurEstimee(updated.getValeurEstimee());
        existing.setDescription(updated.getDescription());

        return garantieRepository.save(existing);
    }

    // 🔹 Supprimer une garantie
    public void delete(Long id) {
        garantieRepository.deleteById(id);
    }

    // 🔹 Récupérer garanties par risque (optionnel mais utile dans ton cas)
    public List<Garantie> findByRisqueId(Long risqueId) {
        return garantieRepository.findByRisqueId(risqueId);
    }
}