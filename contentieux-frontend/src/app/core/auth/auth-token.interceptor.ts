import { inject, isDevMode } from '@angular/core';
import { HttpInterceptorFn } from '@angular/common/http';
import { KeycloakService } from 'keycloak-angular';
import { from, throwError } from 'rxjs';
import { catchError, switchMap } from 'rxjs/operators';

export const authTokenInterceptor: HttpInterceptorFn = (req, next) => {

  // ✅ Ne pas intercepter les assets
  if (req.url.startsWith('/assets')) return next(req);

  const keycloak = inject(KeycloakService);

  return from(
    // ✅ CORRECTIF PRINCIPAL — updateToken(60) avant chaque requête
    // Force le refresh si le token expire dans moins de 60 secondes
    // .catch(() => false) → si refresh échoue, on continue avec token existant
    keycloak.updateToken(60).catch(() => false)

  ).pipe(

    // ✅ Après le refresh → récupérer le token frais
    switchMap(() => from(keycloak.getToken())),

    switchMap(token => {
      if (!token) {
        if (isDevMode()) console.warn('[AuthInterceptor] Pas de token → login');
        keycloak.login();
        return throwError(() => new Error('No token'));
      }

      // ✅ Log debug — rôles + expiration
      if (isDevMode()) {
        try {
          const p = JSON.parse(atob(token.split('.')[1]));
          const expireIn = Math.round(p.exp - Date.now() / 1000);
          console.debug(
            `[Token] user=${p.preferred_username}`,
            `| roles=${p.realm_access?.roles}`,
            `| expire dans ${expireIn}s`
          );
        } catch {}
      }

      // ✅ Attacher le token à la requête
      return next(req.clone({
        setHeaders: { Authorization: `Bearer ${token}` }
      }));
    }),

    catchError(err => {
      if (isDevMode()) console.error('[AuthInterceptor] Erreur:', err);

      // ✅ 401 → token vraiment invalide → forcer reconnexion
      if (err?.status === 401) {
        console.warn('[AuthInterceptor] 401 détecté → reconnexion');
        keycloak.login();
      }

      return throwError(() => err);
    })
  );
};