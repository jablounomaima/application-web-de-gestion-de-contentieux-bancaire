import { KeycloakService } from 'keycloak-angular';
import { environment } from '../../../environments/environment';

/**
 * Fonction d'initialisation de Keycloak.
 *
 * Appelée par APP_INITIALIZER dans app.config.ts AVANT le démarrage
 * de l'application Angular. Cela garantit que l'authentification SSO
 * est établie avant l'affichage de la première page.
 *
 * @param keycloak - Instance de KeycloakService injectée par Angular
 * @returns Une fonction asynchrone retournant une Promise<boolean>
 */
export function initializeKeycloak(keycloak: KeycloakService) {
  return () =>
    keycloak.init({

      /**
       * Configuration de connexion au serveur Keycloak.
       * Les valeurs proviennent de environment.ts pour gérer
       * les différences dev/prod facilement.
       */
      config: {
        url: environment.keycloak.url,         // Ex: http://127.0.0.1:8080
        realm: environment.keycloak.realm,     // Ex: contentieux-realm
        clientId: environment.keycloak.clientId // Ex: contentieux-client2
      },

      initOptions: {
        /**
         * check-sso : vérifie silencieusement si une session SSO existe déjà
         * sans rediriger l'utilisateur vers Keycloak.
         *
         * Avantage : l'utilisateur déjà connecté est reconnu automatiquement.
         * Si non connecté → l'application continue et redirige vers /login.
         *
         * Alternative : 'login-required' forcerait une redirection immédiate
         * vers Keycloak pour toute URL (même publique).
         */
        onLoad: 'check-sso',// ← changez check-sso en login-required

        /**
         * Désactive l'iframe de vérification de session.
         * Recommandé pour éviter les problèmes CORS et de cookies
         * en développement local avec localhost/127.0.0.1.
         */
        checkLoginIframe: false,

        /**
         * URL du fichier HTML silencieux utilisé pour le check-sso.
         * Ce fichier doit exister dans src/assets/silent-check-sso.html.
         *
         * Contenu minimal du fichier :
         * <html><body><script>
         *   parent.postMessage(location.href, location.origin);
         * </script></body></html>
         *
         * Il permet à Keycloak de vérifier la session via une iframe
         * sans exposer le token dans l'URL principale.
         */
         silentCheckSsoRedirectUri:
          window.location.origin + '/assets/silent-check-sso.html',
      },

      /**
       * Active l'intercepteur Bearer intégré de keycloak-angular.
       * Avec notre authTokenInterceptor custom, les deux fonctionnent
       * en parallèle — notre intercepteur prend priorité car déclaré
       * en premier dans app.config.ts.
       */
      enableBearerInterceptor: true,
      bearerPrefix: 'Bearer',

      /**
       * URLs exclues de l'ajout automatique du token Bearer.
       * Les assets statiques n'ont pas besoin d'authentification.
       */
      bearerExcludedUrls: [
        '/assets',
        '/api/public',                              // ✅ exclure routes publiques
        'http://localhost:8098/api/public'          // ✅ URL complète aussi
      ],
    })
    .then(authenticated => {
      console.log('✅ Keycloak init — authenticated:', authenticated);
    })
    .catch(err => {
      console.error('❌ Keycloak init error:', err);
    });
}