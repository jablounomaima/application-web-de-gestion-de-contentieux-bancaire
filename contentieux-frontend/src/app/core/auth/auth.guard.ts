import { inject } from '@angular/core';
import { ActivatedRouteSnapshot, CanActivateFn, Router, RouterStateSnapshot } from '@angular/router';
import { KeycloakService } from 'keycloak-angular';

export const AuthGuard: CanActivateFn = async (
  route: ActivatedRouteSnapshot,
  state: RouterStateSnapshot
) => {
  const keycloak = inject(KeycloakService);
  const router = inject(Router);

  const authenticated = await Promise.resolve(keycloak.isLoggedIn() as unknown as boolean | Promise<boolean>);

  if (!authenticated) {
    await keycloak.login({
      redirectUri: window.location.origin + state.url,
    });
    return false;
  }

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