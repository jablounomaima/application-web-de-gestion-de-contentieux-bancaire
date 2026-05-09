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
    
            String html = """
                <div style="font-family:Arial,sans-serif;max-width:600px;
                            margin:auto;border:1px solid #e0e0e0;
                            border-radius:8px;overflow:hidden;">
    
                  <div style="background:#1a237e;padding:24px;text-align:center;">
                    <h2 style="color:#fff;margin:0;">Plateforme Contentieux</h2>
                    <p style="color:#90caf9;margin:8px 0 0;">
                      Vos identifiants de connexion
                    </p>
                  </div>
    
                  <div style="padding:24px;">
                    <p>Bonjour,</p>
                    <p>Votre compte a été créé. Voici vos identifiants temporaires :</p>
    
                    <div style="background:#f5f5f5;border-left:4px solid #1a237e;
                                padding:16px;border-radius:4px;margin:16px 0;">
                      <p style="margin:0 0 8px;">
                        <strong>Nom d'utilisateur :</strong>
                        <span style="font-family:monospace;font-size:15px;">
                          %s
                        </span>
                      </p>
                      <p style="margin:0;">
                        <strong>Mot de passe temporaire :</strong>
                        <span style="font-family:monospace;font-size:15px;">
                          %s
                        </span>
                      </p>
                    </div>
    
                    <!-- ✅ Message changement obligatoire -->
                    <div style="background:#e8f5e9;border:1px solid #a5d6a7;
                                padding:12px;border-radius:4px;margin:16px 0;">
                      <p style="margin:0;color:#2e7d32;font-size:13px;">
                        🔐 <strong>Action requise :</strong> 
                        Lors de votre première connexion, vous serez automatiquement 
                        redirigé vers une page pour changer votre mot de passe.
                        Ce mot de passe temporaire ne peut être utilisé qu'une seule fois.
                      </p>
                    </div>
    
                    <div style="background:#fff3e0;border:1px solid #ffcc02;
                                padding:12px;border-radius:4px;margin:16px 0;">
                      <p style="margin:0;color:#e65100;font-size:13px;">
                        ⚠️ Ne partagez jamais vos identifiants avec quiconque.
                      </p>
                    </div>
    
                    <p style="color:#757575;font-size:12px;margin-top:24px;
                               border-top:1px solid #eee;padding-top:12px;">
                      Cet email a été envoyé automatiquement. 
                      Merci de ne pas y répondre.
                    </p>
                  </div>
                </div>
                """.formatted(username, motDePasse);
    
            helper.setText(html, true);
            mailSender.send(message);
            System.out.println("✅ Email credentials envoyé à : " + destinataire);
    
        } catch (Exception e) {
            throw new RuntimeException("Impossible d'envoyer l'email : " + e.getMessage());
        }
    }
}