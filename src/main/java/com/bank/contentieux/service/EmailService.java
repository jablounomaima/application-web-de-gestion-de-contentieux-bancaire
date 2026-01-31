package com.bank.contentieux.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import com.bank.contentieux.entity.Resultat;
import org.springframework.stereotype.Service;


@Service
public class EmailService {

    @Autowired
    private JavaMailSender mailSender;

    public void envoyerResultat(Resultat resultat) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(resultat.getAgence().getEmail());
        message.setSubject("Nouveau résultat pour le client : " 
            + resultat.getDossier().getClient().getNom());
        message.setText(resultat.getDescription());

        mailSender.send(message);
    }
}
