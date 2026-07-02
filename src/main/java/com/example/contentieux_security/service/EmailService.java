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


    // ✅ Email à l'admin — notification demande mdp oublié
public void envoyerNotificationAdminMdpOublie(
  String adminEmail, String username, String emailUtilisateur) {
try {
  MimeMessage message = mailSender.createMimeMessage();
  MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

  helper.setTo(adminEmail);
  helper.setSubject("🔐 Demande réinitialisation mot de passe — " + username);

  String html = """
      <div style="font-family:Arial,sans-serif;max-width:600px;margin:auto;
                  border:1px solid #e0e0e0;border-radius:8px;overflow:hidden;">
        <div style="background:#b71c1c;padding:24px;text-align:center;">
          <h2 style="color:#fff;margin:0;">⚠️ Mot de passe oublié</h2>
        </div>
        <div style="padding:24px;">
          <p>Un utilisateur a oublié son mot de passe :</p>
          <div style="background:#f5f5f5;padding:16px;border-radius:4px;
                      border-left:4px solid #b71c1c;margin:16px 0;">
            <p style="margin:0 0 8px;">
              <strong>Username :</strong> %s
            </p>
            <p style="margin:0;">
              <strong>Email :</strong> %s
            </p>
          </div>
          <p>
            Connectez-vous au dashboard admin et cliquez sur 
            <strong>"Réinitialiser le mot de passe"</strong> 
            pour cet utilisateur.
          </p>
        </div>
      </div>
      """.formatted(username, emailUtilisateur);

  helper.setText(html, true);
  mailSender.send(message);
  System.out.println("✅ Notification admin envoyée pour: " + username);

// ✅ Après — relance l'exception
} catch (Exception e) {
  System.err.println("❌ Erreur notification admin: " + e.getMessage());
  e.printStackTrace();  // ← voir l'erreur complète dans les logs
  throw new RuntimeException("Erreur envoi notif admin: " + e.getMessage(), e);
}
}

// ✅ Email à l'utilisateur — accusé de réception
public void envoyerAccuseReceptionMdpOublie(String email, String username) {
try {
  MimeMessage message = mailSender.createMimeMessage();
  MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

  helper.setTo(email);
  helper.setSubject("Demande de réinitialisation reçue — Plateforme Contentieux");

  String html = """
      <div style="font-family:Arial,sans-serif;max-width:600px;margin:auto;
                  border:1px solid #e0e0e0;border-radius:8px;overflow:hidden;">
        <div style="background:#1a237e;padding:24px;text-align:center;">
          <h2 style="color:#fff;margin:0;">Plateforme Contentieux</h2>
        </div>
        <div style="padding:24px;">
          <p>Bonjour <strong>%s</strong>,</p>
          <p>
            Votre demande de réinitialisation de mot de passe 
            a bien été reçue.
          </p>
          <div style="background:#e3f2fd;border:1px solid #90caf9;
                      padding:12px;border-radius:4px;margin:16px 0;">
            <p style="margin:0;color:#1565c0;">
              ℹ️ Un administrateur va traiter votre demande et vous 
              envoyer un nouveau mot de passe par email dans les 
              plus brefs délais.
            </p>
          </div>
        </div>
      </div>
      """.formatted(username);

  helper.setText(html, true);
  mailSender.send(message);

} catch (Exception e) {
  System.err.println("❌ Erreur accusé réception: " + e.getMessage());
  throw new RuntimeException("Erreur envoi email: " + e.getMessage(), e);
}
}

// ✅ Email à l'utilisateur — nouveau mot de passe
public void envoyerNouveauMotDePasse(
  String email, String username, String nouveauMotDePasse) {
try {
  MimeMessage message = mailSender.createMimeMessage();
  MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

  helper.setTo(email);
  helper.setSubject("Votre nouveau mot de passe — Plateforme Contentieux");

  String html = """
      <div style="font-family:Arial,sans-serif;max-width:600px;margin:auto;
                  border:1px solid #e0e0e0;border-radius:8px;overflow:hidden;">
        <div style="background:#1a237e;padding:24px;text-align:center;">
          <h2 style="color:#fff;margin:0;">Plateforme Contentieux</h2>
          <p style="color:#90caf9;margin:8px 0 0;">
            Réinitialisation de mot de passe
          </p>
        </div>
        <div style="padding:24px;">
          <p>Bonjour,</p>
          <p>Voici vos nouveaux identifiants de connexion :</p>
          <div style="background:#f5f5f5;border-left:4px solid #1a237e;
                      padding:16px;border-radius:4px;margin:16px 0;">
            <p style="margin:0 0 8px;">
              <strong>Nom d'utilisateur :</strong>
              <span style="font-family:monospace;">%s</span>
            </p>
            <p style="margin:0;">
              <strong>Mot de passe temporaire :</strong>
              <span style="font-family:monospace;">%s</span>
            </p>
          </div>
          <div style="background:#e8f5e9;border:1px solid #a5d6a7;
                      padding:12px;border-radius:4px;">
            <p style="margin:0;color:#2e7d32;font-size:13px;">
              🔐 Vous serez invité à changer ce mot de passe 
              lors de votre prochaine connexion.
            </p>
          </div>
        </div>
      </div>
      """.formatted(username, nouveauMotDePasse);

  helper.setText(html, true);
  mailSender.send(message);
  System.out.println("✅ Nouveau mdp envoyé à: " + email);

} catch (Exception e) {
  throw new RuntimeException("Erreur envoi nouveau mdp: " + e.getMessage());
}
}

}