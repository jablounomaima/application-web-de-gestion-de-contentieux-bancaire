import { inject } from '@angular/core';
import { ActivatedRouteSnapshot, CanActivateFn, Router, RouterStateSnapshot } from '@angular/router';
import { KeycloakService } from 'keycloak-angular';

export const AuthGuard: CanActivateFn = async (
  route: ActivatedRouteSnapshot,
  state: RouterStateSnapshot
) => {
  const keycloak = inject(KeycloakService);
  const router = inject(Router);

  // ── Étape 1 : Vérifier si connecté ──────────────────────────
  const authenticated = await Promise.resolve(
    keycloak.isLoggedIn() as unknown as boolean | Promise<boolean>
  );

  if (!authenticated) {
    await keycloak.login({
      redirectUri: window.location.origin + state.url,
    });
    return false;
  }

  // ── Étape 2 : ✅ Détecter UPDATE_PASSWORD ───────────────────
  const keycloakInstance = keycloak.getKeycloakInstance();
  const requiredActions: string[] =
    keycloakInstance?.tokenParsed?.['required_actions'] ?? [];

  if (requiredActions.includes('UPDATE_PASSWORD')) {
    console.log('🔐 UPDATE_PASSWORD requis → redirection Keycloak');
    await keycloakInstance.login({
      action: 'UPDATE_PASSWORD',
      redirectUri: window.location.origin + '/login'
    });
    return false;
  }

  // ── Étape 3 : Vérifier les rôles ────────────────────────────
  const requiredRoles = route.data['roles'] as string[];

  if (!requiredRoles || requiredRoles.length === 0) {
    return true;
  }

  const userRoles = keycloak.getUserRoles();
  const normalize = (r: string) => r.replace(/^ROLE_/, '').toUpperCase();

  const hasRole = requiredRoles.some(required =>
    userRoles.some(userRole => normalize(userRole) === normalize(required))
  );

  if (!hasRole) {
    router.navigate(['/access-denied']);
    return false;
  }

  return true;
};