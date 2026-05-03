import { inject } from '@angular/core';
import { HttpInterceptorFn, HttpErrorResponse } from '@angular/common/http';
import { Router } from '@angular/router';
import { catchError, throwError } from 'rxjs';

export const errorInterceptor: HttpInterceptorFn = (req, next) => {
  const router = inject(Router);

  return next(req).pipe(
    catchError((err: HttpErrorResponse) => {
      if ([401, 403].includes(err.status)) {
        // Si no está autorizado, lo mandamos al login
        router.navigate(['/login']);
      }
      return throwError(() => err);
    })
  );
};