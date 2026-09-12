import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, ActivatedRoute, RouterLink } from '@angular/router';
import { HttpClient } from '@angular/common/http';
import { ApiService } from '../../core/services/api.service';
import { FamilyStateService } from '../../core/services/family-state.service';
import { TransformationFlowService } from '../../core/services/transformation-flow.service';
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
  private flow        = inject(TransformationFlowService);
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

  /** Pilar que se estaba diagnosticando en esta sesión */
  activePillar: string = '';

  readonly PILLAR_META: Record<string, { icon: string; name: string; color: string }> = {
    reconocimiento: { icon: '💛', name: 'Reconocimiento', color: '#fbbf24' },
    amor:           { icon: '❤️', name: 'Amor',           color: '#ef4444' },
    entrega:        { icon: '💙', name: 'Entrega',         color: '#3b82f6' },
  };
  get pillarMeta() { return this.PILLAR_META[this.activePillar] ?? null; }

  ngOnInit() {
    // 1. Prioridad: resultado pasado en el estado de navegación (desde EvaluationComponent)
    const nav = window.history.state;
    if (nav?.pillar) this.activePillar = nav.pillar;
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

  // ── NeuroProfile Narratives ──

  getSomaticDesc(score: number): string {
    if (!score) return 'No evaluado.';
    if (score >= 80) return 'Alta sensibilidad para detectar tensiones corporales.';
    if (score >= 50) return 'Consciencia corporal intermitente.';
    return 'Dificultad para notar señales somáticas antes de reaccionar.';
  }

  getEmotionalDesc(score: number): string {
    if (!score) return 'No evaluado.';
    if (score >= 80) return 'Gran claridad para identificar y nombrar emociones.';
    if (score >= 50) return 'Reconocimiento emocional parcial.';
    return 'Tendencia a confundir o ignorar el estado emocional.';
  }

  getCognitiveDesc(score: number): string {
    if (!score) return 'No evaluado.';
    if (score >= 80) return 'Alta capacidad para observar el flujo de pensamientos.';
    if (score >= 50) return 'Identificación ocasional de narrativas mentales.';
    return 'Fusión fuerte con los pensamientos automáticos.';
  }

  getIntegrationDesc(score: number): string {
    if (!score) return 'No evaluado.';
    if (score >= 80) return 'Alta capacidad de pausa y alineación intencional.';
    if (score >= 50) return 'Integración moderada entre sensación y acción.';
    return 'Baja conexión entre consciencia y comportamiento final.';
  }

  getNeuroSynthesis(): string {
    const profile = this.result?.neuroProfile;
    if (!profile) return 'La síntesis neurofenomenológica está siendo procesada...';
    
    const somatic = profile.somaticAwareness || 0;
    const cognitive = profile.cognitiveAwareness || 0;
    const integration = profile.integrationScore || 0;
    
    let part1 = '';
    if (somatic > cognitive + 10) {
      part1 = 'Cuando aparece un conflicto familiar, normalmente tu cuerpo detecta la tensión antes que tus pensamientos.';
    } else if (cognitive > somatic + 10) {
      part1 = 'Tu mente tiende a procesar y narrar las situaciones rápidamente, a veces desconectándose de las señales iniciales del cuerpo.';
    } else {
      part1 = 'Ante un conflicto familiar, logras percibir simultáneamente tanto tus señales corporales como tus pensamientos.';
    }

    let part2 = '';
    if (integration < 60) {
      part2 = 'Sin embargo, esa señal suele convertirse rápidamente en una reacción automática antes de que aparezca una pausa consciente. Esto explica por qué, aunque comprendes la importancia del diálogo, en momentos de presión terminas reaccionando de forma impulsiva.';
    } else {
      part2 = 'Afortunadamente, logras crear un espacio de pausa saludable entre lo que sientes y cómo respondes. Esto te permite actuar con mayor intención en lugar de dejarte llevar por la reacción automática.';
    }

    let part3 = '';
    if (integration < 60) {
      part3 = 'Tus próximas misiones buscarán ampliar esa ventana entre la señal corporal y tu respuesta final.';
    } else {
      part3 = 'Tus próximas misiones se enfocarán en consolidar esta consciencia plena y expandirla a los demás miembros del hogar.';
    }

    return `${part1} ${part2} ${part3}`;
  }

  goToPlans(): void {
    // Diagnóstico completado → el plan se auto-genera → avanzar onboarding
    this.flow.advanceOnboarding('plan-generated');
    this.router.navigate(['/plans']);
  }

  /** Vuelve a /evaluations/start pre-seleccionando el mismo pilar para otra sesión de 20 preguntas */
  doAnotherSession(): void {
    this.router.navigate(['/evaluations/start'], {
      queryParams: this.activePillar ? { pillar: this.activePillar } : {}
    });
  }
}
