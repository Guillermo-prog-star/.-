import { TestBed } from '@angular/core/testing';
import { Router, provideRouter, ActivatedRouteSnapshot, RouterStateSnapshot } from '@angular/router';

import { adminGuard } from './admin.guard';
import { AuthService, AuthUser } from '../services/auth.service';
import { FamilyStateService } from '../services/family-state.service';

// ─── Fixtures ──────────────────────────────────────────────────────────────

const ADMIN_USER: AuthUser = {
  token: 'jwt-admin', fullName: 'Admin',
  email: 'admin@test.com', role: 'ADMIN'
};

const REGULAR_USER: AuthUser = {
  token: 'jwt-user', fullName: 'Usuario',
  email: 'user@test.com', role: 'USER'
};

// Email hardcodeado en el guard como bypass especial
const WILLIAM_EMAIL = 'william_lopezb@soy.sena.edu.co';

// ─── Helper ────────────────────────────────────────────────────────────────

function runGuard() {
  return TestBed.runInInjectionContext(() =>
    adminGuard({} as ActivatedRouteSnapshot, {} as RouterStateSnapshot)
  );
}

// ───────────────────────────────────────────────────────────────────────────

describe('adminGuard', () => {
  let authService: jasmine.SpyObj<AuthService>;
  let router: Router;

  function setupWith(user: AuthUser | null, isAuth: boolean) {
    authService.isAuthenticated.and.returnValue(isAuth);
    (Object.getOwnPropertyDescriptor(authService, 'currentUserValue')?.get as jasmine.Spy)
      ?.and?.returnValue(user);
    // Si no hay descriptor getter, usamos defineProperty para simular la propiedad
    Object.defineProperty(authService, 'currentUserValue', {
      get: () => user,
      configurable: true
    });
  }

  beforeEach(() => {
    localStorage.clear();

    authService = jasmine.createSpyObj<AuthService>(
      'AuthService',
      ['isAuthenticated', 'getToken', 'logout']
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

  it('debe devolver true para usuario con role ADMIN autenticado', () => {
    setupWith(ADMIN_USER, true);
    expect(runGuard()).toBeTrue();
  });

  it('NO debe navegar cuando el acceso es concedido', () => {
    setupWith(ADMIN_USER, true);
    runGuard();
    expect(router.navigate).not.toHaveBeenCalled();
  });

  it('debe devolver true para el email de William (bypass especial)', () => {
    const williamUser: AuthUser = { ...REGULAR_USER, email: WILLIAM_EMAIL, role: 'USER' };
    setupWith(williamUser, true);
    expect(runGuard()).toBeTrue();
  });

  // ═══════════════════════════════════════════════════════════════════════
  //  Acceso denegado
  // ═══════════════════════════════════════════════════════════════════════

  it('debe devolver false para usuario con role USER', () => {
    setupWith(REGULAR_USER, true);
    expect(runGuard()).toBeFalse();
  });

  it('debe navegar a /dashboard cuando el acceso es denegado', () => {
    setupWith(REGULAR_USER, true);
    runGuard();
    expect(router.navigate).toHaveBeenCalledWith(['/dashboard']);
  });

  it('debe devolver false cuando no está autenticado aunque tenga role ADMIN', () => {
    setupWith(ADMIN_USER, false);
    expect(runGuard()).toBeFalse();
  });

  it('debe devolver false cuando user es null', () => {
    setupWith(null, false);
    expect(runGuard()).toBeFalse();
  });

  it('debe devolver false y navegar a /dashboard cuando no hay sesión', () => {
    setupWith(null, false);
    runGuard();
    expect(router.navigate).toHaveBeenCalledWith(['/dashboard']);
  });

  it('debe devolver false para email de admin incorrecto', () => {
    const fakeAdmin: AuthUser = { ...REGULAR_USER, email: 'fake_william@soy.sena.edu.co', role: 'USER' };
    setupWith(fakeAdmin, true);
    expect(runGuard()).toBeFalse();
  });
});
