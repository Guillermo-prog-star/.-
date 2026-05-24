import { TestBed, fakeAsync, tick, discardPeriodicTasks } from '@angular/core/testing';
import { Router, provideRouter } from '@angular/router';
import { NO_ERRORS_SCHEMA, signal } from '@angular/core';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting, HttpTestingController } from '@angular/common/http/testing';

import { PlanListPageComponent } from './plan-list-page.component';
import { FamilyStateService } from '../../core/services/family-state.service';
import { ApiService } from '../../core/services/api.service';
import { TelemetryService } from '../../core/services/telemetry.service';

// ─── Constants ───────────────────────────────────────────────────────────────

const FAMILY_ID = 5;
const API_BASE   = '/api';

// ─── Helpers ─────────────────────────────────────────────────────────────────

function buildComponent(signalFamilyId = FAMILY_ID) {
  const familyStateSpy = jasmine.createSpyObj<FamilyStateService>(
    'FamilyStateService', [],
    { currentFamilyId: signal(signalFamilyId) }
  );
  const telemetrySpy = jasmine.createSpyObj<TelemetryService>(
    'TelemetryService', ['logEvent']
  );

  TestBed.configureTestingModule({
    imports: [PlanListPageComponent],
    providers: [
      provideRouter([]),
      provideHttpClient(),
      provideHttpClientTesting(),
      { provide: FamilyStateService, useValue: familyStateSpy },
      { provide: ApiService,         useValue: { base: API_BASE } },
      { provide: TelemetryService,   useValue: telemetrySpy }
    ],
    schemas: [NO_ERRORS_SCHEMA]
  });

  const fixture   = TestBed.createComponent(PlanListPageComponent);
  const component = fixture.componentInstance;
  const httpMock  = TestBed.inject(HttpTestingController);

  return { fixture, component, httpMock, telemetrySpy };
}

/**
 * Flush the four parallel GET requests that ngOnInit fires when familyId > 0,
 * plus the secondary GET /evidences triggered after plans arrive.
 */
function flushInit(
  httpMock: HttpTestingController,
  familyId = FAMILY_ID,
  plans: any[] = []
) {
  httpMock.expectOne(`${API_BASE}/plans/family/${familyId}`)
    .flush({ data: plans });
  httpMock.expectOne(`${API_BASE}/milestones`)
    .flush([]);
  httpMock.expectOne(`${API_BASE}/analytics/dashboard/family/${familyId}`)
    .flush({ data: null });
  httpMock.expectOne(`${API_BASE}/members/family/${familyId}`)
    .flush({ data: [] });
  // Secondary call: loadFamilyEvidences() triggered after plans arrive
  httpMock.expectOne(`${API_BASE}/evidences/family/${familyId}`)
    .flush({ data: [] });
}

// ─────────────────────────────────────────────────────────────────────────────

describe('PlanListPageComponent', () => {
  afterEach(() => TestBed.resetTestingModule());

  // ═══════════════════════════════════════════════════════════════════════
  //  ngOnInit() / ngOnDestroy()
  // ═══════════════════════════════════════════════════════════════════════

  describe('ngOnInit() / ngOnDestroy()', () => {
    it('familyId=0 → no realiza peticiones HTTP ni activa el polling', fakeAsync(() => {
      const { fixture, component, httpMock } = buildComponent(0);
      fixture.detectChanges();
      tick();

      httpMock.expectNone(`${API_BASE}/plans/family/0`);
      httpMock.verify();
      expect(component.plans).toEqual([]);
    }));

    it('familyId>0 → realiza las 4+1 peticiones HTTP iniciales', fakeAsync(() => {
      const { fixture, component, httpMock } = buildComponent();
      fixture.detectChanges();

      flushInit(httpMock);
      tick();
      discardPeriodicTasks();

      expect(component.loading).toBeFalse();
    }));

    it('ngOnDestroy() → cancela el intervalo de polling (no más peticiones)', fakeAsync(() => {
      const { fixture, component, httpMock } = buildComponent();
      fixture.detectChanges();
      flushInit(httpMock);
      tick();

      fixture.destroy(); // dispara ngOnDestroy

      // Avanzar el tiempo: el intervalo ya no debe dispararse
      tick(1500);
      // No debe haber nuevas peticiones
      httpMock.expectNone(`${API_BASE}/plans/family/${FAMILY_ID}`);
      httpMock.verify();
      // El intervalo fue cancelado — plans permanece como fue cargado (vacío en este test)
      expect(component.plans).toEqual([]);
    }));
  });

  // ═══════════════════════════════════════════════════════════════════════
  //  selectTask()
  // ═══════════════════════════════════════════════════════════════════════

  describe('selectTask()', () => {
    let component: PlanListPageComponent;
    let httpMock: HttpTestingController;

    beforeEach(fakeAsync(() => {
      const ctx = buildComponent();
      ctx.fixture.detectChanges();
      flushInit(ctx.httpMock);
      tick();
      discardPeriodicTasks();
      component = ctx.component;
      httpMock  = ctx.httpMock;
    }));

    it('selecciona una tarea nueva', () => {
      const event = new Event('click');
      spyOn(event, 'stopPropagation');
      component.selectTask(7, event);
      expect(component.selectedTaskId).toBe(7);
    });

    it('deselecciona la tarea si ya estaba seleccionada (toggle)', () => {
      const event = new Event('click');
      spyOn(event, 'stopPropagation');
      component.selectTask(7, event);
      component.selectTask(7, event);
      expect(component.selectedTaskId).toBeNull();
    });

    it('llama stopPropagation para evitar propagación del evento', () => {
      const event = new Event('click');
      const spy = spyOn(event, 'stopPropagation');
      component.selectTask(3, event);
      expect(spy).toHaveBeenCalled();
    });
  });

  // ═══════════════════════════════════════════════════════════════════════
  //  toggleCli()
  // ═══════════════════════════════════════════════════════════════════════

  describe('toggleCli()', () => {
    it('alterna isCliCollapsed', fakeAsync(() => {
      const { fixture, component, httpMock } = buildComponent();
      fixture.detectChanges();
      flushInit(httpMock);
      tick();
      discardPeriodicTasks();

      expect(component.isCliCollapsed).toBeFalse();
      component.toggleCli(); // false→true: no setTimeout
      expect(component.isCliCollapsed).toBeTrue();
      component.toggleCli(); // true→false: dispara setTimeout(150)
      expect(component.isCliCollapsed).toBeFalse();
      tick(150); // drena el setTimeout de foco del input
    }));
  });

  // ═══════════════════════════════════════════════════════════════════════
  //  isFormValid()
  // ═══════════════════════════════════════════════════════════════════════

  describe('isFormValid()', () => {
    let component: PlanListPageComponent;

    beforeEach(fakeAsync(() => {
      const ctx = buildComponent();
      ctx.fixture.detectChanges();
      flushInit(ctx.httpMock);
      tick();
      discardPeriodicTasks();
      component = ctx.component;
    }));

    it('falso si title está vacío', () => {
      component.evidenceForm = {
        title: '', description: '', textContent: 'x', fileUrl: '',
        evidenceType: 'BITACORA', submittedBy: 'Ana', feelingEmoji: ''
      };
      expect(component.isFormValid()).toBeFalse();
    });

    it('falso si submittedBy está vacío', () => {
      component.evidenceForm = {
        title: 'Título', description: '', textContent: 'x', fileUrl: '',
        evidenceType: 'BITACORA', submittedBy: '', feelingEmoji: ''
      };
      expect(component.isFormValid()).toBeFalse();
    });

    it('BITACORA sin textContent → falso', () => {
      component.evidenceForm = {
        title: 'T', description: '', textContent: '', fileUrl: '',
        evidenceType: 'BITACORA', submittedBy: 'Ana', feelingEmoji: ''
      };
      expect(component.isFormValid()).toBeFalse();
    });

    it('PHOTO sin fileUrl → falso', () => {
      component.evidenceForm = {
        title: 'T', description: '', textContent: '', fileUrl: '',
        evidenceType: 'PHOTO', submittedBy: 'Ana', feelingEmoji: ''
      };
      expect(component.isFormValid()).toBeFalse();
    });

    it('BITACORA con todos los campos → verdadero', () => {
      component.evidenceForm = {
        title: 'T', description: '', textContent: 'contenido', fileUrl: '',
        evidenceType: 'BITACORA', submittedBy: 'Ana', feelingEmoji: ''
      };
      expect(component.isFormValid()).toBeTrue();
    });

    it('PHOTO con fileUrl → verdadero', () => {
      component.evidenceForm = {
        title: 'T', description: '', textContent: '', fileUrl: 'https://img.jpg',
        evidenceType: 'PHOTO', submittedBy: 'Ana', feelingEmoji: ''
      };
      expect(component.isFormValid()).toBeTrue();
    });
  });

  // ═══════════════════════════════════════════════════════════════════════
  //  openEvidenceModal() / closeEvidenceModal()
  // ═══════════════════════════════════════════════════════════════════════

  describe('openEvidenceModal() / closeEvidenceModal()', () => {
    let component: PlanListPageComponent;

    beforeEach(fakeAsync(() => {
      const ctx = buildComponent();
      ctx.fixture.detectChanges();
      flushInit(ctx.httpMock);
      tick();
      discardPeriodicTasks();
      component = ctx.component;
    }));

    it('openEvidenceModal() abre el modal y asigna la tarea activa', () => {
      const task = { id: 12, title: 'Tarea de prueba' };
      component.openEvidenceModal(task, 'BITACORA');

      expect(component.isEvidenceModalOpen).toBeTrue();
      expect(component.activeModalTask).toBe(task);
    });

    it('openEvidenceModal() inicializa el formulario con el tipo indicado', () => {
      const task = { id: 12, title: 'Tarea de prueba' };
      component.openEvidenceModal(task, 'PHOTO');

      expect(component.evidenceForm.evidenceType).toBe('PHOTO');
      expect(component.evidenceForm.title).toContain('Tarea de prueba');
    });

    it('closeEvidenceModal() cierra el modal y limpia la tarea activa', () => {
      component.openEvidenceModal({ id: 1, title: 'X' }, 'BITACORA');
      component.closeEvidenceModal();

      expect(component.isEvidenceModalOpen).toBeFalse();
      expect(component.activeModalTask).toBeNull();
    });
  });

  // ═══════════════════════════════════════════════════════════════════════
  //  setFeelingEmoji() / getTaskEvidence()
  // ═══════════════════════════════════════════════════════════════════════

  describe('setFeelingEmoji() / getTaskEvidence()', () => {
    let component: PlanListPageComponent;

    beforeEach(fakeAsync(() => {
      const ctx = buildComponent();
      ctx.fixture.detectChanges();
      flushInit(ctx.httpMock);
      tick();
      discardPeriodicTasks();
      component = ctx.component;
    }));

    it('setFeelingEmoji() asigna el emoji al formulario', () => {
      component.setFeelingEmoji('😊');
      expect(component.evidenceForm.feelingEmoji).toBe('😊');
    });

    it('getTaskEvidence() retorna la evidencia cuyo task.id coincide', () => {
      component.evidences = [
        { id: 1, task: { id: 5 }, title: 'Ev A' },
        { id: 2, task: { id: 9 }, title: 'Ev B' }
      ];
      expect(component.getTaskEvidence(9)?.title).toBe('Ev B');
    });

    it('getTaskEvidence() retorna undefined si no hay coincidencia', () => {
      component.evidences = [{ id: 1, task: { id: 5 } }];
      expect(component.getTaskEvidence(99)).toBeUndefined();
    });
  });

  // ═══════════════════════════════════════════════════════════════════════
  //  getEmoji()
  // ═══════════════════════════════════════════════════════════════════════

  describe('getEmoji()', () => {
    let component: PlanListPageComponent;

    beforeEach(fakeAsync(() => {
      const ctx = buildComponent();
      ctx.fixture.detectChanges();
      flushInit(ctx.httpMock);
      tick();
      discardPeriodicTasks();
      component = ctx.component;
    }));

    it('iconos conocidos → emoji correcto', () => {
      expect(component.getEmoji('palette')).toBe('🎨');
      expect(component.getEmoji('psychology')).toBe('🧠');
      expect(component.getEmoji('assignment')).toBe('📋');
      expect(component.getEmoji('done_all')).toBe('✅');
    });

    it('icono desconocido → ✨ (fallback)', () => {
      expect(component.getEmoji('unknown_icon')).toBe('✨');
    });
  });

  // ═══════════════════════════════════════════════════════════════════════
  //  getDimensionColor() / getDimensionBg()
  // ═══════════════════════════════════════════════════════════════════════

  describe('getDimensionColor() / getDimensionBg()', () => {
    let component: PlanListPageComponent;

    beforeEach(fakeAsync(() => {
      const ctx = buildComponent();
      ctx.fixture.detectChanges();
      flushInit(ctx.httpMock);
      tick();
      discardPeriodicTasks();
      component = ctx.component;
    }));

    it('emociones → rosa / fondo rosa', () => {
      expect(component.getDimensionColor('emociones')).toBe('#fb7185');
      expect(component.getDimensionBg('emociones')).toContain('251, 113, 133');
    });

    it('comunicacion → azul claro', () => {
      expect(component.getDimensionColor('comunicacion')).toBe('#38bdf8');
    });

    it('habitos → ámbar', () => {
      expect(component.getDimensionColor('habitos')).toBe('#fbbf24');
    });

    it('dimensión desconocida → fallback gris', () => {
      expect(component.getDimensionColor('otro')).toBe('#94a3b8');
    });

    it('aceptar mayúsculas (toLowerCase)', () => {
      expect(component.getDimensionColor('EMOCIONES')).toBe('#fb7185');
    });
  });

  // ═══════════════════════════════════════════════════════════════════════
  //  activePillar getter
  // ═══════════════════════════════════════════════════════════════════════

  describe('activePillar getter', () => {
    let component: PlanListPageComponent;

    beforeEach(fakeAsync(() => {
      const ctx = buildComponent();
      ctx.fixture.detectChanges();
      flushInit(ctx.httpMock);
      tick();
      discardPeriodicTasks();
      component = ctx.component;
    }));

    it('W1 → RECONOCIMIENTO', () => {
      component.familyDashboard = { currentMilestone: 'W1' };
      expect(component.activePillar).toBe('RECONOCIMIENTO');
    });

    it('M1 → RECONOCIMIENTO', () => {
      component.familyDashboard = { currentMilestone: 'M1' };
      expect(component.activePillar).toBe('RECONOCIMIENTO');
    });

    it('M6 → AMOR', () => {
      component.familyDashboard = { currentMilestone: 'M6' };
      expect(component.activePillar).toBe('AMOR');
    });

    it('M18 → ENTREGA', () => {
      component.familyDashboard = { currentMilestone: 'M18' };
      expect(component.activePillar).toBe('ENTREGA');
    });

    it('null dashboard → RECONOCIMIENTO (fallback W1)', () => {
      component.familyDashboard = null;
      expect(component.activePillar).toBe('RECONOCIMIENTO');
    });
  });

  // ═══════════════════════════════════════════════════════════════════════
  //  planPct() / getDashOffset() / getActivePillarTasks()
  // ═══════════════════════════════════════════════════════════════════════

  describe('planPct() / getDashOffset() / getActivePillarTasks()', () => {
    let component: PlanListPageComponent;

    beforeEach(fakeAsync(() => {
      const ctx = buildComponent();
      ctx.fixture.detectChanges();
      flushInit(ctx.httpMock);
      tick();
      discardPeriodicTasks();
      component = ctx.component;
      component.familyDashboard = { currentMilestone: 'W1' }; // activePillar=RECONOCIMIENTO
    }));

    it('planPct() → 50 cuando la mitad de las tareas del pilar está completada', () => {
      const plan: any = {
        tasks: [
          { fase: 'RECONOCIMIENTO', completed: true },
          { fase: 'RECONOCIMIENTO', completed: false }
        ]
      };
      expect(component.planPct(plan)).toBe(50);
    });

    it('planPct() → 0 cuando no hay tareas en el pilar activo', () => {
      const plan: any = { tasks: [] };
      expect(component.planPct(plan)).toBe(0);
    });

    it('getDashOffset() → circunferencia completa cuando pct=0', () => {
      const plan: any = { tasks: [] };
      const circumference = 2 * Math.PI * 25;
      expect(component.getDashOffset(plan)).toBeCloseTo(circumference, 1);
    });

    it('getDashOffset() → 0 cuando todas las tareas están completas', () => {
      const plan: any = {
        tasks: [{ fase: 'RECONOCIMIENTO', completed: true }]
      };
      expect(component.getDashOffset(plan)).toBeCloseTo(0, 1);
    });

    it('getActivePillarTasks() filtra tareas por fase del pilar activo', () => {
      const tasks = [
        { fase: 'RECONOCIMIENTO', title: 'T1' },
        { fase: 'AMOR', title: 'T2' },
        { fase: 'RECONOCIMIENTO', title: 'T3' }
      ];
      const result = component.getActivePillarTasks(tasks);
      expect(result.length).toBe(2);
      expect(result.every(t => t.fase === 'RECONOCIMIENTO')).toBeTrue();
    });

    it('getActivePillarTasks() → [] cuando tasks es null', () => {
      expect(component.getActivePillarTasks(null as any)).toEqual([]);
    });
  });

  // ═══════════════════════════════════════════════════════════════════════
  //  getMilestoneMonthLabel() / getMilestoneEmoji()
  // ═══════════════════════════════════════════════════════════════════════

  describe('getMilestoneMonthLabel() / getMilestoneEmoji()', () => {
    let component: PlanListPageComponent;

    beforeEach(fakeAsync(() => {
      const ctx = buildComponent();
      ctx.fixture.detectChanges();
      flushInit(ctx.httpMock);
      tick();
      discardPeriodicTasks();
      component = ctx.component;
    }));

    it('W1 → S1 (semana)', () => {
      expect(component.getMilestoneMonthLabel('W1')).toBe('S1');
    });

    it('M3 → M3 (mes)', () => {
      expect(component.getMilestoneMonthLabel('M3')).toBe('M3');
    });

    it('M12 → 🏆', () => {
      expect(component.getMilestoneEmoji('M12')).toBe('🏆');
    });

    it('código desconocido → ⬜ (fallback)', () => {
      expect(component.getMilestoneEmoji('X99')).toBe('⬜');
    });
  });

  // ═══════════════════════════════════════════════════════════════════════
  //  toggle()
  // ═══════════════════════════════════════════════════════════════════════

  describe('toggle()', () => {
    it('envía PUT /plans/tasks/{id}/complete y recarga planes', fakeAsync(() => {
      const { fixture, component, httpMock } = buildComponent();
      fixture.detectChanges();
      flushInit(httpMock);
      tick();

      component.toggle(33, true);

      const req = httpMock.expectOne(`${API_BASE}/plans/tasks/33/complete`);
      expect(req.request.method).toBe('PUT');
      expect(req.request.body).toEqual({ completed: true });
      req.flush({});

      // load() + loadDashboard() disparan más peticiones
      httpMock.expectOne(`${API_BASE}/plans/family/${FAMILY_ID}`).flush({ data: [] });
      httpMock.expectOne(`${API_BASE}/analytics/dashboard/family/${FAMILY_ID}`).flush({});
      httpMock.expectOne(`${API_BASE}/evidences/family/${FAMILY_ID}`).flush({ data: [] });
      tick();
      discardPeriodicTasks();
    }));
  });

  // ═══════════════════════════════════════════════════════════════════════
  //  onCommand() — CLI parser
  // ═══════════════════════════════════════════════════════════════════════

  describe('onCommand()', () => {
    let component: PlanListPageComponent;

    beforeEach(fakeAsync(() => {
      const ctx = buildComponent();
      ctx.fixture.detectChanges();
      flushInit(ctx.httpMock);
      tick();
      discardPeriodicTasks();
      component = ctx.component;
    }));

    it('comando vacío → no agrega nada a terminalLogs', () => {
      component.onCommand('   ');
      expect(component.terminalLogs).toEqual([]);
    });

    it('clear → vacía terminalLogs', () => {
      component.terminalLogs = ['log1', 'log2'];
      component.onCommand('clear');
      expect(component.terminalLogs).toEqual([]);
    });

    it('limpiar → vacía terminalLogs (alias español)', () => {
      component.terminalLogs = ['x'];
      component.onCommand('limpiar');
      expect(component.terminalLogs).toEqual([]);
    });

    it('help → agrega listado de comandos disponibles', () => {
      component.onCommand('help');
      expect(component.terminalLogs.some(l => l.includes('COMANDOS'))).toBeTrue();
    });

    it('comando con comillas → las elimina antes de parsear', () => {
      // 'clear' → debería limpiarse igual que clear sin comillas
      component.terminalLogs = ['previo'];
      component.onCommand("'clear'");
      expect(component.terminalLogs).toEqual([]);
    });
  });
});
