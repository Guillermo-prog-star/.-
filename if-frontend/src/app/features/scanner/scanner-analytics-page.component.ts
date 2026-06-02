import { Component, OnInit, inject, signal, computed } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, RouterLink } from '@angular/router';
import { forkJoin, catchError, of } from 'rxjs';
import { ScannerService } from '../../core/services/scanner.service';
import { AssessmentService } from '../../core/services/assessment.service';
import { FamilyStateService } from '../../core/services/family-state.service';
import { InferenceRecordDto, TimelineEntryDto } from '../../core/models/models';
import { ScrollPolicyService } from '../../shared/directives/scroll-policy.service';

// ── Chart data types ──────────────────────────────────────────────────────

interface IcfPoint {
  x: number; y: number;
  icf: number; risk: string | null;
  tos: string | null; uncert: number;
  date: string | null; evalId: number;
}

interface IcfChartData {
  W: number; H: number; PL: number; PT: number; UH: number; UW: number;
  bands: { from: number; to: number; color: string }[];
  coords: IcfPoint[];
  linePath: string; areaPath: string; uncertaintyPath: string;
  yLabels: { y: number; label: string }[];
  gridPath: string;
}

interface RadarData {
  cx: number; cy: number;
  rings: string[];
  axes: { x2: string; y2: string }[];
  dataPoints: string;
  prevPoints: string | null;
  dims: { key: string; label: string; value: string; color: string }[];
}

@Component({
  selector: 'app-scanner-analytics-page',
  standalone: true,
  imports: [CommonModule, RouterLink],
  templateUrl: './scanner-analytics-page.component.html',
  styleUrls: ['./scanner-analytics-page.component.css'],
})
export class ScannerAnalyticsPageComponent implements OnInit {
  private scanner     = inject(ScannerService);
  private assessment  = inject(AssessmentService);
  private familyState = inject(FamilyStateService);
  private router       = inject(Router);
  private scrollPolicy = inject(ScrollPolicyService);

  // ── State signals ─────────────────────────────────────────────────────────

  readonly timeline         = signal<TimelineEntryDto[]>([]);  // newest first
  readonly dimensionHistory = signal<any[]>([]);
  readonly inferences       = signal<InferenceRecordDto[]>([]);
  readonly loading          = signal(true);

  get familyId()   { return this.familyState.currentFamilyId(); }
  get familyName() { return this.familyState.currentFamilyName() || 'la familia'; }

  // ── KPI computeds ─────────────────────────────────────────────────────────

  readonly totalEvaluations = computed(() => this.timeline().length);
  readonly latestIcf        = computed(() => this.timeline()[0]?.healthyIndex ?? 0);
  readonly latestTosState   = computed(() => this.timeline()[0]?.operationalState ?? null);

  readonly icfDelta = computed((): number | null => {
    const pts = this.timeline();
    return pts.length < 2 ? null : pts[0].healthyIndex - pts[1].healthyIndex;
  });

  readonly icfDeltaStr = computed(() => {
    const d = this.icfDelta();
    return d === null ? '—' : (d >= 0 ? '+' : '') + d.toFixed(1);
  });

  readonly icfDeltaColor = computed(() => {
    const d = this.icfDelta();
    if (d === null) return '#94a3b8';
    return d > 2 ? '#34d399' : d < -2 ? '#f87171' : '#fbbf24';
  });

  readonly icfDeltaIcon = computed(() => {
    const d = this.icfDelta();
    if (d === null) return '→';
    return d > 2 ? '▲' : d < -2 ? '▼' : '→';
  });

  readonly avgUncertainty = computed(() => {
    const pts = this.timeline().filter(p => p.uncertaintyTotal != null);
    return pts.length ? pts.reduce((s, p) => s + (p.uncertaintyTotal ?? 0), 0) / pts.length : 0;
  });

  // ── Chart computeds ───────────────────────────────────────────────────────

  private readonly DIMS = [
    { key: 'emociones',    angle: -Math.PI / 2, label: 'Emociones' },
    { key: 'comunicacion', angle: 0,             label: 'Comunicación' },
    { key: 'habitos',      angle: Math.PI / 2,   label: 'Hábitos' },
    { key: 'tiempos',      angle: Math.PI,       label: 'Tiempos' },
  ];

  readonly icfChart = computed((): IcfChartData | null => {
    const pts = [...this.timeline()].reverse();  // chronological
    if (!pts.length) return null;

    const W = 680, H = 180, PL = 46, PR = 16, PT = 14, PB = 28;
    const UW = W - PL - PR, UH = H - PT - PB;
    const n = pts.length;

    const toX = (i: number) => n === 1 ? PL + UW / 2 : PL + (i / (n - 1)) * UW;
    const toY = (icf: number) => PT + (1 - Math.min(100, Math.max(0, icf)) / 100) * UH;

    const bands = [
      { from: toY(100), to: toY(70),  color: '#22c55e14' },
      { from: toY(70),  to: toY(40),  color: '#fbbf2414' },
      { from: toY(40),  to: toY(20),  color: '#f9731614' },
      { from: toY(20),  to: toY(0),   color: '#ef444414' },
    ];

    const coords: IcfPoint[] = pts.map((p, i) => ({
      x: toX(i), y: toY(p.healthyIndex),
      icf: p.healthyIndex, risk: p.riskLevel,
      tos: p.operationalState ?? null, uncert: p.uncertaintyTotal ?? 0,
      date: p.finalizedAt, evalId: p.evaluationId,
    }));

    const linePath = coords.map((c, i) =>
      `${i === 0 ? 'M' : 'L'}${c.x.toFixed(1)},${c.y.toFixed(1)}`
    ).join(' ');

    const areaPath = [
      ...coords.map((c, i) => `${i === 0 ? 'M' : 'L'}${c.x.toFixed(1)},${c.y.toFixed(1)}`),
      `L${coords[n - 1].x.toFixed(1)},${(PT + UH).toFixed(1)}`,
      `L${coords[0].x.toFixed(1)},${(PT + UH).toFixed(1)}`, 'Z',
    ].join(' ');

    const upPts  = coords.map((c, i) => {
      const s = Math.min(c.uncert * 24, 18);
      return `${i === 0 ? 'M' : 'L'}${c.x.toFixed(1)},${Math.max(PT, c.y - s).toFixed(1)}`;
    }).join(' ');
    const dnPts  = [...coords].reverse().map((c, i) => {
      const s = Math.min(c.uncert * 24, 18);
      return `${i === 0 ? '' : 'L'}${c.x.toFixed(1)},${Math.min(PT + UH, c.y + s).toFixed(1)}`;
    }).join(' ');
    const uncertaintyPath = `${upPts} L${dnPts} Z`;

    const yLabels = [100, 70, 40, 20, 0].map(v => ({ y: toY(v), label: String(v) }));
    const gridPath = [70, 40, 20].map(v =>
      `M${PL},${toY(v).toFixed(1)} L${(PL + UW).toFixed(1)},${toY(v).toFixed(1)}`
    ).join(' ');

    return { W, H, PL, PT, UH, UW, bands, coords, linePath, areaPath, uncertaintyPath, yLabels, gridPath };
  });

  readonly radarChart = computed((): RadarData | null => {
    const history = this.dimensionHistory();
    if (!history.length) return null;

    const cx = 110, cy = 110, r = 82;

    const rings = [0.25, 0.5, 0.75, 1.0].map(f =>
      this.DIMS.map(d => {
        const x = cx + f * r * Math.cos(d.angle);
        const y = cy + f * r * Math.sin(d.angle);
        return `${x.toFixed(1)},${y.toFixed(1)}`;
      }).join(' ')
    );

    const axes = this.DIMS.map(d => ({
      x2: (cx + r * Math.cos(d.angle)).toFixed(1),
      y2: (cy + r * Math.sin(d.angle)).toFixed(1),
    }));

    const toPoints = (dims: Record<string, number>) =>
      this.DIMS.map(d => {
        const val = Math.min(5, Math.max(0, dims[d.key] ?? 2.5)) / 5;
        return `${(cx + val * r * Math.cos(d.angle)).toFixed(1)},${(cy + val * r * Math.sin(d.angle)).toFixed(1)}`;
      }).join(' ');

    const latest = history[history.length - 1]?.dimensions as Record<string, number> | null;
    const prev   = history.length > 1 ? history[history.length - 2]?.dimensions as Record<string, number> : null;

    const dims = this.DIMS.map(d => ({
      key: d.key, label: d.label,
      value: latest ? (latest[d.key] ?? 0).toFixed(1) : '—',
      color: latest ? this.dimensionColor(latest[d.key] ?? 0) : '#64748b',
    }));

    return {
      cx, cy, rings, axes,
      dataPoints: latest ? toPoints(latest) : '',
      prevPoints: prev   ? toPoints(prev)   : null,
      dims,
    };
  });

  readonly tosHistory = computed(() =>
    this.timeline()
      .filter(p => !!p.operationalState)
      .slice(0, 10)
      .map(p => ({
        state: p.operationalState!,
        date:  p.finalizedAt,
        evalId: p.evaluationId,
        icf:   p.healthyIndex,
      }))
  );

  readonly ruleActivations = computed(() => {
    const counts: Record<string, number> = {};
    this.inferences()
      .filter(r => r.inferenceKey !== 'ICF_CALC')
      .forEach(r => { counts[r.inferenceKey] = (counts[r.inferenceKey] ?? 0) + 1; });

    const entries = Object.entries(counts).sort((a, b) => b[1] - a[1]).slice(0, 8);
    const max = entries[0]?.[1] ?? 1;

    return entries.map(([key, count]) => ({
      key,
      label: key.replace(/_/g, ' ').replace(/\b\w/g, c => c.toUpperCase()),
      count,
      pct: +((count / max) * 100).toFixed(1),
    }));
  });

  exportCsv(): void {
    const headers = [
      'Evaluación ID', 'Fecha', 'ICF', 'Riesgo', 'Dimensión crítica',
      'Estado IF-TOS', 'Incertidumbre',
      'Emociones', 'Comunicación', 'Hábitos', 'Tiempos'
    ];

    const dimMap = new Map(
      this.dimensionHistory().map((d: any) => [d.evaluationId, d.dimensions])
    );

    const rows = [...this.timeline()].reverse().map(p => {
      const dims = dimMap.get(p.evaluationId) as any;
      return [
        p.evaluationId,
        p.finalizedAt ?? '',
        p.healthyIndex.toFixed(1),
        p.riskLevel ?? '',
        p.criticalDimension ?? '',
        p.operationalState ?? '',
        ((p.uncertaintyTotal ?? 0) * 100).toFixed(0) + '%',
        dims?.emociones?.toFixed(1)    ?? '',
        dims?.comunicacion?.toFixed(1) ?? '',
        dims?.habitos?.toFixed(1)      ?? '',
        dims?.tiempos?.toFixed(1)      ?? ''
      ];
    });

    const csv = [headers, ...rows]
      .map(row => row.map(cell => `"${String(cell).replace(/"/g, '""')}"`).join(','))
      .join('\n');

    const blob = new Blob(['﻿' + csv], { type: 'text/csv;charset=utf-8;' });
    const url  = URL.createObjectURL(blob);
    const a    = document.createElement('a');
    a.href     = url;
    a.download = `panel-clinico-familia-${this.familyId}-${new Date().toISOString().slice(0, 10)}.csv`;
    a.click();
    URL.revokeObjectURL(url);
  }

  // ── Lifecycle ─────────────────────────────────────────────────────────────

  ngOnInit(): void {
    this.scrollPolicy.set('scroll-to-new');
    if (!this.familyId) { this.router.navigate(['/families']); return; }
    forkJoin({
      timeline:   this.assessment.getTimeline(this.familyId).pipe(catchError(() => of([]))),
      dimHistory: this.assessment.getDimensionHistory(this.familyId).pipe(catchError(() => of([]))),
      inferences: this.scanner.getInferences(this.familyId).pipe(catchError(() => of([]))),
    }).subscribe(({ timeline, dimHistory, inferences }) => {
      this.timeline.set(timeline);
      this.dimensionHistory.set(dimHistory);
      this.inferences.set(inferences);
      this.loading.set(false);
    });
  }

  // ── Visual helpers ────────────────────────────────────────────────────────

  icfColor(v: number): string {
    if (v >= 70) return '#34d399';
    if (v >= 40) return '#fbbf24';
    if (v >= 20) return '#f97316';
    return '#f87171';
  }

  stateColor(s: string | null | undefined): string {
    const m: Record<string, string> = {
      EMERGING: '#94a3b8', STABLE: '#60a5fa', ESCALATING: '#f97316',
      CRITICAL: '#ef4444', RECOVERING: '#22d3ee', RESOLVED: '#34d399',
    };
    return m[s ?? ''] ?? '#64748b';
  }

  stateLabel(s: string | null | undefined): string {
    const m: Record<string, string> = {
      EMERGING: 'Inicial', STABLE: 'Estable', ESCALATING: 'Deterioro',
      CRITICAL: 'Crisis',  RECOVERING: 'Recuperación', RESOLVED: 'Resuelto',
    };
    return m[s ?? ''] ?? '—';
  }

  stateClass(s: string | null | undefined): string {
    switch (s) {
      case 'EMERGING':   return 'state-emerging';
      case 'STABLE':     return 'state-stable';
      case 'ESCALATING': return 'state-escalating';
      case 'CRITICAL':   return 'state-critical';
      case 'RECOVERING': return 'state-recovering';
      case 'RESOLVED':   return 'state-resolved';
      default:           return 'state-emerging';
    }
  }

  stateGlyph(s: string | null | undefined): string {
    const m: Record<string, string> = {
      EMERGING: '🌱', STABLE: '🔵', ESCALATING: '🔶',
      CRITICAL: '🔴', RECOVERING: '🔷', RESOLVED: '✅',
    };
    return m[s ?? ''] ?? '⬡';
  }

  dimensionColor(score: number): string {
    if (score >= 4.0) return '#34d399';
    if (score >= 3.0) return '#60a5fa';
    if (score >= 2.0) return '#fbbf24';
    return '#f87171';
  }

  uncertaintyColor(v: number): string {
    if (v < 0.15) return '#34d399';
    if (v < 0.35) return '#fbbf24';
    if (v < 0.50) return '#f97316';
    return '#ef4444';
  }

  uncertaintyLabel(v: number): string {
    if (v < 0.15) return 'Baja';
    if (v < 0.35) return 'Media';
    if (v < 0.50) return 'Alta';
    return 'Muy alta';
  }

  formatDateShort(iso: string | null | undefined): string {
    if (!iso) return '—';
    return new Date(iso).toLocaleDateString('es-CO', {
      day: '2-digit', month: 'short', year: '2-digit',
    });
  }

  trackByEvalId(_: number, item: { evalId: number }) { return item.evalId; }
  trackByKey(_: number, item: { key: string }) { return item.key; }
}
