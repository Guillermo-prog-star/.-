import {
  Component, OnInit, ChangeDetectionStrategy, signal, computed, inject
} from '@angular/core';
import { CommonModule }          from '@angular/common';
import { RouterLink }            from '@angular/router';
import { IcafDataService }       from './services/icaf-data.service';
import { ObservatorySnapshot, MADUREZ_CONFIG, DOMAIN_COLORS } from '../../core/models/icaf.model';

interface DomainRow { key: string; label: string; color: string; avg: number }

@Component({
  selector: 'app-observatory-page',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [CommonModule, RouterLink],
  template: `
<div class="observatory-page">

  <!-- Header -->
  <div class="obs-header">
    <div>
      <h1 class="obs-title">🔭 Observatorio Familiar</h1>
      <p class="obs-subtitle">Análisis poblacional de Capital Familiar (ICaF) por mes</p>
    </div>
    <div class="obs-actions">
      <select class="obs-select" (change)="onMonthsChange($event)">
        <option value="3">Últimos 3 meses</option>
        <option value="6" selected>Últimos 6 meses</option>
        <option value="12">Último año</option>
      </select>
      <button class="obs-btn obs-btn--generate" (click)="generateCurrentMonth()" [disabled]="generating()">
        @if (generating()) { Generando… } @else { ⚡ Generar mes actual }
      </button>
      <a routerLink="/capital" class="obs-btn obs-btn--back">← ICaF</a>
    </div>
  </div>

  <!-- Estado de carga -->
  @if (loading()) {
    <div class="obs-loading">
      <div class="obs-spinner"></div>
      <span>Cargando datos del observatorio…</span>
    </div>
  }

  @if (!loading() && history().length === 0) {
    <div class="obs-empty">
      <p>No hay snapshots del observatorio aún.</p>
      <p class="obs-empty-hint">Haz clic en <strong>⚡ Generar mes actual</strong> para crear el primer análisis poblacional.</p>
    </div>
  }

  @if (!loading() && history().length > 0) {

    <!-- KPIs del mes más reciente -->
    <div class="obs-section-label">Mes más reciente — {{ formatMonth(latest()?.snapshotMonth) }}</div>
    <div class="obs-kpi-grid">
      <div class="obs-kpi">
        <span class="obs-kpi-value">{{ latest()?.familiesCount ?? 0 }}</span>
        <span class="obs-kpi-label">Familias</span>
      </div>
      <div class="obs-kpi obs-kpi--icaf">
        <span class="obs-kpi-value">{{ (latest()?.icafAvg ?? 0) | number:'1.1-1' }}</span>
        <span class="obs-kpi-label">ICaF Promedio</span>
      </div>
      <div class="obs-kpi">
        <span class="obs-kpi-value">{{ (latest()?.icafMedian ?? 0) | number:'1.1-1' }}</span>
        <span class="obs-kpi-label">Mediana ICaF</span>
      </div>
      <div class="obs-kpi obs-kpi--green">
        <span class="obs-kpi-value">{{ (latest()?.resolutionRatePct ?? 100) | number:'1.1-1' }}%</span>
        <span class="obs-kpi-label">Tasa Resolución</span>
      </div>
      <div class="obs-kpi obs-kpi--violet">
        <span class="obs-kpi-value">{{ latest()?.familiesImproving ?? 0 }}</span>
        <span class="obs-kpi-label">Mejorando</span>
      </div>
      <div class="obs-kpi obs-kpi--red">
        <span class="obs-kpi-value">{{ latest()?.familiesDeclining ?? 0 }}</span>
        <span class="obs-kpi-label">Declinando</span>
      </div>
    </div>

    <!-- Distribución ICaF (percentiles) -->
    <div class="obs-section-label">Distribución ICaF — {{ formatMonth(latest()?.snapshotMonth) }}</div>
    <div class="obs-card">
      <div class="obs-percentile-bar">
        <div class="obs-percentile-track">
          @if (latest(); as s) {
            <div class="obs-percentile-fill"
                 [style.left.%]="s.icafP25"
                 [style.width.%]="s.icafP75 - s.icafP25">
            </div>
            <div class="obs-percentile-median" [style.left.%]="s.icafMedian"></div>
          }
        </div>
        <div class="obs-percentile-labels">
          <span class="obs-perc-label">P25: {{ (latest()?.icafP25 ?? 0) | number:'1.1-1' }}</span>
          <span class="obs-perc-label obs-perc-label--mid">Mediana: {{ (latest()?.icafMedian ?? 0) | number:'1.1-1' }}</span>
          <span class="obs-perc-label">P75: {{ (latest()?.icafP75 ?? 0) | number:'1.1-1' }}</span>
        </div>
      </div>
    </div>

    <!-- Distribución de madurez -->
    <div class="obs-section-label">Niveles de Madurez — {{ formatMonth(latest()?.snapshotMonth) }}</div>
    <div class="obs-card obs-madurez-grid">
      @for (nivel of madurezRows(); track nivel.nivel) {
        <div class="obs-madurez-item">
          <div class="obs-madurez-bar-wrap">
            <div class="obs-madurez-bar"
                 [style.height.%]="nivel.pct"
                 [style.background]="nivel.color">
            </div>
          </div>
          <span class="obs-madurez-pct" [style.color]="nivel.color">{{ nivel.pct | number:'1.1-1' }}%</span>
          <span class="obs-madurez-lbl">{{ nivel.label }}</span>
        </div>
      }
    </div>

    <!-- Promedios de dominios -->
    <div class="obs-section-label">Promedios por Dominio — {{ formatMonth(latest()?.snapshotMonth) }}</div>
    <div class="obs-card">
      @for (d of domainRows(); track d.key) {
        <div class="obs-domain-row">
          <span class="obs-domain-name" [style.color]="d.color">{{ d.label }}</span>
          <div class="obs-domain-track">
            <div class="obs-domain-fill"
                 [style.width.%]="d.avg"
                 [style.background]="d.color">
            </div>
          </div>
          <span class="obs-domain-val">{{ d.avg | number:'1.1-1' }}</span>
        </div>
      }
    </div>

    <!-- Historial mensual (tabla) -->
    <div class="obs-section-label">Historial ({{ months() }} meses)</div>
    <div class="obs-card obs-table-wrap">
      <table class="obs-table">
        <thead>
          <tr>
            <th>Mes</th>
            <th>Familias</th>
            <th>ICaF avg</th>
            <th>Mediana</th>
            <th>P25</th>
            <th>P75</th>
            <th>Resolución</th>
            <th>Mejorando</th>
            <th>Declinando</th>
          </tr>
        </thead>
        <tbody>
          @for (row of history(); track row.snapshotMonth) {
            <tr [class.obs-tr--latest]="row === latest()">
              <td class="obs-td-month">{{ formatMonth(row.snapshotMonth) }}</td>
              <td>{{ row.familiesCount }}</td>
              <td class="obs-td-icaf">{{ row.icafAvg | number:'1.1-1' }}</td>
              <td>{{ row.icafMedian | number:'1.1-1' }}</td>
              <td>{{ row.icafP25 | number:'1.1-1' }}</td>
              <td>{{ row.icafP75 | number:'1.1-1' }}</td>
              <td [class.obs-td-good]="row.resolutionRatePct >= 80">{{ row.resolutionRatePct | number:'1.1-1' }}%</td>
              <td class="obs-td-up">{{ row.familiesImproving }}</td>
              <td class="obs-td-down">{{ row.familiesDeclining }}</td>
            </tr>
          }
        </tbody>
      </table>
    </div>

  }<!-- end if history -->

</div>
  `,
  styles: [`
.observatory-page {
  padding: 1.5rem 2rem;
  min-height: 100vh;
  background: var(--bg-primary, #0d1117);
  color: var(--text-primary, #e6edf3);
  font-family: system-ui, sans-serif;
}

/* Header */
.obs-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  flex-wrap: wrap;
  gap: 1rem;
  margin-bottom: 2rem;
}
.obs-title {
  font-size: 1.6rem;
  font-weight: 700;
  margin: 0;
  background: linear-gradient(135deg, #a78bfa, #38bdf8);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
}
.obs-subtitle {
  font-size: 0.85rem;
  color: #8b949e;
  margin: 0.2rem 0 0;
}
.obs-actions {
  display: flex;
  gap: 0.6rem;
  align-items: center;
  flex-wrap: wrap;
}
.obs-select {
  background: #161b22;
  border: 1px solid #30363d;
  color: #e6edf3;
  border-radius: 8px;
  padding: 0.45rem 0.75rem;
  font-size: 0.82rem;
  cursor: pointer;
}
.obs-btn {
  padding: 0.45rem 1rem;
  border-radius: 8px;
  font-size: 0.82rem;
  font-weight: 600;
  cursor: pointer;
  border: none;
  text-decoration: none;
  display: inline-flex;
  align-items: center;
  transition: opacity 0.2s;
}
.obs-btn:disabled { opacity: 0.5; cursor: not-allowed; }
.obs-btn--generate { background: rgba(167,139,250,0.15); color: #a78bfa; border: 1px solid rgba(167,139,250,0.3); }
.obs-btn--generate:hover:not(:disabled) { background: rgba(167,139,250,0.25); }
.obs-btn--back { background: rgba(56,189,248,0.1); color: #38bdf8; border: 1px solid rgba(56,189,248,0.25); }
.obs-btn--back:hover { background: rgba(56,189,248,0.2); }

/* Loading / empty */
.obs-loading {
  display: flex;
  align-items: center;
  gap: 1rem;
  padding: 3rem;
  justify-content: center;
  color: #8b949e;
}
.obs-spinner {
  width: 24px; height: 24px;
  border: 2px solid #30363d;
  border-top-color: #a78bfa;
  border-radius: 50%;
  animation: spin 0.8s linear infinite;
}
@keyframes spin { to { transform: rotate(360deg); } }
.obs-empty {
  text-align: center;
  padding: 4rem 2rem;
  color: #8b949e;
}
.obs-empty-hint { font-size: 0.85rem; margin-top: 0.5rem; }

/* Section label */
.obs-section-label {
  font-size: 0.75rem;
  font-weight: 600;
  text-transform: uppercase;
  letter-spacing: 0.08em;
  color: #8b949e;
  margin: 1.5rem 0 0.6rem;
}

/* Card */
.obs-card {
  background: #161b22;
  border: 1px solid #21262d;
  border-radius: 12px;
  padding: 1.25rem;
}

/* KPI grid */
.obs-kpi-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(120px, 1fr));
  gap: 0.75rem;
  margin-bottom: 0.25rem;
}
.obs-kpi {
  background: #161b22;
  border: 1px solid #21262d;
  border-radius: 10px;
  padding: 1rem;
  text-align: center;
}
.obs-kpi-value {
  display: block;
  font-size: 1.8rem;
  font-weight: 700;
  color: #e6edf3;
  line-height: 1.1;
}
.obs-kpi-label {
  display: block;
  font-size: 0.72rem;
  color: #8b949e;
  margin-top: 0.3rem;
  text-transform: uppercase;
  letter-spacing: 0.05em;
}
.obs-kpi--icaf .obs-kpi-value  { color: #a78bfa; }
.obs-kpi--green .obs-kpi-value { color: #10b981; }
.obs-kpi--violet .obs-kpi-value{ color: #a78bfa; }
.obs-kpi--red .obs-kpi-value   { color: #ef4444; }

/* Percentile bar */
.obs-percentile-bar { padding: 0.5rem 0; }
.obs-percentile-track {
  position: relative;
  height: 12px;
  background: #21262d;
  border-radius: 6px;
  overflow: hidden;
}
.obs-percentile-fill {
  position: absolute;
  height: 100%;
  background: rgba(167,139,250,0.45);
  border-radius: 6px;
}
.obs-percentile-median {
  position: absolute;
  top: -2px;
  width: 3px;
  height: 16px;
  background: #a78bfa;
  border-radius: 2px;
  transform: translateX(-50%);
}
.obs-percentile-labels {
  display: flex;
  justify-content: space-between;
  margin-top: 0.5rem;
  font-size: 0.75rem;
  color: #8b949e;
}
.obs-perc-label--mid { color: #a78bfa; font-weight: 600; }

/* Madurez distribution */
.obs-madurez-grid {
  display: flex !important;
  gap: 1.5rem;
  align-items: flex-end;
  padding: 1rem 1.25rem;
}
.obs-madurez-item {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 0.3rem;
}
.obs-madurez-bar-wrap {
  width: 100%;
  height: 80px;
  background: #21262d;
  border-radius: 6px;
  display: flex;
  align-items: flex-end;
  overflow: hidden;
}
.obs-madurez-bar {
  width: 100%;
  border-radius: 4px 4px 0 0;
  transition: height 0.4s ease;
  min-height: 2px;
}
.obs-madurez-pct { font-size: 0.85rem; font-weight: 700; }
.obs-madurez-lbl { font-size: 0.65rem; color: #8b949e; text-align: center; }

/* Domain bars */
.obs-domain-row {
  display: grid;
  grid-template-columns: 130px 1fr 45px;
  align-items: center;
  gap: 0.75rem;
  margin-bottom: 0.55rem;
}
.obs-domain-name { font-size: 0.78rem; font-weight: 500; }
.obs-domain-track {
  height: 8px;
  background: #21262d;
  border-radius: 4px;
  overflow: hidden;
}
.obs-domain-fill {
  height: 100%;
  border-radius: 4px;
  transition: width 0.4s ease;
}
.obs-domain-val { font-size: 0.75rem; color: #8b949e; text-align: right; }

/* Table */
.obs-table-wrap { padding: 0; overflow-x: auto; }
.obs-table {
  width: 100%;
  border-collapse: collapse;
  font-size: 0.78rem;
}
.obs-table th {
  padding: 0.6rem 0.8rem;
  text-align: left;
  font-size: 0.7rem;
  text-transform: uppercase;
  letter-spacing: 0.05em;
  color: #8b949e;
  border-bottom: 1px solid #21262d;
  font-weight: 600;
}
.obs-table td {
  padding: 0.55rem 0.8rem;
  border-bottom: 1px solid #161b22;
  color: #c9d1d9;
}
.obs-tr--latest td { background: rgba(167,139,250,0.06); }
.obs-td-month { font-weight: 600; color: #e6edf3; }
.obs-td-icaf  { color: #a78bfa; font-weight: 600; }
.obs-td-good  { color: #10b981; }
.obs-td-up    { color: #a78bfa; }
.obs-td-down  { color: #ef4444; }
  `]
})
export class ObservatoryPageComponent implements OnInit {

  private readonly dataService = inject(IcafDataService);

  readonly loading    = signal(true);
  readonly generating = signal(false);
  readonly history    = signal<ObservatorySnapshot[]>([]);
  readonly months     = signal(6);

  readonly latest = computed(() => {
    const h = this.history();
    return h.length > 0 ? h[h.length - 1] : null;
  });

  readonly madurezRows = computed(() => {
    const s = this.latest();
    if (!s) return [];
    return [
      { nivel: 1, label: 'Supervivencia', color: MADUREZ_CONFIG[1].color, pct: s.nivel1Pct },
      { nivel: 2, label: 'Reactividad',   color: MADUREZ_CONFIG[2].color, pct: s.nivel2Pct },
      { nivel: 3, label: 'Organización',  color: MADUREZ_CONFIG[3].color, pct: s.nivel3Pct },
      { nivel: 4, label: 'Propósito',     color: MADUREZ_CONFIG[4].color, pct: s.nivel4Pct },
      { nivel: 5, label: 'Legado',        color: MADUREZ_CONFIG[5].color, pct: s.nivel5Pct },
    ];
  });

  readonly domainRows = computed((): DomainRow[] => {
    const s = this.latest();
    if (!s) return [];
    return [
      { key: 'cohesion',      label: 'Cohesión',       color: DOMAIN_COLORS['cohesion'],      avg: s.avgDomCohesion      },
      { key: 'confianza',     label: 'Confianza',      color: DOMAIN_COLORS['confianza'],     avg: s.avgDomConfianza     },
      { key: 'resiliencia',   label: 'Resiliencia',    color: DOMAIN_COLORS['resiliencia'],   avg: s.avgDomResiliencia   },
      { key: 'comunicacion',  label: 'Comunicación',   color: DOMAIN_COLORS['comunicacion'],  avg: s.avgDomComunicacion  },
      { key: 'autonomia',     label: 'Autonomía',      color: DOMAIN_COLORS['autonomia'],     avg: s.avgDomAutonomia     },
      { key: 'bienestar',     label: 'Bienestar',      color: DOMAIN_COLORS['bienestar'],     avg: s.avgDomBienestar     },
      { key: 'proposito',     label: 'Propósito',      color: DOMAIN_COLORS['proposito'],     avg: s.avgDomProposito     },
      { key: 'integracion',   label: 'Integración',    color: DOMAIN_COLORS['integracion'],   avg: s.avgDomIntegracion   },
      { key: 'emprendimiento',label: 'Emprendimiento', color: DOMAIN_COLORS['emprendimiento'],avg: s.avgDomEmprendimiento},
      { key: 'legado',        label: 'Legado',         color: DOMAIN_COLORS['legado'],        avg: s.avgDomLegado        },
    ];
  });

  ngOnInit(): void {
    this.load();
  }

  onMonthsChange(event: Event): void {
    const n = parseInt((event.target as HTMLSelectElement).value, 10);
    this.months.set(n);
    this.load();
  }

  generateCurrentMonth(): void {
    const now   = new Date();
    const month = `${now.getFullYear()}-${String(now.getMonth() + 1).padStart(2, '0')}`;
    this.generating.set(true);
    this.dataService.generateObservatoryMonth(month).subscribe(() => {
      this.generating.set(false);
      this.load();
    });
  }

  formatMonth(raw?: string | null): string {
    if (!raw) return '—';
    const [y, m] = raw.split('-');
    const labels = ['Ene','Feb','Mar','Abr','May','Jun','Jul','Ago','Sep','Oct','Nov','Dic'];
    return `${labels[parseInt(m, 10) - 1]} ${y}`;
  }

  private load(): void {
    this.loading.set(true);
    this.dataService.getObservatoryHistory(this.months()).subscribe(data => {
      this.history.set(data);
      this.loading.set(false);
    });
  }
}
