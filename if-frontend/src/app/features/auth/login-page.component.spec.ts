import { TestBed, fakeAsync, tick } from '@angular/core/testing';
import { Router } from '@angular/router';
import { provideRouter } from '@angular/router';
import { signal } from '@angular/core';
import { of, throwError } from 'rxjs';
import { NO_ERRORS_SCHEMA } from '@angular/core';

import { LoginPageComponent } from './login-page.component';
import { AuthService, AuthUser } from '../../core/services/auth.service';

// ─── Helpers ───────────────────────────────────────────────────────────────

function buildAuthSpy(user: AuthUser | null = null) {
  return jasmine.createSpyObj<AuthService>(
    'AuthService',
    ['login', 'getToken', 'logout'],
    { user: signal(user) }
  );
}

function buildComponent(authSpy: jasmine.SpyObj<AuthService>) {
  TestBed.configureTestingModule({
    imports: [LoginPageComponent],
    providers: [
      provideRouter([]),
      { provide: AuthService, useValue: authSpy }
    ],
    schemas: [NO_ERRORS_SCHEMA]
  });

  const fixture = TestBed.createComponent(LoginPageComponent);
  const component = fixture.componentInstance;
  const router = TestBed.inject(Router);
  spyOn(router, 'navigate').and.returnValue(Promise.resolve(true));
  fixture.detectChanges();
  return { fixture, component, router };
}

// ───────────────────────────────────────────────────────────────────────────

describe('LoginPageComponent', () => {
  afterEach(() => TestBed.resetTestingModule());

  // ═══════════════════════════════════════════════════════════════════════
  //  Estado inicial
  // ═══════════════════════════════════════════════════════════════════════

  describe('Estado inicial', () => {
    it('debe iniciar con loading=false, error vacío y contraseña oculta', () => {
      const auth = buildAuthSpy();
      const { component } = buildComponent(auth);

      expect(component.loading).toBeFalse();
      expect(component.error).toBe('');
      expect(component.showPassword).toBeFalse();
    });
  });

  // ═══════════════════════════════════════════════════════════════════════
  //  togglePassword()
  // ═══════════════════════════════════════════════════════════════════════

  describe('togglePassword()', () => {
    it('debe alternar showPassword de false a true', () => {
      const auth = buildAuthSpy();
      const { component } = buildComponent(auth);

      component.togglePassword();

      expect(component.showPassword).toBeTrue();
    });

    it('debe alternar showPassword de true a false en segunda llamada', () => {
      const auth = buildAuthSpy();
      const { component } = buildComponent(auth);

      component.togglePassword();
      component.togglePassword();

      expect(component.showPassword).toBeFalse();
    });
  });

  // ═══════════════════════════════════════════════════════════════════════
  //  submit() — navegación exitosa
  // ═══════════════════════════════════════════════════════════════════════

  describe('submit() — éxito', () => {
    it('debe navegar a /dashboard cuando el usuario tiene familyId', fakeAsync(() => {
      const user: AuthUser = {
        token: 'tok', fullName: 'William', email: 'w@if.com',
        role: 'USER', familyId: 42, familyName: 'Familia López'
      };
      const auth = buildAuthSpy(user);
      auth.login.and.returnValue(of({ token: 'tok' }));
      const { component, router } = buildComponent(auth);

      component.email = 'w@if.com';
      component.password = 'pass123';
      component.submit();
      tick();

      expect(router.navigate).toHaveBeenCalledWith(['/dashboard']);
    }));

    it('debe navegar a /families/create cuando el usuario NO tiene familyId', fakeAsync(() => {
      const user: AuthUser = {
        token: 'tok', fullName: 'Nuevo', email: 'n@if.com', role: 'USER'
      };
      const auth = buildAuthSpy(user);
      auth.login.and.returnValue(of({ token: 'tok' }));
      const { component, router } = buildComponent(auth);

      component.email = 'n@if.com';
      component.password = 'pass123';
      component.submit();
      tick();

      expect(router.navigate).toHaveBeenCalledWith(['/families/create']);
    }));

    it('debe poner loading=false después de respuesta exitosa', fakeAsync(() => {
      const user: AuthUser = { token: 'tok', fullName: 'U', email: 'u@if.com', role: 'USER', familyId: 1 };
      const auth = buildAuthSpy(user);
      auth.login.and.returnValue(of({ token: 'tok' }));
      const { component } = buildComponent(auth);

      component.submit();
      tick();

      expect(component.loading).toBeFalse();
    }));

    it('debe normalizar email a minúsculas antes de enviar', fakeAsync(() => {
      const user: AuthUser = { token: 'tok', fullName: 'U', email: 'u@if.com', role: 'USER', familyId: 1 };
      const auth = buildAuthSpy(user);
      auth.login.and.returnValue(of({ token: 'tok' }));
      const { component } = buildComponent(auth);

      component.email = 'WILLIAM@IF.COM';
      component.password = 'pass';
      component.submit();
      tick();

      expect(auth.login).toHaveBeenCalledWith(
        jasmine.objectContaining({ email: 'william@if.com' })
      );
    }));
  });

  // ═══════════════════════════════════════════════════════════════════════
  //  submit() — errores de autenticación
  // ═══════════════════════════════════════════════════════════════════════

  describe('submit() — errores', () => {
    it('debe mostrar "Credenciales incorrectas." en error 401', fakeAsync(() => {
      const auth = buildAuthSpy();
      auth.login.and.returnValue(throwError(() => ({ status: 401 })));
      const { component } = buildComponent(auth);

      component.email = 'x@if.com';
      component.password = 'wrong';
      component.submit();
      tick();

      expect(component.error).toBe('Credenciales incorrectas.');
      expect(component.loading).toBeFalse();
    }));

    it('debe mostrar aviso de cuenta bloqueada en error 423', fakeAsync(() => {
      const auth = buildAuthSpy();
      auth.login.and.returnValue(throwError(() => ({ status: 423 })));
      const { component } = buildComponent(auth);

      component.email = 'locked@if.com';
      component.password = 'pass';
      component.submit();
      tick();

      expect(component.error).toContain('bloqueada');
      expect(component.loading).toBeFalse();
    }));

    it('debe mostrar aviso de bloqueo cuando error.message contiene "locked"', fakeAsync(() => {
      const auth = buildAuthSpy();
      auth.login.and.returnValue(throwError(() => ({
        status: 400,
        error: { message: 'Account locked until 2026-01-01' }
      })));
      const { component } = buildComponent(auth);

      component.email = 'locked@if.com';
      component.password = 'pass';
      component.submit();
      tick();

      expect(component.error).toContain('bloqueada');
    }));

    it('debe limpiar el error previo antes de un nuevo submit', fakeAsync(() => {
      const user: AuthUser = { token: 'tok', fullName: 'U', email: 'u@if.com', role: 'USER', familyId: 1 };
      const auth = buildAuthSpy(user);
      auth.login.and.returnValue(of({ token: 'tok' }));
      const { component } = buildComponent(auth);

      component.error = 'Error anterior';
      component.submit();
      tick();

      expect(component.error).toBe('');
    }));
  });
});
