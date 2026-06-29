import {
  Component, Input, OnInit, OnChanges, SimpleChanges,
  ChangeDetectionStrategy, signal, computed, inject
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import {
  FamilyJourneyService,
  FamilyJourneyResponse,
  JourneyStatus
} from '../../../../core/services/family-journey.service';
import { catchError } from 'rxjs/operators';
import { of } from 'rxjs';

@Component({
  selector: 'app-family-journey-widget',
  standalone: true,
  imports: [CommonModule, RouterModule],
  templateUrl: './family-journey-widget.component.html',
  styleUrls: ['./family-journey-widget.component.css'],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class FamilyJourneyWidgetComponent implements OnInit, OnChanges {

  @Input() familyId!: number;

  private readonly journeySvc = inject(FamilyJourneyService);

  readonly loading = signal(true);
  readonly journey = signal<FamilyJourneyResponse | null>(null);

  readonly completedLevels = computed(() =>
    (this.journey()?.levels ?? []).filter(l => l.status === 'COMPLETE').length
  );

  readonly nextLevel = computed(() =>
    (this.journey()?.levels ?? []).find(l => l.status === 'NEXT') ?? null
  );

  readonly progressColor = computed(() => {
    const p = this.journey()?.journeyProgress ?? 0;
    if (p >= 80) return '#34d399';
    if (p >= 50) return '#fbbf24';
    return '#f87171';
  });

  ngOnInit(): void { if (this.familyId > 0) this.load(); }
  ngOnChanges(c: SimpleChanges): void {
    if (c['familyId'] && !c['familyId'].firstChange && this.familyId > 0) this.load();
  }

  private load(): void {
    this.loading.set(true);
    this.journeySvc.getJourney(this.familyId).pipe(catchError(() => of(null)))
      .subscribe(j => { this.journey.set(j); this.loading.set(false); });
  }

  statusIcon(s: JourneyStatus): string {
    return { COMPLETE: '✅', IN_PROGRESS: '🔄', NEXT: '👉', LOCKED: '🔒' }[s] ?? '🔒';
  }

  trackLevel(_: number, l: { level: number }): number { return l.level; }
}
