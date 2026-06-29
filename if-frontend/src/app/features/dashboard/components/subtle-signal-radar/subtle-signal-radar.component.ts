import {
  Component, Input, OnInit, OnChanges, SimpleChanges,
  ChangeDetectionStrategy, signal, computed, inject
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import {
  SubtleSignalRadarService,
  RadarResponse, ScenarioResponse, NarrativeResponse,
  DimensionTrend, MicroSignal, Scenario
} from '../../../../core/services/subtle-signal-radar.service';
import { forkJoin, of } from 'rxjs';
import { catchError } from 'rxjs/operators';

@Component({
  selector: 'app-subtle-signal-radar',
  standalone: true,
  imports: [CommonModule, RouterModule],
  templateUrl: './subtle-signal-radar.component.html',
  styleUrls: ['./subtle-signal-radar.component.css'],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class SubtleSignalRadarComponent implements OnInit, OnChanges {

  @Input() familyId!: number;

  private readonly radarSvc = inject(SubtleSignalRadarService);

  // ── Estado ────────────────────────────────────────────────────────────────
  readonly loading      = signal(true);
  readonly loadingNarr  = signal(false);
  readonly radar        = signal<RadarResponse | null>(null);
  readonly scenarios    = signal<ScenarioResponse | null>(null);
  readonly narrative    = signal<NarrativeResponse | null>(null);
  readonly activeTab    = signal<'signals' | 'scenarios' | 'narrative'>('signals');
  readonly expandedScenario = signal<'A' | 'B' | 'C' | null>(null);

  // ── Computed ─────────────────────────────────────────────────────────────
  readonly dimensions = computed(() => {
    const r = this.radar();
    if (!r) return [];
    return [
      { key: 'emociones',    label: 'Emociones',    icon: '💛', data: r.emociones },
      { key: 'comunicacion', label: 'Comunicación',  icon: '💬', data: r.comunicacion },
      { key: 'habitos',      label: 'Hábitos',       icon: '🔄', data: r.habitos },
      { key: 'tiempos',      label: 'Tiempos',       icon: '⏳', data: r.tiempos },
    ];
  });

  readonly highSignals = computed(() =>
    (this.radar()?.microSignals ?? []).filter(s => s.severity === 'HIGH')
  );

  readonly hasData = computed(() => !!this.radar());

  // ── Lifecycle ─────────────────────────────────────────────────────────────
  ngOnInit(): void { if (this.familyId > 0) this.load(); }
  ngOnChanges(c: SimpleChanges): void {
    if (c['familyId'] && !c['familyId'].firstChange && this.familyId > 0) this.load();
  }

  private load(): void {
    this.loading.set(true);
    forkJoin({
      radar:     this.radarSvc.getRadar(this.familyId).pipe(catchError(() => of(null))),
      scenarios: this.radarSvc.getScenarios(this.familyId).pipe(catchError(() => of(null)))
    }).subscribe(({ radar, scenarios }) => {
      this.radar.set(radar);
      this.scenarios.set(scenarios);
      this.loading.set(false);
    });
  }

  loadNarrative(): void {
    if (this.narrative() || this.loadingNarr()) return;
    this.loadingNarr.set(true);
    this.radarSvc.getNarrative(this.familyId).subscribe(n => {
      this.narrative.set(n);
      this.loadingNarr.set(false);
      this.activeTab.set('narrative');
    });
  }

  // ── Helpers de presentación ───────────────────────────────────────────────
  setTab(tab: 'signals' | 'scenarios' | 'narrative'): void {
    if (tab === 'narrative' && !this.narrative()) { this.loadNarrative(); return; }
    this.activeTab.set(tab);
  }

  toggleScenario(code: 'A' | 'B' | 'C'): void {
    this.expandedScenario.set(this.expandedScenario() === code ? null : code);
  }

  directionIcon(dir: string | null | undefined): string {
    if (!dir) return '—';
    const map: Record<string, string> = {
      STRONG_IMPROVING: '⬆⬆', IMPROVING: '↑', STABLE: '→',
      DECLINING: '↓', CRITICAL_DECLINE: '⬇⬇', NO_DATA: '—'
    };
    return map[dir] ?? '→';
  }

  directionColor(dir: string | null | undefined): string {
    if (!dir) return '#64748b';
    const map: Record<string, string> = {
      STRONG_IMPROVING: '#22d3ee', IMPROVING: '#34d399',
      STABLE: '#94a3b8',
      DECLINING: '#fbbf24', CRITICAL_DECLINE: '#ef4444', NO_DATA: '#475569'
    };
    return map[dir] ?? '#94a3b8';
  }

  severityColor(sev: string): string {
    return sev === 'HIGH' ? '#ef4444' : sev === 'MEDIUM' ? '#f97316' : '#60a5fa';
  }

  severityIcon(sev: string): string {
    return sev === 'HIGH' ? '🔴' : sev === 'MEDIUM' ? '🟠' : '🔵';
  }

  riskColor(risk: string): string {
    const m: Record<string, string> = {
      CRITICO: '#ef4444', ALTO: '#f97316', MODERADO: '#fbbf24', BAJO: '#34d399'
    };
    return m[risk?.toUpperCase()] ?? '#94a3b8';
  }

  scenarioColor(code: string): string {
    return code === 'A' ? '#ef4444' : code === 'B' ? '#fbbf24' : '#34d399';
  }

  scenarioIcon(code: string): string {
    return code === 'A' ? '📉' : code === 'B' ? '📊' : '📈';
  }

  barWidth(score: number | null): number {
    return Math.min(Math.max(score ?? 0, 0), 100);
  }

  deltaClass(delta: number | null): string {
    if (delta === null) return 'text-white/40';
    if (delta >= 5)  return 'text-emerald-400';
    if (delta <= -5) return 'text-red-400';
    return 'text-white/50';
  }

  formatDelta(delta: number | null): string {
    if (delta === null) return '—';
    return (delta >= 0 ? '+' : '') + delta.toFixed(1);
  }

  icfLabel(icf: number): string {
    if (icf >= 80) return 'Fortaleza';
    if (icf >= 60) return 'Creciendo';
    if (icf >= 40) return 'Atención';
    return 'Crítico';
  }

  narrativeLines(text: string): string[] {
    return text.split('\n').filter(l => l.trim().length > 0);
  }

  /**
   * Genera el atributo `d` de un <path> SVG para una sparkline de 80×28 px.
   * Normaliza los valores al rango 0-100 dentro del viewport.
   */
  sparklinePath(history: number[], color: string): { d: string; color: string; cx: number; cy: number } {
    if (!history || history.length < 2) return { d: '', color, cx: 0, cy: 0 };
    const W = 80, H = 28, pad = 2;
    const min = Math.min(...history);
    const max = Math.max(...history);
    const range = max - min || 1;
    const xs = history.map((_, i) => pad + (i / (history.length - 1)) * (W - pad * 2));
    const ys = history.map(v => H - pad - ((v - min) / range) * (H - pad * 2));
    const pts = xs.map((x, i) => `${i === 0 ? 'M' : 'L'}${x.toFixed(1)},${ys[i].toFixed(1)}`);
    return { d: pts.join(' '), color, cx: xs[xs.length - 1], cy: ys[ys.length - 1] };
  }
}
