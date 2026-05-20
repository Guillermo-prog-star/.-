import {
  Component, OnInit, inject, signal, computed, ChangeDetectionStrategy
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { CognitiveService } from '../../../../core/services/cognitive.service';
import { FamilyStateService } from '../../../../core/services/family-state.service';
import { ReflectionResponse, AbandonmentLevel } from '../../../../core/models/cognitive.model';

/**
 * SDD Analytics v2 — Banner de riesgo de abandono.
 * Visible sólo cuando el nivel es HIGH o CRITICAL.
 * Se auto-oculta para LOW/MODERATE para no saturar el dashboard.
 */
@Component({
  selector: 'app-abandonment-risk-banner',
  standalone: true,
  imports: [CommonModule, RouterLink],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <!-- Sólo visible si hay datos y el riesgo es alto -->
    <div *ngIf="shouldShow()"
         class="rounded-3xl p-5 border animate-in slide-in-from-top duration-500"
         [ngClass]="bannerClass()">

      <!-- Icono + título + badge -->
      <div class="flex items-start justify-between gap-4 flex-wrap">
        <div class="flex items-center gap-4">
          <div class="w-11 h-11 rounded-2xl flex items-center justify-center text-2xl flex-shrink-0"
               [ngClass]="iconBgClass()">
            {{ levelIcon() }}
          </div>
          <div>
            <div class="flex items-center gap-2 mb-0.5">
              <h3 class="font-black text-sm uppercase tracking-widest" [ngClass]="titleColor()">
                Riesgo de abandono: {{ levelLabel() }}
              </h3>
              <span class="text-[9px] font-black uppercase tracking-widest px-2 py-0.5 rounded-full"
                    [ngClass]="badgeClass()">
                {{ reflection()!.effectivenessLevel }}
              </span>
            </div>
            <p class="text-white/60 text-xs leading-relaxed max-w-2xl">
              {{ reflection()!.effectivenessSummary }}
            </p>
          </div>
        </div>

        <!-- Métricas rápidas -->
        <div class="flex items-center gap-4 text-center">
          <div>
            <span class="text-[9px] text-white/30 uppercase tracking-widest block">ICF Δ</span>
            <span class="text-lg font-black"
                  [ngClass]="reflection()!.icfTrend >= 0 ? 'text-emerald-400' : 'text-red-400'">
              {{ reflection()!.icfTrend >= 0 ? '+' : '' }}{{ reflection()!.icfTrend | number:'1.1-1' }}
            </span>
          </div>
          <div>
            <span class="text-[9px] text-white/30 uppercase tracking-widest block">Adherencia</span>
            <span class="text-lg font-black text-amber-400">
              {{ reflection()!.avgAdherence | number:'1.0-0' }}%
            </span>
          </div>
          <div>
            <span class="text-[9px] text-white/30 uppercase tracking-widest block">Evals</span>
            <span class="text-lg font-black text-white/60">{{ reflection()!.evaluationCount }}</span>
          </div>
        </div>
      </div>

      <!-- Señales de riesgo -->
      <div *ngIf="(reflection()?.abandonmentSignals?.length ?? 0) > 0"
           class="mt-4 flex flex-wrap gap-2">
        <span class="text-[9px] text-white/30 uppercase tracking-widest self-center">Señales:</span>
        <span *ngFor="let signal of reflection()!.abandonmentSignals"
              class="px-2 py-1 text-[10px] font-bold uppercase tracking-widest rounded-lg"
              [ngClass]="signalClass()">
          {{ signalLabel(signal) }}
        </span>
      </div>

      <!-- CTA -->
      <div class="mt-4 flex items-center gap-3 flex-wrap">
        <a routerLink="/cognitive"
           class="px-4 py-2 text-[10px] font-black uppercase tracking-widest rounded-xl
                  transition-all hover:scale-105 active:scale-95"
           [ngClass]="ctaClass()">
          Ver sistema cognitivo →
        </a>
        <button (click)="dismiss()"
                class="px-3 py-2 text-[10px] font-black uppercase tracking-widest rounded-xl
                       bg-white/5 text-white/30 hover:bg-white/10 hover:text-white/50
                       transition-all">
          Ocultar
        </button>
        <span *ngIf="reflection()!.lessonLearned" class="text-[10px] text-white/30 italic flex-1">
          💡 {{ reflection()!.lessonLearned }}
        </span>
      </div>
    </div>
  `
})
export class AbandonmentRiskBannerComponent implements OnInit {
  private readonly cognitiveService = inject(CognitiveService);
  private readonly familyState      = inject(FamilyStateService);

  loading    = signal(true);
  dismissed  = signal(false);
  reflection = signal<ReflectionResponse | null>(null);

  readonly shouldShow = computed(() => {
    if (this.loading() || this.dismissed() || !this.reflection()) return false;
    const level = this.reflection()!.abandonmentLevel as AbandonmentLevel;
    return level === 'HIGH' || level === 'CRITICAL';
  });

  ngOnInit(): void {
    const id = this.familyState.getSelectedFamilyId();
    if (!id) { this.loading.set(false); return; }
    // Use read-only GET — avoids triggering the full reflection cycle on every dashboard load
    this.cognitiveService.getLatestReflection(id).subscribe({
      next: r => { this.reflection.set(r); this.loading.set(false); },
      error: ()  => this.loading.set(false)
    });
  }

  dismiss(): void { this.dismissed.set(true); }

  // ─── Style helpers ───────────────────────────────────────────────────────

  levelIcon(): string {
    return this.reflection()?.abandonmentLevel === 'CRITICAL' ? '🚨' : '⚠️';
  }

  levelLabel(): string {
    const map: Record<AbandonmentLevel, string> = {
      LOW: 'BAJO', MODERATE: 'MODERADO', HIGH: 'ALTO', CRITICAL: 'CRÍTICO'
    };
    return map[this.reflection()?.abandonmentLevel as AbandonmentLevel] ?? '—';
  }

  bannerClass(): string {
    return this.reflection()?.abandonmentLevel === 'CRITICAL'
      ? 'bg-red-500/5 border-red-500/30'
      : 'bg-amber-500/5 border-amber-500/30';
  }

  iconBgClass(): string {
    return this.reflection()?.abandonmentLevel === 'CRITICAL'
      ? 'bg-red-500/15' : 'bg-amber-500/15';
  }

  titleColor(): string {
    return this.reflection()?.abandonmentLevel === 'CRITICAL'
      ? 'text-red-400' : 'text-amber-400';
  }

  badgeClass(): string {
    const level = this.reflection()?.effectivenessLevel;
    return level === 'REGRESSING'
      ? 'bg-red-500/10 text-red-400'
      : level === 'LOW'
        ? 'bg-orange-500/10 text-orange-400'
        : 'bg-amber-500/10 text-amber-400';
  }

  signalClass(): string {
    return this.reflection()?.abandonmentLevel === 'CRITICAL'
      ? 'bg-red-500/10 text-red-400 border border-red-500/10'
      : 'bg-amber-500/10 text-amber-400 border border-amber-500/10';
  }

  ctaClass(): string {
    return this.reflection()?.abandonmentLevel === 'CRITICAL'
      ? 'bg-red-500/15 text-red-400 hover:bg-red-500/25'
      : 'bg-amber-500/15 text-amber-400 hover:bg-amber-500/25';
  }

  signalLabel(signal: string): string {
    const labels: Record<string, string> = {
      INACTIVITY_14D:          '⏱ Sin actividad +14d',
      ADHERENCE_DROP_SEVERE:   '📉 Adherencia caída severa',
      ADHERENCE_DROP_MODERATE: '📉 Adherencia caída moderada',
      NEGATIVE_REPEAT_INTENT:  '🚫 No desean continuar',
      ICF_DECLINING:           '↓ ICF en descenso'
    };
    return labels[signal] ?? signal;
  }
}
