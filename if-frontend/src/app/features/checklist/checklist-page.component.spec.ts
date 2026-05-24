import { TestBed, fakeAsync, tick } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting, HttpTestingController } from '@angular/common/http/testing';
import { NO_ERRORS_SCHEMA } from '@angular/core';

import { ChecklistPageComponent } from './checklist-page.component';
import { ApiService } from '../../core/services/api.service';
import { FamilyStateService } from '../../core/services/family-state.service';

// ─── Helpers ───────────────────────────────────────────────────────────────

const API_BASE = '/api';
const FAMILY_ID = 42;

function buildComponent(selectedFamilyId = FAMILY_ID) {
  const familyStateSpy = jasmine.createSpyObj<FamilyStateService>(
    'FamilyStateService',
    { getSelectedFamilyId: selectedFamilyId, getFamilyId: selectedFamilyId }
  );

  TestBed.configureTestingModule({
    imports: [ChecklistPageComponent],
    providers: [
      provideHttpClient(),
      provideHttpClientTesting(),
      { provide: ApiService, useValue: { base: API_BASE } as ApiService },
      { provide: FamilyStateService, useValue: familyStateSpy }
    ],
    schemas: [NO_ERRORS_SCHEMA]
  });

  const fixture   = TestBed.createComponent(ChecklistPageComponent);
  const component = fixture.componentInstance;
  const httpMock  = TestBed.inject(HttpTestingController);

  return { fixture, component, httpMock };
}

/**
 * Flusha las tres peticiones paralelas que emite load():
 *   1. GET /checklist/family/{id}
 *   2. GET /family-logbook/family/{id}/status/RESOLVED
 *   3. GET /evidences/family/{id}
 */
function flushLoad(
  httpMock: HttpTestingController,
  familyId = FAMILY_ID,
  items: any[]       = [],
  logbook: any[]     = [],
  evidenceData: any[] = []
) {
  httpMock.expectOne(`${API_BASE}/checklist/family/${familyId}`)
    .flush({ data: items });
  httpMock.expectOne(`${API_BASE}/family-logbook/family/${familyId}/status/RESOLVED`)
    .flush(logbook);
  httpMock.expectOne(`${API_BASE}/evidences/family/${familyId}`)
    .flush({ data: evidenceData });
}

// ───────────────────────────────────────────────────────────────────────────

describe('ChecklistPageComponent', () => {
  afterEach(() => TestBed.resetTestingModule());

  // ═══════════════════════════════════════════════════════════════════════
  //  Estado inicial
  // ═══════════════════════════════════════════════════════════════════════

  describe('Estado inicial', () => {
    it('items=[], loading=false antes de ngOnInit', () => {
      const { component, httpMock } = buildComponent();
      // Sin detectChanges: no se ejecuta ngOnInit
      expect(component.items).toEqual([]);
      expect(component.loading).toBeFalse();
      httpMock.verify();
    });
  });

  // ═══════════════════════════════════════════════════════════════════════
  //  ngOnInit() — condicional sobre familyId
  // ═══════════════════════════════════════════════════════════════════════

  describe('ngOnInit()', () => {
    it('familyId=0 → NO hace peticiones HTTP', fakeAsync(() => {
      const { fixture, httpMock } = buildComponent(0);
      fixture.detectChanges();
      tick();

      httpMock.verify(); // no debe haber ninguna petición
    }));

    it('familyId>0 → emite las 3 peticiones HTTP de load()', fakeAsync(() => {
      const { fixture, httpMock } = buildComponent();
      fixture.detectChanges();
      tick();

      flushLoad(httpMock);
      tick();

      httpMock.verify();
    }));
  });

  // ═══════════════════════════════════════════════════════════════════════
  //  load() — respuestas exitosas
  // ═══════════════════════════════════════════════════════════════════════

  describe('load() — respuestas exitosas', () => {
    it('popula items y establece loading=false', fakeAsync(() => {
      const { fixture, component, httpMock } = buildComponent();
      fixture.detectChanges();

      const mockItems = [
        { id: 1, description: 'Tarea A', completed: false, dimension: 'emociones' },
        { id: 2, description: 'Tarea B', completed: true,  dimension: 'habitos'   }
      ];
      flushLoad(httpMock, FAMILY_ID, mockItems);
      tick();

      expect(component.items.length).toBe(2);
      expect(component.loading).toBeFalse();
      httpMock.verify();
    }));

    it('popula resolvedEvidences con la bitácora RESOLVED', fakeAsync(() => {
      const { fixture, component, httpMock } = buildComponent();
      fixture.detectChanges();

      const logbook = [{ id: 10, status: 'RESOLVED' }, { id: 11, status: 'RESOLVED' }];
      flushLoad(httpMock, FAMILY_ID, [], logbook);
      tick();

      expect(component.resolvedEvidences.length).toBe(2);
      httpMock.verify();
    }));

    it('taskEvidences filtra solo las evidencias VALIDATED', fakeAsync(() => {
      const { fixture, component, httpMock } = buildComponent();
      fixture.detectChanges();

      const allEvidences = [
        { id: 1, status: 'VALIDATED' },
        { id: 2, status: 'PENDING'   },   // debe ignorarse
        { id: 3, status: 'VALIDATED' }
      ];
      flushLoad(httpMock, FAMILY_ID, [], [], allEvidences);
      tick();

      expect(component.taskEvidences.length).toBe(2);
      expect(component.taskEvidences.every((e: any) => e.status === 'VALIDATED')).toBeTrue();
      httpMock.verify();
    }));

    it('error en GET /checklist → loading=false (sin propagación de error)', fakeAsync(() => {
      const { fixture, component, httpMock } = buildComponent();
      fixture.detectChanges();

      httpMock.expectOne(`${API_BASE}/checklist/family/${FAMILY_ID}`)
        .flush('Server Error', { status: 500, statusText: 'Internal Server Error' });
      httpMock.expectOne(`${API_BASE}/family-logbook/family/${FAMILY_ID}/status/RESOLVED`)
        .flush([]);
      httpMock.expectOne(`${API_BASE}/evidences/family/${FAMILY_ID}`)
        .flush({ data: [] });
      tick();

      expect(component.loading).toBeFalse();
      httpMock.verify();
    }));
  });

  // ═══════════════════════════════════════════════════════════════════════
  //  Computed getters
  // ═══════════════════════════════════════════════════════════════════════

  describe('Computed getters', () => {
    it('done cuenta los ítems completados', fakeAsync(() => {
      const { fixture, component, httpMock } = buildComponent();
      fixture.detectChanges();
      flushLoad(httpMock, FAMILY_ID, [
        { id: 1, completed: true  },
        { id: 2, completed: false },
        { id: 3, completed: true  }
      ]);
      tick();

      expect(component.done).toBe(2);
      httpMock.verify();
    }));

    it('pct calcula el porcentaje correcto (2 de 4 → 50)', fakeAsync(() => {
      const { fixture, component, httpMock } = buildComponent();
      fixture.detectChanges();
      flushLoad(httpMock, FAMILY_ID, [
        { id: 1, completed: true  },
        { id: 2, completed: false },
        { id: 3, completed: true  },
        { id: 4, completed: false }
      ]);
      tick();

      expect(component.pct).toBe(50);
      httpMock.verify();
    }));

    it('pct=0 cuando items está vacío (sin división por cero)', fakeAsync(() => {
      const { fixture, component, httpMock } = buildComponent();
      fixture.detectChanges();
      flushLoad(httpMock);
      tick();

      expect(component.pct).toBe(0);
      httpMock.verify();
    }));

    it('itemsByDimension agrupa ítems por dimensión y omite grupos vacíos', fakeAsync(() => {
      const { fixture, component, httpMock } = buildComponent();
      fixture.detectChanges();
      flushLoad(httpMock, FAMILY_ID, [
        { id: 1, description: 'A', completed: false, dimension: 'emociones'    },
        { id: 2, description: 'B', completed: false, dimension: 'emociones'    },
        { id: 3, description: 'C', completed: false, dimension: 'comunicacion' }
      ]);
      tick();

      const groups = component.itemsByDimension;
      const dims = groups.map(([key]) => key);
      expect(dims).toContain('emociones');
      expect(dims).toContain('comunicacion');
      // habitos / tiempos no tienen ítems → no deben aparecer
      expect(dims).not.toContain('habitos');
      expect(dims).not.toContain('tiempos');

      const emoGroup = groups.find(([k]) => k === 'emociones')![1];
      expect(emoGroup.length).toBe(2);
      httpMock.verify();
    }));

    it('dimensión desconocida → va al grupo "general"', fakeAsync(() => {
      const { fixture, component, httpMock } = buildComponent();
      fixture.detectChanges();
      flushLoad(httpMock, FAMILY_ID, [
        { id: 1, description: 'X', completed: false, dimension: 'otra_dimension' }
      ]);
      tick();

      const groups = component.itemsByDimension;
      const generalGroup = groups.find(([k]) => k === 'general');
      expect(generalGroup).toBeDefined();
      expect(generalGroup![1].length).toBe(1);
      httpMock.verify();
    }));
  });

  // ═══════════════════════════════════════════════════════════════════════
  //  toggle()
  // ═══════════════════════════════════════════════════════════════════════

  describe('toggle()', () => {
    it('ítem ya completado → NO hace PUT (solo marca completados hacia adelante)', fakeAsync(() => {
      const { fixture, component, httpMock } = buildComponent();
      fixture.detectChanges();
      flushLoad(httpMock, FAMILY_ID, [{ id: 1, completed: true }]);
      tick();

      component.toggle(1, true); // current=true → no-op

      httpMock.verify(); // sin petición adicional
    }));

    it('ítem pendiente → PUT /checklist/{id}/complete y recarga', fakeAsync(() => {
      const { fixture, component, httpMock } = buildComponent();
      fixture.detectChanges();
      flushLoad(httpMock, FAMILY_ID, [{ id: 7, completed: false }]);
      tick();

      component.toggle(7, false);

      httpMock.expectOne(`${API_BASE}/checklist/7/complete`).flush({});
      tick();

      // Tras el PUT se recarga la lista
      flushLoad(httpMock);
      tick();

      httpMock.verify();
    }));
  });
});
