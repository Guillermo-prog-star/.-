import { TestBed, fakeAsync, tick } from '@angular/core/testing';
import { NO_ERRORS_SCHEMA } from '@angular/core';
import { provideRouter } from '@angular/router';
import { of, throwError } from 'rxjs';

import { CognitivePageComponent } from './cognitive-page.component';
import { CognitiveService } from '../../core/services/cognitive.service';
import { FamilyStateService } from '../../core/services/family-state.service';
import {
  CognitiveSnapshot, NarrativeResponse, GraphResponse,
  MemoryResponse, ReflectionResponse
} from '../../core/models/cognitive.model';

// ─── Stubs ───────────────────────────────────────────────────────────────────

const FAMILY_ID = 42;

const SNAPSHOT_STUB: CognitiveSnapshot = {
  familyId: FAMILY_ID,
  identityProfile: {
    evolutionStage: 'RECOGNITION',
    communicationStyle: 'ASSERTIVE',
    conflictStyle: 'COLLABORATIVE',
    emotionalExpression: 'OPEN',
    adaptabilityIndex: 0.75,
    completedCycles: 2,
    identityNarrative: 'Historia de crecimiento'
  },
  currentChapter: null,
  totalChapters: 2,
  turningPoints: 1,
  graphSummary: {
    totalDyads: 1, cohesionDensity: 70, tensionDensity: 20,
    conflictiveEdges: 0, healthy: true, systemRoles: []
  },
  recentMemories: [],
  appliedSkills: [],
  storyArcSummary: 'Un arco de historia',
  generatedAt: '2026-01-01T00:00:00'
};

const NARRATIVE_STUB: NarrativeResponse = {
  familyId: FAMILY_ID,
  chapters: [],
  currentPhase: 'DISCOVERY',
  totalChapters: 2,
  turningPoints: 1,
  storyArcSummary: 'Resumen de arco'
};

const GRAPH_STUB: GraphResponse = {
  familyId: FAMILY_ID,
  dyads: [],
  systemRoles: [],
  cohesionDensity: 65,
  tensionDensity: 25,
  conflictiveEdges: 0,
  healthy: true,
  summary: 'Grafo saludable'
};

const MEMORY_STUB: MemoryResponse = {
  familyId: FAMILY_ID,
  episodic: [
    { id: 1, memoryType: 'EPISODIC', semanticKey: 'ep1', content: 'Episodio 1',
      importanceScore: 0.8, sourceType: 'EVALUATION', createdAt: '2026-01-01T00:00:00' }
  ],
  semantic: [
    { id: 2, memoryType: 'SEMANTIC', semanticKey: 'sem1', content: 'Semántica 1',
      importanceScore: 0.5, sourceType: 'REFLECTION', createdAt: '2026-01-01T00:00:00' }
  ],
  procedural: []
};

const REFLECTION_STUB: ReflectionResponse = {
  familyId: FAMILY_ID,
  effectivenessLevel: 'HIGH',
  evaluationCount: 3,
  icfTrend: 5,
  avgAdherence: 80,
  reflectionRate: 0.9,
  effectivenessSummary: 'Sistema efectivo',
  abandonmentLevel: 'LOW',
  abandonmentSignals: [],
  abandonmentScore: 10,
  lessonLearned: 'Lección aprendida',
  updatedNarrative: null,
  requiresUrgentAttention: false,
  generatedAt: '2026-01-01T00:00:00'
};

// ─── Helper ──────────────────────────────────────────────────────────────────

function buildComponent(familyId = FAMILY_ID) {
  const cogSpy = jasmine.createSpyObj<CognitiveService>(
    'CognitiveService', {
      getSnapshot:         of(null),
      getNarrative:        of(null),
      getGraph:            of(null),
      getMemory:           of(null),
      getLatestReflection: of(null),
      triggerReflection:   of(null)
    }
  );

  const fsSpy = jasmine.createSpyObj<FamilyStateService>(
    'FamilyStateService', { getSelectedFamilyId: familyId }
  );

  TestBed.configureTestingModule({
    imports: [CognitivePageComponent],
    providers: [
      provideRouter([]),
      { provide: CognitiveService, useValue: cogSpy },
      { provide: FamilyStateService, useValue: fsSpy }
    ],
    schemas: [NO_ERRORS_SCHEMA]
  });

  const fixture   = TestBed.createComponent(CognitivePageComponent);
  const component = fixture.componentInstance;

  return { fixture, component, cogSpy, fsSpy };
}

// ─────────────────────────────────────────────────────────────────────────────

describe('CognitivePageComponent', () => {
  afterEach(() => TestBed.resetTestingModule());

  // ═══════════════════════════════════════════════════════════════════════════
  //  ngOnInit()
  // ═══════════════════════════════════════════════════════════════════════════

  describe('ngOnInit()', () => {
    it('familyId=0 → loading=false sin llamar a ningún servicio', fakeAsync(() => {
      const { fixture, component, cogSpy } = buildComponent(0);
      fixture.detectChanges();
      tick();

      expect(component.loading()).toBeFalse();
      expect(cogSpy.getSnapshot).not.toHaveBeenCalled();
      expect(cogSpy.getNarrative).not.toHaveBeenCalled();
    }));

    it('familyId presente → llama a los 5 servicios con forkJoin', fakeAsync(() => {
      const { fixture, cogSpy } = buildComponent();
      fixture.detectChanges();
      tick();

      expect(cogSpy.getSnapshot).toHaveBeenCalledWith(FAMILY_ID);
      expect(cogSpy.getNarrative).toHaveBeenCalledWith(FAMILY_ID);
      expect(cogSpy.getGraph).toHaveBeenCalledWith(FAMILY_ID);
      expect(cogSpy.getMemory).toHaveBeenCalledWith(FAMILY_ID);
      expect(cogSpy.getLatestReflection).toHaveBeenCalledWith(FAMILY_ID);
    }));

    it('éxito → asigna todos los signals y loading=false', fakeAsync(() => {
      const { fixture, component, cogSpy } = buildComponent();
      cogSpy.getSnapshot.and.returnValue(of(SNAPSHOT_STUB));
      cogSpy.getNarrative.and.returnValue(of(NARRATIVE_STUB));
      cogSpy.getGraph.and.returnValue(of(GRAPH_STUB));
      cogSpy.getMemory.and.returnValue(of(MEMORY_STUB));
      cogSpy.getLatestReflection.and.returnValue(of(REFLECTION_STUB));
      fixture.detectChanges();
      tick();

      expect(component.snapshot()).toEqual(SNAPSHOT_STUB);
      expect(component.narrative()).toEqual(NARRATIVE_STUB);
      expect(component.graph()).toEqual(GRAPH_STUB);
      expect(component.memory()).toEqual(MEMORY_STUB);
      expect(component.reflection()).toEqual(REFLECTION_STUB);
      expect(component.loading()).toBeFalse();
    }));

    it('error en forkJoin → loading=false y signals permanecen null', fakeAsync(() => {
      const { fixture, component, cogSpy } = buildComponent();
      cogSpy.getSnapshot.and.returnValue(throwError(() => new Error('500')));
      fixture.detectChanges();
      tick();

      expect(component.loading()).toBeFalse();
      expect(component.snapshot()).toBeNull();
    }));
  });

  // ═══════════════════════════════════════════════════════════════════════════
  //  runReflection()
  // ═══════════════════════════════════════════════════════════════════════════

  describe('runReflection()', () => {
    it('familyId=0 → no llama a triggerReflection', fakeAsync(() => {
      const { fixture, component, cogSpy, fsSpy } = buildComponent();
      fixture.detectChanges();
      tick();
      fsSpy.getSelectedFamilyId.and.returnValue(0);

      component.runReflection();
      tick();

      expect(cogSpy.triggerReflection).not.toHaveBeenCalled();
    }));

    it('reflecting=true → guard impide llamar triggerReflection', fakeAsync(() => {
      const { fixture, component, cogSpy } = buildComponent();
      fixture.detectChanges();
      tick();

      component.reflecting.set(true);
      component.runReflection();
      tick();

      expect(cogSpy.triggerReflection).not.toHaveBeenCalled();
    }));

    it('éxito → reflection signal actualizado y reflecting=false', fakeAsync(() => {
      const { fixture, component, cogSpy } = buildComponent();
      cogSpy.triggerReflection.and.returnValue(of(REFLECTION_STUB));
      fixture.detectChanges();
      tick();

      component.runReflection();
      tick();

      expect(component.reflection()).toEqual(REFLECTION_STUB);
      expect(component.reflecting()).toBeFalse();
    }));

    it('error → reflecting=false sin propagar excepción', fakeAsync(() => {
      const { fixture, component, cogSpy } = buildComponent();
      cogSpy.triggerReflection.and.returnValue(throwError(() => new Error('AI down')));
      fixture.detectChanges();
      tick();

      expect(() => {
        component.runReflection();
        tick();
      }).not.toThrow();

      expect(component.reflecting()).toBeFalse();
    }));
  });

  // ═══════════════════════════════════════════════════════════════════════════
  //  activeMemories computed / memoryCount()
  // ═══════════════════════════════════════════════════════════════════════════

  describe('activeMemories() / memoryCount()', () => {
    it('tab episodic → devuelve lista episódica', fakeAsync(() => {
      const { fixture, component, cogSpy } = buildComponent();
      cogSpy.getMemory.and.returnValue(of(MEMORY_STUB));
      fixture.detectChanges();
      tick();

      component.activeMemoryTab.set('episodic');
      expect(component.activeMemories().length).toBe(1);
      expect(component.activeMemories()[0].memoryType).toBe('EPISODIC');
    }));

    it('tab semantic → devuelve lista semántica', fakeAsync(() => {
      const { fixture, component, cogSpy } = buildComponent();
      cogSpy.getMemory.and.returnValue(of(MEMORY_STUB));
      fixture.detectChanges();
      tick();

      component.activeMemoryTab.set('semantic');
      expect(component.activeMemories().length).toBe(1);
      expect(component.activeMemories()[0].memoryType).toBe('SEMANTIC');
    }));

    it('tab procedural → devuelve lista procedural (vacía en stub)', fakeAsync(() => {
      const { fixture, component, cogSpy } = buildComponent();
      cogSpy.getMemory.and.returnValue(of(MEMORY_STUB));
      fixture.detectChanges();
      tick();

      component.activeMemoryTab.set('procedural');
      expect(component.activeMemories()).toEqual([]);
    }));

    it('memory=null → activeMemories devuelve []', fakeAsync(() => {
      const { fixture, component } = buildComponent();
      fixture.detectChanges();
      tick();

      // memory signal permanece null (stub retorna of(null))
      expect(component.activeMemories()).toEqual([]);
    }));

    it('memoryCount() con memory → cuenta correctamente', fakeAsync(() => {
      const { fixture, component, cogSpy } = buildComponent();
      cogSpy.getMemory.and.returnValue(of(MEMORY_STUB));
      fixture.detectChanges();
      tick();

      expect(component.memoryCount('episodic')).toBe(1);
      expect(component.memoryCount('semantic')).toBe(1);
      expect(component.memoryCount('procedural')).toBe(0);
    }));

    it('memoryCount() sin memory → siempre 0', fakeAsync(() => {
      const { fixture, component } = buildComponent();
      fixture.detectChanges();
      tick();

      expect(component.memoryCount('episodic')).toBe(0);
      expect(component.memoryCount('semantic')).toBe(0);
      expect(component.memoryCount('procedural')).toBe(0);
    }));
  });

  // ═══════════════════════════════════════════════════════════════════════════
  //  Computed getters: stageEmoji, stageName, adaptabilityPct
  // ═══════════════════════════════════════════════════════════════════════════

  describe('stageEmoji / stageName / adaptabilityPct', () => {
    let component: CognitivePageComponent;

    beforeEach(fakeAsync(() => {
      const ctx = buildComponent();
      ctx.cogSpy.getSnapshot.and.returnValue(of(SNAPSHOT_STUB));
      ctx.fixture.detectChanges();
      tick();
      component = ctx.component;
    }));

    it('stageEmoji → INITIAL:🌱, RECOGNITION:👁️, ADJUSTMENT:⚙️, CONSOLIDATION:🏗️, AUTONOMOUS:🚀', () => {
      const stages = ['INITIAL', 'RECOGNITION', 'ADJUSTMENT', 'CONSOLIDATION', 'AUTONOMOUS'] as const;
      const emojis = ['🌱', '👁️', '⚙️', '🏗️', '🚀'];

      stages.forEach((stage, i) => {
        component.snapshot.set({
          ...SNAPSHOT_STUB,
          identityProfile: { ...SNAPSHOT_STUB.identityProfile, evolutionStage: stage }
        });
        expect(component.stageEmoji).toBe(emojis[i]);
      });
    });

    it('stageEmoji → etapa desconocida → 🌱 (fallback)', () => {
      component.snapshot.set({
        ...SNAPSHOT_STUB,
        identityProfile: { ...SNAPSHOT_STUB.identityProfile, evolutionStage: 'UNKNOWN' }
      });
      expect(component.stageEmoji).toBe('🌱');
    });

    it('stageName → AUTONOMOUS → "Autonomía operativa"', () => {
      component.snapshot.set({
        ...SNAPSHOT_STUB,
        identityProfile: { ...SNAPSHOT_STUB.identityProfile, evolutionStage: 'AUTONOMOUS' }
      });
      expect(component.stageName).toBe('Autonomía operativa');
    });

    it('stageName → etapa desconocida → "En proceso" (fallback)', () => {
      component.snapshot.set({
        ...SNAPSHOT_STUB,
        identityProfile: { ...SNAPSHOT_STUB.identityProfile, evolutionStage: 'UNKNOWN' }
      });
      expect(component.stageName).toBe('En proceso');
    });

    it('adaptabilityPct → 0.75 → "75"', () => {
      expect(component.adaptabilityPct).toBe('75');
    });

    it('adaptabilityPct → snapshot null → "0"', () => {
      component.snapshot.set(null);
      expect(component.adaptabilityPct).toBe('0');
    });
  });

  // ═══════════════════════════════════════════════════════════════════════════
  //  Style helpers
  // ═══════════════════════════════════════════════════════════════════════════

  describe('phaseClass()', () => {
    let component: CognitivePageComponent;

    beforeEach(fakeAsync(() => {
      const ctx = buildComponent();
      ctx.fixture.detectChanges();
      tick();
      component = ctx.component;
    }));

    it('AWAKENING → contiene "indigo"', () =>
      expect(component.phaseClass('AWAKENING')).toContain('indigo'));

    it('DISCOVERY → contiene "cyan"', () =>
      expect(component.phaseClass('DISCOVERY')).toContain('cyan'));

    it('CRISIS → contiene "red"', () =>
      expect(component.phaseClass('CRISIS')).toContain('red'));

    it('AUTONOMY → contiene "violet"', () =>
      expect(component.phaseClass('AUTONOMY')).toContain('violet'));

    it('fase desconocida → fallback con "white"', () =>
      expect(component.phaseClass('UNKNOWN' as any)).toContain('white'));
  });

  describe('dynamicClass()', () => {
    let component: CognitivePageComponent;

    beforeEach(fakeAsync(() => {
      const ctx = buildComponent();
      ctx.fixture.detectChanges();
      tick();
      component = ctx.component;
    }));

    it('SUPPORTIVE → emerald', () =>
      expect(component.dynamicClass('SUPPORTIVE')).toContain('emerald'));

    it('CONFLICTIVE → red', () =>
      expect(component.dynamicClass('CONFLICTIVE')).toContain('red'));

    it('DISTANT → white/40', () =>
      expect(component.dynamicClass('DISTANT')).toContain('white/40'));

    it('desconocido → fallback', () =>
      expect(component.dynamicClass('UNKNOWN')).toBeTruthy());
  });

  describe('roleEmoji() / trendIcon()', () => {
    let component: CognitivePageComponent;

    beforeEach(fakeAsync(() => {
      const ctx = buildComponent();
      ctx.fixture.detectChanges();
      tick();
      component = ctx.component;
    }));

    it('roleEmoji → ANCHOR:⚓, PEACEMAKER:🕊️, ESCALATOR:🔥, DISCONNECTED:🌑, NEUTRAL:⚬', () => {
      expect(component.roleEmoji('ANCHOR')).toBe('⚓');
      expect(component.roleEmoji('PEACEMAKER')).toBe('🕊️');
      expect(component.roleEmoji('ESCALATOR')).toBe('🔥');
      expect(component.roleEmoji('DISCONNECTED')).toBe('🌑');
      expect(component.roleEmoji('NEUTRAL')).toBe('⚬');
    });

    it('roleEmoji → rol desconocido → ⚬ (fallback)', () =>
      expect(component.roleEmoji('UNKNOWN')).toBe('⚬'));

    it('trendIcon → IMPROVING:↑, STABLE:→, DECLINING:↓', () => {
      expect(component.trendIcon('IMPROVING')).toBe('↑');
      expect(component.trendIcon('STABLE')).toBe('→');
      expect(component.trendIcon('DECLINING')).toBe('↓');
    });

    it('trendIcon → tendencia desconocida → →', () =>
      expect(component.trendIcon('UNKNOWN')).toBe('→'));
  });

  describe('effectivenessLabel() / effectivenessColor()', () => {
    let component: CognitivePageComponent;

    beforeEach(fakeAsync(() => {
      const ctx = buildComponent();
      ctx.fixture.detectChanges();
      tick();
      component = ctx.component;
    }));

    it('effectivenessLabel → HIGH:ALTA, MODERATE:MEDIA, LOW:BAJA, REGRESSING:REGRESIÓN, INSUFFICIENT_DATA:S/D', () => {
      expect(component.effectivenessLabel('HIGH')).toBe('ALTA');
      expect(component.effectivenessLabel('MODERATE')).toBe('MEDIA');
      expect(component.effectivenessLabel('LOW')).toBe('BAJA');
      expect(component.effectivenessLabel('REGRESSING')).toBe('REGRESIÓN');
      expect(component.effectivenessLabel('INSUFFICIENT_DATA')).toBe('S/D');
    });

    it('effectivenessColor → HIGH:emerald, REGRESSING:red, INSUFFICIENT_DATA:white/30', () => {
      expect(component.effectivenessColor('HIGH')).toContain('emerald');
      expect(component.effectivenessColor('REGRESSING')).toContain('red');
      expect(component.effectivenessColor('INSUFFICIENT_DATA')).toContain('white/30');
    });
  });

  describe('abandonmentColor() / memoryTypeClass()', () => {
    let component: CognitivePageComponent;

    beforeEach(fakeAsync(() => {
      const ctx = buildComponent();
      ctx.fixture.detectChanges();
      tick();
      component = ctx.component;
    }));

    it('abandonmentColor → LOW:emerald, MODERATE:amber, HIGH:orange, CRITICAL:red', () => {
      expect(component.abandonmentColor('LOW')).toContain('emerald');
      expect(component.abandonmentColor('MODERATE')).toContain('amber');
      expect(component.abandonmentColor('HIGH')).toContain('orange');
      expect(component.abandonmentColor('CRITICAL')).toContain('red');
    });

    it('memoryTypeClass → EPISODIC:cyan, SEMANTIC:violet, PROCEDURAL:amber, IDENTITY:pink', () => {
      expect(component.memoryTypeClass('EPISODIC')).toContain('cyan');
      expect(component.memoryTypeClass('SEMANTIC')).toContain('violet');
      expect(component.memoryTypeClass('PROCEDURAL')).toContain('amber');
      expect(component.memoryTypeClass('IDENTITY')).toContain('pink');
    });

    it('memoryTypeClass → tipo desconocido → fallback con "white/40"', () =>
      expect(component.memoryTypeClass('UNKNOWN')).toContain('white/40'));
  });

  // ═══════════════════════════════════════════════════════════════════════════
  //  systemRoleClass()
  // ═══════════════════════════════════════════════════════════════════════════

  describe('systemRoleClass()', () => {
    let component: CognitivePageComponent;

    beforeEach(fakeAsync(() => {
      const ctx = buildComponent();
      ctx.fixture.detectChanges();
      tick();
      component = ctx.component;
    }));

    it('ANCHOR → contiene "violet"', () =>
      expect(component.systemRoleClass('ANCHOR')).toContain('violet'));

    it('PEACEMAKER → contiene "emerald"', () =>
      expect(component.systemRoleClass('PEACEMAKER')).toContain('emerald'));

    it('ESCALATOR → contiene "red"', () =>
      expect(component.systemRoleClass('ESCALATOR')).toContain('red'));

    it('rol desconocido → retorna algún estilo (no vacío)', () =>
      expect(component.systemRoleClass('UNKNOWN').length).toBeGreaterThan(0));
  });
});
