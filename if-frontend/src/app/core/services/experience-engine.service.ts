import { Injectable, inject, signal, computed, effect } from '@angular/core';
import { DOCUMENT } from '@angular/common';
import { FamilyContextService, FamilyContextDto } from './family-context.service';
import { FamilyStateService } from './family-state.service';
import { catchError, of } from 'rxjs';

export type ExperienceMood =
  | 'EN_CRISIS' | 'TENSO' | 'SERENO' | 'CRECIENDO' | 'CELEBRANDO' | 'NEUTRO';

export interface ExperienceProfile {
  mood:         ExperienceMood;
  icon:         string;
  headline:     string;
  subline:      string;
  accentColor:  string;
  accentFaint:  string;
  accentBorder: string;
  bodyClass:    string;    // clase CSS que se aplica al <body>
  showBanner:   boolean;
  streakMessage: string | null;
  alertCount:   number;
  recommendations: string[];
}

const PROFILES: Record<ExperienceMood, Omit<ExperienceProfile,
  'showBanner' | 'streakMessage' | 'alertCount' | 'recommendations' | 'mood'>> = {
  EN_CRISIS: {
    icon: '🫂',
    headline: 'Tu familia está siendo acompañada',
    subline:  'El sistema está en modo de apoyo. Vayan a su ritmo.',
    accentColor:  '#ef4444',
    accentFaint:  'rgba(239,68,68,0.06)',
    accentBorder: 'rgba(239,68,68,0.2)',
    bodyClass:    'exp-crisis',
  },
  TENSO: {
    icon: '🌊',
    headline: 'Hay tensión — pero también hay camino',
    subline:  'Los momentos difíciles también forman a las grandes familias.',
    accentColor:  '#f97316',
    accentFaint:  'rgba(249,115,22,0.06)',
    accentBorder: 'rgba(249,115,22,0.2)',
    bodyClass:    'exp-tension',
  },
  SERENO: {
    icon: '🟠',
    headline: 'Juntos hoy — avancemos un paso más',
    subline:  'No tienen que hacerlo perfecto. Solo den el siguiente paso.',
    accentColor:  '#f97316',
    accentFaint:  'rgba(249,115,22,0.06)',
    accentBorder: 'rgba(249,115,22,0.2)',
    bodyClass:    'exp-serene',
  },
  CRECIENDO: {
    icon: '🌱',
    headline: 'Tu familia está creciendo',
    subline:  'Se nota el esfuerzo. Sigan construyendo juntos.',
    accentColor:  '#22c55e',
    accentFaint:  'rgba(34,197,94,0.07)',
    accentBorder: 'rgba(34,197,94,0.2)',
    bodyClass:    'exp-growing',
  },
  CELEBRANDO: {
    icon: '🎉',
    headline: '¡Están en racha — sigan así!',
    subline:  'Lo que hacen juntos día a día se está convirtiendo en historia.',
    accentColor:  '#f59e0b',
    accentFaint:  'rgba(245,158,11,0.08)',
    accentBorder: 'rgba(245,158,11,0.25)',
    bodyClass:    'exp-celebrating',
  },
  NEUTRO: {
    icon: '🏠',
    headline: 'Bienvenidos de vuelta',
    subline:  'Cada día es una oportunidad para crecer como familia.',
    accentColor:  '#6366f1',
    accentFaint:  'rgba(99,102,241,0.05)',
    accentBorder: 'rgba(99,102,241,0.15)',
    bodyClass:    'exp-neutral',
  },
};

@Injectable({ providedIn: 'root' })
export class ExperienceEngineService {
  private readonly document      = inject(DOCUMENT);
  private readonly ctxService    = inject(FamilyContextService);
  private readonly familyState   = inject(FamilyStateService);

  // Estado reactivo
  readonly context  = signal<FamilyContextDto | null>(null);
  readonly loading  = signal(false);

  readonly profile = computed<ExperienceProfile>(() => {
    const ctx = this.context();
    return ctx ? this.buildProfile(ctx) : this.neutralProfile();
  });

  constructor() {
    // Aplica la clase al body cada vez que cambia el perfil
    effect(() => {
      const p = this.profile();
      const body = this.document.body;
      // Elimina clases anteriores
      body.classList.remove(
        'exp-crisis', 'exp-tension', 'exp-serene',
        'exp-growing', 'exp-celebrating', 'exp-neutral'
      );
      body.classList.add(p.bodyClass);
    });
  }

  /** Carga el contexto de la familia activa */
  load(): void {
    const id = this.familyState.getSelectedFamilyId();
    if (!id) return;
    this.loading.set(true);
    this.ctxService.get(id).pipe(
      catchError(() => of(null))
    ).subscribe(ctx => {
      this.context.set(ctx);
      this.loading.set(false);
    });
  }

  /** Fuerza recómputo y recarga el contexto */
  refresh(): void {
    const id = this.familyState.getSelectedFamilyId();
    if (!id) return;
    this.ctxService.refresh(id).pipe(
      catchError(() => of(null))
    ).subscribe(ctx => this.context.set(ctx));
  }

  // ─── Construcción del perfil ──────────────────────────────────────────────

  private buildProfile(ctx: FamilyContextDto): ExperienceProfile {
    const mood = ctx.overallMood as ExperienceMood;
    const base = PROFILES[mood] ?? PROFILES['NEUTRO'];

    return {
      mood,
      ...base,
      showBanner: true,
      streakMessage: ctx.currentStreak >= 3
        ? `🔥 ${ctx.currentStreak} días de racha activa`
        : ctx.daysWithoutActivity >= 7
        ? `${ctx.daysWithoutActivity} días sin actividad`
        : null,
      alertCount:   ctx.alerts.length,
      recommendations: ctx.recommendations,
    };
  }

  private neutralProfile(): ExperienceProfile {
    return {
      mood: 'NEUTRO',
      ...PROFILES['NEUTRO'],
      showBanner:    false,
      streakMessage: null,
      alertCount:    0,
      recommendations: [],
    };
  }
}
