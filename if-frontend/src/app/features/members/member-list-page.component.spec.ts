import { TestBed, fakeAsync, tick } from '@angular/core/testing';
import { Router, provideRouter } from '@angular/router';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting, HttpTestingController } from '@angular/common/http/testing';
import { NO_ERRORS_SCHEMA, signal } from '@angular/core';

import { MemberListPageComponent } from './member-list-page.component';
import { ApiService } from '../../core/services/api.service';
import { FamilyStateService } from '../../core/services/family-state.service';
import { AuthService } from '../../core/services/auth.service';

// ─── Helpers ───────────────────────────────────────────────────────────────

const API_BASE = '/api';

function buildComponent(signalFamilyId = 0, role: 'ADMIN' | 'USER' = 'USER') {
  // currentFamilyId es un Signal<number>; usamos signal() real para satisfacer el tipo Angular.
  const familyStateSpy = jasmine.createSpyObj<FamilyStateService>(
    'FamilyStateService',
    ['setFamily', 'clearFamily'],
    { currentFamilyId: signal(signalFamilyId) }
  );

  const authServiceSpy = jasmine.createSpyObj<AuthService>(
    'AuthService',
    ['logout'],
    { user: signal({ role } as any) }
  );

  TestBed.configureTestingModule({
    imports: [MemberListPageComponent],
    providers: [
      provideRouter([]),
      provideHttpClient(),
      provideHttpClientTesting(),
      { provide: ApiService, useValue: { base: API_BASE } as ApiService },
      { provide: FamilyStateService, useValue: familyStateSpy },
      { provide: AuthService, useValue: authServiceSpy }
    ],
    schemas: [NO_ERRORS_SCHEMA]
  });

  const fixture   = TestBed.createComponent(MemberListPageComponent);
  const component = fixture.componentInstance;
  const httpMock  = TestBed.inject(HttpTestingController);
  const router    = TestBed.inject(Router);
  spyOn(router, 'navigate').and.returnValue(Promise.resolve(true));

  return { fixture, component, httpMock, router, familyStateSpy, authServiceSpy };
}

// ───────────────────────────────────────────────────────────────────────────

describe('MemberListPageComponent', () => {
  afterEach(() => TestBed.resetTestingModule());

  // ═══════════════════════════════════════════════════════════════════════
  //  Estado inicial
  // ═══════════════════════════════════════════════════════════════════════

  describe('Estado inicial', () => {
    it('debe inicializar con members=[], error="" y saving=false', fakeAsync(() => {
      const { fixture, component, httpMock } = buildComponent();
      fixture.detectChanges();
      httpMock.expectOne(`${API_BASE}/members/mine`).flush({ data: [] });
      tick();

      expect(component.members).toEqual([]);
      expect(component.error).toBe('');
      expect(component.saving).toBeFalse();
      httpMock.verify();
    }));
  });

  // ═══════════════════════════════════════════════════════════════════════
  //  familyId getter
  // ═══════════════════════════════════════════════════════════════════════

  describe('familyId getter', () => {
    it('devuelve el valor de la señal cuando es > 0', () => {
      const { component, httpMock } = buildComponent(99);
      // detectChanges no llamado → no hay peticiones HTTP pendientes
      expect(component.familyId).toBe(99);
      httpMock.verify();
    });

    it('usa localStorage como fallback cuando la señal vale 0', () => {
      const { component, httpMock } = buildComponent(0);
      localStorage.setItem('selectedFamilyId', '77');

      expect(component.familyId).toBe(77);

      localStorage.removeItem('selectedFamilyId');
      httpMock.verify();
    });

    it('devuelve null si la señal es 0 y localStorage no tiene valor', () => {
      localStorage.removeItem('selectedFamilyId');
      const { component, httpMock } = buildComponent(0);

      expect(component.familyId).toBeNull();
      httpMock.verify();
    });
  });

  // ═══════════════════════════════════════════════════════════════════════
  //  load() — GET /members/mine
  // ═══════════════════════════════════════════════════════════════════════

  describe('load()', () => {
    it('popula members con la lista recibida del servidor', fakeAsync(() => {
      const { fixture, component, httpMock } = buildComponent();
      fixture.detectChanges();
      httpMock.expectOne(`${API_BASE}/members/mine`).flush({
        data: [
          { id: 1, fullName: 'Alice', roleType: 'MADRE' },
          { id: 2, fullName: 'Bob',   roleType: 'PADRE' }
        ]
      });
      tick();

      expect(component.members.length).toBe(2);
      expect(component.members[0].fullName).toBe('Alice');
      httpMock.verify();
    }));

    it('elimina duplicados por id (deduplicación)', fakeAsync(() => {
      const { fixture, component, httpMock } = buildComponent();
      fixture.detectChanges();
      httpMock.expectOne(`${API_BASE}/members/mine`).flush({
        data: [
          { id: 1, fullName: 'Alice' },
          { id: 2, fullName: 'Bob'   },
          { id: 1, fullName: 'Alice (dup)' }   // duplicado — debe ignorarse
        ]
      });
      tick();

      expect(component.members.length).toBe(2);
      expect(component.members.map(m => m.id)).toEqual([1, 2]);
      httpMock.verify();
    }));

    it('maneja data=null sin lanzar excepción', fakeAsync(() => {
      const { fixture, component, httpMock } = buildComponent();
      fixture.detectChanges();
      httpMock.expectOne(`${API_BASE}/members/mine`).flush({ data: null });
      tick();

      expect(component.members).toEqual([]);
      httpMock.verify();
    }));

    it('error HTTP → muestra mensaje y conserva members vacío', fakeAsync(() => {
      const { fixture, component, httpMock } = buildComponent();
      fixture.detectChanges();
      httpMock.expectOne(`${API_BASE}/members/mine`).flush(
        'Unauthorized', { status: 401, statusText: 'Unauthorized' }
      );
      tick();

      expect(component.error).toContain('miembros');
      expect(component.members).toEqual([]);
      httpMock.verify();
    }));
  });

  // ═══════════════════════════════════════════════════════════════════════
  //  create() — validación y flujo POST
  // ═══════════════════════════════════════════════════════════════════════

  describe('create()', () => {
    function initReady(component: MemberListPageComponent) {
      component.fullName = 'Carlos Gómez';
      component.role = 'PADRE';
      component.age = 35;
      component.aut  = 80;
      component.resp = 75;
    }

    it('nombre vacío → error "obligatorio", sin petición HTTP', fakeAsync(() => {
      const { fixture, component, httpMock } = buildComponent();
      fixture.detectChanges();
      httpMock.expectOne(`${API_BASE}/members/mine`).flush({ data: [] });
      tick();

      component.fullName = '   ';
      component.create();

      expect(component.error).toContain('obligatorio');
      httpMock.verify(); // no debe haber POST
    }));

    it('éxito → limpia fullName, saving=false y recarga la lista', fakeAsync(() => {
      const { fixture, component, httpMock } = buildComponent();
      fixture.detectChanges();
      httpMock.expectOne(`${API_BASE}/members/mine`).flush({ data: [] });
      tick();

      initReady(component);
      component.create();
      expect(component.saving).toBeTrue();

      httpMock.expectOne(`${API_BASE}/members/mine`).flush({});  // POST success
      tick();

      // Reload: una segunda GET
      httpMock.expectOne(`${API_BASE}/members/mine`).flush({ data: [] });
      tick();

      expect(component.fullName).toBe('');
      expect(component.saving).toBeFalse();
      httpMock.verify();
    }));

    it('error del servidor → muestra e.error.message y saving=false', fakeAsync(() => {
      const { fixture, component, httpMock } = buildComponent();
      fixture.detectChanges();
      httpMock.expectOne(`${API_BASE}/members/mine`).flush({ data: [] });
      tick();

      initReady(component);
      component.create();
      httpMock.expectOne(`${API_BASE}/members/mine`).flush(
        { message: 'Límite de miembros alcanzado' },
        { status: 400, statusText: 'Bad Request' }
      );
      tick();

      expect(component.error).toBe('Límite de miembros alcanzado');
      expect(component.saving).toBeFalse();
      httpMock.verify();
    }));
  });

  // ═══════════════════════════════════════════════════════════════════════
  //  remove()
  // ═══════════════════════════════════════════════════════════════════════

  describe('remove()', () => {
    it('confirm=false → no hace DELETE', fakeAsync(() => {
      const { fixture, component, httpMock } = buildComponent();
      fixture.detectChanges();
      httpMock.expectOne(`${API_BASE}/members/mine`).flush({ data: [] });
      tick();

      spyOn(window, 'confirm').and.returnValue(false);
      component.remove(5);

      httpMock.verify(); // no debe haber petición DELETE
    }));

    it('confirm=true → DELETE + recarga lista', fakeAsync(() => {
      const { fixture, component, httpMock } = buildComponent();
      fixture.detectChanges();
      httpMock.expectOne(`${API_BASE}/members/mine`).flush({
        data: [{ id: 5, fullName: 'Alice' }]
      });
      tick();

      spyOn(window, 'confirm').and.returnValue(true);
      component.remove(5);

      httpMock.expectOne(`${API_BASE}/members/5`).flush({});
      tick();

      httpMock.expectOne(`${API_BASE}/members/mine`).flush({ data: [] });
      tick();

      expect(component.members).toEqual([]);
      httpMock.verify();
    }));
  });

  // ═══════════════════════════════════════════════════════════════════════
  //  goToEvaluation()
  // ═══════════════════════════════════════════════════════════════════════

  describe('goToEvaluation()', () => {
    it('navega a /evaluations/start', fakeAsync(() => {
      const { fixture, component, httpMock, router } = buildComponent();
      fixture.detectChanges();
      httpMock.expectOne(`${API_BASE}/members/mine`).flush({ data: [] });
      tick();

      component.goToEvaluation();

      expect(router.navigate).toHaveBeenCalledWith(['/evaluations/start']);
      httpMock.verify();
    }));
  });

  // ═══════════════════════════════════════════════════════════════════════
  //  Admin Auto-Connect Protocol
  // ═══════════════════════════════════════════════════════════════════════

  describe('Admin Auto-Connect Protocol', () => {
    it('si es ADMIN y no tiene familia activa, consulta familias y carga miembros', fakeAsync(() => {
      const { fixture, component, httpMock, familyStateSpy } = buildComponent(0, 'ADMIN');
      fixture.detectChanges();

      const req = httpMock.expectOne(`${API_BASE}/families`);
      expect(req.request.method).toBe('GET');
      req.flush({
        data: [{ id: 12, name: 'Rozo Henao', familyCode: 'ROZO-12' }]
      });
      tick();

      expect(familyStateSpy.setFamily).toHaveBeenCalledWith({ id: 12, name: 'Rozo Henao', familyCode: 'ROZO-12' });
      
      httpMock.expectOne(`${API_BASE}/members/mine`).flush({ data: [] });
      tick();

      httpMock.verify();
    }));

    it('si es ADMIN y no hay familias, redirige a /families/create', fakeAsync(() => {
      const { fixture, component, httpMock, router } = buildComponent(0, 'ADMIN');
      fixture.detectChanges();

      httpMock.expectOne(`${API_BASE}/families`).flush({ data: [] });
      tick();

      expect(router.navigate).toHaveBeenCalledWith(['/families/create']);
      httpMock.verify();
    }));
  });
});
