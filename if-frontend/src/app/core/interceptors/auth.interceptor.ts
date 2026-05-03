import { HttpInterceptorFn, HttpErrorResponse } from '@angular/common/http';
import { inject } from '@angular/core';
import { catchError, throwError } from 'rxjs';
import { AuthService } from '../services/auth.service';

/**
 * SDD: Sentinel Security Interceptor (Functional)
 * Hecho: Adjunta JWT y gestiona fallas de autorización 401/403.
 * Rigor: Sincronizado con el estado reactivo de AuthService (Signals).
 */
export const authInterceptor: HttpInterceptorFn = (req, next) => {
  const authService = inject(AuthService);
  const token = authService.currentUserValue?.token;

  // 1. Inyección de Identidad: Clonación inmutable para adjuntar el Bearer Token
  const authReq = token
    ? req.clone({ setHeaders: { Authorization: `Bearer ${token}` } })
    : req;

  // 2. Vigilancia de Errores (Sentinel Guard): Protocolo de purga automática
  return next(authReq).pipe(
    catchError((error: HttpErrorResponse) => {
      // 401: No autorizado (Token expirado/Inválido) -> Logout
      // 403: Prohibido -> Mantener sesión, solo alertar
      if (error.status === 401) {
        console.error('SENTINEL: Sesión expirada. Ejecutando salida forzada...');
        authService.logout();
      } else if (error.status === 403) {
        console.warn('SENTINEL: Acceso denegado a recurso restringido (403).');
      }
      return throwError(() => error);
    })
  );
};