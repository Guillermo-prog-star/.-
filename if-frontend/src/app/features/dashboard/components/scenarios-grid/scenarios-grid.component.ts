import {
  Component, inject, computed, ChangeDetectionStrategy
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { DashboardDataService } from '../../services/dashboard-data.service';
import { FamilyStateService } from '../../../../core/services/family-state.service';
import { SuggestedAction } from '../../../../core/models/dashboard.model';

interface ScenarioCard {
  icon: string;
  title: string;
  status: string;
  description: string;
  theme: 'emerald' | 'red' | 'indigo' | 'amber' | 'teal' | 'rose' | 'blue';
  badge?: string;
  completed?: boolean;
}

/**
 * SDD — Matriz de Escenarios y Acciones IA.
 * Visualiza la proyección principal del sistema IA más las acciones del plan activo.
 * Rediseñado con Tailwind glassmorphism y ChangeDetectionStrategy.OnPush.
 */
@Component({
  selector: 'app-scenarios-grid',
  standalone: true,
  imports: [CommonModule, RouterLink],
  changeDetection: ChangeDetectionStrategy.OnPush,
  styles: [],
  template: `
    <div class="glass-premium p-6 rounded-[2rem] border border-white/5
                animate-in fade-in slide-in-from-bottom-4 duration-700 delay-200">

      <!-- Header -->
      <div class="flex items-center justify-between mb-6">
        <div class="flex items-center gap-3">
          <div class="w-9 h-9 bg-indigo-500/15 rounded-xl flex items-center justify-center text-lg">🎯</div>
          <div>
            <h3 class="font-bold text-white/90 text-sm">Matriz de Escenarios</h3>
            <span class="text-[9px] text-white/30 uppercase tracking-[0.15em]">
              Proyección IA · Acciones del Plan
            </span>
          </div>
        </div>
        <button (click)="refresh()"
                class="text-[9px] font-black uppercase tracking-widest text-white/20
                       hover:text-white/60 transition-colors flex items-center gap-1 px-3 py-2
                       bg-white/[0.02] rounded-xl border border-white/5 hover:bg-white/5">
          ↻ Sincronizar
        </button>
      </div>

      <!-- Pillar Progress Bar -->
      @if (pillarProgress() !== null) {
      <div class="mb-6 p-4 bg-white/[0.02] rounded-2xl border border-white/5">
        <div class="flex justify-between items-center mb-2">
          <span class="text-[9px] font-black uppercase tracking-widest text-white/30">Progreso del Pilar</span>
          <span class="text-xs font-black text-white/70">{{ pillarProgress() }}%</span>
        </div>
        <div class="h-1.5 bg-white/5 rounded-full overflow-hidden">
          <div class="h-full rounded-full transition-all duration-700 ease-out"
               [style.width.%]="pillarProgress()"
               [ngClass]="pillarProgress()! >= 70 ? 'bg-gradient-to-r from-emerald-500 to-teal-500'
                         : pillarProgress()! >= 40 ? 'bg-gradient-to-r from-indigo-500 to-blue-500'
                         : 'bg-gradient-to-r from-amber-500 to-orange-500'">
          </div>
        </div>
        <div class="flex justify-between mt-1.5">
          <span class="text-[8px] text-white/20">{{ completedTasks() }} / {{ totalTasks() }} misiones</span>
          <a routerLink="/plans" class="text-[8px] text-indigo-400/50 hover:text-indigo-400 uppercase tracking-widest transition-colors">
            Ver plan →
          </a>
        </div>
      </div>
      }

      <!-- Loading skeleton -->
      @if (loading()) {
      <div class="grid grid-cols-1 md:grid-cols-2 gap-3">
        @for (_ of [1,2,3,4]; track _) {
        <div class="h-28 bg-white/[0.02] rounded-2xl border border-white/5 animate-pulse"></div>
        }
      </div>
      }

      <!-- Scenarios grid -->
      @if (!loading()) {
      <div class="grid grid-cols-1 md:grid-cols-2 gap-3">
        @for (card of cards(); track card.title) {
        <div class="group p-4 rounded-2xl border transition-all duration-300 hover:-translate-y-0.5"
             [ngClass]="cardBg(card.theme)">

          <!-- Card header -->
          <div class="flex items-start justify-between gap-3 mb-2">
            <div class="flex items-center gap-2.5">
              <span class="text-lg leading-none">{{ card.icon }}</span>
              <div>
                <p class="text-[10px] font-black uppercase tracking-widest" [ngClass]="cardTitle(card.theme)">
                  {{ card.title }}
                </p>
                <div class="flex items-center gap-1.5 mt-0.5">
                  <span class="w-1.5 h-1.5 rounded-full" [ngClass]="statusDot(card.theme)"></span>
                  <span class="text-[9px] font-bold uppercase tracking-wider text-white/40">
                    {{ card.status }}
                  </span>
                </div>
              </div>
            </div>
            @if (card.badge) {
            <span class="text-[8px] font-black uppercase tracking-widest px-2 py-0.5 rounded-full flex-shrink-0"
                  [ngClass]="badgeClass(card.theme)">
              {{ card.badge }}
            </span>
            }
            @if (card.completed === true) {
            <span class="text-[8px] font-black uppercase tracking-widest px-2 py-0.5 rounded-full
                         bg-emerald-500/10 text-emerald-400 flex-shrink-0">
              ✓ Hecho
            </span>
            }
          </div>

          <!-- Description -->
          <p class="text-[11px] text-white/55 leading-relaxed line-clamp-3">
            {{ card.description }}
          </p>
        </div>
        }

        <!-- Empty state -->
        @if (cards().length === 0) {
        <div class="col-span-2 text-center py-10 text-white/20">
          <p class="text-3xl mb-3">🧭</p>
          <p class="text-xs font-bold uppercase tracking-widest">
            Sin proyecciones disponibles
          </p>
          <p class="text-[10px] mt-1 text-white/20">
            Completa un diagnóstico para activar el motor IA.
          </p>
        </div>
        }
      </div>
      }
    </div>
  `
})
export class ScenariosGridComponent {
  private readonly dashboardSvc = inject(DashboardDataService);
  private readonly familyState  = inject(FamilyStateService);

  /** Loading while signal has no data yet */
  readonly loading = computed(() => this.dashboardSvc.dashboardStateSignal() === null);

  // Reactive selectors from dashboard stream
  readonly pillarProgress = computed(() =>
    this.dashboardSvc.dashboardStateSignal()?.pillarProgress ?? null
  );
  readonly completedTasks = computed(() =>
    this.dashboardSvc.dashboardStateSignal()?.completedPlanTasks ?? 0
  );
  readonly totalTasks = computed(() =>
    this.dashboardSvc.dashboardStateSignal()?.totalPlanTasks ?? 0
  );

  readonly cards = computed<ScenarioCard[]>(() => {
    const data = this.dashboardSvc.dashboardStateSignal();
    if (!data) return [];

    const list: ScenarioCard[] = [];
    const isCritical = data.activeCrisesCount > 0;

    // 1. AI projection card
    if (data.aiRecommendation) {
      list.push({
        icon: isCritical ? '🚨' : '🤖',
        title: 'Proyección IA',
        status: isCritical ? 'Atención Crítica' : 'Evolución Estable',
        description: data.aiRecommendation,
        theme: isCritical ? 'red' : 'emerald',
        badge: isCritical ? 'CRÍTICO' : 'ESTABLE'
      });
    }

    // 2. Suggested actions from the active plan
    if (data.suggestedActions?.length) {
      data.suggestedActions.slice(0, 5).forEach((action: SuggestedAction) => {
        list.push({
          icon: this.dimensionIcon(action.dimension),
          title: action.dimension || 'Estrategia',
          status: action.completed ? 'Completada' : 'En curso',
          description: action.description,
          theme: this.dimensionTheme(action.dimension),
          completed: action.completed
        });
      });
    }

    return list;
  });

  refresh(): void {
    const id = this.familyState.getSelectedFamilyId();
    if (id) this.dashboardSvc.fetchData(id).subscribe();
  }

  trackCard(_: number, c: ScenarioCard): string { return c.title; }

  // ─── Style helpers ───────────────────────────────────────────────────────

  cardBg(theme: ScenarioCard['theme']): string {
    const map: Record<ScenarioCard['theme'], string> = {
      emerald: 'bg-emerald-500/5 border-emerald-500/15 hover:bg-emerald-500/10 hover:border-emerald-500/30',
      red:     'bg-red-500/5 border-red-500/15 hover:bg-red-500/10 hover:border-red-500/30',
      indigo:  'bg-indigo-500/5 border-indigo-500/15 hover:bg-indigo-500/10 hover:border-indigo-500/30',
      amber:   'bg-amber-500/5 border-amber-500/15 hover:bg-amber-500/10 hover:border-amber-500/30',
      teal:    'bg-teal-500/5 border-teal-500/15 hover:bg-teal-500/10 hover:border-teal-500/30',
      rose:    'bg-rose-500/5 border-rose-500/15 hover:bg-rose-500/10 hover:border-rose-500/30',
      blue:    'bg-blue-500/5 border-blue-500/15 hover:bg-blue-500/10 hover:border-blue-500/30',
    };
    return map[theme];
  }

  cardTitle(theme: ScenarioCard['theme']): string {
    const map: Record<ScenarioCard['theme'], string> = {
      emerald: 'text-emerald-400', red: 'text-red-400',    indigo: 'text-indigo-400',
      amber:   'text-amber-400',   teal: 'text-teal-400',  rose:   'text-rose-400',
      blue:    'text-blue-400'
    };
    return map[theme];
  }

  statusDot(theme: ScenarioCard['theme']): string {
    const map: Record<ScenarioCard['theme'], string> = {
      emerald: 'bg-emerald-500 animate-pulse', red: 'bg-red-500 animate-pulse',
      indigo:  'bg-indigo-500',                amber: 'bg-amber-500',
      teal:    'bg-teal-500',                  rose: 'bg-rose-500',
      blue:    'bg-blue-500'
    };
    return map[theme];
  }

  badgeClass(theme: ScenarioCard['theme']): string {
    const map: Record<ScenarioCard['theme'], string> = {
      emerald: 'bg-emerald-500/10 text-emerald-400', red: 'bg-red-500/10 text-red-400',
      indigo:  'bg-indigo-500/10 text-indigo-400',   amber: 'bg-amber-500/10 text-amber-400',
      teal:    'bg-teal-500/10 text-teal-400',       rose: 'bg-rose-500/10 text-rose-400',
      blue:    'bg-blue-500/10 text-blue-400'
    };
    return map[theme];
  }

  private dimensionTheme(dim: string): ScenarioCard['theme'] {
    const map: Record<string, ScenarioCard['theme']> = {
      'EMOCIONES': 'rose',    'COMUNICACION': 'indigo',
      'HABITOS': 'teal',      'TIEMPOS': 'amber',
      'GENERAL': 'blue'
    };
    return map[dim?.toUpperCase()] ?? 'indigo';
  }

  private dimensionIcon(dim: string): string {
    const map: Record<string, string> = {
      'EMOCIONES': '💜', 'COMUNICACION': '💬',
      'HABITOS': '🔄',   'TIEMPOS': '⏱️', 'GENERAL': '⚡'
    };
    return map[dim?.toUpperCase()] ?? '🎯';
  }
}
