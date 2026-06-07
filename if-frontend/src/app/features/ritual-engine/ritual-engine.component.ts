import { Component, OnInit, inject, signal, computed } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RitualService, RitualDto, RitualType } from '../../core/services/ritual.service';
import { FamilyStateService } from '../../core/services/family-state.service';
import { catchError, of } from 'rxjs';

const RITUAL_CONFIG: Record<RitualType, { icon: string; color: string; bg: string }> = {
  CUMPLEANOS:       { icon: '🎂', color: '#f59e0b', bg: 'rgba(245,158,11,0.1)'  },
  DOMINGO_FAMILIAR: { icon: '🌅', color: '#6366f1', bg: 'rgba(99,102,241,0.1)'  },
  ANIVERSARIO:      { icon: '🎊', color: '#ec4899', bg: 'rgba(236,72,153,0.1)'  },
  LOGRO_CELEBRADO:  { icon: '🏆', color: '#8b5cf6', bg: 'rgba(139,92,246,0.1)' },
  CRISIS_SUPERADA:  { icon: '🌈', color: '#10b981', bg: 'rgba(16,185,129,0.1)' },
  FIN_DE_MES:       { icon: '🌙', color: '#64748b', bg: 'rgba(100,116,139,0.1)' },
  SIN_ACTIVIDAD:    { icon: '🌱', color: '#84cc16', bg: 'rgba(132,204,22,0.1)'  },
  RACHA_POSITIVA:   { icon: '🔥', color: '#f97316', bg: 'rgba(249,115,22,0.1)' },
  PRIMER_ANO:       { icon: '⭐', color: '#eab308', bg: 'rgba(234,179,8,0.1)'   },
  META_ALCANZADA:   { icon: '🎯', color: '#06b6d4', bg: 'rgba(6,182,212,0.1)'   },
};

@Component({
  selector: 'app-ritual-engine',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div class="re-page">

      <!-- Header -->
      <div class="re-header">
        <div class="re-icon">🕯️</div>
        <div>
          <h1 class="re-title">Motor de Rituales</h1>
          <p class="re-sub">Momentos especiales que la IA detecta y activa para tu familia</p>
        </div>
        <button class="btn-detect" (click)="triggerDetect()" [disabled]="detecting()">
          {{ detecting() ? '⏳' : '🔍' }} Detectar
        </button>
      </div>

      <!-- Cargando -->
      @if (loading()) {
        <div class="re-loading">
          <div class="re-spinner"></div>
          <p>Buscando rituales para tu familia...</p>
        </div>
      }

      <!-- Sin familia -->
      @if (!familyId() && !loading()) {
        <div class="re-empty">
          <div class="ei">👨‍👩‍👧‍👦</div>
          <p>Selecciona una familia para ver sus rituales.</p>
        </div>
      }

      <!-- Rituales activos -->
      @if (!loading() && activeRituals().length) {
        <div class="re-section">
          <div class="sec-label">✨ Rituales pendientes de vivir</div>
          <div class="ritual-list">
            @for (r of activeRituals(); track r.id) {
              <div class="ritual-card" [style.border-left-color]="color(r.ritualType)">

                <!-- Cabecera del ritual -->
                <div class="rc-header">
                  <div class="rc-icon-wrap" [style.background]="bg(r.ritualType)">
                    <span class="rc-icon">{{ icon(r.ritualType) }}</span>
                  </div>
                  <div class="rc-meta">
                    <div class="rc-title">{{ r.title }}</div>
                    @if (r.triggerContext) {
                      <div class="rc-context">{{ r.triggerContext }}</div>
                    }
                  </div>
                  <div class="rc-actions">
                    <button class="btn-dismiss" (click)="dismiss(r)" title="Omitir">✕</button>
                  </div>
                </div>

                <!-- Descripción -->
                @if (r.description) {
                  <p class="rc-desc">{{ r.description }}</p>
                }

                <!-- Pasos guiados -->
                @if (r.guidedSteps?.length) {
                  <div class="rc-steps">
                    <div class="steps-label">Pasos guiados</div>
                    @for (step of r.guidedSteps; track $index) {
                      <div class="step-item" [class.done]="completedSteps()[r.id]?.has($index)">
                        <button
                          class="step-check"
                          [style.border-color]="color(r.ritualType)"
                          [style.background]="completedSteps()[r.id]?.has($index) ? color(r.ritualType) : 'transparent'"
                          (click)="toggleStep(r.id, $index)"
                        >
                          @if (completedSteps()[r.id]?.has($index)) { ✓ }
                        </button>
                        <span class="step-text">{{ step }}</span>
                      </div>
                    }
                  </div>
                }

                <!-- Barra de progreso + completar -->
                <div class="rc-footer">
                  <div class="progress-bar">
                    <div
                      class="progress-fill"
                      [style.width.%]="stepProgress(r)"
                      [style.background]="color(r.ritualType)"
                    ></div>
                  </div>
                  @if (allStepsDone(r)) {
                    <button class="btn-complete" [style.background]="color(r.ritualType)" (click)="complete(r)">
                      🎉 Marcar como vivido
                    </button>
                  }
                </div>

              </div>
            }
          </div>
        </div>
      }

      <!-- Sin rituales activos -->
      @if (!loading() && familyId() && !activeRituals().length && !historyRituals().length) {
        <div class="re-empty">
          <div class="ei">🕯️</div>
          <h2>Todo en calma</h2>
          <p>No hay rituales pendientes hoy. El motor detecta automáticamente cada mañana cumpleaños, aniversarios, logros, rachas positivas y más.</p>
          <button class="btn-detect-empty" (click)="triggerDetect()" [disabled]="detecting()">
            {{ detecting() ? 'Detectando...' : '🔍 Buscar rituales ahora' }}
          </button>
        </div>
      }

      <!-- Historial -->
      @if (!loading() && historyRituals().length) {
        <div class="re-section" style="margin-top: 36px">
          <div class="sec-label">📜 Rituales vividos</div>
          <div class="history-list">
            @for (r of historyRituals(); track r.id) {
              <div class="history-item">
                <span class="hi-icon">{{ icon(r.ritualType) }}</span>
                <div class="hi-info">
                  <div class="hi-title">{{ r.title }}</div>
                  <div class="hi-date">{{ formatDate(r.completedAt ?? r.triggeredAt) }}</div>
                </div>
                <span class="hi-badge" [class]="'badge-' + r.status.toLowerCase()">
                  {{ statusLabel(r.status) }}
                </span>
              </div>
            }
          </div>
        </div>
      }

      @if (error()) {
        <div class="re-error">⚠️ {{ error() }}</div>
      }

    </div>
  `,
  styles: [`
    .re-page {
      max-width: 760px;
      margin: 0 auto;
      padding: 24px 20px 60px;
      font-family: inherit;
      color: var(--if-text-primary, #e0e0e0);
    }

    /* Header */
    .re-header { display: flex; align-items: center; gap: 14px; margin-bottom: 28px; }
    .re-icon   { font-size: 40px; flex-shrink: 0; }
    .re-title  { font-size: 24px; font-weight: 800; margin: 0 0 4px; }
    .re-sub    { font-size: 13px; color: var(--if-text-secondary, #888); margin: 0; }
    .btn-detect {
      margin-left: auto; flex-shrink: 0;
      background: rgba(255,255,255,0.06);
      border: 1px solid rgba(255,255,255,0.1);
      color: var(--if-text-secondary, #aaa);
      padding: 8px 16px; border-radius: 9px;
      font-size: 13px; font-weight: 600; cursor: pointer;
      transition: all 0.2s;
    }
    .btn-detect:hover:not(:disabled) { background: rgba(255,255,255,0.1); }
    .btn-detect:disabled { opacity: 0.5; cursor: not-allowed; }

    /* Loading / vacío */
    .re-loading, .re-empty {
      text-align: center; padding: 60px 20px;
      color: var(--if-text-secondary, #888);
    }
    .re-loading { display: flex; flex-direction: column; align-items: center; gap: 16px; }
    .re-spinner {
      width: 36px; height: 36px;
      border: 3px solid rgba(255,255,255,0.08);
      border-top-color: #6366f1;
      border-radius: 50%;
      animation: spin 0.8s linear infinite;
    }
    @keyframes spin { to { transform: rotate(360deg); } }
    .ei { font-size: 48px; margin-bottom: 12px; }
    .re-empty h2 { font-size: 18px; font-weight: 700; margin: 0 0 8px; color: var(--if-text-primary, #ccc); }
    .btn-detect-empty {
      margin-top: 16px;
      background: rgba(99,102,241,0.15);
      border: 1px solid rgba(99,102,241,0.3);
      color: #a5b4fc; padding: 10px 22px;
      border-radius: 10px; font-size: 13px; font-weight: 600;
      cursor: pointer; transition: all 0.2s;
    }
    .btn-detect-empty:disabled { opacity: 0.5; cursor: not-allowed; }

    /* Sección */
    .re-section { margin-bottom: 12px; }
    .sec-label {
      font-size: 11px; font-weight: 800;
      letter-spacing: 0.1em; text-transform: uppercase;
      color: var(--if-text-secondary, #666);
      margin-bottom: 14px;
    }
    .ritual-list { display: flex; flex-direction: column; gap: 16px; }

    /* Tarjeta de ritual */
    .ritual-card {
      background: var(--if-surface, rgba(255,255,255,0.04));
      border: 1px solid rgba(255,255,255,0.07);
      border-left: 4px solid #6366f1;
      border-radius: 16px;
      padding: 20px;
      transition: transform 0.2s;
    }
    .ritual-card:hover { transform: translateY(-1px); }

    .rc-header { display: flex; align-items: flex-start; gap: 14px; margin-bottom: 12px; }
    .rc-icon-wrap {
      width: 44px; height: 44px; border-radius: 12px;
      display: flex; align-items: center; justify-content: center;
      flex-shrink: 0;
    }
    .rc-icon  { font-size: 22px; }
    .rc-meta  { flex: 1; }
    .rc-title { font-size: 16px; font-weight: 700; line-height: 1.3; margin-bottom: 3px; }
    .rc-context { font-size: 11px; color: var(--if-text-secondary, #888); }
    .rc-actions { flex-shrink: 0; }
    .btn-dismiss {
      background: transparent; border: none;
      color: var(--if-text-secondary, #666);
      font-size: 14px; cursor: pointer; padding: 4px 8px;
      border-radius: 6px; transition: all 0.15s;
    }
    .btn-dismiss:hover { background: rgba(239,68,68,0.1); color: #fca5a5; }

    .rc-desc {
      font-size: 13px; color: var(--if-text-secondary, #aaa);
      line-height: 1.6; margin: 0 0 16px;
    }

    /* Pasos guiados */
    .rc-steps { margin-bottom: 16px; }
    .steps-label {
      font-size: 10px; font-weight: 700;
      letter-spacing: 0.08em; text-transform: uppercase;
      color: var(--if-text-secondary, #777);
      margin-bottom: 10px;
    }
    .step-item {
      display: flex; align-items: flex-start; gap: 12px;
      padding: 8px 0;
      border-bottom: 1px solid rgba(255,255,255,0.05);
      transition: opacity 0.2s;
    }
    .step-item.done { opacity: 0.6; }
    .step-item:last-child { border-bottom: none; }
    .step-check {
      width: 22px; height: 22px; border-radius: 50%;
      border: 2px solid #6366f1;
      background: transparent;
      color: white; font-size: 11px; font-weight: 800;
      cursor: pointer; flex-shrink: 0;
      display: flex; align-items: center; justify-content: center;
      transition: all 0.18s;
    }
    .step-text {
      font-size: 13px; line-height: 1.5;
      color: var(--if-text-primary, #ccc);
      padding-top: 2px;
    }

    /* Footer del ritual */
    .rc-footer { display: flex; align-items: center; gap: 14px; margin-top: 4px; }
    .progress-bar {
      flex: 1; height: 4px; border-radius: 99px;
      background: rgba(255,255,255,0.07);
    }
    .progress-fill { height: 100%; border-radius: 99px; transition: width 0.3s; }
    .btn-complete {
      border: none; color: white;
      padding: 8px 18px; border-radius: 9px;
      font-size: 13px; font-weight: 700;
      cursor: pointer; white-space: nowrap;
      transition: opacity 0.2s;
    }
    .btn-complete:hover { opacity: 0.85; }

    /* Historial */
    .history-list { display: flex; flex-direction: column; gap: 8px; }
    .history-item {
      display: flex; align-items: center; gap: 12px;
      padding: 12px 14px;
      background: rgba(255,255,255,0.03);
      border: 1px solid rgba(255,255,255,0.06);
      border-radius: 10px;
    }
    .hi-icon  { font-size: 20px; flex-shrink: 0; }
    .hi-info  { flex: 1; }
    .hi-title { font-size: 13px; font-weight: 600; }
    .hi-date  { font-size: 11px; color: var(--if-text-secondary, #777); margin-top: 2px; }
    .hi-badge {
      font-size: 10px; font-weight: 700;
      padding: 3px 8px; border-radius: 99px;
      text-transform: uppercase; letter-spacing: 0.06em;
    }
    .badge-completed { background: rgba(16,185,129,0.15); color: #6ee7b7; }
    .badge-dismissed { background: rgba(100,116,139,0.15); color: #94a3b8; }
    .badge-active    { background: rgba(99,102,241,0.15); color: #a5b4fc; }
    .badge-pending   { background: rgba(245,158,11,0.15); color: #fcd34d; }

    /* Error */
    .re-error {
      background: rgba(239,68,68,0.1); border: 1px solid rgba(239,68,68,0.3);
      border-radius: 10px; padding: 14px 18px;
      font-size: 13px; color: #fca5a5; margin-top: 20px;
    }
  `]
})
export class RitualEngineComponent implements OnInit {
  private readonly ritualSvc  = inject(RitualService);
  private readonly familyState = inject(FamilyStateService);

  readonly familyId      = this.familyState.currentFamilyId;
  readonly loading       = signal(false);
  readonly detecting     = signal(false);
  readonly error         = signal<string | null>(null);
  readonly activeRituals = signal<RitualDto[]>([]);
  readonly historyRituals = signal<RitualDto[]>([]);

  // Pasos completados por ritual: { [ritualId]: Set<stepIndex> }
  readonly completedSteps = signal<Record<number, Set<number>>>({});

  ngOnInit(): void {
    const id = this.familyId();
    if (!id) return;
    this.load(id);
  }

  private load(id: number): void {
    this.loading.set(true);
    this.error.set(null);

    this.ritualSvc.getActive(id).pipe(
      catchError(() => { this.error.set('No se pudieron cargar los rituales.'); return of([]); })
    ).subscribe(data => {
      this.activeRituals.set(data);
      this.loading.set(false);
    });

    this.ritualSvc.getHistory(id).pipe(
      catchError(() => of([]))
    ).subscribe(data => {
      this.historyRituals.set(data.filter(r => r.status !== 'PENDING'));
    });
  }

  triggerDetect(): void {
    const id = this.familyId();
    if (!id) return;
    this.detecting.set(true);
    this.ritualSvc.detect(id).pipe(
      catchError(() => of(null))
    ).subscribe(() => {
      this.detecting.set(false);
      this.load(id);
    });
  }

  toggleStep(ritualId: number, stepIndex: number): void {
    this.completedSteps.update(current => {
      const next = { ...current };
      const set = new Set(next[ritualId] ?? []);
      if (set.has(stepIndex)) set.delete(stepIndex);
      else set.add(stepIndex);
      next[ritualId] = set;
      return next;
    });
  }

  stepProgress(r: RitualDto): number {
    const total = r.guidedSteps?.length ?? 0;
    if (!total) return 0;
    const done = this.completedSteps()[r.id]?.size ?? 0;
    return Math.round((done / total) * 100);
  }

  allStepsDone(r: RitualDto): boolean {
    const total = r.guidedSteps?.length ?? 0;
    if (!total) return true;
    return (this.completedSteps()[r.id]?.size ?? 0) >= total;
  }

  complete(r: RitualDto): void {
    const id = this.familyId();
    if (!id) return;
    this.ritualSvc.complete(id, r.id).pipe(catchError(() => of(null))).subscribe(() => {
      this.activeRituals.update(list => list.filter(x => x.id !== r.id));
      this.historyRituals.update(list => [{ ...r, status: 'COMPLETED' }, ...list]);
    });
  }

  dismiss(r: RitualDto): void {
    const id = this.familyId();
    if (!id) return;
    this.ritualSvc.dismiss(id, r.id).pipe(catchError(() => of(null))).subscribe(() => {
      this.activeRituals.update(list => list.filter(x => x.id !== r.id));
    });
  }

  icon(type: RitualType): string  { return RITUAL_CONFIG[type]?.icon  ?? '🕯️'; }
  color(type: RitualType): string { return RITUAL_CONFIG[type]?.color ?? '#6366f1'; }
  bg(type: RitualType): string    { return RITUAL_CONFIG[type]?.bg    ?? 'rgba(99,102,241,0.1)'; }

  statusLabel(status: string): string {
    return { COMPLETED: 'Vivido', DISMISSED: 'Omitido', ACTIVE: 'Activo', PENDING: 'Pendiente' }[status] ?? status;
  }

  formatDate(iso: string | null): string {
    if (!iso) return '—';
    try {
      return new Date(iso).toLocaleDateString('es-CO', { day: '2-digit', month: 'short', year: 'numeric' });
    } catch { return iso; }
  }
}
