import {
  Component, OnInit, inject, signal, computed,
  ChangeDetectionStrategy
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { HttpClient } from '@angular/common/http';
import { catchError, of } from 'rxjs';
import { FamilyStateService } from '../../core/services/family-state.service';
import { ApiService } from '../../core/services/api.service';
import { environment } from '../../../environments/environment';

export interface TransformationSummary {
  familyId: number;
  familyName: string;
  initialIcf: number | null;
  currentIcf: number | null;
  peakIcf: number | null;
  regionalAverageIcf: number | null;
  sentinelAlertsTriggered: number;
  missionsCompleted: number;
  currentMilestone: string | null;
  dimensionProgress: string[];
}

@Component({
  selector: 'app-reports-page',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './reports-page.component.html',
  styleUrls: ['./reports-page.component.css'],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class ReportsPageComponent implements OnInit {

  private readonly http         = inject(HttpClient);
  private readonly familyState  = inject(FamilyStateService);
  private readonly api          = inject(ApiService);

  readonly loading      = signal(true);
  readonly summary      = signal<TransformationSummary | null>(null);
  readonly synthesis    = signal<string | null>(null);
  readonly synthLoading = signal(false);
  readonly error        = signal('');

  // download states per button
  readonly dlPdf       = signal(false);
  readonly dlTraj      = signal(false);

  protected familyId = 0;

  // ── Computed ─────────────────────────────────────────────────────────────

  readonly icfDelta = computed(() => {
    const s = this.summary();
    if (!s?.initialIcf || !s?.currentIcf) return null;
    return s.currentIcf - s.initialIcf;
  });

  readonly icfDeltaSign = computed(() => {
    const d = this.icfDelta();
    if (d === null) return '';
    return (d >= 0 ? '+' : '') + d.toFixed(1);
  });

  readonly icfVsRegion = computed(() => {
    const s = this.summary();
    if (!s?.currentIcf || !s?.regionalAverageIcf) return null;
    return s.currentIcf - s.regionalAverageIcf;
  });

  readonly milestoneLabel = computed(() => {
    const raw = this.summary()?.currentMilestone ?? null;
    if (!raw) return 'Sin hito activo';
    return raw.replace(/_/g, ' ').toLowerCase()
      .replace(/^\w/, c => c.toUpperCase());
  });

  // ── Lifecycle ────────────────────────────────────────────────────────────

  ngOnInit(): void {
    this.familyId = this.familyState.getSelectedFamilyId() ?? 0;
    if (!this.familyId) { this.loading.set(false); return; }
    this.loadSummary();
  }

  private loadSummary(): void {
    this.http.get<any>(`${this.api.base}/reports/family/${this.familyId}/summary`)
      .pipe(catchError(() => of(null)))
      .subscribe(res => {
        this.summary.set(res?.data ?? res ?? null);
        this.loading.set(false);
      });
  }

  // ── Synthesis ────────────────────────────────────────────────────────────

  generateSynthesis(): void {
    if (this.synthLoading()) return;
    this.synthLoading.set(true);
    this.synthesis.set(null);
    this.http.get<any>(`${this.api.base}/reports/family/${this.familyId}/synthesis`)
      .pipe(catchError(() => of(null)))
      .subscribe(res => {
        const text = res?.data ?? res ?? null;
        this.synthesis.set(typeof text === 'string' ? text : JSON.stringify(text));
        this.synthLoading.set(false);
      });
  }

  // ── Downloads ────────────────────────────────────────────────────────────

  downloadEvolutivePdf(): void {
    if (this.dlPdf()) return;
    this.dlPdf.set(true);
    this.triggerDownload(
      `${environment.apiBaseUrl}/v1/reports/export/pdf/family/${this.familyId}`,
      `Reporte_Evolutivo_${this.familyId}.pdf`,
      () => this.dlPdf.set(false)
    );
  }

  downloadTrajectoryPdf(): void {
    if (this.dlTraj()) return;
    this.dlTraj.set(true);
    this.triggerDownload(
      `${environment.apiBaseUrl}/v1/reports/export/pdf/family/${this.familyId}/trajectories`,
      `Trayectorias_${this.familyId}.pdf`,
      () => this.dlTraj.set(false)
    );
  }

  downloadExecutivePdf(): void {
    this.triggerDownload(
      `${this.api.base}/reports/family/${this.familyId}/download`,
      `Reporte_Ejecutivo_${this.familyId}.pdf`,
      () => {}
    );
  }

  private triggerDownload(url: string, filename: string, done: () => void): void {
    this.http.get(url, { responseType: 'blob' })
      .pipe(catchError(() => of(null)))
      .subscribe(blob => {
        if (blob) {
          const link = document.createElement('a');
          link.href = URL.createObjectURL(blob);
          link.download = filename;
          link.click();
          URL.revokeObjectURL(link.href);
        }
        done();
      });
  }

  // ── Helpers ──────────────────────────────────────────────────────────────

  icfColor(icf: number | null): string {
    if (icf === null) return '#475569';
    if (icf >= 80) return '#34d399';
    if (icf >= 60) return '#60a5fa';
    if (icf >= 40) return '#fbbf24';
    return '#f87171';
  }

  deltaClass(d: number | null): string {
    if (d === null) return 'rp-neutral';
    if (d > 0) return 'rp-positive';
    if (d < 0) return 'rp-negative';
    return 'rp-neutral';
  }

  formatSynthesis(text: string): string {
    return text
      .replace(/^### (.*)/gm, '<h4 class="rp-syn-h4">$1</h4>')
      .replace(/^## (.*)/gm, '<h3 class="rp-syn-h3">$1</h3>')
      .replace(/\*\*(.*?)\*\*/g, '<strong>$1</strong>')
      .replace(/\*(.*?)\*/g, '<em>$1</em>')
      .replace(/^\s*[\*\-]\s*(.*)/gm, '<li>$1</li>')
      .replace(/\n/g, '<br>');
  }
}
