import { TestBed, fakeAsync, tick } from '@angular/core/testing';
import { Router, provideRouter } from '@angular/router';
import { NO_ERRORS_SCHEMA, signal } from '@angular/core';
import { of, throwError } from 'rxjs';

import { EvaluationHistoryPageComponent } from './evaluation-history-page.component';
import { AssessmentService } from '../../core/services/assessment.service';
import { FamilyStateService } from '../../core/services/family-state.service';
import { EvaluationHistory, TimelineEntryDto } from '../../core/models/models';

// ─── Helpers ────────────────────────────────────────────────────────────────

const FAMILY_ID = 42;

function buildComponent(signalFamilyId = FAMILY_ID) {
  const familyStateSpy = jasmine.createSpyObj<FamilyStateService>(
    'FamilyStateService', [],
    { currentFamilyId: signal(signalFamilyId) }
  );
  const assessmentSpy = jasmine.createSpyObj<AssessmentService>(
    'AssessmentService', {
      getHistory:  of([] as EvaluationHistory[]),
      getTimeline: of([] as TimelineEntryDto[])
    }
  );

  TestBed.configureTestingModule({
    imports: [EvaluationHistoryPageComponent],
    providers: [
      provideRouter([]),
      { provide: AssessmentService, useValue: assessmentSpy },
      { provide: FamilyStateService, useValue: familyStateSpy }
    ],
    schemas: [NO_ERRORS_SCHEMA]
  });

  const fixture   = TestBed.createComponent(EvaluationHistoryPageComponent);
  const component = fixture.componentInstance;
  const router    = TestBed.inject(Router);
  spyOn(router, 'navigate').and.returnValue(Promise.resolve(true));

  return { fixture, component, router, assessmentSpy };
}

// ─────────────────────────────────────────────────────────────────────────────

describe('EvaluationHistoryPageComponent', () => {
  afterEach(() => TestBed.resetTestingModule());

  // ═══════════════════════════════════════════════════════════════════════
  //  ngOnInit()
  // ═══════════════════════════════════════════════════════════════════════

  describe('ngOnInit()', () => {
    it('familyId=0 → navega a /families sin llamar a AssessmentService', fakeAsync(() => {
      const { fixture, router, assessmentSpy } = buildComponent(0);
      fixture.detectChanges();
      tick();

      expect(router.navigate).toHaveBeenCalledWith(['/families']);
      expect(assessmentSpy.getHistory).not.toHaveBeenCalled();
    }));

    it('familyId>0 → llama getHistory y getTimeline con el familyId correcto', fakeAsync(() => {
      const { fixture, assessmentSpy } = buildComponent();
      fixture.detectChanges();
      tick();

      expect(assessmentSpy.getHistory).toHaveBeenCalledWith(FAMILY_ID);
      expect(assessmentSpy.getTimeline).toHaveBeenCalledWith(FAMILY_ID);
    }));

    it('error en getHistory → history permanece vacío (catchError silencia el error)', fakeAsync(() => {
      const { fixture, component, assessmentSpy } = buildComponent();
      assessmentSpy.getHistory.and.returnValue(throwError(() => new Error('500')));
      fixture.detectChanges();
      tick();

      expect(component.history()).toEqual([]);
    }));

    it('loading se establece en false tras recibir timeline', fakeAsync(() => {
      const { fixture, component, assessmentSpy } = buildComponent();
      assessmentSpy.getTimeline.and.returnValue(of([]));
      fixture.detectChanges();
      tick();

      expect(component.loading()).toBeFalse();
    }));
  });

  // ═══════════════════════════════════════════════════════════════════════
  //  Computed signals
  // ═══════════════════════════════════════════════════════════════════════

  describe('Computed signals', () => {
    const historyData: EvaluationHistory[] = [
      { id: 1, familyId: 42, memberId: null, status: 'FINALIZED', startedAt: '2026-01-10T10:00:00', finalizedAt: '2026-01-12T10:00:00', icf: 75 },
      { id: 2, familyId: 42, memberId: null, status: 'STARTED',   startedAt: '2026-02-01T10:00:00', finalizedAt: null },
      { id: 3, familyId: 42, memberId: null, status: 'FINALIZED', startedAt: '2025-12-01T10:00:00', finalizedAt: '2025-12-05T10:00:00', icf: 50 }
    ];

    it('finalized() → solo evaluaciones FINALIZED, ordenadas más reciente primero', fakeAsync(() => {
      const { fixture, component, assessmentSpy } = buildComponent();
      assessmentSpy.getHistory.and.returnValue(of(historyData));
      fixture.detectChanges();
      tick();

      const fin = component.finalized();
      expect(fin.length).toBe(2);
      expect(fin[0].id).toBe(1);   // 2026-01-12 > 2025-12-05
      expect(fin[1].id).toBe(3);
    }));

    it('started() → solo evaluaciones STARTED', fakeAsync(() => {
      const { fixture, component, assessmentSpy } = buildComponent();
      assessmentSpy.getHistory.and.returnValue(of(historyData));
      fixture.detectChanges();
      tick();

      expect(component.started().length).toBe(1);
      expect(component.started()[0].id).toBe(2);
    }));

    it('sparklinePoints() → "" cuando hay menos de 2 puntos', fakeAsync(() => {
      const { fixture, component, assessmentSpy } = buildComponent();
      assessmentSpy.getTimeline.and.returnValue(of([
        { evaluationId: 1, finalizedAt: '2026-01-01', healthyIndex: 70, riskLevel: 'BAJO' }
      ]));
      fixture.detectChanges();
      tick();

      expect(component.sparklinePoints()).toBe('');
    }));

    it('sparklinePoints() → cadena SVG con N coordenadas cuando hay ≥2 puntos', fakeAsync(() => {
      const timeline: TimelineEntryDto[] = [
        { evaluationId: 1, finalizedAt: '2026-01-01T00:00:00', healthyIndex: 80, riskLevel: 'BAJO' },
        { evaluationId: 2, finalizedAt: '2026-02-01T00:00:00', healthyIndex: 60, riskLevel: 'MODERADO' },
        { evaluationId: 3, finalizedAt: '2026-03-01T00:00:00', healthyIndex: 45, riskLevel: 'ALTO' }
      ];
      const { fixture, component, assessmentSpy } = buildComponent();
      assessmentSpy.getTimeline.and.returnValue(of(timeline));
      fixture.detectChanges();
      tick();

      const pts = component.sparklinePoints();
      expect(pts).not.toBe('');
      // 3 puntos separados por espacios
      expect(pts.split(' ').length).toBe(3);
    }));
  });

  // ═══════════════════════════════════════════════════════════════════════
  //  Helpers de presentación
  // ═══════════════════════════════════════════════════════════════════════

  describe('Helpers de presentación', () => {
    let component: EvaluationHistoryPageComponent;

    beforeEach(fakeAsync(() => {
      const ctx = buildComponent();
      ctx.fixture.detectChanges();
      tick();
      component = ctx.component;
    }));

    it('riskLabel() mapea claves conocidas', () => {
      expect(component.riskLabel('BAJO')).toBe('Bajo');
      expect(component.riskLabel('MODERADO')).toBe('Moderado');
      expect(component.riskLabel('ALTO')).toBe('Alto');
      expect(component.riskLabel('CRITICO')).toBe('Crítico');
    });

    it('riskLabel() devuelve la clave tal cual si es desconocida', () => {
      expect(component.riskLabel('OTRO')).toBe('OTRO');
      expect(component.riskLabel(null)).toBe('—');
    });

    it('riskBadgeClass() devuelve la clase CSS correcta', () => {
      expect(component.riskBadgeClass('BAJO')).toBe('badge-bajo');
      expect(component.riskBadgeClass('CRITICO')).toBe('badge-critico');
      expect(component.riskBadgeClass(null)).toBe('badge-default');
    });

    it('statusLabel() traduce FINALIZED y STARTED', () => {
      expect(component.statusLabel('FINALIZED')).toBe('Finalizada');
      expect(component.statusLabel('STARTED')).toBe('En curso');
      expect(component.statusLabel('OTRA')).toBe('OTRA');
    });

    it('formatDate() devuelve "—" para null/undefined', () => {
      expect(component.formatDate(null)).toBe('—');
      expect(component.formatDate(undefined)).toBe('—');
    });

    it('icfColor() y icfBg() respetan los umbrales 70/40', () => {
      expect(component.icfColor(75)).toBe('#065F46');
      expect(component.icfColor(50)).toBe('#92400E');
      expect(component.icfColor(30)).toBe('#991B1B');
      expect(component.icfBg(75)).toBe('#D1FAE5');
      expect(component.icfBg(50)).toBe('#FEF3C7');
      expect(component.icfBg(null)).toBe('#FEE2E2');  // null → 0 → rojo
    });
  });

  // ═══════════════════════════════════════════════════════════════════════
  //  Navegación
  // ═══════════════════════════════════════════════════════════════════════

  describe('Navegación', () => {
    it('viewResult() navega a /evaluations/{id}/result', fakeAsync(() => {
      const { fixture, component, router } = buildComponent();
      fixture.detectChanges();
      tick();

      component.viewResult(10);
      expect(router.navigate).toHaveBeenCalledWith(['/evaluations', 10, 'result']);
    }));

    it('resumeEval() navega a /evaluations/{id}/form', fakeAsync(() => {
      const { fixture, component, router } = buildComponent();
      fixture.detectChanges();
      tick();

      component.resumeEval(5);
      expect(router.navigate).toHaveBeenCalledWith(['/evaluations', 5, 'form']);
    }));

    it('startNew() navega a /evaluations/start', fakeAsync(() => {
      const { fixture, component, router } = buildComponent();
      fixture.detectChanges();
      tick();

      component.startNew();
      expect(router.navigate).toHaveBeenCalledWith(['/evaluations/start']);
    }));
  });
});
