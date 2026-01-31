package com.bank.contentieux.service;

import com.bank.contentieux.entity.Resultat;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class ResultatService {

    @Autowired
    private MailService mailService;

    public void creerResultat(Resultat resultat) {
        // Sauvegarder le résultat dans la base (repository.save(resultat))
        // ...

        // Envoyer par mail à l'agence
        mailService.envoyerResultat(resultat);
    }
}
