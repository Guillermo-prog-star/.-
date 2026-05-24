import { TestBed, fakeAsync, tick } from '@angular/core/testing';
import { NO_ERRORS_SCHEMA } from '@angular/core';
import { signal } from '@angular/core';
import { of, throwError } from 'rxjs';

import { FamilyGratitudeComponent } from './family-gratitude.component';
import { FamilyGratitudeService } from './family-gratitude.service';
import { AuthService, AuthUser } from '../../core/services/auth.service';
import { FamilyGratitude } from './family-gratitude.model';

// ─── Stubs ───────────────────────────────────────────────────────────────────

const FAMILY_ID = 5;

const USER_STUB: AuthUser = {
  token: 'jwt', fullName: 'Ana López', email: 'ana@test.com',
  role: 'USER', familyId: FAMILY_ID
};

const GRATITUDE_STUB: FamilyGratitude = {
  id: 1, familyId: FAMILY_ID,
  fromMember: 'Ana', toMember: 'Pedro',
  description: 'Gracias por tu apoyo', createdAt: '2026-01-01T00:00:00'
};

// ─── Helper ──────────────────────────────────────────────────────────────────

function buildComponent(user: AuthUser | null = USER_STUB) {
  const serviceSpy = jasmine.createSpyObj<FamilyGratitudeService>(
    'FamilyGratitudeService', {
      findByFamily: of([]),
      create:       of(GRATITUDE_STUB)
    }
  );

  const authSpy = jasmine.createSpyObj<AuthService>(
    'AuthService', [], { user: signal(user) }
  );

  TestBed.configureTestingModule({
    imports: [FamilyGratitudeComponent],
    providers: [
      { provide: FamilyGratitudeService, useValue: serviceSpy },
      { provide: AuthService,            useValue: authSpy }
    ],
    schemas: [NO_ERRORS_SCHEMA]
  });

  const fixture   = TestBed.createComponent(FamilyGratitudeComponent);
  const component = fixture.componentInstance;

  return { fixture, component, serviceSpy };
}

// ─────────────────────────────────────────────────────────────────────────────

describe('FamilyGratitudeComponent', () => {
  afterEach(() => TestBed.resetTestingModule());

  // ═══════════════════════════════════════════════════════════════════════════
  //  ngOnInit()
  // ═══════════════════════════════════════════════════════════════════════════

  describe('ngOnInit()', () => {
    it('user con familyId → carga agradecimientos y pre-rellena fromMember', fakeAsync(() => {
      const { fixture, component, serviceSpy } = buildComponent();
      fixture.detectChanges();
      tick();

      expect(serviceSpy.findByFamily).toHaveBeenCalledWith(FAMILY_ID);
      expect(component.form.fromMember).toBe('Ana López');
      expect(component.familyId).toBe(FAMILY_ID);
    }));

    it('user sin familyId → errorMessage y no llama servicio', fakeAsync(() => {
      const { fixture, component, serviceSpy } = buildComponent({ ...USER_STUB, familyId: undefined });
      fixture.detectChanges();
      tick();

      expect(serviceSpy.findByFamily).not.toHaveBeenCalled();
      expect(component.errorMessage).toBeTruthy();
    }));

    it('user null → errorMessage', fakeAsync(() => {
      const { fixture, component } = buildComponent(null);
      fixture.detectChanges();
      tick();

      expect(component.errorMessage).toBeTruthy();
    }));
  });

  // ═══════════════════════════════════════════════════════════════════════════
  //  loadGratitudes()
  // ═══════════════════════════════════════════════════════════════════════════

  describe('loadGratitudes()', () => {
    it('éxito → entries poblado y loading=false', fakeAsync(() => {
      const { fixture, component, serviceSpy } = buildComponent();
      serviceSpy.findByFamily.and.returnValue(of([GRATITUDE_STUB]));
      fixture.detectChanges();
      tick();

      expect(component.entries.length).toBe(1);
      expect(component.loading).toBeFalse();
    }));

    it('error → errorMessage y loading=false', fakeAsync(() => {
      const { fixture, component, serviceSpy } = buildComponent();
      serviceSpy.findByFamily.and.returnValue(throwError(() => new Error('500')));
      fixture.detectChanges();
      tick();

      expect(component.loading).toBeFalse();
      expect(component.errorMessage).toBeTruthy();
    }));
  });

  // ═══════════════════════════════════════════════════════════════════════════
  //  isFormValid()
  // ═══════════════════════════════════════════════════════════════════════════

  describe('isFormValid()', () => {
    it('todos los campos rellenos → true', fakeAsync(() => {
      const { fixture, component } = buildComponent();
      fixture.detectChanges();
      tick();

      component.form = { familyId: FAMILY_ID, fromMember: 'Ana', toMember: 'Pedro', description: 'Gracias' };
      expect(component.isFormValid()).toBeTrue();
    }));

    it('toMember vacío → false', fakeAsync(() => {
      const { fixture, component } = buildComponent();
      fixture.detectChanges();
      tick();

      component.form = { familyId: FAMILY_ID, fromMember: 'Ana', toMember: '', description: 'Gracias' };
      expect(component.isFormValid()).toBeFalse();
    }));

    it('description vacío → false', fakeAsync(() => {
      const { fixture, component } = buildComponent();
      fixture.detectChanges();
      tick();

      component.form = { familyId: FAMILY_ID, fromMember: 'Ana', toMember: 'Pedro', description: '  ' };
      expect(component.isFormValid()).toBeFalse();
    }));
  });

  // ═══════════════════════════════════════════════════════════════════════════
  //  createGratitude()
  // ═══════════════════════════════════════════════════════════════════════════

  describe('createGratitude()', () => {
    it('formulario inválido → errorMessage y no llama create', fakeAsync(() => {
      const { fixture, component, serviceSpy } = buildComponent();
      fixture.detectChanges();
      tick();

      component.form.toMember = '';
      component.createGratitude();

      expect(component.errorMessage).toBeTruthy();
      expect(serviceSpy.create).not.toHaveBeenCalled();
    }));

    it('éxito → successMessage, resetea descripción, recarga entries', fakeAsync(() => {
      const { fixture, component, serviceSpy } = buildComponent();
      serviceSpy.findByFamily.and.returnValue(of([GRATITUDE_STUB]));
      fixture.detectChanges();
      tick();
      serviceSpy.findByFamily.calls.reset();

      component.form = { familyId: FAMILY_ID, fromMember: 'Ana', toMember: 'Pedro', description: 'Gracias' };
      component.createGratitude();
      tick();

      expect(component.successMessage).toBeTruthy();
      expect(component.form.description).toBe('');
      expect(component.form.fromMember).toBe('Ana'); // conserva fromMember
      expect(serviceSpy.findByFamily).toHaveBeenCalled();
      tick(4000); // drain successMessage timeout
    }));

    it('éxito → successMessage desaparece después de 4000ms', fakeAsync(() => {
      const { fixture, component } = buildComponent();
      fixture.detectChanges();
      tick();

      component.form = { familyId: FAMILY_ID, fromMember: 'Ana', toMember: 'Pedro', description: 'Gracias' };
      component.createGratitude();
      tick();

      expect(component.successMessage).toBeTruthy();
      tick(4000); // drain the setTimeout
      expect(component.successMessage).toBe('');
    }));

    it('error → errorMessage y loading=false', fakeAsync(() => {
      const { fixture, component, serviceSpy } = buildComponent();
      serviceSpy.create.and.returnValue(throwError(() => new Error('503')));
      fixture.detectChanges();
      tick();

      component.form = { familyId: FAMILY_ID, fromMember: 'Ana', toMember: 'Pedro', description: 'Gracias' };
      component.createGratitude();
      tick();

      expect(component.loading).toBeFalse();
      expect(component.errorMessage).toBeTruthy();
    }));
  });
});
