import { TestBed, fakeAsync, tick } from '@angular/core/testing';
import { Router, provideRouter } from '@angular/router';
import { NO_ERRORS_SCHEMA, signal } from '@angular/core';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting, HttpTestingController } from '@angular/common/http/testing';

import { EvaluationStartPageComponent } from './evaluation-start-page.component';
import { FamilyStateService } from '../../core/services/family-state.service';
import { ApiService } from '../../core/services/api.service';

// ─── Constants ───────────────────────────────────────────────────────────────

const FAMILY_ID = 42;
const API_BASE   = '/api';

// ─── Helpers ─────────────────────────────────────────────────────────────────

function buildComponent(signalFamilyId = FAMILY_ID) {
  const familyStateSpy = jasmine.createSpyObj<FamilyStateService>(
    'FamilyStateService', [],
    { currentFamilyId: signal(signalFamilyId) }
  );

  TestBed.configureTestingModule({
    imports: [EvaluationStartPageComponent],
    providers: [
      provideRouter([]),
      provideHttpClient(),
      provideHttpClientTesting(),
      { provide: FamilyStateService, useValue: familyStateSpy },
      {
        provide: ApiService,
        useValue: { base: API_BASE }
      }
    ],
    schemas: [NO_ERRORS_SCHEMA]
  });

  const fixture   = TestBed.createComponent(EvaluationStartPageComponent);
  const component = fixture.componentInstance;
  const router    = TestBed.inject(Router);
  const httpMock  = TestBed.inject(HttpTestingController);
  spyOn(router, 'navigate').and.returnValue(Promise.resolve(true));

  return { fixture, component, router, httpMock };
}

/** Flush the two GET requests fired on init (members + history). */
function flushInit(
  httpMock: HttpTestingController,
  familyId = FAMILY_ID,
  members: any[]  = [],
  history: any[]  = []
) {
  httpMock.expectOne(`${API_BASE}/members/family/${familyId}`)
    .flush({ data: members });
  httpMock.expectOne(`${API_BASE}/assessments/family/${familyId}/history`)
    .flush({ data: history });
}

// ─────────────────────────────────────────────────────────────────────────────

describe('EvaluationStartPageComponent', () => {
  afterEach(() => TestBed.resetTestingModule());

  // ═══════════════════════════════════════════════════════════════════════
  //  ngOnInit()
  // ═══════════════════════════════════════════════════════════════════════

  describe('ngOnInit()', () => {
    it('familyId=0 → no realiza peticiones HTTP', fakeAsync(() => {
      const { fixture, component, httpMock } = buildComponent(0);
      fixture.detectChanges();
      tick();

      httpMock.expectNone(`${API_BASE}/members/family/0`);
      httpMock.verify();
      expect(component.members).toEqual([]);
    }));

    it('familyId>0 → carga los miembros desde la API', fakeAsync(() => {
      const members = [
        { id: 1, fullName: 'Ana López', role: 'MADRE' },
        { id: 2, fullName: 'Pedro López', role: 'PADRE' }
      ];
      const { fixture, component, httpMock } = buildComponent();
      fixture.detectChanges();
      flushInit(httpMock, FAMILY_ID, members);
      tick();

      expect(component.members.length).toBe(2);
      expect(component.members[0].fullName).toBe('Ana López');
    }));

    it('familyId>0 → detecta evaluación pendiente (STARTED) y asigna pendingEvalId', fakeAsync(() => {
      const history = [
        { id: 99, status: 'STARTED' },
        { id: 50, status: 'FINALIZED' }
      ];
      const { fixture, component, httpMock } = buildComponent();
      fixture.detectChanges();
      flushInit(httpMock, FAMILY_ID, [], history);
      tick();

      expect(component.pendingEvalId).toBe(99);
    }));

    it('sin evaluación STARTED → pendingEvalId permanece null', fakeAsync(() => {
      const history = [{ id: 50, status: 'FINALIZED' }];
      const { fixture, component, httpMock } = buildComponent();
      fixture.detectChanges();
      flushInit(httpMock, FAMILY_ID, [], history);
      tick();

      expect(component.pendingEvalId).toBeNull();
    }));

    it('error en GET historial → catchError silencia y pendingEvalId sigue null', fakeAsync(() => {
      const { fixture, component, httpMock } = buildComponent();
      fixture.detectChanges();

      httpMock.expectOne(`${API_BASE}/members/family/${FAMILY_ID}`).flush({ data: [] });
      httpMock.expectOne(`${API_BASE}/assessments/family/${FAMILY_ID}/history`)
        .flush('Server error', { status: 500, statusText: 'Internal Server Error' });
      tick();

      expect(component.pendingEvalId).toBeNull();
    }));

    it('respuesta historial null → pendingEvalId sigue null (guard response?.data)', fakeAsync(() => {
      const { fixture, component, httpMock } = buildComponent();
      fixture.detectChanges();

      httpMock.expectOne(`${API_BASE}/members/family/${FAMILY_ID}`).flush({ data: [] });
      httpMock.expectOne(`${API_BASE}/assessments/family/${FAMILY_ID}/history`).flush(null);
      tick();

      expect(component.pendingEvalId).toBeNull();
    }));
  });

  // ═══════════════════════════════════════════════════════════════════════
  //  start()
  // ═══════════════════════════════════════════════════════════════════════

  describe('start()', () => {
    it('familyId=0 → alert y redirige a /families sin POST', fakeAsync(() => {
      const { fixture, component, router, httpMock } = buildComponent(0);
      fixture.detectChanges();
      tick();
      spyOn(window, 'alert');

      component.start();
      tick();

      expect(window.alert).toHaveBeenCalled();
      expect(router.navigate).toHaveBeenCalledWith(['/families']);
      httpMock.expectNone(`${API_BASE}/assessments/start`);
    }));

    it('familyId>0 → POST /assessments/start con payload correcto', fakeAsync(() => {
      const { fixture, component, httpMock } = buildComponent();
      fixture.detectChanges();
      flushInit(httpMock);
      tick();

      component.selectedMember = 3;
      component.start();

      const req = httpMock.expectOne(`${API_BASE}/assessments/start`);
      expect(req.request.method).toBe('POST');
      expect(req.request.body).toEqual({ familyId: FAMILY_ID, memberId: 3 });
      req.flush({ data: { id: 101 } });
      tick();
    }));

    it('respuesta con data.id → navega a /evaluations/{id}/form', fakeAsync(() => {
      const { fixture, component, router, httpMock } = buildComponent();
      fixture.detectChanges();
      flushInit(httpMock);
      tick();

      component.start();
      httpMock.expectOne(`${API_BASE}/assessments/start`)
        .flush({ data: { id: 101 } });
      tick();

      expect(router.navigate).toHaveBeenCalledWith(['/evaluations', 101, 'form']);
      expect(component.loading).toBeFalse();
    }));

    it('respuesta con id en raíz (no data.id) → navega correctamente', fakeAsync(() => {
      const { fixture, component, router, httpMock } = buildComponent();
      fixture.detectChanges();
      flushInit(httpMock);
      tick();

      component.start();
      httpMock.expectOne(`${API_BASE}/assessments/start`).flush({ id: 202 });
      tick();

      expect(router.navigate).toHaveBeenCalledWith(['/evaluations', 202, 'form']);
    }));

    it('loading=true durante la petición, false al completar', fakeAsync(() => {
      const { fixture, component, httpMock } = buildComponent();
      fixture.detectChanges();
      flushInit(httpMock);
      tick();

      component.start();
      expect(component.loading).toBeTrue();

      httpMock.expectOne(`${API_BASE}/assessments/start`).flush({ data: { id: 5 } });
      tick();

      expect(component.loading).toBeFalse();
    }));

    it('error en POST → loading=false, no navega', fakeAsync(() => {
      const { fixture, component, router, httpMock } = buildComponent();
      fixture.detectChanges();
      flushInit(httpMock);
      tick();
      spyOn(window, 'alert');

      component.start();
      httpMock.expectOne(`${API_BASE}/assessments/start`)
        .flush('Error', { status: 500, statusText: 'Server Error' });
      tick();

      expect(component.loading).toBeFalse();
      expect(router.navigate).not.toHaveBeenCalledWith(
        jasmine.arrayContaining(['/evaluations'])
      );
    }));
  });

  // ═══════════════════════════════════════════════════════════════════════
  //  resume()
  // ═══════════════════════════════════════════════════════════════════════

  describe('resume()', () => {
    it('pendingEvalId establecido → navega a /evaluations/{id}/form', fakeAsync(() => {
      const { fixture, component, router, httpMock } = buildComponent();
      fixture.detectChanges();
      flushInit(httpMock, FAMILY_ID, [], [{ id: 77, status: 'STARTED' }]);
      tick();

      component.resume();

      expect(router.navigate).toHaveBeenCalledWith(['/evaluations', 77, 'form']);
    }));

    it('pendingEvalId=null → no navega', fakeAsync(() => {
      const { fixture, component, router, httpMock } = buildComponent();
      fixture.detectChanges();
      flushInit(httpMock);
      tick();

      component.resume();

      expect(router.navigate).not.toHaveBeenCalled();
    }));
  });

  // ═══════════════════════════════════════════════════════════════════════
  //  discardAndStart()
  // ═══════════════════════════════════════════════════════════════════════

  describe('discardAndStart()', () => {
    it('limpia pendingEvalId y llama start() (POST /assessments/start)', fakeAsync(() => {
      const { fixture, component, httpMock } = buildComponent();
      fixture.detectChanges();
      flushInit(httpMock, FAMILY_ID, [], [{ id: 77, status: 'STARTED' }]);
      tick();

      expect(component.pendingEvalId).toBe(77); // precondition

      component.discardAndStart();
      expect(component.pendingEvalId).toBeNull();

      httpMock.expectOne(`${API_BASE}/assessments/start`).flush({ data: { id: 88 } });
      tick();
    }));
  });

  // ═══════════════════════════════════════════════════════════════════════
  //  goToFamilies()
  // ═══════════════════════════════════════════════════════════════════════

  describe('goToFamilies()', () => {
    it('navega a /families', fakeAsync(() => {
      const { fixture, component, router, httpMock } = buildComponent();
      fixture.detectChanges();
      flushInit(httpMock);
      tick();

      component.goToFamilies();

      expect(router.navigate).toHaveBeenCalledWith(['/families']);
    }));
  });
});
