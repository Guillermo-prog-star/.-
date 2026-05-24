import { TestBed, fakeAsync, tick } from '@angular/core/testing';
import { NO_ERRORS_SCHEMA } from '@angular/core';
import { of, throwError, Subject } from 'rxjs';

import { CrisisPageComponent } from './crisis-page.component';
import { CrisisService } from '../../core/services/crisis.service';
import { FamilyStateService } from '../../core/services/family-state.service';

// ─── Helpers ────────────────────────────────────────────────────────────────

const FAMILY_ID = 10;

const HISTORY_STUB = [
  { id: 1, category: 'Ruptura de Diálogo', description: 'desc1', createdAt: '2026-01-01T10:00:00', aiContainmentGuide: 'Guía 1' },
  { id: 2, category: 'Emergencia Emocional', description: 'desc2', createdAt: '2026-01-02T10:00:00', aiContainmentGuide: 'Guía 2' }
];

function buildComponent(familyId: number | null = FAMILY_ID) {
  const familyStateSpy = jasmine.createSpyObj<FamilyStateService>(
    'FamilyStateService',
    { getSelectedFamilyId: familyId as unknown as number }
  );

  const crisisServiceSpy = jasmine.createSpyObj<CrisisService>(
    'CrisisService', {
      getHistory:   of(HISTORY_STUB),
      reportCrisis: of({ id: 99, category: 'Conflicto de Convivencia', aiContainmentGuide: 'Guía OK' })
    }
  );

  TestBed.configureTestingModule({
    imports: [CrisisPageComponent],
    providers: [
      { provide: CrisisService,     useValue: crisisServiceSpy },
      { provide: FamilyStateService, useValue: familyStateSpy }
    ],
    schemas: [NO_ERRORS_SCHEMA]
  });

  const fixture   = TestBed.createComponent(CrisisPageComponent);
  const component = fixture.componentInstance;

  return { fixture, component, crisisServiceSpy, familyStateSpy };
}

// ─────────────────────────────────────────────────────────────────────────────

describe('CrisisPageComponent', () => {
  afterEach(() => TestBed.resetTestingModule());

  // ═══════════════════════════════════════════════════════════════════════
  //  ngOnInit() / loadHistory()
  // ═══════════════════════════════════════════════════════════════════════

  describe('ngOnInit() / loadHistory()', () => {
    it('familyId presente → llama getHistory y puebla history', fakeAsync(() => {
      const { fixture, component, crisisServiceSpy } = buildComponent();
      fixture.detectChanges();
      tick();

      expect(crisisServiceSpy.getHistory).toHaveBeenCalledWith(FAMILY_ID);
      expect(component.history.length).toBe(2);
      expect(component.history[0].id).toBe(1);
    }));

    it('familyId null → no llama getHistory, history permanece vacío', fakeAsync(() => {
      const { fixture, component, crisisServiceSpy } = buildComponent(null);
      fixture.detectChanges();
      tick();

      expect(crisisServiceSpy.getHistory).not.toHaveBeenCalled();
      expect(component.history).toEqual([]);
    }));

    it('familyId=0 (falsy) → no llama getHistory', fakeAsync(() => {
      const { fixture, component, crisisServiceSpy } = buildComponent(0);
      fixture.detectChanges();
      tick();

      expect(crisisServiceSpy.getHistory).not.toHaveBeenCalled();
      expect(component.history).toEqual([]);
    }));
  });

  // ═══════════════════════════════════════════════════════════════════════
  //  selectEmotion()
  // ═══════════════════════════════════════════════════════════════════════

  describe('selectEmotion()', () => {
    it('asigna la emoción seleccionada a crisis.emotion', fakeAsync(() => {
      const { fixture, component } = buildComponent();
      fixture.detectChanges();
      tick();

      component.selectEmotion('Ira ⚡');
      expect(component.crisis.emotion).toBe('Ira ⚡');
    }));

    it('sobrescribe una emoción previa', fakeAsync(() => {
      const { fixture, component } = buildComponent();
      fixture.detectChanges();
      tick();

      component.selectEmotion('Tristeza 💧');
      component.selectEmotion('Frustración 🌀');
      expect(component.crisis.emotion).toBe('Frustración 🌀');
    }));
  });

  // ═══════════════════════════════════════════════════════════════════════
  //  resetChecklist()
  // ═══════════════════════════════════════════════════════════════════════

  describe('resetChecklist()', () => {
    it('pone los tres pasos en false', fakeAsync(() => {
      const { fixture, component } = buildComponent();
      fixture.detectChanges();
      tick();

      component.step1Ticked = true;
      component.step2Ticked = true;
      component.step3Ticked = true;

      component.resetChecklist();

      expect(component.step1Ticked).toBeFalse();
      expect(component.step2Ticked).toBeFalse();
      expect(component.step3Ticked).toBeFalse();
    }));
  });

  // ═══════════════════════════════════════════════════════════════════════
  //  selectHistoryItem()
  // ═══════════════════════════════════════════════════════════════════════

  describe('selectHistoryItem()', () => {
    it('asigna lastResponse y resetea el checklist', fakeAsync(() => {
      const { fixture, component } = buildComponent();
      fixture.detectChanges();
      tick();

      component.step1Ticked = true;
      const item = HISTORY_STUB[1];
      component.selectHistoryItem(item);

      expect(component.lastResponse).toBe(item);
      expect(component.step1Ticked).toBeFalse();
    }));
  });

  // ═══════════════════════════════════════════════════════════════════════
  //  getCategoryColor()
  // ═══════════════════════════════════════════════════════════════════════

  describe('getCategoryColor()', () => {
    let component: CrisisPageComponent;

    beforeEach(fakeAsync(() => {
      const ctx = buildComponent();
      ctx.fixture.detectChanges();
      tick();
      component = ctx.component;
    }));

    it('Emergencia Emocional → rojo', () => {
      expect(component.getCategoryColor('Emergencia Emocional')).toBe('#ef4444');
    });

    it('Crisis de Autoridad → púrpura', () => {
      expect(component.getCategoryColor('Crisis de Autoridad')).toBe('#a855f7');
    });

    it('Tensión Financiera → ámbar', () => {
      expect(component.getCategoryColor('Tensión Financiera')).toBe('#f59e0b');
    });

    it('Ruptura de Diálogo → azul', () => {
      expect(component.getCategoryColor('Ruptura de Diálogo')).toBe('#3b82f6');
    });

    it('Conflicto de Convivencia → sky blue', () => {
      expect(component.getCategoryColor('Conflicto de Convivencia')).toBe('#0ea5e9');
    });

    it('categoría desconocida → índigo (fallback)', () => {
      expect(component.getCategoryColor('Otro')).toBe('#6366f1');
    });
  });

  // ═══════════════════════════════════════════════════════════════════════
  //  submitCrisis()
  // ═══════════════════════════════════════════════════════════════════════

  describe('submitCrisis()', () => {
    it('familyId=null → no llama reportCrisis', fakeAsync(() => {
      const { fixture, component, crisisServiceSpy } = buildComponent(null);
      fixture.detectChanges();
      tick();

      component.submitCrisis();
      tick();

      expect(crisisServiceSpy.reportCrisis).not.toHaveBeenCalled();
    }));

    it('familyId presente → llama reportCrisis con el payload correcto', fakeAsync(() => {
      const { fixture, component, crisisServiceSpy } = buildComponent();
      fixture.detectChanges();
      tick();

      component.crisis = { category: 'Tensión Financiera', emotion: 'Ansiedad', description: 'Sin dinero' };
      component.submitCrisis();
      tick();

      expect(crisisServiceSpy.reportCrisis).toHaveBeenCalledWith({
        category: 'Tensión Financiera',
        emotion: 'Ansiedad',
        description: 'Sin dinero',
        familyId: FAMILY_ID
      });
    }));

    it('éxito → asigna lastResponse, resetea el form, recarga historial', fakeAsync(() => {
      const { fixture, component, crisisServiceSpy } = buildComponent();
      fixture.detectChanges();
      tick();

      crisisServiceSpy.reportCrisis.and.returnValue(of({
        id: 7, category: 'Ruptura de Diálogo', aiContainmentGuide: 'Respira y escucha.'
      }));
      component.crisis.description = 'Discusión fuerte';
      component.submitCrisis();
      tick();

      expect(component.lastResponse).toEqual(
        jasmine.objectContaining({ id: 7, category: 'Ruptura de Diálogo' })
      );
      expect(component.crisis.description).toBe(''); // form reseteado
      expect(component.loading).toBeFalse();
      // loadHistory se llama de nuevo tras éxito
      expect(crisisServiceSpy.getHistory.calls.count()).toBeGreaterThan(1);
    }));

    it('loading=true durante la petición, false al completar', fakeAsync(() => {
      const { fixture, component, crisisServiceSpy } = buildComponent();
      fixture.detectChanges();
      tick();

      // Usamos un observable que no emite síncronamente
      const subj = new Subject<any>();
      crisisServiceSpy.reportCrisis.and.returnValue(subj.asObservable());

      component.crisis.description = 'desc';
      component.submitCrisis();
      expect(component.loading).toBeTrue();

      subj.next({ id: 1 });
      subj.complete();
      tick();
      expect(component.loading).toBeFalse();
    }));

    it('error en reportCrisis → loading=false, lastResponse no cambia', fakeAsync(() => {
      const { fixture, component, crisisServiceSpy } = buildComponent();
      fixture.detectChanges();
      tick();

      crisisServiceSpy.reportCrisis.and.returnValue(throwError(() => new Error('500')));
      component.crisis.description = 'desc';
      component.submitCrisis();
      tick();

      expect(component.loading).toBeFalse();
      expect(component.lastResponse).toBeNull();
    }));
  });

  // ═══════════════════════════════════════════════════════════════════════
  //  formatAiResponse()
  // ═══════════════════════════════════════════════════════════════════════

  describe('formatAiResponse()', () => {
    let component: CrisisPageComponent;

    beforeEach(fakeAsync(() => {
      const ctx = buildComponent();
      ctx.fixture.detectChanges();
      tick();
      component = ctx.component;
    }));

    it('texto vacío / null → retorna cadena vacía', () => {
      expect(component.formatAiResponse('')).toBe('');
      expect(component.formatAiResponse(null as any)).toBe('');
    });

    it('### título → <h4>', () => {
      expect(component.formatAiResponse('### Paso 1')).toContain('<h4>Paso 1</h4>');
    });

    it('## título → <h3>', () => {
      expect(component.formatAiResponse('## Sección')).toContain('<h3>Sección</h3>');
    });

    it('**texto** → <strong>', () => {
      expect(component.formatAiResponse('**importante**')).toContain('<strong>importante</strong>');
    });

    it('*texto* → <em>', () => {
      expect(component.formatAiResponse('*énfasis*')).toContain('<em>énfasis</em>');
    });

    it('- item de lista → <li>', () => {
      expect(component.formatAiResponse('- punto uno')).toContain('<li');
    });

    it('1. item numerado → <li> con decimal', () => {
      const html = component.formatAiResponse('1. Primer paso');
      expect(html).toContain('list-style-type: decimal');
    });

    it('salto de línea → <br>', () => {
      expect(component.formatAiResponse('línea1\nlínea2')).toContain('<br>');
    });
  });
});
