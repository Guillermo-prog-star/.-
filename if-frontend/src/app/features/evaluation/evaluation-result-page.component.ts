import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, ActivatedRoute, RouterLink } from '@angular/router';
import { HttpClient } from '@angular/common/http';
import { ApiService } from '../../core/services/api.service';
import { FamilyStateService } from '../../core/services/family-state.service';
import { EvaluationResultResponse } from '../../core/models/models';

@Component({
  selector: 'app-evaluation-result-page',
  standalone: true,
  imports: [CommonModule, RouterLink],
  templateUrl: './evaluation-result-page.component.html',
  styleUrls: ['./evaluation-result-page.component.css']
})
export class EvaluationResultPageComponent implements OnInit {
  private http        = inject(HttpClient);
  private api         = inject(ApiService);
  private familyState = inject(FamilyStateService);
  private route       = inject(ActivatedRoute);
  private router      = inject(Router);

  result: EvaluationResultResponse | null = null;
  loading = true;
  get currentMilestone() { return this.familyState.currentMilestone() || 'Inicio'; }

  /** Dimensiones en el orden del radar (emociones, comunicacion, habitos, tiempos) */
  readonly dimConfig = [
    { key: 'emociones',    label: 'Emociones',    bg: '#FDF2F8', text: '#9D174D', dot: '#EC4899' },
    { key: 'comunicacion', label: 'Comunicación', bg: '#EFF6FF', text: '#1E40AF', dot: '#3B82F6' },
    { key: 'habitos',      label: 'Hábitos',       bg: '#F0FDF4', text: '#166534', dot: '#22C55E' },
    { key: 'tiempos',      label: 'Tiempos',       bg: '#FFFBEB', text: '#92400E', dot: '#F59E0B' },
  ];

  ngOnInit() {
    // 1. Prioridad: resultado pasado en el estado de navegación (desde EvaluationComponent)
    const nav = window.history.state;
    if (nav?.result) {
      this.result = nav.result as EvaluationResultResponse;
      this.loading = false;
    } else {
      // 2. Fallback: cargar desde la API si el usuario refresca (F5)
      const id = this.route.snapshot.paramMap.get('id');
      if (id) {
        this.loadFromApi(id);
      } else {
        this.loading = false;
      }
    }
  }

  private loadFromApi(id: string) {
    // Intentar desde el historial de la familia
    const familyId = this.familyState.currentFamilyId();
    if (!familyId) { this.loading = false; return; }

    this.http.get<any>(`${this.api.base}/assessments/family/${familyId}/history`).subscribe({
      next: ({ data }: any) => {
        const entry = (data as any[])?.find((e: any) => String(e.id) === id);
        if (entry) {
          // Construir un EvaluationResultResponse aproximado desde el resumen
          this.result = {
            evaluationId: entry.id,
            familyId: entry.familyId,
            riskLevel: entry.riskLevel ?? 'MODERADO',
            dimensionScores: [],
            healthyIndex: entry.icf ?? 0,
            hasCrisis: false
          };
        }
        this.loading = false;
      },
      error: () => { this.loading = false; }
    });
  }

  /** Score 0-100 para una dimensión del resultado */
  getScore(dimKey: string): number {
    if (!this.result?.dimensionScores?.length) return 0;
    const match = this.result.dimensionScores.find(d =>
      d.dimension.toLowerCase().includes(dimKey.toLowerCase())
    );
    return match?.score ?? 0;
  }

  /** Porcentaje para la barra de progreso (score ya está en 0-100) */
  getScorePercent(dimKey: string): number {
    return Math.min(100, Math.max(0, this.getScore(dimKey)));
  }

  // ── Helpers de riesgo ────────────────────────────────────────────────────────

  riskLabel(r: string | undefined): string {
    const labels: Record<string, string> = {
      BAJO: 'Bajo', MODERADO: 'Moderado', ALTO: 'Alto', CRITICO: 'Crítico',
      // Compatibilidad con valores en inglés
      LOW: 'Bajo', MEDIUM: 'Medio', HIGH: 'Alto'
    };
    return labels[r ?? ''] ?? (r || '—');
  }

  riskBg(r: string | undefined): string {
    const bg: Record<string, string> = {
      BAJO: '#D1FAE5', MODERADO: '#FEF3C7', ALTO: '#FEE2E2', CRITICO: '#FEE2E2',
      LOW: '#D1FAE5', MEDIUM: '#FEF3C7', HIGH: '#FEE2E2'
    };
    return bg[r ?? ''] ?? '#F3F4F6';
  }

  riskColor(r: string | undefined): string {
    const color: Record<string, string> = {
      BAJO: '#065F46', MODERADO: '#92400E', ALTO: '#991B1B', CRITICO: '#7F1D1D',
      LOW: '#065F46', MEDIUM: '#92400E', HIGH: '#991B1B'
    };
    return color[r ?? ''] ?? '#374151';
  }

  // ── Consciencia ──────────────────────────────────────────────────────────────

  consciousnessIcon(label: string | undefined): string {
    const icons: Record<string, string> = {
      Plena: '🌟', Madura: '💡', Consciente: '🔆', Reactiva: '⚡', Inconsciente: '😴'
    };
    return icons[label ?? ''] ?? '🔷';
  }

  goToPlans(): void {
    this.router.navigate(['/plans']);
  }
}
