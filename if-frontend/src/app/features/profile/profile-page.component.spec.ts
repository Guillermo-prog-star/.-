import { TestBed, fakeAsync, tick } from '@angular/core/testing';
import {
  provideHttpClient,
  HttpClient
} from '@angular/common/http';
import {
  provideHttpClientTesting,
  HttpTestingController
} from '@angular/common/http/testing';
import { signal } from '@angular/core';
import { NO_ERRORS_SCHEMA } from '@angular/core';

import { ProfilePageComponent } from './profile-page.component';
import { AuthService, AuthUser } from '../../core/services/auth.service';

// ─── Fixtures ───────────────────────────────────────────────────────────────

const PROFILE_FIXTURE = {
  id: 1,
  email: 'william@if.com',
  fullName: 'William López Rivera',
  role: 'ROLE_USER',
  familyId: 42,
  familyName: 'Familia López'
};

const AUTH_USER: AuthUser = {
  token: 'jwt-abc',
  fullName: 'William López Rivera',
  email: 'william@if.com',
  role: 'USER',
  familyId: 42,
  familyName: 'Familia López'
};

// ─── Helpers ────────────────────────────────────────────────────────────────

function buildComponent(user: AuthUser | null = AUTH_USER, hasToken = true) {
  const authSpy = jasmine.createSpyObj<AuthService>(
    'AuthService',
    ['getToken', 'logout'],
    { user: signal(user) }
  );
  authSpy.getToken.and.returnValue(hasToken ? 'jwt-abc' : null);

  TestBed.configureTestingModule({
    imports: [ProfilePageComponent],
    providers: [
      provideHttpClient(),
      provideHttpClientTesting(),
      { provide: AuthService, useValue: authSpy }
    ],
    schemas: [NO_ERRORS_SCHEMA]
  });

  const fixture = TestBed.createComponent(ProfilePageComponent);
  const component = fixture.componentInstance;
  const httpMock = TestBed.inject(HttpTestingController);

  return { fixture, component, httpMock, authSpy };
}

// ───────────────────────────────────────────────────────────────────────────

describe('ProfilePageComponent', () => {
  afterEach(() => TestBed.resetTestingModule());

  // ═══════════════════════════════════════════════════════════════════════
  //  loadProfile() — ruta HTTP exitosa
  // ═══════════════════════════════════════════════════════════════════════

  describe('loadProfile() — respuesta HTTP exitosa', () => {
    it('debe setear profile con los datos del servidor y loading=false', fakeAsync(() => {
      const { fixture, component, httpMock } = buildComponent();
      fixture.detectChanges(); // dispara ngOnInit → HTTP request

      const req = httpMock.expectOne('/api/auth/me');
      req.flush(PROFILE_FIXTURE);
      tick();
      fixture.detectChanges();

      expect(component.profile()).toEqual(PROFILE_FIXTURE as any);
      expect(component.loading()).toBeFalse();
      expect(component.hasBackendError()).toBeFalse();
      httpMock.verify();
    }));

    it('debe arrancar con loading=true antes de recibir respuesta', () => {
      const { fixture, component, httpMock } = buildComponent();
      fixture.detectChanges();

      expect(component.loading()).toBeTrue();
      httpMock.expectOne('/api/auth/me').flush(PROFILE_FIXTURE);
      httpMock.verify();
    });
  });

  // ═══════════════════════════════════════════════════════════════════════
  //  loadProfile() — fallback cuando el backend falla
  // ═══════════════════════════════════════════════════════════════════════

  describe('loadProfile() — fallback a caché local', () => {
    it('debe usar los datos del AuthService cuando el backend devuelve error', fakeAsync(() => {
      const { fixture, component, httpMock } = buildComponent();
      fixture.detectChanges();

      const req = httpMock.expectOne('/api/auth/me');
      req.flush('Server Error', { status: 500, statusText: 'Internal Server Error' });
      tick();
      fixture.detectChanges();

      expect(component.hasBackendError()).toBeTrue();
      expect(component.loading()).toBeFalse();
      expect(component.profile()?.email).toBe('william@if.com');
      expect(component.profile()?.fullName).toBe('William López Rivera');
      httpMock.verify();
    }));

    it('no debe tener error de backend cuando HTTP responde con éxito', fakeAsync(() => {
      const { fixture, component, httpMock } = buildComponent();
      fixture.detectChanges();

      httpMock.expectOne('/api/auth/me').flush(PROFILE_FIXTURE);
      tick();

      expect(component.hasBackendError()).toBeFalse();
      httpMock.verify();
    }));
  });

  // ═══════════════════════════════════════════════════════════════════════
  //  Señales computadas
  // ═══════════════════════════════════════════════════════════════════════

  describe('initials()', () => {
    it('debe computar iniciales de dos palabras', fakeAsync(() => {
      const { fixture, component, httpMock } = buildComponent();
      fixture.detectChanges();
      httpMock.expectOne('/api/auth/me').flush(PROFILE_FIXTURE);
      tick();

      // "William López Rivera" → "WL"
      expect(component.initials()).toBe('WL');
      httpMock.verify();
    }));

    it('debe devolver "?" cuando no hay nombre', fakeAsync(() => {
      const { fixture, component, httpMock } = buildComponent(null);
      fixture.detectChanges();
      // Respuesta 500 para que profile quede null también
      httpMock.expectOne('/api/auth/me').flush('err', { status: 500, statusText: 'Error' });
      tick();

      expect(component.initials()).toBe('?');
      httpMock.verify();
    }));
  });

  describe('roleLabel()', () => {
    it('debe retornar "Administrador" para roles con ADMIN', fakeAsync(() => {
      const adminUser: AuthUser = { ...AUTH_USER, role: 'ADMIN' };
      const { fixture, component, httpMock } = buildComponent(adminUser);
      fixture.detectChanges();
      httpMock.expectOne('/api/auth/me').flush({
        ...PROFILE_FIXTURE, role: 'ROLE_ADMIN'
      });
      tick();

      expect(component.roleLabel()).toBe('Administrador');
      httpMock.verify();
    }));

    it('debe retornar "Consultor Familiar" para roles USER', fakeAsync(() => {
      const { fixture, component, httpMock } = buildComponent();
      fixture.detectChanges();
      httpMock.expectOne('/api/auth/me').flush(PROFILE_FIXTURE);
      tick();

      expect(component.roleLabel()).toBe('Consultor Familiar');
      httpMock.verify();
    }));
  });

  describe('roleColorClass()', () => {
    it('debe retornar "badge-admin" para rol ADMIN', fakeAsync(() => {
      const { fixture, component, httpMock } = buildComponent();
      fixture.detectChanges();
      httpMock.expectOne('/api/auth/me').flush({ ...PROFILE_FIXTURE, role: 'ROLE_ADMIN' });
      tick();

      expect(component.roleColorClass()).toBe('badge-admin');
      httpMock.verify();
    }));

    it('debe retornar "badge-user" para rol USER', fakeAsync(() => {
      const { fixture, component, httpMock } = buildComponent();
      fixture.detectChanges();
      httpMock.expectOne('/api/auth/me').flush(PROFILE_FIXTURE);
      tick();

      expect(component.roleColorClass()).toBe('badge-user');
      httpMock.verify();
    }));
  });

  describe('isTokenActive()', () => {
    it('debe ser true cuando el AuthService tiene token', fakeAsync(() => {
      const { fixture, component, httpMock } = buildComponent(AUTH_USER, true);
      fixture.detectChanges();
      httpMock.expectOne('/api/auth/me').flush(PROFILE_FIXTURE);
      tick();

      expect(component.isTokenActive()).toBeTrue();
      httpMock.verify();
    }));

    it('debe ser false cuando no hay token', fakeAsync(() => {
      const { fixture, component, httpMock } = buildComponent(AUTH_USER, false);
      fixture.detectChanges();
      httpMock.expectOne('/api/auth/me').flush(PROFILE_FIXTURE);
      tick();

      expect(component.isTokenActive()).toBeFalse();
      httpMock.verify();
    }));
  });

  // ═══════════════════════════════════════════════════════════════════════
  //  logout()
  // ═══════════════════════════════════════════════════════════════════════

  describe('logout()', () => {
    it('debe llamar auth.logout() cuando el usuario confirma', fakeAsync(() => {
      const { fixture, component, httpMock, authSpy } = buildComponent();
      fixture.detectChanges();
      httpMock.expectOne('/api/auth/me').flush(PROFILE_FIXTURE);
      tick();

      spyOn(window, 'confirm').and.returnValue(true);
      component.logout();

      expect(authSpy.logout).toHaveBeenCalled();
      httpMock.verify();
    }));

    it('NO debe llamar auth.logout() cuando el usuario cancela', fakeAsync(() => {
      const { fixture, component, httpMock, authSpy } = buildComponent();
      fixture.detectChanges();
      httpMock.expectOne('/api/auth/me').flush(PROFILE_FIXTURE);
      tick();

      spyOn(window, 'confirm').and.returnValue(false);
      component.logout();

      expect(authSpy.logout).not.toHaveBeenCalled();
      httpMock.verify();
    }));
  });
});
