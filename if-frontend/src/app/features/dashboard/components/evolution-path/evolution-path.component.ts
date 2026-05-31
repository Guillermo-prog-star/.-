import { Component, Input, OnChanges, SimpleChanges } from '@angular/core';
import { CommonModule } from '@angular/common';

export type NarrativeStage = 'RECONOCIMIENTO' | 'AMOR' | 'ENTREGA';
export type EvolutionPhase = 'inconsciente' | 'reactivo' | 'consciente' | 'pleno';

interface StageConfig {
  key: NarrativeStage;
  label: string;
  subtitle: string;
  icon: string;
  color: string;
  glow: string;
  phases: EvolutionPhase[];
}

/**
 * EvolutionPathComponent — Viaje narrativo de la familia.
 *
 * Visualiza el trayecto RECONOCIMIENTO → AMOR → ENTREGA con:
 *   - Fase actual resaltada
 *   - Progreso dentro de la etapa (inconsciente → reactivo → consciente → pleno)
 *   - Indicador de tendencia (IMPROVING / STABLE / DETERIORATING)
 *   - Delta ICF vs 30 días atrás
 *
 * GAP 4 resuelto: los datos llegaban del backend pero no había
 * ningún componente que los renderizara en el dashboard.
 */
@Component({
  selector: 'app-evolution-path',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div class="evolution-path glass-premium p-6 rounded-3xl border border-white/5">

      <!-- Header -->
      <div class="flex items-center justify-between mb-6">
        <div>
          <h4 class="text-white/30 uppercase tracking-widest text-[10px] font-black flex items-center gap-2">
            <span class="w-2 h-2 rounded-full animate-pulse" [style.background]="currentStageConfig.color"></span>
            Trayecto de Transformación
          </h4>
          <p class="text-white/60 text-xs mt-1">{{ evolutionPhaseLabel }} · Etapa {{ narrativeStage }}</p>
        </div>
        <div class="flex items-center gap-2">
          <span class="text-lg">{{ trendIcon }}</span>
          <div class="text-right">
            <div class="text-xs font-bold" [ngClass]="deltaClass">
              {{ icfDelta30d >= 0 ? '+' : '' }}{{ icfDelta30d | number:'1.0-1' }} pts
            </div>
            <div class="text-[9px] text-white/30 uppercase tracking-wider">vs 30 días</div>
          </div>
        </div>
      </div>

      <!-- Etapas narrativas -->
      <div class="flex items-center gap-0 mb-6">
        <ng-container *ngFor="let stage of stages; let i = index; let last = last">

          <!-- Nodo de etapa -->
          <div class="flex flex-col items-center flex-shrink-0 cursor-default"
               [class.opacity-40]="stageOrder(stage.key) > stageOrder(safeNarrativeStage)"
               [class.opacity-100]="stageOrder(stage.key) <= stageOrder(safeNarrativeStage)">

            <!-- Círculo -->
            <div class="relative w-12 h-12 rounded-2xl flex items-center justify-center text-xl transition-all duration-500"
                 [style.background]="isActive(stage.key) ? stage.color + '22' : '#ffffff08'"
                 [style.border]="'1px solid ' + (isActive(stage.key) ? stage.color + '60' : '#ffffff10')"
                 [style.box-shadow]="isActive(stage.key) ? '0 0 20px ' + stage.glow : 'none'">
              {{ stage.icon }}
              <!-- Pulso si es la etapa activa -->
              <span *ngIf="isActive(stage.key)"
                    class="absolute -top-1 -right-1 w-3 h-3 rounded-full animate-ping"
                    [style.background]="stage.color">
              </span>
              <span *ngIf="isActive(stage.key)"
                    class="absolute -top-1 -right-1 w-3 h-3 rounded-full"
                    [style.background]="stage.color">
              </span>
            </div>

            <!-- Label -->
            <div class="mt-2 text-center">
              <div class="text-[10px] font-black uppercase tracking-wider"
                   [style.color]="isActive(stage.key) ? stage.color : 'rgba(255,255,255,0.3)'">
                {{ stage.label }}
              </div>
              <div class="text-[9px] text-white/20 mt-0.5">{{ stage.subtitle }}</div>
            </div>
          </div>

          <!-- Conector -->
          <div *ngIf="!last" class="flex-1 h-px mx-2 mt-[-20px]"
               [style.background]="stageOrder(stage.key) < stageOrder(safeNarrativeStage)
                 ? 'linear-gradient(90deg, ' + stage.color + '80, ' + stages[i+1].color + '80)'
                 : 'rgba(255,255,255,0.06)'">
          </div>
        </ng-container>
      </div>

      <!-- Progreso dentro de la etapa actual -->
      <div class="space-y-2">
        <div class="flex justify-between items-center">
          <span class="text-[9px] text-white/30 uppercase tracking-widest font-bold">
            Progreso en {{ currentStageConfig.label }}
          </span>
          <span class="text-[10px] font-bold" [style.color]="currentStageConfig.color">
            {{ phaseProgressLabel }}
          </span>
        </div>

        <!-- Mini-fases dentro de la etapa -->
        <div class="flex gap-1.5">
          <div *ngFor="let phase of currentStageConfig.phases"
               class="flex-1 h-1.5 rounded-full transition-all duration-700"
               [style.background]="isPhaseReached(phase)
                 ? currentStageConfig.color
                 : 'rgba(255,255,255,0.06)'">
          </div>
        </div>

        <div class="text-[10px] text-white/40 mt-1">
          {{ phaseDescription }}
        </div>
      </div>

      <!-- Señales de alerta -->
      <div *ngIf="communicationCollapseActive || consecutiveDeteriorations >= 3 || inActiveCrisis"
           class="mt-4 space-y-2">
        <div *ngIf="inActiveCrisis"
             class="flex items-center gap-2 px-3 py-2 rounded-xl bg-red-500/10 border border-red-500/20">
          <span class="text-sm">🆘</span>
          <span class="text-[10px] text-red-400 font-semibold">Crisis activa — evolución pausada</span>
        </div>
        <div *ngIf="communicationCollapseActive && !inActiveCrisis"
             class="flex items-center gap-2 px-3 py-2 rounded-xl bg-orange-500/10 border border-orange-500/20">
          <span class="text-sm">⚠️</span>
          <span class="text-[10px] text-orange-400 font-semibold">Colapso comunicacional activo</span>
        </div>
        <div *ngIf="consecutiveDeteriorations >= 3 && !inActiveCrisis"
             class="flex items-center gap-2 px-3 py-2 rounded-xl bg-amber-500/10 border border-amber-500/20">
          <span class="text-sm">📉</span>
          <span class="text-[10px] text-amber-400 font-semibold">
            {{ consecutiveDeteriorations }} entradas de deterioro consecutivas
          </span>
        </div>
      </div>

    </div>
  `,
  styles: [`
    .evolution-path { transition: all 0.3s ease; }
  `]
})
export class EvolutionPathComponent implements OnChanges {

  @Input() narrativeStage: string = 'RECONOCIMIENTO';
  @Input() evolutionPhase: string = 'inconsciente';
  @Input() icfDelta30d: number = 0;
  @Input() riskTrend: string = 'STABLE';
  @Input() communicationCollapseActive: boolean = false;
  @Input() consecutiveDeteriorations: number = 0;
  @Input() inActiveCrisis: boolean = false;

  // Tipos internos para la lógica del componente
  get safeNarrativeStage(): NarrativeStage {
    const valid: NarrativeStage[] = ['RECONOCIMIENTO', 'AMOR', 'ENTREGA'];
    return valid.includes(this.narrativeStage as NarrativeStage)
      ? (this.narrativeStage as NarrativeStage)
      : 'RECONOCIMIENTO';
  }

  get safeEvolutionPhase(): EvolutionPhase {
    const valid: EvolutionPhase[] = ['inconsciente', 'reactivo', 'consciente', 'pleno'];
    return valid.includes(this.evolutionPhase as EvolutionPhase)
      ? (this.evolutionPhase as EvolutionPhase)
      : 'inconsciente';
  }

  readonly stages: StageConfig[] = [
    {
      key: 'RECONOCIMIENTO',
      label: 'Reconocimiento',
      subtitle: 'Despertar',
      icon: '👁️',
      color: '#f59e0b',
      glow: '#f59e0b44',
      phases: ['inconsciente', 'reactivo']
    },
    {
      key: 'AMOR',
      label: 'Amor',
      subtitle: 'Transformación',
      icon: '❤️',
      color: '#f43f5e',
      glow: '#f43f5e44',
      phases: ['reactivo', 'consciente']
    },
    {
      key: 'ENTREGA',
      label: 'Entrega',
      subtitle: 'Plenitud',
      icon: '✨',
      color: '#10b981',
      glow: '#10b98144',
      phases: ['consciente', 'pleno']
    }
  ];

  currentStageConfig!: StageConfig;
  evolutionPhaseLabel = '';
  phaseProgressLabel = '';
  phaseDescription = '';
  trendIcon = '→';
  deltaClass = 'text-white/60';

  ngOnChanges(_: SimpleChanges) {
    this.currentStageConfig = this.stages.find(s => s.key === this.safeNarrativeStage) ?? this.stages[0];
    this.evolutionPhaseLabel = this.phaseToLabel(this.safeEvolutionPhase);
    this.phaseProgressLabel  = this.buildPhaseProgressLabel();
    this.phaseDescription    = this.buildPhaseDescription();
    this.trendIcon           = this.buildTrendIcon();
    this.deltaClass          = this.icfDelta30d > 0 ? 'text-emerald-400' :
                               this.icfDelta30d < 0 ? 'text-red-400' : 'text-white/40';
  }

  isActive(key: NarrativeStage): boolean {
    return key === this.safeNarrativeStage;
  }

  stageOrder(key: NarrativeStage): number {
    return { 'RECONOCIMIENTO': 1, 'AMOR': 2, 'ENTREGA': 3 }[key] ?? 1;
  }

  isPhaseReached(phase: EvolutionPhase): boolean {
    const order: Record<EvolutionPhase, number> = {
      inconsciente: 1, reactivo: 2, consciente: 3, pleno: 4
    };
    return order[phase] <= order[this.safeEvolutionPhase];
  }

  private phaseToLabel(phase: EvolutionPhase): string {
    return {
      inconsciente: 'Fase Inconsciente',
      reactivo:     'Fase Reactiva',
      consciente:   'Fase Consciente',
      pleno:        'Fase Plena'
    }[phase] ?? 'Fase Inconsciente';
  }

  private buildPhaseProgressLabel(): string {
    const idx = this.currentStageConfig.phases.indexOf(this.safeEvolutionPhase);
    if (idx < 0) return '1 / ' + this.currentStageConfig.phases.length;
    return `${idx + 1} / ${this.currentStageConfig.phases.length}`;
  }

  private buildPhaseDescription(): string {
    return {
      inconsciente: 'La familia comienza a reconocer sus patrones emocionales.',
      reactivo:     'Reaccionando conscientemente — los primeros cambios son visibles.',
      consciente:   'Actuando con intención — los hábitos se transforman.',
      pleno:        'Viviendo en coherencia — la transformación es sostenida.'
    }[this.safeEvolutionPhase] ?? '';
  }

  private buildTrendIcon(): string {
    return {
      IMPROVING:    '↗️',
      DETERIORATING:'↘️',
      CRITICAL:     '🆘',
      STABLE:       '→',
      UP:           '↗️',
      DOWN:         '↘️'
    }[this.riskTrend] ?? '→';
  }
}
