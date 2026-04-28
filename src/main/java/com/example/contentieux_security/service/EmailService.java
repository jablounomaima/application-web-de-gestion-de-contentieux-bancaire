package com.example.contentieux_security.service;

import lombok.RequiredArgsConstructor;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import jakarta.mail.internet.MimeMessage;

@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;

    // ✅ Envoie les credentials au prestataire nouvellement créé
    public void envoyerCredentiels(String destinataire, String username, String motDePasse) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setTo(destinataire);
            helper.setSubject("Vos identifiants de connexion — Plateforme Contentieux");

            // Corps du mail en HTML
            String html = """
                <div style="font-family: Arial, sans-serif; max-width: 600px; margin: auto;">
                    <h2 style="color: #1a237e;">Bienvenue sur la Plateforme Contentieux</h2>
                    <p>Votre compte prestataire a été créé avec succès.</p>
                    <p>Voici vos identifiants de connexion :</p>
                    <div style="background:#f5f5f5; padding:16px; border-radius:8px; margin:16px 0;">
                        <p><strong>Nom d'utilisateur :</strong> %s</p>
                        <p><strong>Mot de passe :</strong> %s</p>
                    </div>
                    <p style="color:#e53935;">
                        Pour des raisons de sécurité, veuillez changer votre mot de passe 
                        lors de votre première connexion.
                    </p>
                    <hr/>
                    <p style="color:#9e9e9e; font-size:12px;">
                        Cet email a été envoyé automatiquement, merci de ne pas y répondre.
                    </p>
                </div>
                """.formatted(username, motDePasse);

            helper.setText(html, true); // true = HTML
            mailSender.send(message);

            System.out.println("✅ Email credentials envoyé à : " + destinataire);

        } catch (Exception e) {
            System.out.println("❌ Erreur envoi email : " + e.getMessage());
            throw new RuntimeException("Impossible d'envoyer l'email : " + e.getMessage());
        }
    }
}