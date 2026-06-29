import {
  Component, Input, OnInit, OnChanges, SimpleChanges,
  ChangeDetectionStrategy, signal, inject
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import {
  FamilyHealthSummaryService,
  FamilyHealthSummary
} from '../../../../core/services/family-health-summary.service';
import { catchError } from 'rxjs/operators';
import { of } from 'rxjs';

@Component({
  selector: 'app-health-summary-card',
  standalone: true,
  imports: [CommonModule, RouterModule],
  templateUrl: './health-summary-card.component.html',
  styleUrls: ['./health-summary-card.component.css'],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class HealthSummaryCardComponent implements OnInit, OnChanges {

  @Input() familyId!: number;

  private readonly svc = inject(FamilyHealthSummaryService);

  readonly loading = signal(true);
  readonly summary = signal<FamilyHealthSummary | null>(null);

  ngOnInit(): void { if (this.familyId > 0) this.load(); }
  ngOnChanges(c: SimpleChanges): void {
    if (c['familyId'] && !c['familyId'].firstChange && this.familyId > 0) this.load();
  }

  private load(): void {
    this.loading.set(true);
    this.svc.getSummary(this.familyId).pipe(catchError(() => of(null)))
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

  phaseIcon(phase: string): string {
    const m: Record<string, string> = {
      inconsciente: '😶', reactivo: '😤', consciente: '🧠', pleno: '✨'
    };
    return m[phase?.toLowerCase()] ?? '🔵';
  }

  directionIcon(dir: string): string {
    const m: Record<string, string> = {
      STRONG_IMPROVING: '⬆⬆', IMPROVING: '↑', STABLE: '→',
      DECLINING: '↓', CRITICAL_DECLINE: '⬇⬇', NO_DATA: '—'
    };
    return m[dir] ?? '—';
  }

  progressBarColor(pct: number): string {
    if (pct >= 80) return '#34d399';
    if (pct >= 50) return '#fbbf24';
    return '#f87171';
  }

  deltaSign(d: number | null): string {
    if (d === null) return '';
    return (d >= 0 ? '+' : '') + d.toFixed(1);
  }

  deltaColor(d: number | null): string {
    if (d === null) return '#64748b';
    if (d >= 3)  return '#34d399';
    if (d <= -3) return '#f87171';
    return '#94a3b8';
  }
}
