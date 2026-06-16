import { HttpInterceptorFn, HttpErrorResponse } from '@angular/common/http';
import { inject } from '@angular/core';
import { catchError, throwError } from 'rxjs';
import { Router } from '@angular/router';
import { AuthService } from '../services/auth.service';

export const authInterceptor: HttpInterceptorFn = (req, next) => {
  const authService = inject(AuthService);
  const router     = inject(Router);
  const token      = authService.currentUserValue?.token ?? null;

  const authReq = token
    ? req.clone({ setHeaders: { Authorization: `Bearer ${token}` } })
    : req;

  return next(authReq).pipe(
    catchError((error: HttpErrorResponse) => {
      if (error.status === 401) {
        // Token inexistente o expirado: redirigir a login conservando returnUrl.
        // NO se llama logout() para no borrar el nombre/familia del localStorage
        // (permite mostrar UX de "usuario recurrente" en la pantalla de login).
        const returnUrl = router.url;
        console.warn(`SENTINEL: 401 en ${req.url} → redirigiendo a login con returnUrl=${returnUrl}`);
        router.navigate(['/auth/login'], {
          queryParams: { returnUrl },
          replaceUrl: true
        });
      } else if (error.status === 403) {
        console.warn('SENTINEL: Acceso denegado (403).');
      }
      return throwError(() => error);
    })
  );
};
