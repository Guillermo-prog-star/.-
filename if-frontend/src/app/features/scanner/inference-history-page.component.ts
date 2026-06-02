import { Component, OnInit, inject, signal, computed } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, RouterLink } from '@angular/router';
import { catchError, of } from 'rxjs';
import { ScannerService } from '../../core/services/scanner.service';
import { FamilyStateService } from '../../core/services/family-state.service';
import { InferenceRecordDto, OperationalStateDto, FamilyAlertDto } from '../../core/models/models';
import { ScrollPolicyService } from '../../shared/directives/scroll-policy.service';

@Component({
  selector: 'app-inference-history-page',
  standalone: true,
  imports: [CommonModule, RouterLink],
  templateUrl: './inference-history-page.component.html',
  styleUrls: ['./inference-history-page.component.css']
})
export class InferenceHistoryPageComponent implements OnInit {
  private scannerService = inject(ScannerService);
  private familyState    = inject(FamilyStateService);
  private router         = inject(Router);
  private scrollPolicy   = inject(ScrollPolicyService);

  readonly inferences     = signal<InferenceRecordDto[]>([]);
  readonly currentState   = signal<OperationalStateDto | null>(null);
  readonly alerts         = signal<FamilyAlertDto[]>([]);
  readonly loading        = signal(true);
  readonly expandedId     = signal<number | null>(null);

  get familyId()   { return this.familyState.currentFamilyId(); }
  get familyName() { return this.familyState.currentFamilyName() || 'la familia'; }

  // KPIs
  readonly totalInferences = computed(() => this.inferences().length);
  readonly latestIcf       = computed(() => this.inferences()[0]?.icfValue ?? 0);
  readonly latestUncert    = computed(() => this.inferences()[0]?.uncertaintyTotal ?? 0);
  readonly stabilizedCount = computed(() =>
    this.inferences().filter(r => r.epistemicState === 'STABILIZED').length
  );

  ngOnInit(): void {
    this.scrollPolicy.set('scroll-to-new');
    if (!this.familyId) {
      this.router.navigate(['/families']);
      return;
    }
    this.loadData();
  }

  private loadData(): void {
    this.loading.set(true);

    this.scannerService.getInferences(this.familyId).pipe(
      catchError(() => of([]))
    ).subscribe(data => {
      this.inferences.set(data);
    });

    this.scannerService.getAlerts(this.familyId).pipe(
      catchError(() => of([]))
    ).subscribe(data => {
      this.alerts.set(data);
    });

    this.scannerService.getOperationalState(this.familyId).pipe(
      catchError(() => of(null))
    ).subscribe(state => {
      this.currentState.set(state);
      this.loading.set(false);
    });
  }

  exportCsv(): void {
    const headers = [
      'ID', 'Clave inferencia', 'Estado epistémico', 'Estado operacional',
      'ICF', 'Riesgo', 'Dimensión crítica', 'Incertidumbre',
      'Simulación sospechada', 'Hash evidencia', 'Fecha'
    ];
    const rows = this.inferences().map(r => [
      r.id,
      r.inferenceKey,
      r.epistemicState,
      r.operationalState ?? '',
      r.icfValue.toFixed(1),
      r.riskLevel,
      r.criticalDimension ?? '',
      ((r.uncertaintyTotal ?? 0) * 100).toFixed(0) + '%',
      r.simulationSuspected ? 'Sí' : 'No',
      r.evidenceHash ?? '',
      r.createdAt
    ]);

    const csv = [headers, ...rows]
      .map(row => row.map(cell => `"${String(cell).replace(/"/g, '""')}"`).join(','))
      .join('\n');

    const blob = new Blob(['﻿' + csv], { type: 'text/csv;charset=utf-8;' });
    const url  = URL.createObjectURL(blob);
    const a    = document.createElement('a');
    a.href     = url;
    a.download = `inferencias-familia-${this.familyId}-${new Date().toISOString().slice(0, 10)}.csv`;
    a.click();
    URL.revokeObjectURL(url);
  }

  resolveAlert(alertId: number): void {
    this.scannerService.resolveAlert(this.familyId, alertId).pipe(
      catchError(() => of(void 0))
    ).subscribe(() => {
      this.alerts.update(list => list.filter(a => a.id !== alertId));
    });
  }

  toggleExpand(id: number): void {
    this.expandedId.set(this.expandedId() === id ? null : id);
  }

  // ── Helpers visuales ──────────────────────────────────────────────────────

  formatDate(iso: string | null | undefined): string {
    if (!iso) return '—';
    return new Date(iso).toLocaleDateString('es-CO', {
      day: '2-digit', month: 'short', year: 'numeric',
      hour: '2-digit', minute: '2-digit'
    });
  }

  formatDateShort(iso: string | null | undefined): string {
    if (!iso) return '—';
    return new Date(iso).toLocaleDateString('es-CO', { day: '2-digit', month: 'short', year: 'numeric' });
  }

  truncateHash(key: string | null | undefined): string {
    if (!key) return '—';
    return key.length > 16 ? key.slice(0, 8) + '…' + key.slice(-6) : key;
  }

  icfColor(v: number | null | undefined): string {
    const n = v ?? 0;
    if (n >= 70) return '#34d399';
    if (n >= 40) return '#fbbf24';
    return '#f87171';
  }

  riskLabel(r: string | null | undefined): string {
    const map: Record<string, string> = {
      BAJO: 'Bajo', MODERADO: 'Moderado', ALTO: 'Alto', CRITICO: 'Crítico'
    };
    return map[r ?? ''] ?? (r || '—');
  }

  riskClass(r: string | null | undefined): string {
    switch (r) {
      case 'BAJO':     return 'badge-bajo';
      case 'MODERADO': return 'badge-moderado';
      case 'ALTO':     return 'badge-alto';
      case 'CRITICO':  return 'badge-critico';
      default:         return 'badge-default';
    }
  }

  epistemicLabel(e: string | null | undefined): string {
    const map: Record<string, string> = {
      INFERRED:   'Inferido',
      STABILIZED: 'Estabilizado',
      REVISED:    'Revisado',
      DEPRECATED: 'Obsoleto'
    };
    return map[e ?? ''] ?? (e || '—');
  }

  epistemicClass(e: string | null | undefined): string {
    switch (e) {
      case 'INFERRED':   return 'ep-inferred';
      case 'STABILIZED': return 'ep-stabilized';
      case 'REVISED':    return 'ep-revised';
      case 'DEPRECATED': return 'ep-deprecated';
      default:           return 'ep-inferred';
    }
  }

  stateLabel(s: string | null | undefined): string {
    const map: Record<string, string> = {
      EMERGING: 'Inicial', STABLE: 'Estable', ESCALATING: 'Deterioro',
      CRITICAL: 'Crisis', RECOVERING: 'Recuperación', RESOLVED: 'Resuelto'
    };
    return map[s ?? ''] ?? '—';
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

  stateColor(s: string | null | undefined): string {
    const map: Record<string, string> = {
      EMERGING: '#94a3b8', STABLE: '#60a5fa', ESCALATING: '#f97316',
      CRITICAL: '#ef4444', RECOVERING: '#22d3ee', RESOLVED: '#34d399'
    };
    return map[s ?? ''] ?? '#64748b';
  }

  uncertaintyLabel(v: number | null | undefined): string {
    const n = v ?? 0;
    if (n < 0.15) return 'Baja';
    if (n < 0.35) return 'Media';
    if (n < 0.50) return 'Alta';
    return 'Muy alta';
  }

  uncertaintyColor(v: number | null | undefined): string {
    const n = v ?? 0;
    if (n < 0.15) return '#34d399';
    if (n < 0.35) return '#fbbf24';
    if (n < 0.50) return '#f97316';
    return '#ef4444';
  }

  uncertaintyPct(v: number | null | undefined): string {
    return ((v ?? 0) * 100).toFixed(0) + '%';
  }

  uncertaintyBarWidth(v: number | null | undefined): string {
    return ((v ?? 0) * 100).toFixed(1) + '%';
  }

  accentColor(r: InferenceRecordDto): string {
    if (this.isRuleActivation(r)) return '#a78bfa';  // violeta para activaciones IF-REE
    return this.stateColor(r.operationalState);
  }

  stateGlyph(s: string | null | undefined): string {
    const map: Record<string, string> = {
      EMERGING: '🌱', STABLE: '🔵', ESCALATING: '🔶',
      CRITICAL: '🔴', RECOVERING: '🔷', RESOLVED: '✅'
    };
    return map[s ?? ''] ?? '⬡';
  }

  // ── IF-REE helpers ────────────────────────────────────────────────────────

  isRuleActivation(r: InferenceRecordDto): boolean {
    return r.inferenceKey !== 'ICF_CALC';
  }

  ruleLabel(key: string): string {
    return key.replace(/_/g, ' ').replace(/\b\w/g, c => c.toUpperCase());
  }

  readonly ruleCount = computed(() =>
    this.inferences().filter(r => this.isRuleActivation(r)).length
  );

  readonly baseCount = computed(() =>
    this.inferences().filter(r => !this.isRuleActivation(r)).length
  );

  readonly criticalAlerts = computed(() =>
    this.alerts().filter(a => a.severity === 'CRITICAL' || a.severity === 'HIGH')
  );

  // ── IF-ALT helpers ────────────────────────────────────────────────────────

  alertSeverityClass(severity: string): string {
    switch (severity) {
      case 'CRITICAL': return 'alert-critical';
      case 'HIGH':     return 'alert-high';
      case 'MEDIUM':   return 'alert-medium';
      default:         return 'alert-low';
    }
  }

  alertSeverityIcon(severity: string): string {
    switch (severity) {
      case 'CRITICAL': return '🔴';
      case 'HIGH':     return '🟠';
      case 'MEDIUM':   return '🟡';
      default:         return '🔵';
    }
  }

  alertTypeLabel(type: string): string {
    const map: Record<string, string> = {
      CONSECUTIVE_HIGH_RISK:    'Riesgo alto sostenido',
      CRITICAL_STATE_SUSTAINED: 'Crisis sostenida',
      SIMULATION_REPEAT:        'Simulación reiterada',
      RELAPSE_CONFIRMED:        'Recaída confirmada',
      MULTI_RULE_ACTIVATION:    'Convergencia de señales',
    };
    return map[type] ?? type;
  }
}
