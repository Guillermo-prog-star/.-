import { Component, OnInit, inject, signal, computed } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, RouterLink } from '@angular/router';
import { catchError, of } from 'rxjs';
import { AssessmentService } from '../../core/services/assessment.service';
import { FamilyStateService } from '../../core/services/family-state.service';
import { TimelineEntryDto } from '../../core/models/models';

interface DimensionHistoryPoint {
  evaluationId: number;
  finalizedAt: string | null;
  dimensions: {
    emociones: number;
    comunicacion: number;
    habitos: number;
    tiempos: number;
  };
}

@Component({
  selector: 'app-evaluation-evolution-page',
  standalone: true,
  imports: [CommonModule, RouterLink],
  templateUrl: './evaluation-evolution-page.component.html',
  styleUrls: ['./evaluation-evolution-page.component.css']
})
export class EvaluationEvolutionPageComponent implements OnInit {
  private assessmentService = inject(AssessmentService);
  private familyState       = inject(FamilyStateService);
  private router            = inject(Router);

  // Señales de Estado Reactivo
  readonly timeline = signal<TimelineEntryDto[]>([]);
  readonly dimHistory = signal<DimensionHistoryPoint[]>([]);
  readonly loading = signal(true);
  readonly hoveredPointIndex = signal<number | null>(null);

  get familyId() { return this.familyState.currentFamilyId(); }
  get familyName() { return localStorage.getItem('selectedFamilyName') ?? 'la familia'; }

  // Ordenar timeline cronológicamente de más antiguo a más reciente para el eje X
  readonly chronologicalTimeline = computed(() => {
    return [...this.timeline()].sort((a, b) => 
      new Date(a.finalizedAt ?? '').getTime() - new Date(b.finalizedAt ?? '').getTime()
    );
  });

  // Métricas Clave (KPIs)
  readonly latestIcf = computed(() => {
    const pts = this.chronologicalTimeline();
    return pts.length > 0 ? pts[pts.length - 1].healthyIndex : 0;
  });

  readonly baselineIcf = computed(() => {
    const pts = this.chronologicalTimeline();
    return pts.length > 0 ? pts[0].healthyIndex : 0;
  });

  readonly deltaIcf = computed(() => {
    return this.latestIcf() - this.baselineIcf();
  });

  readonly highestIcf = computed(() => {
    const pts = this.chronologicalTimeline();
    return pts.length > 0 ? Math.max(...pts.map(p => p.healthyIndex)) : 0;
  });

  readonly latestRisk = computed(() => {
    const pts = this.chronologicalTimeline();
    return pts.length > 0 ? pts[pts.length - 1].riskLevel : 'SIN DIAGNÓSTICO';
  });

  readonly latestCriticalDimension = computed(() => {
    const pts = this.chronologicalTimeline();
    return pts.length > 0 ? (pts[pts.length - 1].criticalDimension ?? 'Ninguna') : 'Ninguna';
  });

  // ── IF-TOS: Estado Operacional ───────────────────────────────────────────────
  readonly latestOperationalState = computed(() => {
    const pts = this.chronologicalTimeline();
    return pts.length > 0 ? (pts[pts.length - 1].operationalState ?? 'EMERGING') : 'EMERGING';
  });

  // ── IF-SUM: Incertidumbre Estructural ─────────────────────────────────────
  readonly latestUncertaintyTotal = computed(() => {
    const pts = this.chronologicalTimeline();
    return pts.length > 0 ? (pts[pts.length - 1].uncertaintyTotal ?? 0) : 0;
  });

  // --- Cálculos de SVG del Gráfico de Evolución Principal ---
  readonly svgWidth = 720;
  readonly svgHeight = 280;
  readonly padX = 40;
  readonly padY = 30;

  readonly chartPoints = computed(() => {
    const pts = this.chronologicalTimeline();
    if (pts.length < 2) return [];

    const W = this.svgWidth;
    const H = this.svgHeight;

    return pts.map((p, i) => {
      const x = this.padX + (i / (pts.length - 1)) * (W - this.padX * 2);
      const y = H - this.padY - (p.healthyIndex / 100) * (H - this.padY * 2);
      return { x, y, data: p };
    });
  });

  readonly trendLinePath = computed((): string => {
    const points = this.chartPoints();
    if (points.length < 2) return '';
    return points.map((p, i) => `${i === 0 ? 'M' : 'L'} ${p.x} ${p.y}`).join(' ');
  });

  readonly trendAreaPath = computed((): string => {
    const points = this.chartPoints();
    if (points.length < 2) return '';
    const linePath = this.trendLinePath();
    const lastPoint = points[points.length - 1];
    const firstPoint = points[0];
    const floorY = this.svgHeight - this.padY;
    return `${linePath} L ${lastPoint.x} ${floorY} L ${firstPoint.x} ${floorY} Z`;
  });

  // --- Sparklines por Dimensión ---
  readonly sparkWidth = 140;
  readonly sparkHeight = 40;

  getSparklinePath(dimKey: 'emociones' | 'comunicacion' | 'habitos' | 'tiempos'): string {
    const history = [...this.dimHistory()].sort((a, b) => 
      new Date(a.finalizedAt ?? '').getTime() - new Date(b.finalizedAt ?? '').getTime()
    );
    if (history.length < 2) return '';

    const W = this.sparkWidth;
    const H = this.sparkHeight;
    const pad = 4;

    return history.map((pt, i) => {
      const val = pt.dimensions[dimKey] ?? 3.0; // Puntuación de 1 a 5
      const x = pad + (i / (history.length - 1)) * (W - pad * 2);
      // Mapear 1.0 - 5.0 a la altura de la caja (invertido)
      const y = H - pad - ((val - 1.0) / 4.0) * (H - pad * 2);
      return `${i === 0 ? 'M' : 'L'} ${x} ${y}`;
    }).join(' ');
  }

  getLatestDimensionScore(dimKey: 'emociones' | 'comunicacion' | 'habitos' | 'tiempos'): number {
    const history = this.dimHistory();
    if (history.length === 0) return 0.0;
    const sorted = [...history].sort((a, b) => 
      new Date(a.finalizedAt ?? '').getTime() - new Date(b.finalizedAt ?? '').getTime()
    );
    return sorted[sorted.length - 1].dimensions[dimKey] ?? 0.0;
  }

  getBaselineDimensionScore(dimKey: 'emociones' | 'comunicacion' | 'habitos' | 'tiempos'): number {
    const history = this.dimHistory();
    if (history.length === 0) return 0.0;
    const sorted = [...history].sort((a, b) => 
      new Date(a.finalizedAt ?? '').getTime() - new Date(b.finalizedAt ?? '').getTime()
    );
    return sorted[0].dimensions[dimKey] ?? 0.0;
  }

  getDimensionDelta(dimKey: 'emociones' | 'comunicacion' | 'habitos' | 'tiempos'): string {
    const current = this.getLatestDimensionScore(dimKey);
    const baseline = this.getBaselineDimensionScore(dimKey);
    const delta = current - baseline;
    if (delta > 0) return `+${delta.toFixed(1)}`;
    return `${delta.toFixed(1)}`;
  }

  ngOnInit(): void {
    if (!this.familyId) {
      this.router.navigate(['/families']);
      return;
    }
    this.loadEvolutionData();
  }

  exportCsv(): void {
    const headers = [
      'Evaluación ID', 'Fecha', 'ICF', 'Riesgo', 'Dimensión crítica',
      'Estado operacional', 'Incertidumbre',
      'Emociones', 'Comunicación', 'Hábitos', 'Tiempos'
    ];

    const dimMap = new Map(
      this.dimHistory().map(d => [d.evaluationId, d.dimensions])
    );

    const rows = this.chronologicalTimeline().map(p => {
      const dims = dimMap.get(p.evaluationId);
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
    a.download = `evolucion-familia-${this.familyId}-${new Date().toISOString().slice(0, 10)}.csv`;
    a.click();
    URL.revokeObjectURL(url);
  }

  private loadEvolutionData(): void {
    this.loading.set(true);

    // Cargar timeline general para ICF
    this.assessmentService.getTimeline(this.familyId).pipe(
      catchError(() => of([]))
    ).subscribe(data => {
      this.timeline.set(data);
    });

    // Cargar evolución detallada de las 4 dimensiones
    this.assessmentService.getDimensionHistory(this.familyId).pipe(
      catchError(() => of([]))
    ).subscribe(data => {
      this.dimHistory.set(data);
      this.loading.set(false);
    });
  }

  // --- Helpers de Formato y UX ---
  formatDate(iso: string | null | undefined): string {
    if (!iso) return '—';
    return new Date(iso).toLocaleDateString('es-CO', {
      day: '2-digit', month: 'short'
    });
  }

  riskLabel(r: string | null | undefined): string {
    const labels: Record<string, string> = {
      BAJO: 'Bajo', MODERADO: 'Moderado', ALTO: 'Alto', CRITICO: 'Crítico'
    };
    return labels[r ?? ''] ?? (r || '—');
  }

  riskBadgeClass(r: string | null | undefined): string {
    switch (r) {
      case 'BAJO':    return 'badge-bajo';
      case 'MODERADO': return 'badge-moderado';
      case 'ALTO':    return 'badge-alto';
      case 'CRITICO': return 'badge-critico';
      default:        return 'badge-default';
    }
  }

  readonly hoveredPoint = computed(() => {
    const idx = this.hoveredPointIndex();
    if (idx === null) return null;
    return this.chartPoints()[idx] ?? null;
  });

  icfColor(icf: number | null | undefined): string {
    const v = icf ?? 0;
    if (v >= 70) return '#34d399'; // Esmeralda (Bajo riesgo)
    if (v >= 40) return '#fbbf24'; // Ámbar (Riesgo moderado)
    return '#f87171'; // Rojo (Riesgo alto/crítico)
  }

  // ── IF-TOS: Helpers de Estado Operacional ─────────────────────────────────

  operationalStateLabel(state: string | null | undefined): string {
    const labels: Record<string, string> = {
      EMERGING:   'Inicial',
      STABLE:     'Estable',
      ESCALATING: 'Deterioro',
      CRITICAL:   'Crisis',
      RECOVERING: 'Recuperación',
      RESOLVED:   'Resuelto'
    };
    return labels[state ?? ''] ?? '—';
  }

  operationalStateColor(state: string | null | undefined): string {
    const colors: Record<string, string> = {
      EMERGING:   '#94a3b8',
      STABLE:     '#60a5fa',
      ESCALATING: '#f97316',
      CRITICAL:   '#ef4444',
      RECOVERING: '#22d3ee',
      RESOLVED:   '#34d399'
    };
    return colors[state ?? ''] ?? '#64748b';
  }

  operationalStateBadgeClass(state: string | null | undefined): string {
    const classes: Record<string, string> = {
      EMERGING:   'state-emerging',
      STABLE:     'state-stable',
      ESCALATING: 'state-escalating',
      CRITICAL:   'state-critical',
      RECOVERING: 'state-recovering',
      RESOLVED:   'state-resolved'
    };
    return classes[state ?? ''] ?? 'state-emerging';
  }

  // ── IF-SUM: Helper de Incertidumbre ───────────────────────────────────────

  uncertaintyLabel(total: number | null | undefined): string {
    const v = total ?? 0;
    if (v < 0.15) return 'Baja';
    if (v < 0.35) return 'Media';
    if (v < 0.50) return 'Alta';
    return 'Muy alta';
  }
}
