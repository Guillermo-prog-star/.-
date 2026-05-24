import { TestBed, fakeAsync, tick } from '@angular/core/testing';
import { Router, provideRouter } from '@angular/router';
import { of, throwError } from 'rxjs';
import { NO_ERRORS_SCHEMA } from '@angular/core';

import { RegisterPageComponent } from './register-page.component';
import { AuthService } from '../../core/services/auth.service';

// ─── Helpers ───────────────────────────────────────────────────────────────

function buildAuthSpy() {
  return jasmine.createSpyObj<AuthService>(
    'AuthService',
    ['register', 'registerFamily']
  );
}

function buildComponent(authSpy: jasmine.SpyObj<AuthService>) {
  TestBed.configureTestingModule({
    imports: [RegisterPageComponent],
    providers: [
      provideRouter([]),
      { provide: AuthService, useValue: authSpy }
    ],
    schemas: [NO_ERRORS_SCHEMA]
  });

  const fixture = TestBed.createComponent(RegisterPageComponent);
  const component = fixture.componentInstance;
  const router = TestBed.inject(Router);
  spyOn(router, 'navigate').and.returnValue(Promise.resolve(true));
  fixture.detectChanges();
  return { fixture, component, router };
}

// Rellena los campos mínimos del modo VOUCHER
function fillVoucher(c: RegisterPageComponent) {
  c.fullName = 'William López';
  c.email = 'william@if.com';
  c.password = 'Password1';
}

// Rellena los campos mínimos del modo NEW_FAMILY
function fillFamily(c: RegisterPageComponent) {
  c.fullName = 'Carlos Admin';
  c.email = 'carlos@if.com';
  c.password = 'Password1';
  c.confirmPassword = 'Password1';
  c.familyName = 'Familia Gómez';
  c.municipio = 'Bogotá';
  c.departmentCode = 'CUN';
}

// ───────────────────────────────────────────────────────────────────────────

describe('RegisterPageComponent', () => {
  afterEach(() => TestBed.resetTestingModule());

  // ═══════════════════════════════════════════════════════════════════════
  //  Estado inicial
  // ═══════════════════════════════════════════════════════════════════════

  describe('Estado inicial', () => {
    it('debe iniciar en modo VOUCHER', () => {
      const { component } = buildComponent(buildAuthSpy());
      expect(component.mode).toBe('VOUCHER');
    });

    it('debe iniciar con loading=false y error vacío', () => {
      const { component } = buildComponent(buildAuthSpy());
      expect(component.loading).toBeFalse();
      expect(component.error).toBe('');
    });
  });

  // ═══════════════════════════════════════════════════════════════════════
  //  setMode()
  // ═══════════════════════════════════════════════════════════════════════

  describe('setMode()', () => {
    it('debe cambiar el modo a NEW_FAMILY', () => {
      const { component } = buildComponent(buildAuthSpy());
      component.setMode('NEW_FAMILY');
      expect(component.mode).toBe('NEW_FAMILY');
    });

    it('debe limpiar el error al cambiar de modo', () => {
      const { component } = buildComponent(buildAuthSpy());
      component.error = 'error previo';
      component.setMode('NEW_FAMILY');
      expect(component.error).toBe('');
    });
  });

  // ═══════════════════════════════════════════════════════════════════════
  //  submit() — validaciones de campos obligatorios
  // ═══════════════════════════════════════════════════════════════════════

  describe('submit() — validaciones', () => {
    it('debe mostrar error cuando faltan campos obligatorios (fullName, email, password)', () => {
      const auth = buildAuthSpy();
      const { component } = buildComponent(auth);
      component.submit();
      expect(component.error).toContain('Campos obligatorios');
      expect(auth.register).not.toHaveBeenCalled();
    });

    it('debe mostrar error en modo NEW_FAMILY cuando falta familyName', () => {
      const { component } = buildComponent(buildAuthSpy());
      component.mode = 'NEW_FAMILY';
      component.fullName = 'Alguien';
      component.email = 'a@if.com';
      component.password = 'pass';
      // familyName vacío
      component.submit();
      expect(component.error).toContain('nombre');
    });

    it('debe mostrar error en modo NEW_FAMILY cuando falta municipio', () => {
      const { component } = buildComponent(buildAuthSpy());
      component.mode = 'NEW_FAMILY';
      fillFamily(component);
      component.municipio = '';
      component.submit();
      expect(component.error).toContain('municipio');
    });

    it('debe mostrar error en modo NEW_FAMILY cuando falta departmentCode', () => {
      const { component } = buildComponent(buildAuthSpy());
      component.mode = 'NEW_FAMILY';
      fillFamily(component);
      component.departmentCode = '';
      component.submit();
      expect(component.error).toContain('municipio');
    });

    it('debe mostrar error cuando las contraseñas no coinciden en modo NEW_FAMILY', () => {
      const { component } = buildComponent(buildAuthSpy());
      component.mode = 'NEW_FAMILY';
      fillFamily(component);
      component.confirmPassword = 'OtraPassword9';
      component.submit();
      expect(component.error).toContain('contraseñas');
    });
  });

  // ═══════════════════════════════════════════════════════════════════════
  //  submit() — modo VOUCHER
  // ═══════════════════════════════════════════════════════════════════════

  describe('submit() — modo VOUCHER', () => {
    it('debe navegar a /dashboard cuando el servidor devuelve familyId', fakeAsync(() => {
      const auth = buildAuthSpy();
      auth.register.and.returnValue(of({ user: { familyId: 10 } }));
      const { component, router } = buildComponent(auth);

      fillVoucher(component);
      component.submit();
      tick();

      expect(router.navigate).toHaveBeenCalledWith(['/dashboard']);
    }));

    it('debe navegar a /families/create cuando no hay familyId', fakeAsync(() => {
      const auth = buildAuthSpy();
      auth.register.and.returnValue(of({ user: {} }));
      const { component, router } = buildComponent(auth);

      fillVoucher(component);
      component.submit();
      tick();

      expect(router.navigate).toHaveBeenCalledWith(['/families/create']);
    }));

    it('debe mostrar el mensaje de error del servidor en fallo de registro', fakeAsync(() => {
      const auth = buildAuthSpy();
      auth.register.and.returnValue(throwError(() => ({
        error: { message: 'Voucher no válido' }
      })));
      const { component } = buildComponent(auth);

      fillVoucher(component);
      component.submit();
      tick();

      expect(component.error).toBe('Voucher no válido');
      expect(component.loading).toBeFalse();
    }));

    it('debe normalizar el email a minúsculas', fakeAsync(() => {
      const auth = buildAuthSpy();
      auth.register.and.returnValue(of({ user: { familyId: 1 } }));
      const { component } = buildComponent(auth);

      fillVoucher(component);
      component.email = 'WILLIAM@IF.COM';
      component.submit();
      tick();

      expect(auth.register).toHaveBeenCalledWith(
        jasmine.objectContaining({ email: 'william@if.com' })
      );
    }));
  });

  // ═══════════════════════════════════════════════════════════════════════
  //  submit() — modo NEW_FAMILY
  // ═══════════════════════════════════════════════════════════════════════

  describe('submit() — modo NEW_FAMILY', () => {
    it('debe navegar a /dashboard tras registro exitoso de familia', fakeAsync(() => {
      const auth = buildAuthSpy();
      auth.registerFamily.and.returnValue(of({ token: 'tok', user: { familyId: 20 } }));
      const { component, router } = buildComponent(auth);

      component.mode = 'NEW_FAMILY';
      fillFamily(component);
      component.submit();
      tick();

      expect(router.navigate).toHaveBeenCalledWith(['/dashboard']);
    }));

    it('debe mostrar mensaje de error del servidor en fallo de creación de familia', fakeAsync(() => {
      const auth = buildAuthSpy();
      auth.registerFamily.and.returnValue(throwError(() => ({
        error: { message: 'El correo ya está registrado en el sistema.' }
      })));
      const { component } = buildComponent(auth);

      component.mode = 'NEW_FAMILY';
      fillFamily(component);
      component.submit();
      tick();

      expect(component.error).toBe('El correo ya está registrado en el sistema.');
      expect(component.loading).toBeFalse();
    }));

    it('debe incluir campos de ubicación en el payload enviado al backend', fakeAsync(() => {
      const auth = buildAuthSpy();
      auth.registerFamily.and.returnValue(of({ token: 'tok', user: { familyId: 20 } }));
      const { component } = buildComponent(auth);

      component.mode = 'NEW_FAMILY';
      fillFamily(component);
      component.municipio = 'Medellín';
      component.countryCode = 'CO';
      component.departmentCode = 'ANT';
      component.submit();
      tick();

      expect(auth.registerFamily).toHaveBeenCalledWith(
        jasmine.objectContaining({
          municipio: 'Medellín',
          countryCode: 'CO',
          departmentCode: 'ANT'
        })
      );
    }));
  });
});
