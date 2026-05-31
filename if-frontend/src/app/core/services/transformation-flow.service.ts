import { Injectable, signal, computed, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { catchError, of } from 'rxjs';
import { FamilyStateService } from './family-state.service';

/**
 * TransformationFlowService — Sistema Operativo de Transformación Familiar
 *
 * Orquesta el viaje completo de 36 meses de una familia:
 *   SETUP → DIAGNÓSTICO → PLAN → TRANSFORMACIÓN (3 pilares) → LEGADO
 *
 * Este servicio es la ÚNICA fuente de verdad sobre en qué paso del
 * viaje se encuentra la familia activa. Todos los componentes de navegación
 * y flujo deben consumirlo reactivamente.
 */

export type OnboardingStep =
  | 'create-family'
  | 'add-members'
  | 'choose-guardian'
  | 'diagnosis'
  | 'plan-generated'
  | 'completed';

export type TransformationPillar = 'reconocimiento' | 'amor' | 'entrega';

export interface TransformationState {
  onboardingStep: OnboardingStep;
  currentPillar: TransformationPillar;
  /** Mes activo dentro del pilar (1–36) */
  currentMonth: number;
  /** Número de sprint activo en el mes corriente */
  currentSprintNumber: number;
  /** ID de la misión activa (null si no hay) */
  activeMissionId: string | null;
  /** Porcentaje global de avance en el plan */
  progressPercent: number;
  /** Hito literal del sistema (W1, M1, M3, M6 ...) */
  currentMilestoneLabel: string;
}

const STORAGE_KEY = 'if_transformation_state';

const DEFAULT_STATE: TransformationState = {
  onboardingStep: 'create-family',
  currentPillar: 'reconocimiento',
  currentMonth: 1,
  currentSprintNumber: 1,
  activeMissionId: null,
  progressPercent: 0,
  currentMilestoneLabel: 'M1',
};

@Injectable({ providedIn: 'root' })
export class TransformationFlowService {
  private familyState = inject(FamilyStateService);
  private http        = inject(HttpClient);

  // ── Estado privado ──────────────────────────────────────────────────────────
  private readonly _state = signal<TransformationState>(this.loadState());

  // ── Estado público (solo lectura) ───────────────────────────────────────────
  readonly state              = this._state.asReadonly();
  readonly onboardingStep     = computed(() => this._state().onboardingStep);
  readonly currentPillar      = computed(() => this._state().currentPillar);
  readonly currentMonth       = computed(() => this._state().currentMonth);
  readonly currentSprintNumber = computed(() => this._state().currentSprintNumber);
  readonly activeMissionId    = computed(() => this._state().activeMissionId);
  readonly progressPercent    = computed(() => this._state().progressPercent);
  readonly milestoneLabel     = computed(() => this._state().currentMilestoneLabel);

  /** ¿El onboarding está completado? */
  readonly isOnboardingDone   = computed(() => this._state().onboardingStep === 'completed');

  /** ¿La familia necesita pasar por el setup? */
  readonly needsSetup         = computed(() => {
    const step = this._state().onboardingStep;
    return step !== 'completed';
  });

  /** Nombre legible del pilar activo */
  readonly pillarLabel = computed(() => {
    const map: Record<TransformationPillar, string> = {
      reconocimiento: '💛 Reconocimiento',
      amor:           '❤️ Amor',
      entrega:        '💙 Entrega',
    };
    return map[this._state().currentPillar];
  });

  /** Rango de meses del pilar activo */
  readonly pillarMonthRange = computed(() => {
    const map: Record<TransformationPillar, string> = {
      reconocimiento: 'Meses 1–6',
      amor:           'Meses 7–18',
      entrega:        'Meses 19–36',
    };
    return map[this._state().currentPillar];
  });

  /** Fase narrativa dentro del pilar */
  readonly currentPhaseLabel = computed(() => {
    const month = this._state().currentMonth;
    const pillar = this._state().currentPillar;
    if (pillar === 'reconocimiento') {
      if (month <= 2) return 'Estabilización';
      if (month <= 4) return 'Conciencia Inicial';
      return 'Cimentación de Vínculos';
    }
    if (pillar === 'amor') {
      if (month <= 9)  return 'Transformación Profunda';
      if (month <= 14) return 'Consolidación de Hábitos';
      return 'Integridad Familiar';
    }
    // entrega
    if (month <= 24) return 'Crecimiento Generacional';
    if (month <= 30) return 'Legado Familiar';
    return 'Trascendencia';
  });

  // ── Mutaciones ───────────────────────────────────────────────────────────────

  advanceOnboarding(step: OnboardingStep): void {
    this.patch({ onboardingStep: step });
  }

  completeOnboarding(): void {
    this.patch({ onboardingStep: 'completed' });
  }

  setPillar(pillar: TransformationPillar): void {
    this.patch({ currentPillar: pillar });
  }

  setMonth(month: number): void {
    const pillar: TransformationPillar =
      month <= 6  ? 'reconocimiento' :
      month <= 18 ? 'amor' : 'entrega';

    this.patch({
      currentMonth: month,
      currentPillar: pillar,
      currentMilestoneLabel: `M${month}`,
      progressPercent: Math.round((month / 36) * 100),
    });
    this.familyState.setMilestone(`M${month}`);
  }

  setSprint(sprintNumber: number): void {
    this.patch({ currentSprintNumber: sprintNumber });
  }

  setActiveMission(missionId: string | null): void {
    this.patch({ activeMissionId: missionId });
  }

  updateProgress(percent: number): void {
    this.patch({ progressPercent: Math.min(100, Math.max(0, percent)) });
  }

  /**
   * Carga el estado desde el backend para la familia activa.
   * Llamar cuando se selecciona/cambia la familia (ej. FamilyListPage).
   */
  loadFromBackend(familyId: number): void {
    if (!familyId || familyId === 0) return;
    this.http.get<any>(`/api/families/${familyId}/transformation`).pipe(
      catchError(() => of(null))
    ).subscribe(server => {
      if (!server) return;
      const mapped: Partial<TransformationState> = {};
      if (server.onboardingStep) {
        mapped.onboardingStep = server.onboardingStep.toLowerCase().replace('_', '-') as OnboardingStep;
      }
      if (server.currentPillar) {
        mapped.currentPillar = server.currentPillar.toLowerCase() as TransformationState['currentPillar'];
      }
      if (server.currentMonth)       mapped.currentMonth        = server.currentMonth;
      if (server.currentSprintNumber) mapped.currentSprintNumber = server.currentSprintNumber;
      if (server.progressPercent !== undefined) mapped.progressPercent = server.progressPercent;
      if (server.milestoneLabel)     mapped.currentMilestoneLabel = server.milestoneLabel;
      if (server.activeMissionId)    mapped.activeMissionId     = String(server.activeMissionId);
      this.patch(mapped);
    });
  }

  /** Sincroniza el estado desde el backend (versión manual) */
  syncFromBackend(serverState: Partial<TransformationState>): void {
    this.patch(serverState);
  }

  reset(): void {
    const fresh = { ...DEFAULT_STATE };
    this._state.set(fresh);
    this.persist(fresh);
  }

  // ── Helpers ──────────────────────────────────────────────────────────────────

  private patch(partial: Partial<TransformationState>): void {
    const next = { ...this._state(), ...partial };
    this._state.set(next);
    this.persist(next);
  }

  private persist(state: TransformationState): void {
    try {
      const key = `${STORAGE_KEY}_${this.familyState.currentFamilyId()}`;
      localStorage.setItem(key, JSON.stringify(state));
    } catch { /* storage not available */ }
  }

  private loadState(): TransformationState {
    try {
      const familyId = localStorage.getItem('selectedFamilyId') ?? '0';
      const raw = localStorage.getItem(`${STORAGE_KEY}_${familyId}`);
      if (raw) return { ...DEFAULT_STATE, ...JSON.parse(raw) };
    } catch { /* ignore */ }
    return { ...DEFAULT_STATE };
  }
}
