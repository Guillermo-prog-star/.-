import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { FamilyContextService, FamilyContextDto } from '../../core/services/family-context.service';
import { FamilyStateService } from '../../core/services/family-state.service';
import { catchError, of } from 'rxjs';

// ─── Configuración visual por señal ────────────────────────────────────────

const MOOD_CFG: Record<string, { icon: string; label: string; color: string; bg: string }> = {
  CELEBRANDO: { icon: '🎉', label: 'Celebrando',      color: '#f59e0b', bg: 'rgba(245,158,11,0.12)'  },
  CRECIENDO:  { icon: '🌱', label: 'En crecimiento',  color: '#22c55e', bg: 'rgba(34,197,94,0.12)'   },
  SERENO:     { icon: '😌', label: 'En calma',         color: '#6366f1', bg: 'rgba(99,102,241,0.12)'  },
  TENSO:      { icon: '😤', label: 'Con tensión',      color: '#f97316', bg: 'rgba(249,115,22,0.12)'  },
  EN_CRISIS:  { icon: '🆘', label: 'En crisis',        color: '#ef4444', bg: 'rgba(239,68,68,0.12)'   },
};

const LEVEL_COLOR: Record<string, string> = {
  ALTA: '#22c55e', MEDIA: '#f59e0b', BAJA: '#ef4444',
  BAJO: '#22c55e', MODERADO: '#f59e0b', ALTO: '#f97316', CRITICO: '#ef4444',
  MEJORANDO: '#22c55e', ESTABLE: '#6366f1', DETERIORANDO: '#ef4444',
  ASCENDENTE: '#22c55e', DESCENDENTE: '#ef4444', CRITICA: '#ef4444',
};

const LEVEL_LABEL: Record<string, string> = {
  ALTA: 'Alta', MEDIA: 'Media', BAJA: 'Baja',
  BAJO: 'Bajo', MODERADO: 'Moderado', ALTO: 'Alto', CRITICO: 'Crítico',
  MEJORANDO: 'Mejorando', ESTABLE: 'Estable', DETERIORANDO: 'Deteriorando',
  ASCENDENTE: 'Ascendente', DESCENDENTE: 'Descendente', CRITICA: 'Crítica',
};

@Component({
  selector: 'app-family-pulse',
  standalone: true,
  imports: [CommonModule, RouterLink],
  template: `
    <div class="fp-page">

      <!-- Header -->
      <div class="fp-header">
        <div class="fp-icon">💓</div>
        <div>
          <h1 class="fp-title">Pulso Familiar</h1>
          <p class="fp-sub">Estado en tiempo real — cómo está tu familia ahora mismo</p>
        </div>
        <button class="btn-refresh" (click)="refresh()" [disabled]="refreshing()">
          {{ refreshing() ? '⏳' : '🔄' }}
        </button>
      </div>

      <!-- Cargando -->
      @if (loading()) {
        <div class="fp-loading">
          <div class="fp-spinner"></div>
          <p>Leyendo el pulso de tu familia...</p>
        </div>
      }

      <!-- Estado disponible -->
      @if (!loading() && ctx()) {
        <!-- Mood general — hero card -->
        <div class="mood-hero" [style.background]="moodBg()">
          <div class="mh-icon">{{ moodIcon() }}</div>
          <div class="mh-info">
            <div class="mh-label">Estado general</div>
            <div class="mh-mood">{{ moodLabel() }}</div>
            @if (ctx()!.currentStreak > 0) {
              <div class="mh-streak">🔥 {{ ctx()!.currentStreak }} días de racha</div>
            }
            @if (ctx()!.daysWithoutActivity > 0) {
              <div class="mh-inactive">{{ ctx()!.daysWithoutActivity }} días sin actividad</div>
            }
          </div>
          @if (ctx()!.icfCurrent != null) {
            <div class="mh-icf">
              <div class="icf-value">{{ ctx()!.icfCurrent!.toFixed(0) }}</div>
              <div class="icf-label">ICF</div>
            </div>
          }
        </div>

        <!-- Grid de señales -->
        <div class="signals-grid">
          <div class="signal-card">
            <div class="sc-label">Conexión</div>
            <div class="sc-value" [style.color]="levelColor(ctx()!.connectionLevel)">
              {{ levelLabel(ctx()!.connectionLevel) }}
            </div>
            <div class="sc-bar">
              <div class="sc-fill" [style.width]="levelPct(ctx()!.connectionLevel)" [style.background]="levelColor(ctx()!.connectionLevel)"></div>
            </div>
          </div>

          <div class="signal-card">
            <div class="sc-label">Estrés</div>
            <div class="sc-value" [style.color]="levelColor(ctx()!.stressLevel)">
              {{ levelLabel(ctx()!.stressLevel) }}
            </div>
            <div class="sc-bar">
              <div class="sc-fill" [style.width]="stressPct(ctx()!.stressLevel)" [style.background]="levelColor(ctx()!.stressLevel)"></div>
            </div>
          </div>

          <div class="signal-card">
            <div class="sc-label">Comunicación</div>
            <div class="sc-value" [style.color]="levelColor(ctx()!.communicationTrend)">
              {{ levelLabel(ctx()!.communicationTrend) }}
            </div>
            <div class="sc-icon-trend">
              {{ ctx()!.communicationTrend === 'MEJORANDO' ? '↑' : ctx()!.communicationTrend === 'DETERIORANDO' ? '↓' : '→' }}
            </div>
          </div>

          <div class="signal-card">
            <div class="sc-label">Participación</div>
            <div class="sc-value" [style.color]="levelColor(ctx()!.participationLevel)">
              {{ levelLabel(ctx()!.participationLevel) }}
            </div>
            <div class="sc-bar">
              <div class="sc-fill" [style.width]="levelPct(ctx()!.participationLevel)" [style.background]="levelColor(ctx()!.participationLevel)"></div>
            </div>
          </div>
        </div>

        <!-- Sprint progress -->
        @if (ctx()!.sprintProgress != null) {
          <div class="sprint-card">
            <div class="spc-top">
              <span class="spc-label">🏃 Sprint activo</span>
              <span class="spc-pct">{{ ctx()!.sprintProgress!.toFixed(0) }}%</span>
            </div>
            <div class="spc-bar">
              <div class="spc-fill" [style.width.%]="ctx()!.sprintProgress!"></div>
            </div>
          </div>
        }

        <!-- Alertas -->
        @if (ctx()!.alerts.length) {
          <div class="alerts-section">
            <div class="as-label">⚠️ Señales de atención</div>
            @for (alert of ctx()!.alerts; track alert) {
              <div class="alert-item">{{ alert }}</div>
            }
          </div>
        }

        <!-- Recomendaciones -->
        @if (ctx()!.recommendations.length) {
          <div class="recs-section">
            <div class="rs-label">✨ Recomendaciones</div>
            @for (rec of ctx()!.recommendations; track rec) {
              <div class="rec-item">
                <span class="rec-dot">→</span>
                <span>{{ rec }}</span>
              </div>
            }
          </div>
        }

        <!-- Rituales activos -->
        @if (ctx()!.activeRitualsCount > 0) {
          <a routerLink="/rituals" class="ritual-banner">
            <span class="rb-icon">🕯️</span>
            <span>{{ ctx()!.activeRitualsCount }} ritual{{ ctx()!.activeRitualsCount > 1 ? 'es' : '' }} pendiente{{ ctx()!.activeRitualsCount > 1 ? 's' : '' }} de vivir</span>
            <span class="rb-arrow">→</span>
          </a>
        }

        <!-- Timestamp -->
        <div class="fp-meta">
          Calculado {{ formatAgo(ctx()!.computedAt) }}
          @if (!ctx()!.fresh) { · desde caché }
        </div>
      }

      <!-- Sin familia -->
      @if (!loading() && !ctx() && !familyId()) {
        <div class="fp-empty">
          <div class="ei">👨‍👩‍👧‍👦</div>
          <p>Selecciona una familia para ver su pulso.</p>
        </div>
      }

      @if (error()) {
        <div class="fp-error">⚠️ {{ error() }}</div>
      }

    </div>
  `,
  styles: [`
    .fp-page {
      max-width: 680px; margin: 0 auto;
      padding: 24px 20px 60px;
      font-family: inherit;
      color: var(--if-text-primary, #e0e0e0);
    }

    /* Header */
    .fp-header { display: flex; align-items: center; gap: 14px; margin-bottom: 24px; }
    .fp-icon   { font-size: 38px; }
    .fp-title  { font-size: 24px; font-weight: 800; margin: 0 0 3px; }
    .fp-sub    { font-size: 13px; color: var(--if-text-secondary, #888); margin: 0; }
    .btn-refresh {
      margin-left: auto; width: 36px; height: 36px;
      background: rgba(255,255,255,0.06);
      border: 1px solid rgba(255,255,255,0.1);
      border-radius: 9px; font-size: 16px;
      cursor: pointer; display: flex; align-items: center; justify-content: center;
      transition: all 0.2s;
    }
    .btn-refresh:disabled { opacity: 0.5; cursor: not-allowed; }

    /* Loading */
    .fp-loading { display: flex; flex-direction: column; align-items: center; gap: 14px; padding: 52px 20px; color: var(--if-text-secondary, #888); }
    .fp-spinner { width: 34px; height: 34px; border: 3px solid rgba(255,255,255,0.07); border-top-color: #6366f1; border-radius: 50%; animation: spin 0.8s linear infinite; }
    @keyframes spin { to { transform: rotate(360deg); } }

    /* Mood hero */
    .mood-hero {
      border-radius: 18px; padding: 24px;
      display: flex; align-items: center; gap: 18px;
      margin-bottom: 20px;
      border: 1px solid rgba(255,255,255,0.08);
      transition: all 0.3s;
    }
    .mh-icon  { font-size: 48px; flex-shrink: 0; }
    .mh-info  { flex: 1; }
    .mh-label { font-size: 10px; font-weight: 700; text-transform: uppercase; letter-spacing: 0.1em; color: rgba(255,255,255,0.5); margin-bottom: 4px; }
    .mh-mood  { font-size: 22px; font-weight: 800; }
    .mh-streak   { font-size: 12px; color: #fbbf24; margin-top: 4px; }
    .mh-inactive { font-size: 12px; color: #94a3b8; margin-top: 4px; }
    .mh-icf   { text-align: center; flex-shrink: 0; }
    .icf-value { font-size: 32px; font-weight: 900; font-variant-numeric: tabular-nums; }
    .icf-label { font-size: 10px; font-weight: 700; text-transform: uppercase; letter-spacing: 0.08em; color: rgba(255,255,255,0.5); }

    /* Grid de señales */
    .signals-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 12px; margin-bottom: 16px; }
    .signal-card {
      background: rgba(255,255,255,0.04);
      border: 1px solid rgba(255,255,255,0.07);
      border-radius: 14px; padding: 16px;
    }
    .sc-label { font-size: 10px; font-weight: 700; text-transform: uppercase; letter-spacing: 0.08em; color: var(--if-text-secondary, #888); margin-bottom: 8px; }
    .sc-value { font-size: 16px; font-weight: 800; margin-bottom: 8px; }
    .sc-bar   { height: 4px; border-radius: 99px; background: rgba(255,255,255,0.07); }
    .sc-fill  { height: 100%; border-radius: 99px; transition: width 0.5s; }
    .sc-icon-trend { font-size: 22px; font-weight: 900; }

    /* Sprint */
    .sprint-card {
      background: rgba(99,102,241,0.08);
      border: 1px solid rgba(99,102,241,0.2);
      border-radius: 14px; padding: 14px 16px;
      margin-bottom: 16px;
    }
    .spc-top  { display: flex; justify-content: space-between; align-items: center; margin-bottom: 8px; }
    .spc-label { font-size: 13px; font-weight: 600; color: #a5b4fc; }
    .spc-pct   { font-size: 15px; font-weight: 800; color: #a5b4fc; }
    .spc-bar   { height: 6px; border-radius: 99px; background: rgba(255,255,255,0.07); }
    .spc-fill  { height: 100%; border-radius: 99px; background: #6366f1; transition: width 0.5s; }

    /* Alertas */
    .alerts-section { margin-bottom: 16px; }
    .as-label { font-size: 11px; font-weight: 700; text-transform: uppercase; letter-spacing: 0.08em; color: #fca5a5; margin-bottom: 10px; }
    .alert-item {
      background: rgba(239,68,68,0.08);
      border: 1px solid rgba(239,68,68,0.2);
      border-radius: 10px; padding: 10px 14px;
      font-size: 13px; color: #fca5a5; margin-bottom: 6px;
    }

    /* Recomendaciones */
    .recs-section { margin-bottom: 16px; }
    .rs-label { font-size: 11px; font-weight: 700; text-transform: uppercase; letter-spacing: 0.08em; color: #86efac; margin-bottom: 10px; }
    .rec-item { display: flex; gap: 10px; align-items: flex-start; padding: 8px 0; border-bottom: 1px solid rgba(255,255,255,0.05); }
    .rec-item:last-child { border-bottom: none; }
    .rec-dot  { color: #4ade80; font-weight: 800; flex-shrink: 0; }

    /* Ritual banner */
    .ritual-banner {
      display: flex; align-items: center; gap: 12px;
      background: rgba(99,102,241,0.1);
      border: 1px solid rgba(99,102,241,0.25);
      border-radius: 12px; padding: 14px 18px;
      margin-bottom: 16px; text-decoration: none;
      color: #a5b4fc; font-size: 14px; font-weight: 600;
      transition: all 0.2s; cursor: pointer;
    }
    .ritual-banner:hover { background: rgba(99,102,241,0.16); }
    .rb-icon  { font-size: 20px; }
    .rb-arrow { margin-left: auto; }

    /* Meta */
    .fp-meta { font-size: 11px; color: var(--if-text-secondary, #666); text-align: right; margin-top: 8px; }

    /* Vacío / error */
    .fp-empty { text-align: center; padding: 52px 20px; color: var(--if-text-secondary, #888); }
    .ei { font-size: 46px; margin-bottom: 12px; }
    .fp-error { background: rgba(239,68,68,0.1); border: 1px solid rgba(239,68,68,0.3); border-radius: 10px; padding: 12px 16px; font-size: 13px; color: #fca5a5; }
  `]
})
export class FamilyPulseComponent implements OnInit {
  private readonly ctxSvc      = inject(FamilyContextService);
  private readonly familyState = inject(FamilyStateService);

  readonly familyId  = this.familyState.currentFamilyId;
  readonly ctx       = signal<FamilyContextDto | null>(null);
  readonly loading   = signal(false);
  readonly refreshing = signal(false);
  readonly error     = signal<string | null>(null);

  ngOnInit(): void {
    const id = this.familyId();
    if (!id) return;
    this.load(id);
  }

  private load(id: number): void {
    this.loading.set(true);
    this.ctxSvc.get(id).pipe(
      catchError(() => { this.error.set('No se pudo cargar el pulso familiar.'); return of(null); })
    ).subscribe(data => {
      this.ctx.set(data);
      this.loading.set(false);
    });
  }

  refresh(): void {
    const id = this.familyId();
    if (!id) return;
    this.refreshing.set(true);
    this.ctxSvc.refresh(id).pipe(
      catchError(() => { this.refreshing.set(false); return of(null); })
    ).subscribe(data => {
      if (data) this.ctx.set(data);
      this.refreshing.set(false);
    });
  }

  moodIcon():  string { return MOOD_CFG[this.ctx()?.overallMood ?? 'SERENO']?.icon  ?? '😌'; }
  moodLabel(): string { return MOOD_CFG[this.ctx()?.overallMood ?? 'SERENO']?.label ?? 'En calma'; }
  moodBg():    string { return MOOD_CFG[this.ctx()?.overallMood ?? 'SERENO']?.bg    ?? 'rgba(99,102,241,0.1)'; }

  levelColor(key: string): string { return LEVEL_COLOR[key] ?? '#6366f1'; }
  levelLabel(key: string): string { return LEVEL_LABEL[key] ?? key; }

  levelPct(level: string): string {
    return { ALTA: '90%', MEDIA: '55%', BAJA: '20%' }[level] ?? '50%';
  }
  stressPct(level: string): string {
    return { BAJO: '15%', MODERADO: '50%', ALTO: '80%', CRITICO: '100%' }[level] ?? '30%';
  }

  formatAgo(iso: string): string {
    try {
      const diff = Date.now() - new Date(iso).getTime();
      const mins = Math.floor(diff / 60000);
      if (mins < 2)  return 'ahora mismo';
      if (mins < 60) return `hace ${mins} minutos`;
      const hrs = Math.floor(mins / 60);
      return `hace ${hrs} hora${hrs > 1 ? 's' : ''}`;
    } catch { return '—'; }
  }
}
