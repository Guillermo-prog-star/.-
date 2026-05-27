import { Component, OnInit, inject, computed, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, RouterLink } from '@angular/router';
import { catchError, of } from 'rxjs';
import { AssessmentService } from '../../core/services/assessment.service';
import { FamilyStateService } from '../../core/services/family-state.service';
import { EvaluationHistory, TimelineEntryDto } from '../../core/models/models';

@Component({
  selector: 'app-evaluation-history-page',
  standalone: true,
  imports: [CommonModule, RouterLink],
  templateUrl: './evaluation-history-page.component.html',
  styleUrls: ['./evaluation-history-page.component.css']
})
export class EvaluationHistoryPageComponent implements OnInit {
  private assessmentService = inject(AssessmentService);
  private familyState       = inject(FamilyStateService);
  private router            = inject(Router);

  readonly history  = signal<EvaluationHistory[]>([]);
  readonly timeline = signal<TimelineEntryDto[]>([]);
  readonly loading  = signal(true);

  get familyId()   { return this.familyState.currentFamilyId(); }
  get familyName() { return this.familyState.currentFamilyName() || 'la familia'; }

  /** Evaluaciones finalizadas ordenadas de más reciente a más antigua */
  readonly finalized = computed(() =>
    this.history()
      .filter(e => e.status === 'FINALIZED')
      .sort((a, b) => new Date(b.finalizedAt ?? b.startedAt).getTime()
                     - new Date(a.finalizedAt ?? a.startedAt).getTime())
  );

  /** Evaluaciones en curso (STARTED) */
  readonly started = computed(() =>
    this.history().filter(e => e.status === 'STARTED')
  );

  /** Puntos del sparkline SVG — ICF de las últimas 10 evaluaciones finalizadas */
  readonly sparklinePoints = computed((): string => {
    const pts = [...this.timeline()]
      .sort((a, b) => new Date(a.finalizedAt ?? '').getTime()
                     - new Date(b.finalizedAt ?? '').getTime())
      .slice(-10);
    if (pts.length < 2) return '';
    const W = 240, H = 48, padX = 8, padY = 6;
    return pts
      .map((p, i) => {
        const x = padX + (i / (pts.length - 1)) * (W - padX * 2);
        const y = H - padY - (Math.min(100, Math.max(0, p.healthyIndex)) / 100) * (H - padY * 2);
        return `${x},${y}`;
      })
      .join(' ');
  });

  ngOnInit(): void {
    if (!this.familyId) {
      this.router.navigate(['/families']);
      return;
    }
    this.loadData();
  }

  private loadData(): void {
    this.loading.set(true);

    this.assessmentService.getHistory(this.familyId).pipe(
      catchError(() => of([]))
    ).subscribe(data => {
      this.history.set(data);
    });

    this.assessmentService.getTimeline(this.familyId).pipe(
      catchError(() => of([]))
    ).subscribe(data => {
      this.timeline.set(data);
      this.loading.set(false);
    });
  }

  // ── Helpers de presentación ────────────────────────────────────────────────

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

  statusLabel(s: string): string {
    return s === 'FINALIZED' ? 'Finalizada' : s === 'STARTED' ? 'En curso' : s;
  }

  formatDate(iso: string | null | undefined): string {
    if (!iso) return '—';
    return new Date(iso).toLocaleDateString('es-CO', {
      day: '2-digit', month: 'short', year: 'numeric'
    });
  }

  icfColor(icf: number | null | undefined): string {
    const v = icf ?? 0;
    if (v >= 70) return '#065F46';
    if (v >= 40) return '#92400E';
    return '#991B1B';
  }

  icfBg(icf: number | null | undefined): string {
    const v = icf ?? 0;
    if (v >= 70) return '#D1FAE5';
    if (v >= 40) return '#FEF3C7';
    return '#FEE2E2';
  }

  viewResult(evalId: number): void {
    this.router.navigate(['/evaluations', evalId, 'result']);
  }

  resumeEval(evalId: number): void {
    this.router.navigate(['/evaluations', evalId, 'form']);
  }

  startNew(): void {
    this.router.navigate(['/evaluations/start']);
  }
}
