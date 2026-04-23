import { inject, isDevMode } from '@angular/core';
import { HttpInterceptorFn } from '@angular/common/http';
import { KeycloakService } from 'keycloak-angular';
import { of } from 'rxjs';
import { catchError, switchMap } from 'rxjs/operators';
import { environment } from '../../../environments/environment';
import { from, throwError } from 'rxjs';
/**
 * Intercepteur HTTP — ajoute Authorization: Bearer <token> à chaque requête
 * vers le backend Spring Boot.
 *
 * CORRECTIF 401 :
 * - On ne se fie plus à keycloak.isLoggedIn() qui peut retourner false
 *   pendant l'init SSO silencieux.
 * - On lit directement kc.authenticated sur l'instance Keycloak JS.
 * - On force getToken() pour obtenir un token valide (avec refresh auto).
 */
export const authTokenInterceptor: HttpInterceptorFn = (req, next) => {

  if (req.url.startsWith('/assets')) {
    return next(req);
  }

  const keycloak = inject(KeycloakService);

  return from(keycloak.getToken()).pipe(

    switchMap(token => {
      // ── LOGS DE DEBUG ──────────────────────────────
      console.log('🔑 Token:', token ? token.substring(0, 50) + '...' : 'VIDE');
      console.log('🔑 isLoggedIn:', keycloak.isLoggedIn());
      // ───────────────────────────────────────────────

      if (!token) {
        if (isDevMode()) console.warn('[AuthInterceptor] Pas de token — login requis');
        keycloak.login();
        return throwError(() => new Error('No token'));
      }

      if (isDevMode()) console.debug('[AuthInterceptor] ✅ Bearer attaché →', req.url);

      return next(req.clone({
        setHeaders: { Authorization: `Bearer ${token}` }
      }));
    }),

    catchError(err => {
      if (isDevMode()) console.error('[AuthInterceptor] Erreur:', err);
      // ❌ NE PAS appeler keycloak.login() ici — cause une boucle infinie
      // Laisser passer l'erreur 401 au component
      return throwError(() => err);
    })
  );
};