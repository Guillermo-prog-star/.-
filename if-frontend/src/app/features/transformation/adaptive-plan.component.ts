import { Component, OnInit, inject, signal, computed } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import {
  AdaptivePlanService,
  AdaptiveAdjustmentEntity,
  AdjustmentStatus,
  AdaptiveRuleType
} from '../../core/services/adaptive-plan.service';
import { FamilyStateService } from '../../core/services/family-state.service';
import { catchError, of } from 'rxjs';

const RULE_CONFIG: Record<AdaptiveRuleType, { icon: string; title: string; desc: string; color: string; bg: string }> = {
  REDUCE_LOAD: {
    icon: '⚖️',
    title: 'Reducir Carga',
    desc: 'Espaciar misiones para aliviar la presión sobre la familia',
    color: '#f59e0b',
    bg: 'rgba(245,158,11,0.08)'
  },
  SOFT_RESET: {
    icon: '🔄',
    title: 'Reinicio Suave',
    desc: 'Agregar misiones introductorias para retomar el ritmo',
    color: '#6366f1',
    bg: 'rgba(99,102,241,0.08)'
  },
  GUIDED_LISTENING: {
    icon: '🎧',
    title: 'Escucha Guiada',
    desc: 'Introducir misiones de escucha activa para reconectar',
    color: '#22d3ee',
    bg: 'rgba(34,211,238,0.08)'
  },
  PAUSE_NON_CRITICAL: {
    icon: '⏸️',
    title: 'Pausa Estratégica',
    desc: 'Posponer tareas no críticas y enfocarse en lo esencial',
    color: '#94a3b8',
    bg: 'rgba(148,163,184,0.08)'
  }
};

const STATUS_CONFIG: Record<AdjustmentStatus, { label: string; color: string; bg: string }> = {
  PROPOSED: { label: 'Propuesto',  color: '#f59e0b', bg: 'rgba(245,158,11,0.15)'  },
  APPROVED: { label: 'Aprobado',   color: '#6366f1', bg: 'rgba(99,102,241,0.15)'  },
  APPLIED:  { label: 'Aplicado',   color: '#22c55e', bg: 'rgba(34,197,94,0.15)'   },
  REJECTED: { label: 'Rechazado',  color: '#ef4444', bg: 'rgba(239,68,68,0.15)'   }
};

@Component({
  selector: 'app-adaptive-plan',
  standalone: true,
  imports: [CommonModule, RouterLink],
  template: `
<div class="ap-page">

  <!-- Header -->
  <div class="ap-header">
    <div class="ap-icon">🧠</div>
    <div class="ap-header-text">
      <h1 class="ap-title">Integración Adaptativa</h1>
      <p class="ap-sub">El motor inteligente evalúa el estado familiar y propone ajustes al plan de mejora</p>
    </div>
    <button class="btn-evaluate" (click)="evaluate()" [disabled]="evaluating()">
      @if (evaluating()) { ⏳ Evaluando… } @else { ⚡ Evaluar ahora }
    </button>
  </div>

  <!-- Leyenda de estados -->
  <div class="ap-legend">
    @for (s of statuses; track s) {
      <span class="legend-chip" [style.color]="statusCfg(s).color" [style.background]="statusCfg(s).bg">
        {{ statusCfg(s).label }}
      </span>
    }
    <span class="legend-total">{{ adjustments().length }} ajuste{{ adjustments().length !== 1 ? 's' : '' }} en total</span>
  </div>

  <!-- Cargando -->
  @if (loading()) {
    <div class="ap-loading">
      <div class="ap-spinner"></div>
      <p>Cargando ajustes adaptativos…</p>
    </div>
  }

  <!-- Sin datos -->
  @if (!loading() && adjustments().length === 0) {
    <div class="ap-empty">
      <div class="ap-empty-icon">🤖</div>
      <p class="ap-empty-title">Sin ajustes propuestos aún</p>
      <p class="ap-empty-hint">Haz clic en <strong>⚡ Evaluar ahora</strong> para que el motor analice el estado de la familia y proponga ajustes inteligentes al plan de mejora.</p>
    </div>
  }

  <!-- Lista de ajustes -->
  @if (!loading() && adjustments().length > 0) {

    <!-- Pendientes de acción -->
    @if (proposed().length > 0) {
      <section class="ap-section">
        <h2 class="ap-section-title">🟡 Pendientes de aprobación ({{ proposed().length }})</h2>
        <div class="ap-cards">
          @for (a of proposed(); track a.id) {
            <div class="ap-card ap-card--proposed" [style.border-color]="ruleCfg(a.ruleType).color + '50'">
              <div class="ap-card-header" [style.background]="ruleCfg(a.ruleType).bg">
                <span class="ap-rule-icon">{{ ruleCfg(a.ruleType).icon }}</span>
                <div>
                  <p class="ap-rule-name">{{ ruleCfg(a.ruleType).title }}</p>
                  <p class="ap-rule-desc">{{ ruleCfg(a.ruleType).desc }}</p>
                </div>
                <span class="ap-status-badge" [style.color]="statusCfg(a.status).color" [style.background]="statusCfg(a.status).bg">
                  {{ statusCfg(a.status).label }}
                </span>
              </div>
              <p class="ap-reason">{{ a.reason }}</p>
              <p class="ap-date">Propuesto el {{ formatDate(a.createdAt) }}</p>
              <div class="ap-actions">
                <button class="btn-approve" (click)="approve(a)" [disabled]="busy() === a.id">
                  @if (busy() === a.id) { ⏳ } @else { ✅ } Aprobar
                </button>
                <button class="btn-reject" (click)="reject(a)" [disabled]="busy() === a.id">
                  ✕ No aplicar
                </button>
              </div>
            </div>
          }
        </div>
      </section>
    }

    <!-- Aprobados, listos para aplicar -->
    @if (approved().length > 0) {
      <section class="ap-section">
        <h2 class="ap-section-title">🟣 Aprobados — listos para aplicar ({{ approved().length }})</h2>
        <div class="ap-cards">
          @for (a of approved(); track a.id) {
            <div class="ap-card ap-card--approved" [style.border-color]="'rgba(99,102,241,0.4)'">
              <div class="ap-card-header" [style.background]="ruleCfg(a.ruleType).bg">
                <span class="ap-rule-icon">{{ ruleCfg(a.ruleType).icon }}</span>
                <div>
                  <p class="ap-rule-name">{{ ruleCfg(a.ruleType).title }}</p>
                  <p class="ap-rule-desc">{{ ruleCfg(a.ruleType).desc }}</p>
                </div>
                <span class="ap-status-badge" [style.color]="statusCfg(a.status).color" [style.background]="statusCfg(a.status).bg">
                  {{ statusCfg(a.status).label }}
                </span>
              </div>
              <p class="ap-reason">{{ a.reason }}</p>
              <p class="ap-date">
                Aprobado el {{ formatDate(a.approvedAt!) }}
                @if (a.approvedBy) { por {{ a.approvedBy }} }
              </p>
              <div class="ap-actions">
                <button class="btn-apply" (click)="apply(a)" [disabled]="busy() === a.id">
                  @if (busy() === a.id) { ⏳ Aplicando… } @else { 🚀 Aplicar ajuste }
                </button>
              </div>
            </div>
          }
        </div>
      </section>
    }

    <!-- Historial -->
    @if (history().length > 0) {
      <section class="ap-section">
        <h2 class="ap-section-title">📋 Historial de ajustes</h2>
        <div class="ap-history">
          @for (a of history(); track a.id) {
            <div class="ap-hist-row">
              <span class="ap-hist-icon">{{ ruleCfg(a.ruleType).icon }}</span>
              <div class="ap-hist-info">
                <span class="ap-hist-name">{{ ruleCfg(a.ruleType).title }}</span>
                <span class="ap-hist-reason">{{ a.reason }}</span>
              </div>
              <div class="ap-hist-right">
                <span class="ap-status-badge" [style.color]="statusCfg(a.status).color" [style.background]="statusCfg(a.status).bg">
                  {{ statusCfg(a.status).label }}
                </span>
                <span class="ap-hist-date">{{ formatDate(a.createdAt) }}</span>
              </div>
            </div>
          }
        </div>
      </section>
    }
  }

</div>
  `,
  styles: [`
    .ap-page { max-width: 860px; margin: 0 auto; padding: 24px 16px 64px; color: #f1f5f9; }

    .ap-header {
      display: flex; align-items: flex-start; gap: 16px;
      background: rgba(255,255,255,0.04); border: 1px solid rgba(255,255,255,0.08);
      border-radius: 20px; padding: 24px; margin-bottom: 20px;
    }
    .ap-icon { font-size: 40px; line-height: 1; }
    .ap-header-text { flex: 1; }
    .ap-title { font-size: 22px; font-weight: 800; margin: 0 0 4px; }
    .ap-sub { font-size: 13px; color: #94a3b8; margin: 0; }

    .btn-evaluate {
      flex-shrink: 0; padding: 10px 20px; border-radius: 12px; border: none; cursor: pointer;
      background: linear-gradient(135deg, #6366f1, #8b5cf6); color: #fff;
      font-weight: 700; font-size: 13px; transition: opacity .2s;
    }
    .btn-evaluate:disabled { opacity: .5; cursor: not-allowed; }

    .ap-legend {
      display: flex; flex-wrap: wrap; align-items: center; gap: 8px;
      margin-bottom: 24px;
    }
    .legend-chip { padding: 4px 12px; border-radius: 99px; font-size: 12px; font-weight: 600; }
    .legend-total { margin-left: auto; font-size: 12px; color: #64748b; }

    .ap-loading { text-align: center; padding: 60px 0; color: #64748b; }
    .ap-spinner {
      width: 36px; height: 36px; border: 3px solid rgba(255,255,255,0.1);
      border-top-color: #6366f1; border-radius: 50%;
      animation: spin .8s linear infinite; margin: 0 auto 16px;
    }
    @keyframes spin { to { transform: rotate(360deg); } }

    .ap-empty { text-align: center; padding: 60px 20px; }
    .ap-empty-icon { font-size: 48px; margin-bottom: 12px; }
    .ap-empty-title { font-size: 18px; font-weight: 700; margin: 0 0 8px; }
    .ap-empty-hint { color: #64748b; font-size: 14px; max-width: 480px; margin: 0 auto; }

    .ap-section { margin-bottom: 32px; }
    .ap-section-title { font-size: 15px; font-weight: 700; margin: 0 0 12px; color: #cbd5e1; }

    .ap-cards { display: flex; flex-direction: column; gap: 12px; }
    .ap-card {
      border: 1px solid; border-radius: 16px; padding: 0;
      background: rgba(255,255,255,0.03); overflow: hidden;
    }
    .ap-card-header {
      display: flex; align-items: center; gap: 12px;
      padding: 14px 16px;
    }
    .ap-rule-icon { font-size: 24px; flex-shrink: 0; }
    .ap-rule-name { font-size: 15px; font-weight: 700; margin: 0 0 2px; }
    .ap-rule-desc { font-size: 12px; color: #94a3b8; margin: 0; }
    .ap-status-badge {
      margin-left: auto; flex-shrink: 0;
      padding: 4px 10px; border-radius: 99px; font-size: 11px; font-weight: 700;
    }

    .ap-reason {
      padding: 12px 16px 4px; font-size: 13px; color: #cbd5e1; margin: 0;
      border-top: 1px solid rgba(255,255,255,0.05);
    }
    .ap-date { padding: 4px 16px 12px; font-size: 11px; color: #475569; margin: 0; }

    .ap-actions {
      display: flex; gap: 8px; padding: 12px 16px;
      border-top: 1px solid rgba(255,255,255,0.05);
    }
    .btn-approve, .btn-apply, .btn-reject {
      padding: 8px 16px; border-radius: 10px; border: none; cursor: pointer;
      font-size: 13px; font-weight: 600; transition: opacity .2s;
    }
    .btn-approve:disabled, .btn-apply:disabled { opacity: .5; cursor: not-allowed; }
    .btn-approve { background: rgba(34,197,94,0.2); color: #4ade80; }
    .btn-apply   { background: rgba(99,102,241,0.2); color: #818cf8; }
    .btn-reject  { background: rgba(239,68,68,0.1); color: #f87171; }

    .ap-history { display: flex; flex-direction: column; gap: 1px; }
    .ap-hist-row {
      display: flex; align-items: center; gap: 12px;
      padding: 12px 16px; background: rgba(255,255,255,0.02);
      border-radius: 10px;
    }
    .ap-hist-icon { font-size: 18px; flex-shrink: 0; }
    .ap-hist-info { flex: 1; min-width: 0; }
    .ap-hist-name { font-size: 13px; font-weight: 600; display: block; }
    .ap-hist-reason { font-size: 12px; color: #64748b; display: block; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
    .ap-hist-right { display: flex; flex-direction: column; align-items: flex-end; gap: 4px; flex-shrink: 0; }
    .ap-hist-date { font-size: 11px; color: #475569; }
  `]
})
export class AdaptivePlanComponent implements OnInit {
  private svc     = inject(AdaptivePlanService);
  private famState = inject(FamilyStateService);

  adjustments = signal<AdaptiveAdjustmentEntity[]>([]);
  loading     = signal(true);
  evaluating  = signal(false);
  busy        = signal<string | null>(null);

  proposed = computed(() => this.adjustments().filter(a => a.status === 'PROPOSED'));
  approved = computed(() => this.adjustments().filter(a => a.status === 'APPROVED'));
  history  = computed(() => this.adjustments().filter(a => a.status === 'APPLIED' || a.status === 'REJECTED'));

  readonly statuses: AdjustmentStatus[] = ['PROPOSED', 'APPROVED', 'APPLIED', 'REJECTED'];

  ngOnInit(): void {
    const familyId = this.famState.getSelectedFamilyId();
    if (familyId) this.loadList(familyId);
    else this.loading.set(false);
  }

  evaluate(): void {
    const familyId = this.famState.getSelectedFamilyId();
    if (!familyId) return;
    this.evaluating.set(true);
    this.svc.evaluate(familyId).pipe(catchError(() => of([]))).subscribe(items => {
      this.evaluating.set(false);
      if (items.length > 0) this.loadList(familyId);
    });
  }

  approve(a: AdaptiveAdjustmentEntity): void {
    this.busy.set(a.id);
    this.svc.approve(a.id, 'Guardián').pipe(catchError(() => of(null))).subscribe(updated => {
      this.busy.set(null);
      if (updated) this.adjustments.update(list => list.map(x => x.id === updated.id ? updated : x));
    });
  }

  apply(a: AdaptiveAdjustmentEntity): void {
    this.busy.set(a.id);
    this.svc.apply(a.id).pipe(catchError(() => of(null))).subscribe(updated => {
      this.busy.set(null);
      if (updated) this.adjustments.update(list => list.map(x => x.id === updated.id ? updated : x));
    });
  }

  reject(a: AdaptiveAdjustmentEntity): void {
    this.busy.set(a.id);
    this.svc.reject(a.id).pipe(catchError(() => of(null))).subscribe(updated => {
      this.busy.set(null);
      if (updated) this.adjustments.update(list => list.map(x => x.id === updated.id ? updated : x));
    });
  }

  ruleCfg(type: AdaptiveRuleType) { return RULE_CONFIG[type]; }
  statusCfg(s: AdjustmentStatus)  { return STATUS_CONFIG[s];  }

  formatDate(iso: string | null): string {
    if (!iso) return '—';
    return new Date(iso).toLocaleDateString('es-CO', { day: 'numeric', month: 'short', year: 'numeric' });
  }

  private loadList(familyId: number): void {
    this.loading.set(true);
    this.svc.list(familyId).pipe(catchError(() => of([]))).subscribe(items => {
      this.adjustments.set(items);
      this.loading.set(false);
    });
  }
}
