package com.bank.contentieux.service;

import com.bank.contentieux.entity.Agence;
import com.bank.contentieux.entity.Resultat;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class MailService {

    @Autowired
    private JavaMailSender mailSender;

    // Méthode pour envoyer le résultat à l'agence
    public void envoyerResultat(Resultat resultat) {
        Agence agence = resultat.getDossier().getClient().getAgence(); // récupérer l'agence du client
        String emailAgence = agence.getEmail();

        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(emailAgence);
        message.setSubject("Nouveau résultat pour le client: " + resultat.getDossier().getClient().getNom());
        message.setText("Bonjour,\n\nVoici le résultat concernant le dossier: \n\n" + resultat.getDescription());

        mailSender.send(message);
    }
}
