// Ubicación: frontend/src/app/features/admin/stats/stats.component.ts
import { Component, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { SentinelCoreService } from '../../../core/services/sentinel-core.service';

@Component({
  selector: 'app-stats',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './stats.component.html',
  styleUrls: ['./stats.component.css']
})
export class StatsComponent {
  public sentinel = inject(SentinelCoreService);

  // Acceso directo a Signals
  get stats() { return this.sentinel.stats(); }
  get alerts() { return this.sentinel.alerts(); }
  get sentiment() { return this.sentinel.sentiment(); }
  get loading() { return this.sentinel.loading(); }

  /**
   * Retorna las entradas de hitos con tipado explícito.
   */
  getMilestoneEntries(): [string, number][] {
    const data = this.stats?.milestoneDistribution;
    if (!data) return [];
    return Object.entries(data) as [string, number][];
  }

  /**
   * Retorna y ordena las entradas de sentimiento.
   * El cast 'as number' previene el error de compilación NG9 en Docker.
   */
  getSentimentEntries(): [string, number][] {
    const data = this.sentiment?.sentimentDistribution;
    if (!data) return [];
    return (Object.entries(data) as [string, number][])
      .sort((a, b) => b[1] - a[1]);
  }

  refresh() {
    this.sentinel.refreshAll();
  }

  exportPdf() {
    this.sentinel.downloadExecutivePdf();
  }
}