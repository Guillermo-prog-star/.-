import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting, HttpTestingController } from '@angular/common/http/testing';
import { provideRouter } from '@angular/router';
import { Router } from '@angular/router';

import { AuthService, AuthUser } from './auth.service';
import { FamilyStateService } from './family-state.service';

// ─── Fixtures ──────────────────────────────────────────────────────────────

const USER_FIXTURE: AuthUser = {
  token: 'jwt-abc123',
  fullName: 'William Lopez',
  email: 'william@test.com',
  role: 'USER',
  familyId: 42,
  familyName: 'Familia Lopez'
};

const LOGIN_RESPONSE = {
  token: USER_FIXTURE.token,
  user: {
    fullName: USER_FIXTURE.fullName,
    email:    USER_FIXTURE.email,
    role:     'ROLE_USER',
    familyId: USER_FIXTURE.familyId,
    familyName: USER_FIXTURE.familyName
  }
};

// ───────────────────────────────────────────────────────────────────────────

describe('AuthService', () => {
  let service: AuthService;
  let http: HttpTestingController;
  let router: Router;
  let familyState: jasmine.SpyObj<FamilyStateService>;

  beforeEach(() => {
    // localStorage se limpia ANTES de que TestBed cree el servicio para que
    // loadUserFromStorage() comience siempre desde cero.
    localStorage.clear();

    familyState = jasmine.createSpyObj<FamilyStateService>(
      'FamilyStateService', ['setFamily', 'clearFamily']
    );

    TestBed.configureTestingModule({
      providers: [
        AuthService,
        provideHttpClient(),
        provideHttpClientTesting(),
        provideRouter([]),
        { provide: FamilyStateService, useValue: familyState }
      ]
    });

    service    = TestBed.inject(AuthService);
    http       = TestBed.inject(HttpTestingController);
    router     = TestBed.inject(Router);

    spyOn(router, 'navigate').and.returnValue(Promise.resolve(true));
  });

  afterEach(() => {
    http.verify();
    localStorage.clear();
  });

  // ═══════════════════════════════════════════════════════════════════════
  //  1. Estado inicial
  // ═══════════════════════════════════════════════════════════════════════

  describe('estado inicial (localStorage vacío)', () => {
    it('no debe estar autenticado', () => {
      expect(service.isAuthenticated()).toBeFalse();
    });

    it('getToken() debe devolver null', () => {
      expect(service.getToken()).toBeNull();
    });

    it('fullName debe devolver "Invitado"', () => {
      expect(service.fullName).toBe('Invitado');
    });

    it('currentUserValue debe ser null', () => {
      expect(service.currentUserValue).toBeNull();
    });

    it('user signal debe ser null', () => {
      expect(service.user()).toBeNull();
    });
  });

  describe('estado inicial con usuario en localStorage', () => {
    beforeEach(() => {
      // Simula sesión previa guardada en disco
      localStorage.setItem('auth_user', JSON.stringify(USER_FIXTURE));

      // Recrear el servicio para que lea el localStorage cargado
      TestBed.resetTestingModule();
      TestBed.configureTestingModule({
        providers: [
          AuthService,
          provideHttpClient(),
          provideHttpClientTesting(),
          provideRouter([]),
          { provide: FamilyStateService, useValue: familyState }
        ]
      });
      service = TestBed.inject(AuthService);
      http    = TestBed.inject(HttpTestingController);
    });

    it('debe estar autenticado', () => {
      expect(service.isAuthenticated()).toBeTrue();
    });

    it('getToken() debe devolver el token guardado', () => {
      expect(service.getToken()).toBe('jwt-abc123');
    });

    it('fullName debe devolver el nombre del usuario', () => {
      expect(service.fullName).toBe('William Lopez');
    });

    it('currentUserValue debe tener todos los campos del fixture', () => {
      const u = service.currentUserValue!;
      expect(u.email).toBe('william@test.com');
      expect(u.role).toBe('USER');
      expect(u.familyId).toBe(42);
    });
  });

  describe('carga desde localStorage corrupto', () => {
    it('debe devolver null si el JSON es inválido (sin lanzar excepción)', () => {
      localStorage.setItem('auth_user', '{ NOT VALID JSON ');

      TestBed.resetTestingModule();
      TestBed.configureTestingModule({
        providers: [
          AuthService,
          provideHttpClient(),
          provideHttpClientTesting(),
          provideRouter([]),
          { provide: FamilyStateService, useValue: familyState }
        ]
      });
      service = TestBed.inject(AuthService);
      http    = TestBed.inject(HttpTestingController);

      expect(service.isAuthenticated()).toBeFalse();
      expect(service.currentUserValue).toBeNull();
    });
  });

  // ═══════════════════════════════════════════════════════════════════════
  //  2. login()
  // ═══════════════════════════════════════════════════════════════════════

  describe('login()', () => {
    it('debe hacer POST a /api/auth/login', () => {
      service.login({ email: 'william@test.com', password: 'pass' }).subscribe();

      const req = http.expectOne('/api/auth/login');
      expect(req.request.method).toBe('POST');
      req.flush(LOGIN_RESPONSE);
    });

    it('debe actualizar el signal user tras login exitoso', () => {
      service.login({ email: 'william@test.com', password: 'pass' }).subscribe();

      http.expectOne('/api/auth/login').flush(LOGIN_RESPONSE);

      expect(service.isAuthenticated()).toBeTrue();
      expect(service.getToken()).toBe('jwt-abc123');
      expect(service.user()?.email).toBe('william@test.com');
    });

    it('debe guardar el usuario en localStorage tras login exitoso', () => {
      service.login({ email: 'william@test.com', password: 'pass' }).subscribe();

      http.expectOne('/api/auth/login').flush(LOGIN_RESPONSE);

      const stored = JSON.parse(localStorage.getItem('auth_user')!);
      expect(stored.token).toBe('jwt-abc123');
      expect(stored.email).toBe('william@test.com');
    });

    it('debe llamar a familyState.setFamily() cuando la respuesta incluye familyId', () => {
      service.login({ email: 'william@test.com', password: 'pass' }).subscribe();

      http.expectOne('/api/auth/login').flush(LOGIN_RESPONSE);

      expect(familyState.setFamily).toHaveBeenCalledWith({ id: 42, name: 'Familia Lopez' });
    });

    it('NO debe llamar a familyState.setFamily() cuando no hay familyId', () => {
      const responseWithoutFamily = { token: 'jwt-abc123', user: { fullName: 'Test', email: 'a@b.com', role: 'ROLE_USER' } };
      service.login({ email: 'a@b.com', password: 'pass' }).subscribe();

      http.expectOne('/api/auth/login').flush(responseWithoutFamily);

      expect(familyState.setFamily).not.toHaveBeenCalled();
    });

    it('debe mapear ROLE_ADMIN → "ADMIN"', () => {
      const adminResponse = { token: 'jwt', user: { fullName: 'Admin', email: 'a@b.com', role: 'ROLE_ADMIN' } };
      service.login({ email: 'a@b.com', password: 'pass' }).subscribe();

      http.expectOne('/api/auth/login').flush(adminResponse);

      expect(service.currentUserValue?.role).toBe('ADMIN');
    });

    it('debe mapear ROLE_SENTINEL → "ADMIN"', () => {
      const sentinelResponse = { token: 'jwt', user: { fullName: 'Sentinel', email: 'a@b.com', role: 'ROLE_SENTINEL' } };
      service.login({ email: 'a@b.com', password: 'pass' }).subscribe();

      http.expectOne('/api/auth/login').flush(sentinelResponse);

      expect(service.currentUserValue?.role).toBe('ADMIN');
    });

    it('debe mapear ROLE_USER → "USER"', () => {
      service.login({ email: 'william@test.com', password: 'pass' }).subscribe();

      http.expectOne('/api/auth/login').flush(LOGIN_RESPONSE); // ROLE_USER

      expect(service.currentUserValue?.role).toBe('USER');
    });

    it('NO debe modificar el estado si la respuesta no tiene token', () => {
      service.login({ email: 'a@b.com', password: 'pass' }).subscribe();

      http.expectOne('/api/auth/login').flush({ message: 'Invalid credentials' });

      expect(service.isAuthenticated()).toBeFalse();
    });
  });

  // ═══════════════════════════════════════════════════════════════════════
  //  3. logout()
  // ═══════════════════════════════════════════════════════════════════════

  describe('logout()', () => {
    beforeEach(() => {
      // Precondición: usuario autenticado
      localStorage.setItem('auth_user', JSON.stringify(USER_FIXTURE));
      TestBed.resetTestingModule();
      TestBed.configureTestingModule({
        providers: [
          AuthService,
          provideHttpClient(),
          provideHttpClientTesting(),
          provideRouter([]),
          { provide: FamilyStateService, useValue: familyState }
        ]
      });
      service = TestBed.inject(AuthService);
      http    = TestBed.inject(HttpTestingController);
      router  = TestBed.inject(Router);
      spyOn(router, 'navigate').and.returnValue(Promise.resolve(true));
    });

    it('debe eliminar auth_user de localStorage', () => {
      service.logout();
      expect(localStorage.getItem('auth_user')).toBeNull();
    });

    it('debe poner user signal en null', () => {
      service.logout();
      expect(service.user()).toBeNull();
      expect(service.isAuthenticated()).toBeFalse();
    });

    it('debe llamar a familyState.clearFamily()', () => {
      service.logout();
      expect(familyState.clearFamily).toHaveBeenCalledTimes(1);
    });

    it('debe navegar a /auth/login', () => {
      service.logout();
      expect(router.navigate).toHaveBeenCalledWith(['/auth/login']);
    });
  });

  // ═══════════════════════════════════════════════════════════════════════
  //  4. register()
  // ═══════════════════════════════════════════════════════════════════════

  describe('register()', () => {
    it('debe hacer POST a /api/auth/register', () => {
      service.register({ fullName: 'Test', email: 'a@b.com', password: '123' }).subscribe();

      const req = http.expectOne('/api/auth/register');
      expect(req.request.method).toBe('POST');
      req.flush(LOGIN_RESPONSE);
    });

    it('debe autenticar al usuario tras registro exitoso', () => {
      service.register({ fullName: 'Test', email: 'william@test.com', password: '123' }).subscribe();

      http.expectOne('/api/auth/register').flush(LOGIN_RESPONSE);

      expect(service.isAuthenticated()).toBeTrue();
      expect(service.user()?.email).toBe('william@test.com');
    });
  });
});
