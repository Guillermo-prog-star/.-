import { inject } from '@angular/core';
import { Router, CanActivateFn } from '@angular/router';
import { AuthService } from '../services/auth.service';

/**
 * authGuard: El vigilante de seguridad del Nodo Armenia.
 * Utiliza el nuevo estándar de verificación de tokens para proteger las rutas.
 */
export const authGuard: CanActivateFn = (route, state) => {
  const router = inject(Router);
  const authService = inject(AuthService);

  // Sincronizado con el nuevo método getToken() del AuthService optimizado
  if (authService.getToken()) {
    return true;
  }

  // Redirección lógica: si no hay sesión real, vuelve al login
  router.navigate(['/auth/login'], { queryParams: { returnUrl: state.url } });
  return false;
};