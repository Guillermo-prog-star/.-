import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { AuthService } from '../services/auth.service';

/**
 * SDD: Admin Sentinel Guard
 * Hecho: Blindaje del Nodo Central mediante validación de rango e identidad.
 */
export const adminGuard: CanActivateFn = (route, state) => {
  const authService = inject(AuthService);
  const router = inject(Router);

  // 1. Obtener estado actual (Sincronizado con Signals/LocalStorage)
  const user = authService.currentUserValue;

  // 2. Definir privilegios (Criterio de éxito: William o Rango ADMIN)
  const isAuthorized = user?.role === 'ADMIN' || user?.email === 'william_lopezb@soy.sena.edu.co';

  if (authService.isAuthenticated() && isAuthorized) {
    console.log(`SENTINEL: Acceso validado para nodo administrativo: ${user?.email}`);
    return true;
  }

  // 3. Protocolo de Denegación (Prevención de fuga de información)
  console.error('SENTINEL: Intento de violación de acceso detectado. Redirigiendo...');
  router.navigate(['/dashboard']);
  return false;
};