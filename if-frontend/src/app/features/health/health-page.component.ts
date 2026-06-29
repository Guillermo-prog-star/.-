import {
  Component, OnInit, ChangeDetectionStrategy,
  signal, computed, inject
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { FamilyStateService } from '../../core/services/family-state.service';
import {
  FamilyHealthSummaryService,
  FamilyHealthSummary
} from '../../core/services/family-health-summary.service';
import { catchError } from 'rxjs/operators';
import { of } from 'rxjs';

@Component({
  selector: 'app-health-page',
  standalone: true,
  imports: [CommonModule, RouterModule],
  templateUrl: './health-page.component.html',
  styleUrls: ['./health-page.component.css'],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class HealthPageComponent implements OnInit {

  private readonly familyState = inject(FamilyStateService);
  private readonly svc = inject(FamilyHealthSummaryService);

  readonly loading = signal(true);
  readonly summary = signal<FamilyHealthSummary | null>(null);

  // Gauge arc (SVG circle r=45 → circumference = 2π*45 ≈ 283)
  readonly ARC_CIRC = 283;

  readonly icfArcOffset = computed(() => {
    const icf = this.summary()?.currentIcf ?? 0;
    return this.ARC_CIRC - (icf / 100) * this.ARC_CIRC;
  });

  readonly journeyArcOffset = computed(() => {
    const p = this.summary()?.journeyProgress ?? 0;
    return this.ARC_CIRC - (p / 100) * this.ARC_CIRC;
  });

  ngOnInit(): void {
    const id = this.familyState.getSelectedFamilyId();
    if (!id) { this.loading.set(false); return; }
    this.svc.getSummary(id).pipe(catchError(() => of(null)))
      .subscribe(s => { this.summary.set(s); this.loading.set(false); });
  }

  icfColor(icf: number | null): string {
    if (icf === null) return '#475569';
    if (icf >= 80) return '#34d399';
    if (icf >= 60) return '#60a5fa';
    if (icf >= 40) return '#fbbf24';
    return '#f87171';
  }

  riskColor(risk: string): string {
    const m: Record<string, string> = {
      CRITICO: '#ef4444', ALTO: '#f97316', MODERADO: '#fbbf24', BAJO: '#34d399'
    };
    return m[risk?.toUpperCase()] ?? '#94a3b8';
  }

  progressColor(pct: number): string {
    if (pct >= 80) return '#34d399';
    if (pct >= 50) return '#fbbf24';
    return '#f87171';
  }

  phaseLabel(p: string): string {
    const m: Record<string, string> = {
      inconsciente: 'Inconsciente', reactivo: 'Reactivo',
      consciente: 'Consciente', pleno: 'Pleno'
    };
    return m[p?.toLowerCase()] ?? p;
  }

  phaseIcon(p: string): string {
    const m: Record<string, string> = {
      inconsciente: '😶', reactivo: '😤', consciente: '🧠', pleno: '✨'
    };
    return m[p?.toLowerCase()] ?? '🔵';
  }

  phaseColor(p: string): string {
    const m: Record<string, string> = {
      inconsciente: '#ef4444', reactivo: '#f97316',
      consciente: '#60a5fa', pleno: '#34d399'
    };
    return m[p?.toLowerCase()] ?? '#94a3b8';
  }

  directionText(d: string): string {
    const m: Record<string, string> = {
      STRONG_IMPROVING: '⬆⬆ Mejorando fuerte', IMPROVING: '↑ Mejorando',
      STABLE: '→ Estable', DECLINING: '↓ Declinando',
      CRITICAL_DECLINE: '⬇⬇ Declive crítico', NO_DATA: '— Sin datos'
    };
    return m[d] ?? d;
  }

  deltaSign(d: number | null): string {
    if (d === null) return '';
    return (d >= 0 ? '+' : '') + d.toFixed(1);
  }
}
