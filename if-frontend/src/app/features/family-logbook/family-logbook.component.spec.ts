import { TestBed, fakeAsync, tick } from '@angular/core/testing';
import { NO_ERRORS_SCHEMA } from '@angular/core';
import { provideRouter } from '@angular/router';
import { signal } from '@angular/core';
import { of, throwError } from 'rxjs';

import { FamilyLogbookComponent } from './family-logbook.component';
import { FamilyLogbookService } from './family-logbook.service';
import { SprintService } from './sprint.service';
import { AuthService, AuthUser } from '../../core/services/auth.service';
import { FamilyLogbookEntry } from './family-logbook.model';
import { SprintResponse, SprintMissionResponse } from './sprint.model';

// ─── Stubs ───────────────────────────────────────────────────────────────────

const FAMILY_ID = 7;

const USER_STUB: AuthUser = {
  token: 'jwt-token',
  fullName: 'William Test',
  email: 'william@test.com',
  role: 'ADMIN',
  familyId: FAMILY_ID,
  familyName: 'Familia Test'
};

const ENTRY_STUB: FamilyLogbookEntry = {
  id: 1,
  familyId: FAMILY_ID,
  situation: 'Conflicto en la cena',
  difficultyDetected: 'Gritos recurrentes',
  emotionIdentified: 'Enojo',
  understanding: 'Falta de comunicación',
  correctionAction: 'Acordar turnos para hablar',
  familyAgreement: 'Cenar sin celulares',
  status: 'OPEN',
  createdBy: 'William Test',
  createdAt: '2026-01-01T00:00:00'
};

const SPRINT_STUB: SprintResponse = {
  id: 10,
  familyId: FAMILY_ID,
  objective: 'Reducir discusiones',
  riskDimension: 'comunicacion',
  durationDays: 7,
  startDate: '2026-05-01',
  endDate: '2026-05-08',
  status: 'ACTIVE',
  missions: [],
  dailies: [],
  createdAt: '2026-05-01T00:00:00'
};

// ─── Helper ──────────────────────────────────────────────────────────────────

function buildComponent(user: AuthUser | null = USER_STUB) {
  const logSpy = jasmine.createSpyObj<FamilyLogbookService>(
    'FamilyLogbookService', {
      findByFamily:  of([]),
      getCorrelation: of({ data: null }),
      create:        of(ENTRY_STUB),
      resolve:       of({ ...ENTRY_STUB, status: 'RESOLVED' as const })
    }
  );

  const sprintSpy = jasmine.createSpyObj<SprintService>(
    'SprintService', {
      getActiveSprint:   of(null),
      getSprintHistory:  of([]),
      createSprint:      of(SPRINT_STUB),
      toggleMission:     of(SPRINT_STUB),
      submitDaily:       of({ id: 1, memberName: 'W', checkinDate: '', yesterdayText: '',
                              todayText: '', blockagesText: '', resolutionText: '', createdAt: '' }),
      closeSprint:       of(SPRINT_STUB)
    }
  );

  const authSpy = jasmine.createSpyObj<AuthService>(
    'AuthService', [], { user: signal(user) }
  );

  TestBed.configureTestingModule({
    imports: [FamilyLogbookComponent],
    providers: [
      provideRouter([]),
      { provide: FamilyLogbookService, useValue: logSpy },
      { provide: SprintService,        useValue: sprintSpy },
      { provide: AuthService,          useValue: authSpy }
    ],
    schemas: [NO_ERRORS_SCHEMA]
  });

  const fixture   = TestBed.createComponent(FamilyLogbookComponent);
  const component = fixture.componentInstance;

  return { fixture, component, logSpy, sprintSpy, authSpy };
}

// ─────────────────────────────────────────────────────────────────────────────

describe('FamilyLogbookComponent', () => {
  afterEach(() => TestBed.resetTestingModule());

  // ═══════════════════════════════════════════════════════════════════════════
  //  ngOnInit()
  // ═══════════════════════════════════════════════════════════════════════════

  describe('ngOnInit()', () => {
    it('user con familyId → carga entradas, correlación y sprint activo', fakeAsync(() => {
      const { fixture, logSpy, sprintSpy } = buildComponent();
      fixture.detectChanges();
      tick();

      expect(logSpy.findByFamily).toHaveBeenCalledWith(FAMILY_ID);
      expect(logSpy.getCorrelation).toHaveBeenCalledWith(FAMILY_ID);
      expect(sprintSpy.getActiveSprint).toHaveBeenCalledWith(FAMILY_ID);
    }));

    it('user sin familyId → error.set y sin llamar servicios', fakeAsync(() => {
      const { fixture, component, logSpy } = buildComponent({
        ...USER_STUB, familyId: undefined
      });
      fixture.detectChanges();
      tick();

      expect(logSpy.findByFamily).not.toHaveBeenCalled();
      expect(component.error()).toBeTruthy();
    }));

    it('user null → error.set y sin llamar servicios', fakeAsync(() => {
      const { fixture, component, logSpy } = buildComponent(null);
      fixture.detectChanges();
      tick();

      expect(logSpy.findByFamily).not.toHaveBeenCalled();
      expect(component.error()).toBeTruthy();
    }));

    it('ngOnInit rellena authorName → form.createdBy = fullName del usuario', fakeAsync(() => {
      const { fixture, component } = buildComponent();
      fixture.detectChanges();
      tick();

      expect(component.form().createdBy).toBe('William Test');
    }));
  });

  // ═══════════════════════════════════════════════════════════════════════════
  //  setTab()
  // ═══════════════════════════════════════════════════════════════════════════

  describe('setTab()', () => {
    it('LOGBOOK → activeTab=LOGBOOK y recarga entradas', fakeAsync(() => {
      const { fixture, component, logSpy } = buildComponent();
      fixture.detectChanges();
      tick();
      logSpy.findByFamily.calls.reset();

      component.setTab('LOGBOOK');
      tick();

      expect(component.activeTab()).toBe('LOGBOOK');
      expect(logSpy.findByFamily).toHaveBeenCalled();
    }));

    it('SPRINT → activeTab=SPRINT y carga sprint activo', fakeAsync(() => {
      const { fixture, component, sprintSpy } = buildComponent();
      fixture.detectChanges();
      tick();
      sprintSpy.getActiveSprint.calls.reset();

      component.setTab('SPRINT');
      tick();

      expect(component.activeTab()).toBe('SPRINT');
      expect(sprintSpy.getActiveSprint).toHaveBeenCalled();
    }));

    it('HISTORY → activeTab=HISTORY y carga historial', fakeAsync(() => {
      const { fixture, component, sprintSpy } = buildComponent();
      fixture.detectChanges();
      tick();

      component.setTab('HISTORY');
      tick();

      expect(component.activeTab()).toBe('HISTORY');
      expect(sprintSpy.getSprintHistory).toHaveBeenCalledWith(FAMILY_ID);
    }));
  });

  // ═══════════════════════════════════════════════════════════════════════════
  //  loadEntries()
  // ═══════════════════════════════════════════════════════════════════════════

  describe('loadEntries()', () => {
    it('éxito → entries poblado y loading=false', fakeAsync(() => {
      const { fixture, component, logSpy } = buildComponent();
      logSpy.findByFamily.and.returnValue(of([ENTRY_STUB]));
      fixture.detectChanges();
      tick();

      expect(component.entries().length).toBe(1);
      expect(component.loading()).toBeFalse();
    }));

    it('error → error.set y loading=false', fakeAsync(() => {
      const { fixture, component, logSpy } = buildComponent();
      fixture.detectChanges();
      tick();

      // loadActiveSprint() limpia error en ngOnInit; llamamos loadEntries() directamente
      logSpy.findByFamily.and.returnValue(throwError(() => new Error('500')));
      component.loadEntries();
      tick();

      expect(component.loading()).toBeFalse();
      expect(component.error()).toBeTruthy();
    }));
  });

  // ═══════════════════════════════════════════════════════════════════════════
  //  loadCorrelation()
  // ═══════════════════════════════════════════════════════════════════════════

  describe('loadCorrelation()', () => {
    it('éxito con data → correlation signal actualizado', fakeAsync(() => {
      const { fixture, component, logSpy } = buildComponent();
      logSpy.getCorrelation.and.returnValue(of({ data: { score: 0.8 } }));
      fixture.detectChanges();
      tick();

      expect(component.correlation()).toEqual({ score: 0.8 });
      expect(component.loadingCorrelation()).toBeFalse();
    }));

    it('error → loadingCorrelation=false sin propagar', fakeAsync(() => {
      const { fixture, component, logSpy } = buildComponent();
      logSpy.getCorrelation.and.returnValue(throwError(() => new Error('err')));
      fixture.detectChanges();
      tick();

      expect(component.loadingCorrelation()).toBeFalse();
    }));
  });

  // ═══════════════════════════════════════════════════════════════════════════
  //  toggleForm()
  // ═══════════════════════════════════════════════════════════════════════════

  describe('toggleForm()', () => {
    it('toggleForm() alterna showForm', fakeAsync(() => {
      const { fixture, component } = buildComponent();
      fixture.detectChanges();
      tick();

      expect(component.showForm()).toBeFalse();
      component.toggleForm();
      expect(component.showForm()).toBeTrue();
      component.toggleForm();
      expect(component.showForm()).toBeFalse();
    }));

    it('al cerrar el form se resetea con createdBy=authorName', fakeAsync(() => {
      const { fixture, component } = buildComponent();
      fixture.detectChanges();
      tick();

      component.toggleForm(); // open
      component.patchForm('situation', 'Algo');
      component.toggleForm(); // close → reset

      expect(component.form().situation).toBe('');
      expect(component.form().createdBy).toBe('William Test');
    }));
  });

  // ═══════════════════════════════════════════════════════════════════════════
  //  setFilter() / patchForm() / patchEvidence()
  // ═══════════════════════════════════════════════════════════════════════════

  describe('setFilter() / patchForm() / patchEvidence()', () => {
    it('setFilter cambia el filtro activo', fakeAsync(() => {
      const { fixture, component } = buildComponent();
      fixture.detectChanges();
      tick();

      component.setFilter('OPEN');
      expect(component.filter()).toBe('OPEN');

      component.setFilter('RESOLVED');
      expect(component.filter()).toBe('RESOLVED');
    }));

    it('patchForm actualiza el campo especificado', fakeAsync(() => {
      const { fixture, component } = buildComponent();
      fixture.detectChanges();
      tick();

      component.patchForm('situation', 'Nueva situación');
      expect(component.form().situation).toBe('Nueva situación');
    }));

    it('patchEvidence actualiza resolveEvidence por id', fakeAsync(() => {
      const { fixture, component } = buildComponent();
      fixture.detectChanges();
      tick();

      component.patchEvidence(42, 'Mi evidencia');
      expect(component.resolveEvidence()[42]).toBe('Mi evidencia');
    }));
  });

  // ═══════════════════════════════════════════════════════════════════════════
  //  Computed signals
  // ═══════════════════════════════════════════════════════════════════════════

  describe('Computed signals', () => {
    it('openCount / resolvedCount / allCount reflejan el estado de entries', fakeAsync(() => {
      const { fixture, component, logSpy } = buildComponent();
      const openEntry    = { ...ENTRY_STUB, id: 1, status: 'OPEN' as const };
      const resolvedEntry = { ...ENTRY_STUB, id: 2, status: 'RESOLVED' as const };
      logSpy.findByFamily.and.returnValue(of([openEntry, resolvedEntry, openEntry]));
      fixture.detectChanges();
      tick();

      // Note: ENTRY_STUB is OPEN, so 3 items: 2 OPEN copies + 1 RESOLVED
      expect(component.allCount()).toBe(3);
      expect(component.openCount()).toBe(2);
      expect(component.resolvedCount()).toBe(1);
    }));

    it('filteredEntries filtra correctamente por status', fakeAsync(() => {
      const { fixture, component, logSpy } = buildComponent();
      logSpy.findByFamily.and.returnValue(
        of([{ ...ENTRY_STUB, id: 1, status: 'OPEN' as const },
            { ...ENTRY_STUB, id: 2, status: 'RESOLVED' as const }])
      );
      fixture.detectChanges();
      tick();

      component.setFilter('OPEN');
      expect(component.filteredEntries().length).toBe(1);
      expect(component.filteredEntries()[0].status).toBe('OPEN');

      component.setFilter('ALL');
      expect(component.filteredEntries().length).toBe(2);
    }));
  });

  // ═══════════════════════════════════════════════════════════════════════════
  //  createEntry()
  // ═══════════════════════════════════════════════════════════════════════════

  describe('createEntry()', () => {
    it('campos vacíos → error.set y no llama create', fakeAsync(() => {
      const { fixture, component, logSpy } = buildComponent();
      fixture.detectChanges();
      tick();
      logSpy.create.calls.reset();

      component.createEntry();

      expect(component.error()).toBeTruthy();
      expect(logSpy.create).not.toHaveBeenCalled();
    }));

    it('campos válidos → llama create con el payload correcto', fakeAsync(() => {
      const { fixture, component, logSpy } = buildComponent();
      fixture.detectChanges();
      tick();
      logSpy.findByFamily.calls.reset();

      component.form.set({
        situation: 'S', difficultyDetected: 'D', emotionIdentified: 'E',
        understanding: 'U', correctionAction: 'C', familyAgreement: 'A',
        createdBy: 'William Test'
      });
      component.createEntry();
      tick();

      expect(logSpy.create).toHaveBeenCalledWith(
        jasmine.objectContaining({ situation: 'S', familyId: FAMILY_ID })
      );
    }));

    it('éxito → resetea form y recarga entradas', fakeAsync(() => {
      const { fixture, component, logSpy } = buildComponent();
      fixture.detectChanges();
      tick();
      logSpy.findByFamily.calls.reset();

      component.form.set({
        situation: 'S', difficultyDetected: 'D', emotionIdentified: 'E',
        understanding: 'U', correctionAction: 'C', familyAgreement: 'A',
        createdBy: 'William Test'
      });
      component.createEntry();
      tick();

      expect(component.form().situation).toBe('');
      expect(component.showForm()).toBeFalse();
      expect(logSpy.findByFamily).toHaveBeenCalled();
    }));

    it('error → saving=false y error.set', fakeAsync(() => {
      const { fixture, component, logSpy } = buildComponent();
      logSpy.create.and.returnValue(throwError(() => new Error('403')));
      fixture.detectChanges();
      tick();

      component.form.set({
        situation: 'S', difficultyDetected: 'D', emotionIdentified: 'E',
        understanding: 'U', correctionAction: 'C', familyAgreement: 'A',
        createdBy: 'W'
      });
      component.createEntry();
      tick();

      expect(component.saving()).toBeFalse();
      expect(component.error()).toBeTruthy();
    }));
  });

  // ═══════════════════════════════════════════════════════════════════════════
  //  resolveEntry()
  // ═══════════════════════════════════════════════════════════════════════════

  describe('resolveEntry()', () => {
    it('evidencia vacía → error.set y no llama resolve', fakeAsync(() => {
      const { fixture, component, logSpy } = buildComponent();
      fixture.detectChanges();
      tick();

      component.resolveEntry(ENTRY_STUB);

      expect(component.error()).toBeTruthy();
      expect(logSpy.resolve).not.toHaveBeenCalled();
    }));

    it('con evidencia → llama resolve y recarga entradas', fakeAsync(() => {
      const { fixture, component, logSpy } = buildComponent();
      fixture.detectChanges();
      tick();
      logSpy.findByFamily.calls.reset();

      component.patchEvidence(ENTRY_STUB.id, 'Se resolvió con éxito');
      component.resolveEntry(ENTRY_STUB);
      tick();

      expect(logSpy.resolve).toHaveBeenCalledWith(
        ENTRY_STUB.id,
        jasmine.objectContaining({ progressEvidence: 'Se resolvió con éxito' })
      );
      expect(logSpy.findByFamily).toHaveBeenCalled();
    }));

    it('error en resolve → saving=false y error.set', fakeAsync(() => {
      const { fixture, component, logSpy } = buildComponent();
      logSpy.resolve.and.returnValue(throwError(() => new Error('500')));
      fixture.detectChanges();
      tick();

      component.patchEvidence(ENTRY_STUB.id, 'Evidencia');
      component.resolveEntry(ENTRY_STUB);
      tick();

      expect(component.saving()).toBeFalse();
      expect(component.error()).toBeTruthy();
    }));
  });

  // ═══════════════════════════════════════════════════════════════════════════
  //  Sprint: addSprintMission / removeSprintMission
  // ═══════════════════════════════════════════════════════════════════════════

  describe('addSprintMission() / removeSprintMission()', () => {
    it('addSprintMission agrega a la lista y limpia newMission', fakeAsync(() => {
      const { fixture, component } = buildComponent();
      fixture.detectChanges();
      tick();

      component.patchSprintForm('newMission', 'Misión 1');
      component.addSprintMission();

      expect(component.sprintForm().missions).toContain('Misión 1');
      expect(component.sprintForm().newMission).toBe('');
    }));

    it('addSprintMission con texto vacío → no agrega nada', fakeAsync(() => {
      const { fixture, component } = buildComponent();
      fixture.detectChanges();
      tick();

      component.addSprintMission(); // newMission=''
      expect(component.sprintForm().missions.length).toBe(0);
    }));

    it('removeSprintMission elimina la misión en el índice dado', fakeAsync(() => {
      const { fixture, component } = buildComponent();
      fixture.detectChanges();
      tick();

      component.patchSprintForm('newMission', 'A');
      component.addSprintMission();
      component.patchSprintForm('newMission', 'B');
      component.addSprintMission();

      component.removeSprintMission(0); // remove 'A'
      expect(component.sprintForm().missions).toEqual(['B']);
    }));
  });

  // ═══════════════════════════════════════════════════════════════════════════
  //  Sprint: createSprint()
  // ═══════════════════════════════════════════════════════════════════════════

  describe('createSprint()', () => {
    it('sin objetivo → error.set y no llama createSprint', fakeAsync(() => {
      const { fixture, component, sprintSpy } = buildComponent();
      fixture.detectChanges();
      tick();

      component.createSprint();

      expect(component.error()).toBeTruthy();
      expect(sprintSpy.createSprint).not.toHaveBeenCalled();
    }));

    it('éxito → activeSprint actualizado y sprintForm reseteado', fakeAsync(() => {
      const { fixture, component, sprintSpy } = buildComponent();
      sprintSpy.createSprint.and.returnValue(of(SPRINT_STUB));
      fixture.detectChanges();
      tick();

      component.patchSprintForm('objective', 'Conectar mejor');
      component.patchSprintForm('newMission', 'Cenar juntos');
      component.addSprintMission();
      component.createSprint();
      tick();

      expect(component.activeSprint()).toEqual(SPRINT_STUB);
      expect(component.sprintForm().objective).toBe('');
      expect(component.savingSprint()).toBeFalse();
    }));

    it('error → savingSprint=false y error.set', fakeAsync(() => {
      const { fixture, component, sprintSpy } = buildComponent();
      sprintSpy.createSprint.and.returnValue(throwError(() => ({ error: { message: 'Conflict' } })));
      fixture.detectChanges();
      tick();

      component.patchSprintForm('objective', 'Objetivo');
      component.patchSprintForm('newMission', 'Misión');
      component.addSprintMission();
      component.createSprint();
      tick();

      expect(component.savingSprint()).toBeFalse();
      expect(component.error()).toBeTruthy();
    }));
  });

  // ═══════════════════════════════════════════════════════════════════════════
  //  Sprint: toggleDailyForm / submitDaily()
  // ═══════════════════════════════════════════════════════════════════════════

  describe('toggleDailyForm() / submitDaily()', () => {
    it('toggleDailyForm alterna showDailyForm', fakeAsync(() => {
      const { fixture, component } = buildComponent();
      fixture.detectChanges();
      tick();

      component.toggleDailyForm();
      expect(component.showDailyForm()).toBeTrue();
      component.toggleDailyForm();
      expect(component.showDailyForm()).toBeFalse();
    }));

    it('submitDaily sin sprint activo → no llama submitDaily service', fakeAsync(() => {
      const { fixture, component, sprintSpy } = buildComponent();
      fixture.detectChanges();
      tick();
      sprintSpy.submitDaily.calls.reset();

      // activeSprint() is null (default)
      component.submitDaily();
      expect(sprintSpy.submitDaily).not.toHaveBeenCalled();
    }));

    it('submitDaily con campos vacíos → error.set', fakeAsync(() => {
      const { fixture, component, sprintSpy } = buildComponent();
      fixture.detectChanges();
      tick();

      component.activeSprint.set(SPRINT_STUB);
      component.submitDaily(); // memberName/yesterdayText/todayText vacíos

      expect(component.error()).toBeTruthy();
      expect(sprintSpy.submitDaily).not.toHaveBeenCalled();
    }));

    it('submitDaily éxito → showDailyForm=false y recarga sprint', fakeAsync(() => {
      const { fixture, component, sprintSpy } = buildComponent();
      fixture.detectChanges();
      tick();
      sprintSpy.getActiveSprint.calls.reset();

      component.activeSprint.set(SPRINT_STUB);
      component.patchDailyForm('memberName', 'William');
      component.patchDailyForm('yesterdayText', 'Logré algo');
      component.patchDailyForm('todayText', 'Haré algo');
      component.showDailyForm.set(true);
      component.submitDaily();
      tick();

      expect(component.showDailyForm()).toBeFalse();
      expect(sprintSpy.getActiveSprint).toHaveBeenCalled();
    }));
  });

  // ═══════════════════════════════════════════════════════════════════════════
  //  Sprint: openRetrospectiveModal / closeRetrospectiveModal / closeSprint()
  // ═══════════════════════════════════════════════════════════════════════════

  describe('retroModal / closeSprint()', () => {
    it('openRetrospectiveModal → showRetroForm=true', fakeAsync(() => {
      const { fixture, component } = buildComponent();
      fixture.detectChanges();
      tick();

      component.openRetrospectiveModal();
      expect(component.showRetroForm()).toBeTrue();
    }));

    it('closeRetrospectiveModal → showRetroForm=false', fakeAsync(() => {
      const { fixture, component } = buildComponent();
      fixture.detectChanges();
      tick();

      component.openRetrospectiveModal();
      component.closeRetrospectiveModal();
      expect(component.showRetroForm()).toBeFalse();
    }));

    it('closeSprint sin activeSprint → no llama servicio', fakeAsync(() => {
      const { fixture, component, sprintSpy } = buildComponent();
      fixture.detectChanges();
      tick();

      component.closeSprint(); // activeSprint is null
      expect(sprintSpy.closeSprint).not.toHaveBeenCalled();
    }));

    it('closeSprint campos vacíos → error.set', fakeAsync(() => {
      const { fixture, component, sprintSpy } = buildComponent();
      fixture.detectChanges();
      tick();

      component.activeSprint.set(SPRINT_STUB);
      component.closeSprint();

      expect(component.error()).toBeTruthy();
      expect(sprintSpy.closeSprint).not.toHaveBeenCalled();
    }));

    it('closeSprint éxito → activeSprint=null y setTab(HISTORY)', fakeAsync(() => {
      const { fixture, component, sprintSpy } = buildComponent();
      fixture.detectChanges();
      tick();

      component.activeSprint.set(SPRINT_STUB);
      component.patchRetroForm('whatWentWell', 'Fue bien');
      component.patchRetroForm('whatWasDifficult', 'Fue difícil');
      component.closeSprint();
      tick();

      expect(component.activeSprint()).toBeNull();
      expect(component.activeTab()).toBe('HISTORY');
    }));

    it('closeSprint error → savingRetro=false y error.set', fakeAsync(() => {
      const { fixture, component, sprintSpy } = buildComponent();
      sprintSpy.closeSprint.and.returnValue(throwError(() => new Error('500')));
      fixture.detectChanges();
      tick();

      component.activeSprint.set(SPRINT_STUB);
      component.patchRetroForm('whatWentWell', 'Fue bien');
      component.patchRetroForm('whatWasDifficult', 'Fue difícil');
      component.closeSprint();
      tick();

      expect(component.savingRetro()).toBeFalse();
      expect(component.error()).toBeTruthy();
    }));
  });

  // ═══════════════════════════════════════════════════════════════════════════
  //  UI Helpers
  // ═══════════════════════════════════════════════════════════════════════════

  describe('getDimensionFriendlyName()', () => {
    let component: FamilyLogbookComponent;

    beforeEach(fakeAsync(() => {
      const ctx = buildComponent();
      ctx.fixture.detectChanges();
      tick();
      component = ctx.component;
    }));

    it('comunicacion → "Comunicación Asertiva"', () =>
      expect(component.getDimensionFriendlyName('comunicacion')).toBe('Comunicación Asertiva'));

    it('emociones → "Regulación & Clima Emocional"', () =>
      expect(component.getDimensionFriendlyName('emociones')).toBe('Regulación & Clima Emocional'));

    it('habitos → "Hábitos & Convivencia Colectiva"', () =>
      expect(component.getDimensionFriendlyName('habitos')).toBe('Hábitos & Convivencia Colectiva'));

    it('tiempos → "Tiempos de Conexión Activa"', () =>
      expect(component.getDimensionFriendlyName('tiempos')).toBe('Tiempos de Conexión Activa'));

    it('desconocida → retorna la misma string', () =>
      expect(component.getDimensionFriendlyName('UNKNOWN')).toBe('UNKNOWN'));

    it('null/undefined → fallback "Comunicación"', () =>
      expect(component.getDimensionFriendlyName(null as any)).toBe('Comunicación'));
  });

  describe('getMoodIcon()', () => {
    let component: FamilyLogbookComponent;

    beforeEach(fakeAsync(() => {
      const ctx = buildComponent();
      ctx.fixture.detectChanges();
      tick();
      component = ctx.component;
    }));

    it('HAPPY → 😊', () => expect(component.getMoodIcon('HAPPY')).toBe('😊'));
    it('CALM → 🧘', () => expect(component.getMoodIcon('CALM')).toBe('🧘'));
    it('TIRED → 🥱', () => expect(component.getMoodIcon('TIRED')).toBe('🥱'));
    it('STRESSED → 😡', () => expect(component.getMoodIcon('STRESSED')).toBe('😡'));
    it('undefined → 😊 (default)', () => expect(component.getMoodIcon(undefined)).toBe('😊'));
  });

  describe('getCompletedMissionsCount() / getMissionsProgressPercent()', () => {
    let component: FamilyLogbookComponent;

    beforeEach(fakeAsync(() => {
      const ctx = buildComponent();
      ctx.fixture.detectChanges();
      tick();
      component = ctx.component;
    }));

    const missions: SprintMissionResponse[] = [
      { id: 1, description: 'M1', status: 'COMPLETED' },
      { id: 2, description: 'M2', status: 'PENDING' },
      { id: 3, description: 'M3', status: 'COMPLETED' }
    ];

    it('2 de 3 completadas → count=2, progress=67%', () => {
      expect(component.getCompletedMissionsCount(missions)).toBe(2);
      expect(component.getMissionsProgressPercent(missions)).toBe(67);
    });

    it('lista vacía → progress=0', () =>
      expect(component.getMissionsProgressPercent([])).toBe(0));
  });

  describe('getDaysRemaining()', () => {
    let component: FamilyLogbookComponent;

    beforeEach(fakeAsync(() => {
      const ctx = buildComponent();
      ctx.fixture.detectChanges();
      tick();
      component = ctx.component;
    }));

    it('fecha en el pasado → 0', () => {
      expect(component.getDaysRemaining('2020-01-01')).toBe(0);
    });

    it('fecha en el futuro → valor positivo', () => {
      const future = new Date();
      future.setDate(future.getDate() + 5);
      expect(component.getDaysRemaining(future.toISOString())).toBeGreaterThan(0);
    });

    it('string vacío → 0', () =>
      expect(component.getDaysRemaining('')).toBe(0));
  });

  describe('formatAiResponse()', () => {
    let component: FamilyLogbookComponent;

    beforeEach(fakeAsync(() => {
      const ctx = buildComponent();
      ctx.fixture.detectChanges();
      tick();
      component = ctx.component;
    }));

    it('texto vacío → ""', () =>
      expect(component.formatAiResponse('')).toBe(''));

    it('convierte **texto** → <strong>texto</strong>', () =>
      expect(component.formatAiResponse('**bold**')).toContain('<strong>bold</strong>'));

    it('convierte # título → <h2>título</h2>', () =>
      expect(component.formatAiResponse('# Título')).toContain('<h2>Título</h2>'));

    it('convierte - item → <li>item</li>', () =>
      expect(component.formatAiResponse('- elemento')).toContain('<li>elemento</li>'));
  });

  describe('trackById()', () => {
    it('retorna entry.id', fakeAsync(() => {
      const { fixture, component } = buildComponent();
      fixture.detectChanges();
      tick();

      expect(component.trackById(0, ENTRY_STUB)).toBe(1);
    }));
  });
});
