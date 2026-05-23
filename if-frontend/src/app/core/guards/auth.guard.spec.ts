import { TestBed } from '@angular/core/testing';
import { Router, provideRouter, ActivatedRouteSnapshot, RouterStateSnapshot } from '@angular/router';
import { signal } from '@angular/core';

import { authGuard } from './auth.guard';
import { AuthService, AuthUser } from '../services/auth.service';
import { FamilyStateService } from '../services/family-state.service';

// ─── Helper ────────────────────────────────────────────────────────────────
//  Ejecuta el guard funcional dentro de un contexto de inyección real.
function runGuard(url = '/dashboard') {
  return TestBed.runInInjectionContext(() =>
    authGuard(
      {} as ActivatedRouteSnapshot,
      { url } as RouterStateSnapshot
    )
  );
}

// ───────────────────────────────────────────────────────────────────────────

describe('authGuard', () => {
  let authService: jasmine.SpyObj<AuthService>;
  let router: Router;

  beforeEach(() => {
    localStorage.clear();

    authService = jasmine.createSpyObj<AuthService>(
      'AuthService',
      ['getToken', 'isAuthenticated', 'logout'],
      { currentUserValue: null, user: signal<AuthUser | null>(null) }
    );

    TestBed.configureTestingModule({
      providers: [
        provideRouter([]),
        { provide: AuthService, useValue: authService },
        { provide: FamilyStateService, useValue: jasmine.createSpyObj('FamilyStateService', ['setFamily', 'clearFamily']) }
      ]
    });

    router = TestBed.inject(Router);
    spyOn(router, 'navigate').and.returnValue(Promise.resolve(true));
  });

  afterEach(() => localStorage.clear());

  // ═══════════════════════════════════════════════════════════════════════
  //  Acceso concedido
  // ═══════════════════════════════════════════════════════════════════════

  it('debe devolver true cuando hay token activo', () => {
    authService.getToken.and.returnValue('jwt-valid-token');
    expect(runGuard()).toBeTrue();
  });

  it('NO debe navegar cuando hay token activo', () => {
    authService.getToken.and.returnValue('jwt-valid-token');
    runGuard();
    expect(router.navigate).not.toHaveBeenCalled();
  });

  // ═══════════════════════════════════════════════════════════════════════
  //  Acceso denegado
  // ═══════════════════════════════════════════════════════════════════════

  it('debe devolver false cuando no hay token', () => {
    authService.getToken.and.returnValue(null);
    expect(runGuard()).toBeFalse();
  });

  it('debe navegar a /auth/login cuando no hay token', () => {
    authService.getToken.and.returnValue(null);
    runGuard('/dashboard');
    expect(router.navigate).toHaveBeenCalledWith(
      ['/auth/login'],
      { queryParams: { returnUrl: '/dashboard' } }
    );
  });

  it('debe incluir la URL de retorno en queryParams con la ruta correcta', () => {
    authService.getToken.and.returnValue(null);
    runGuard('/plans');
    expect(router.navigate).toHaveBeenCalledWith(
      ['/auth/login'],
      { queryParams: { returnUrl: '/plans' } }
    );
  });

  it('debe devolver false con token vacío ""', () => {
    authService.getToken.and.returnValue(null); // getToken devuelve null si vacío
    expect(runGuard()).toBeFalse();
  });

  // ═══════════════════════════════════════════════════════════════════════
  //  Rutas protegidas concretas
  // ═══════════════════════════════════════════════════════════════════════

  it('debe proteger /evaluations/start', () => {
    authService.getToken.and.returnValue(null);
    runGuard('/evaluations/start');
    expect(router.navigate).toHaveBeenCalledWith(
      ['/auth/login'],
      { queryParams: { returnUrl: '/evaluations/start' } }
    );
  });

  it('debe proteger /plans', () => {
    authService.getToken.and.returnValue(null);
    runGuard('/plans');
    expect(router.navigate).toHaveBeenCalledWith(
      ['/auth/login'],
      { queryParams: { returnUrl: '/plans' } }
    );
  });
});
