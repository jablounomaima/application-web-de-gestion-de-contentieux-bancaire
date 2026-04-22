import { inject, isDevMode } from '@angular/core';
import { HttpInterceptorFn } from '@angular/common/http';
import { KeycloakService } from 'keycloak-angular';
import { from } from 'rxjs';
import { catchError, switchMap } from 'rxjs/operators';
import { environment } from '../../../environments/environment';

export const authTokenInterceptor: HttpInterceptorFn = (req, next) => {
  // Avoid adding auth headers on static assets.
  if (req.url.startsWith('/assets')) {
    return next(req);
  }

  const keycloak = inject(KeycloakService);

  return from(Promise.resolve(keycloak.isLoggedIn() as unknown as boolean | Promise<boolean>)).pipe(
    switchMap((isLoggedIn) => {
      if (!isLoggedIn) {
        if (isDevMode() && req.url.startsWith(environment.apiUrl)) {
          console.warn('[AuthInterceptor] Not logged in, no token for', req.url);
        }
        return next(req);
      }

      const kcInstance = (keycloak as any).getKeycloakInstance?.();
      const currentToken: string | undefined = kcInstance?.token;

      return from(
        Promise.resolve(currentToken || keycloak.getToken().catch(() => undefined))
      ).pipe(
        switchMap((token) => {
          if (!token) {
            if (isDevMode() && req.url.startsWith(environment.apiUrl)) {
              console.warn('[AuthInterceptor] Missing token for', req.url);
            }
            return next(req);
          }

          const authReq = req.clone({
            setHeaders: {
              Authorization: `Bearer ${token}`
            }
          });

          if (isDevMode() && req.url.startsWith(environment.apiUrl)) {
            console.debug('[AuthInterceptor] Bearer attached:', req.url);
          }
          return next(authReq);
        }),
        catchError(() => next(req))
      );
    }),
    catchError(() => next(req))
  );
};
