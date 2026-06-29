import {
  Component, OnInit, ChangeDetectionStrategy,
  signal, computed, inject
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { forkJoin, of } from 'rxjs';
import { catchError } from 'rxjs/operators';
import { FamilyStateService } from '../../core/services/family-state.service';
import {
  FamilyJourneyService,
  FamilyJourneyResponse,
  JourneyHistoryResponse,
  SnapshotPoint,
  JourneyLevel,
  JourneyStatus
} from '../../core/services/family-journey.service';

interface ChartPoint { x: number; y: number; levelUp: boolean; level: number; date: string; }

@Component({
  selector: 'app-family-journey-page',
  standalone: true,
  imports: [CommonModule, RouterModule],
  templateUrl: './family-journey-page.component.html',
  styleUrls: ['./family-journey-page.component.css'],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class FamilyJourneyPageComponent implements OnInit {

  private readonly familyState = inject(FamilyStateService);
  private readonly journeySvc  = inject(FamilyJourneyService);

  readonly loading  = signal(true);
  readonly snapping = signal(false);
  readonly journey  = signal<FamilyJourneyResponse | null>(null);
  readonly history  = signal<JourneyHistoryResponse | null>(null);
  readonly snapMsg  = signal<string | null>(null);

  private familyId = 0;

  readonly completedCount = computed(() =>
    (this.journey()?.levels ?? []).filter(l => l.status === 'COMPLETE').length
  );

  // ── SVG chart (240×80 viewport, Y=0 top, Y=80 bottom, levels 0-13) ─────
  readonly W = 240; readonly H = 80; readonly PAD = 10;

  readonly chartPoints = computed<ChartPoint[]>(() => {
    const pts = this.history()?.points ?? [];
    if (pts.length < 2) return [];
    const n = pts.length;
    return pts.map((p, i) => ({
      x: this.PAD + (i / (n - 1)) * (this.W - this.PAD * 2),
      y: this.H - this.PAD - (p.level / 13) * (this.H - this.PAD * 2),
      levelUp: p.levelUp,
      level: p.level,
      date: p.date
    }));
  });

  readonly chartPath = computed<string>(() => {
    const pts = this.chartPoints();
    if (pts.length < 2) return '';
    return pts.map((p, i) => `${i === 0 ? 'M' : 'L'}${p.x.toFixed(1)},${p.y.toFixed(1)}`).join(' ');
  });

  readonly yGridLines = [0, 3, 6, 9, 13].map(lvl => ({
    y: this.H - this.PAD - (lvl / 13) * (this.H - this.PAD * 2),
    label: lvl
  }));

  ngOnInit(): void {
    this.familyId = this.familyState.getSelectedFamilyId() ?? 0;
    if (!this.familyId) { this.loading.set(false); return; }
    this.load();
  }

  private load(): void {
    this.loading.set(true);
    forkJoin({
      journey: this.journeySvc.getJourney(this.familyId).pipe(catchError(() => of(null))),
      history: this.journeySvc.getHistory(this.familyId).pipe(catchError(() => of(null)))
    }).subscribe(({ journey, history }) => {
      this.journey.set(journey);
      this.history.set(history);
      this.loading.set(false);
    });
  }

  takeSnapshot(): void {
    if (this.snapping()) return;
    this.snapping.set(true);
    this.snapMsg.set(null);
    this.journeySvc.takeSnapshot(this.familyId).pipe(catchError(() => of(null)))
      .subscribe(levelUp => {
        this.snapping.set(false);
        if (levelUp === null) {
          this.snapMsg.set('Error al registrar snapshot.');
        } else if (levelUp) {
          this.snapMsg.set('🎉 ¡Nuevo nivel alcanzado! Se envió celebración.');
          this.load();
        } else {
          this.snapMsg.set('✅ Snapshot registrado (sin cambio de nivel).');
          this.load();
        }
      });
  }

  statusClass(s: JourneyStatus): string {
    return { COMPLETE: 'status-complete', IN_PROGRESS: 'status-progress',
             NEXT: 'status-next', LOCKED: 'status-locked' }[s] ?? 'status-locked';
  }

  statusIcon(s: JourneyStatus): string {
    return { COMPLETE: '✅', IN_PROGRESS: '🔄', NEXT: '👉', LOCKED: '🔒' }[s] ?? '🔒';
  }

  progressBarColor(pct: number): string {
    if (pct >= 80) return '#34d399';
    if (pct >= 50) return '#fbbf24';
    return '#f87171';
  }

  trackLevel(_: number, l: JourneyLevel): number { return l.level; }
  trackPoint(_: number, p: ChartPoint): string { return p.date; }
}
