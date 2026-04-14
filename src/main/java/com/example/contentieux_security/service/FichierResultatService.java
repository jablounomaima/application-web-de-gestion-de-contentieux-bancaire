package com.example.contentieux_security.service;

import com.example.contentieux_security.entity.FichierResultat;
import com.example.contentieux_security.repository.FichierResultatRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class FichierResultatService {

    private final FichierResultatRepository repository;
    
    public FichierResultat save(FichierResultat fichier) {
        return repository.save(fichier);
    }
    
    public List<FichierResultat> saveAll(List<FichierResultat> fichiers) {
        return repository.saveAll(fichiers);
    }
    
    @Transactional(readOnly = true)
    public FichierResultat findByNomFichierServeur(String nomServeur) {
        return repository.findByNomFichierServeur(nomServeur).orElse(null);
    }

    public FichierResultat findById(Long id) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'findById'");
    }
}