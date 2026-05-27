import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { AssessmentService } from '../../core/services/assessment.service';
import { Question, SaveAnswerRequest, FinalizeRequest } from '../../core/models/question.model';
import { NarrativeCompanionComponent } from '../../shared/components/narrative-companion.component';
import { FamilyStateService } from '../../core/services/family-state.service';
import { catchError, EMPTY } from 'rxjs';
import { PRESENCE_SCALE } from '../../../domain/constants/presenceScaleDomain';

@Component({
  selector: 'app-evaluation',
  standalone: true,
  imports: [CommonModule, RouterLink, NarrativeCompanionComponent],
  templateUrl: './evaluation.component.html',
  styleUrls: ['./evaluation.component.css']
})
export class EvaluationComponent implements OnInit {
  private assessmentService = inject(AssessmentService);
  private familyState       = inject(FamilyStateService);
  private router = inject(Router);
  private route  = inject(ActivatedRoute);

  evaluationId: number = 0;
  questions: Question[] = [];
  currentIndex: number = 0;
  /** Mapa questionId → valor (1-5). Se usa como estado local y como fallback clásico. */
  answers: Map<number, number> = new Map();
  isFinished: boolean = false;
  isTransitioning: boolean = false;
  isLoadingResults: boolean = false;
  familyId: number = 0;
  familyName: string = '';

  /**
   * Indica si algún guardado incremental falló.
   * En ese caso, sendResults() vuelve al modo clásico (envía todas las respuestas en el body).
   */
  incrementalSaveFailed: boolean = false;
  finalizeError: string = '';

  ngOnInit(): void {
    this.familyId   = this.familyState.currentFamilyId();
    this.familyName = this.familyState.currentFamilyName() || 'la familia';

    this.route.params.subscribe((params: any) => {
      this.evaluationId = Number(params['id']);
      if (!this.evaluationId) {
        this.router.navigate(['/evaluations/start']);
      }
    });

    if (this.familyId === 0) {
      this.router.navigate(['/families']);
      return;
    }
    this.loadQuestions();
  }

  loadQuestions(): void {
    if (this.familyId === 0) return;

    // Lee el hito actual desde el servicio de estado (fuente reactiva única)
    const milestone = this.familyState.currentMilestone() || undefined;

    this.assessmentService.getRandomQuestions(this.familyId, milestone).subscribe({
      next: (data: Question[]) => {
        this.questions = data;
        // Tras cargar las preguntas, intentar recuperar el progreso guardado
        if (this.evaluationId > 0) {
          this.restoreProgress();
        }
      },
      error: (err: any) => console.error('[ASSESSMENT] Error cargando preguntas:', err)
    });
  }

  /** Reanuda el cuestionario: restaura las respuestas ya guardadas en el servidor. */
  private restoreProgress(): void {
    this.assessmentService.getProgress(this.evaluationId).pipe(
      catchError(() => EMPTY)
    ).subscribe(progress => {
      if (!progress || progress.answered === 0) return;

      // Reconstruir el Map de respuestas desde las que ya estaban guardadas
      progress.answers.forEach(saved => {
        if (saved.questionId != null) {
          this.answers.set(saved.questionId, saved.score);
        }
      });

      // Avanzar al índice de la primera pregunta sin responder
      const firstUnanswered = this.questions.findIndex(q => !this.answers.has(q.id));
      if (firstUnanswered > 0) {
        this.currentIndex = firstUnanswered;
        console.log(`[ASSESSMENT] Reanudando desde pregunta ${firstUnanswered + 1} (${progress.answered} ya respondidas).`);
      }
    });
  }

  selectAnswer(score: number): void {
    if (!this.questions[this.currentIndex] || this.isTransitioning) return;

    const currentQuestion = this.questions[this.currentIndex];
    this.answers.set(currentQuestion.id, score);

    // ── Guardado incremental (mobile-first) ─────────────────────────────────
    // Se persiste en background sin bloquear la animación de transición.
    // Si el servidor no está disponible, se activa el fallback clásico en sendResults().
    if (this.evaluationId > 0) {
      const payload: SaveAnswerRequest[] = [{ questionId: currentQuestion.id, value: score }];
      this.assessmentService.saveAnswers(this.evaluationId, payload).pipe(
        catchError(err => {
          console.warn('[ASSESSMENT] Guardado incremental falló (modo offline activado):', err);
          this.incrementalSaveFailed = true;
          return EMPTY;
        })
      ).subscribe();
    }

    console.log(`Paso ${this.currentIndex + 1}/${this.questions.length}: Q${currentQuestion.id} = ${score}`);

    this.isTransitioning = true;
    setTimeout(() => {
      if (this.currentIndex < this.questions.length - 1) {
        this.currentIndex++;
        this.isTransitioning = false;
      } else {
        this.isTransitioning = false;
        this.sendResults();
      }
    }, 350);
  }

  prevQuestion(): void {
    if (this.currentIndex > 0 && !this.isTransitioning) {
      this.isTransitioning = true;
      setTimeout(() => {
        this.currentIndex--;
        this.isTransitioning = false;
      }, 200);
    }
  }

  sendResults(): void {
    this.isFinished = true;
    this.isLoadingResults = true;

    let payload: FinalizeRequest;

    if (!this.incrementalSaveFailed) {
      // ── Flujo mobile-first ──────────────────────────────────────────────
      // Las respuestas ya están en BD (guardadas incrementalmente).
      // El backend las carga y ejecuta RISK_ALGO_V1 sin necesitar el body.
      payload = {};
      console.log('[ASSESSMENT] Finalizando en modo mobile-first (respuestas ya en BD).');
    } else {
      // ── Flujo clásico (fallback) ────────────────────────────────────────
      // Algún guardado incremental falló (sin conexión temporal, etc.).
      // Se envían todas las respuestas acumuladas en el Map.
      payload = {
        answers: Array.from(this.answers.keys()).map(qId => ({
          questionId: Number(qId),
          value: Number(this.answers.get(qId))  // campo correcto: 'value', no 'answerValue'
        }))
      };
      console.log('[ASSESSMENT] Finalizando en modo clásico (fallback):', payload.answers?.length, 'respuestas.');
    }

    this.assessmentService.finalizeEvaluation(this.evaluationId, payload).subscribe({
      next: (response: any) => {
        console.log('[ASSESSMENT] Evaluación finalizada. Redirigiendo al resultado...');
        this.isLoadingResults = false;
        // Pasar el resultado en el estado de navegación para que el result-page lo muestre sin petición extra
        const result = response?.data ?? response;
        this.router.navigate(['/evaluations', this.evaluationId, 'result'], {
          state: { result }
        });
      },
      error: (err: any) => {
        console.error('[ASSESSMENT] Error al finalizar:', err);
        this.isLoadingResults = false;
        this.isFinished = false; // Permitir reintentar
        this.finalizeError = err?.error?.message || err?.message || 'Error al finalizar el diagnóstico. Intenta de nuevo.';
      }
    });
  }

  restartTest(): void {
    this.currentIndex = 0;
    this.answers.clear();
    this.isFinished = false;
    this.isTransitioning = false;
    this.isLoadingResults = false;
    this.incrementalSaveFailed = false;
    this.loadQuestions();
  }

  // ── Helpers de Taxonomía y Diseño Premium ──────────────────────────────────

  getDimensionColor(dimension: string): string {
    const normalized = (dimension || '').toLowerCase();
    switch (normalized) {
      case 'comunicacion': return 'var(--dim-communication)';
      case 'emociones':    return 'var(--dim-emotions)';
      case 'habitos':      return 'var(--dim-habits)';
      case 'tiempos':      return 'var(--dim-times)';
      default:             return 'var(--accent)';
    }
  }

  getDimensionColorGlow(dimension: string): string {
    const normalized = (dimension || '').toLowerCase();
    switch (normalized) {
      case 'comunicacion': return 'rgba(56, 189, 248, 0.2)';
      case 'emociones':    return 'rgba(251, 113, 133, 0.2)';
      case 'habitos':      return 'rgba(251, 191, 36, 0.2)';
      case 'tiempos':      return 'rgba(167, 139, 250, 0.2)';
      default:             return 'rgba(168, 85, 247, 0.2)';
    }
  }

  getDimensionName(dimension: string): string {
    const normalized = (dimension || '').toLowerCase();
    switch (normalized) {
      case 'comunicacion': return 'Comunicación Asertiva';
      case 'emociones':    return 'Regulación & Clima Emocional';
      case 'habitos':      return 'Hábitos & Convivencia Colectiva';
      case 'tiempos':      return 'Tiempos de Conexión Activa';
      default:             return dimension || 'Consciencia Familiar';
    }
  }

  getQuestionTypeLabel(type: string | undefined): string {
    switch ((type || '').toUpperCase()) {
      case 'CORE':        return 'Medición Longitudinal Base';
      case 'ADAPTIVE':    return 'Profundización por Vulnerabilidad';
      case 'FASE_PILLAR': return 'Evaluación de Hito Temporal';
      case 'MIRROR':      return 'Control de Consistencia Interna';
      case 'EXPLORATORY': return 'Exploración de Entorno IA';
      default:            return 'Reactivo de Consciencia';
    }
  }

  getQuestionTypeDesc(type: string | undefined): string {
    switch ((type || '').toUpperCase()) {
      case 'CORE':        return 'Mide la evolución histórica constante de los pilares del hogar.';
      case 'ADAPTIVE':    return 'Adaptada para indagar a fondo la dimensión con mayor riesgo detectado.';
      case 'FASE_PILLAR': return 'Alineada al hito temporal de la ruta de transformación familiar.';
      case 'MIRROR':      return 'Validador psicométrico para asegurar la sinceridad y consistencia clínica.';
      case 'EXPLORATORY': return 'Análisis predictivo de patrones emergentes asistido por Inteligencia Artificial.';
      default:            return 'Reactivo de diagnóstico familiar.';
    }
  }

  getSeverityLevel(weight: number | undefined): string {
    const val = weight ?? 0.5;
    if (val >= 0.8) return 'Impacto Crítico';
    if (val >= 0.6) return 'Impacto Alto';
    return 'Impacto Estructural';
  }

  getSeverityStars(weight: number | undefined): number[] {
    const val = weight ?? 0.5;
    const count = val >= 0.8 ? 3 : val >= 0.6 ? 2 : 1;
    return Array(count).fill(0);
  }

  getScoreLabel(score: number): string {
    const labels: Record<number, string> = {
      1: 'Inconsciente',
      2: 'Reactivo',
      3: 'Consciente',
      4: 'Intencional',
      5: 'Pleno'
    };
    return labels[score] || '';
  }

  getScoreDetail(score: number): string {
    const details: Record<number, string> = {
      1: 'No nos damos cuenta o lo vemos normal en el día a día.',
      2: 'Reaccionamos por impulso cuando ocurre, sin control previo.',
      3: 'Nos damos cuenta del patrón pero nos cuesta mucho gestionarlo.',
      4: 'Elegimos activamente actuar por el bienestar del hogar.',
      5: 'Fluye de manera natural con paz profunda y amor voluntario.'
    };
    return details[score] || '';
  }

  /**
   * SDD: Obtiene las opciones de respuesta adaptativas y en primera persona
   * mapeadas dinámicamente según la dimensión de la pregunta.
   */
  getCustomOptions(dimension: string): { score: number; label: string; text: string; colorClass: string; hexColor: string }[] {
    const dim = (dimension || '').toLowerCase().trim();
    if (dim.includes('emocion')) {
      return [
        { score: 1, label: 'Inconsciente', text: 'No noto que hago esto', colorClass: 'Gris', hexColor: '#64748b' },
        { score: 2, label: 'Reactivo', text: 'Me pasa frecuentemente', colorClass: 'Rojo Suave', hexColor: '#f87171' },
        { score: 3, label: 'Consciente', text: 'Ya empiezo a reconocerlo', colorClass: 'Amarillo', hexColor: '#f59e0b' },
        { score: 4, label: 'Intencional', text: 'Intento detenerme antes', colorClass: 'Azul', hexColor: '#3b82f6' },
        { score: 5, label: 'Pleno', text: 'Manejo mis emociones con calma', colorClass: 'Verde', hexColor: '#10b981' }
      ];
    } else if (dim.includes('comunicac')) {
      return [
        { score: 1, label: 'Inconsciente', text: 'Casi nunca escucho realmente', colorClass: 'Gris', hexColor: '#64748b' },
        { score: 2, label: 'Reactivo', text: 'Me altero fácilmente', colorClass: 'Rojo Suave', hexColor: '#f87171' },
        { score: 3, label: 'Consciente', text: 'A veces logro escuchar', colorClass: 'Amarillo', hexColor: '#f59e0b' },
        { score: 4, label: 'Intencional', text: 'Estoy aprendiendo a dialogar', colorClass: 'Azul', hexColor: '#3b82f6' },
        { score: 5, label: 'Pleno', text: 'Escucho con apertura y respeto', colorClass: 'Verde', hexColor: '#10b981' }
      ];
    } else if (dim.includes('habit') || dim.includes('hábito')) {
      return [
        { score: 1, label: 'Inconsciente', text: 'No pienso mucho en eso', colorClass: 'Gris', hexColor: '#64748b' },
        { score: 2, label: 'Reactivo', text: 'Solo cumplo si me presionan', colorClass: 'Rojo Suave', hexColor: '#f87171' },
        { score: 3, label: 'Consciente', text: 'A veces lo logro', colorClass: 'Amarillo', hexColor: '#f59e0b' },
        { score: 4, label: 'Intencional', text: 'Estoy creando disciplina', colorClass: 'Azul', hexColor: '#3b82f6' },
        { score: 5, label: 'Pleno', text: 'Ya es parte de mí', colorClass: 'Verde', hexColor: '#10b981' }
      ];
    } else if (dim.includes('tiemp')) {
      return [
        { score: 1, label: PRESENCE_SCALE[1].state, text: PRESENCE_SCALE[1].description, colorClass: PRESENCE_SCALE[1].colorCode, hexColor: '#64748b' },
        { score: 2, label: PRESENCE_SCALE[2].state, text: PRESENCE_SCALE[2].description, colorClass: PRESENCE_SCALE[2].colorCode, hexColor: '#f87171' },
        { score: 3, label: PRESENCE_SCALE[3].state, text: PRESENCE_SCALE[3].description, colorClass: PRESENCE_SCALE[3].colorCode, hexColor: '#f59e0b' },
        { score: 4, label: PRESENCE_SCALE[4].state, text: PRESENCE_SCALE[4].description, colorClass: PRESENCE_SCALE[4].colorCode, hexColor: '#3b82f6' },
        { score: 5, label: PRESENCE_SCALE[5].state, text: PRESENCE_SCALE[5].description, colorClass: PRESENCE_SCALE[5].colorCode, hexColor: '#10b981' }
      ];
    }
    
    // Default fallback
    return [
      { score: 1, label: 'Inconsciente', text: 'Casi nunca me doy cuenta', colorClass: 'Gris', hexColor: '#64748b' },
      { score: 2, label: 'Reactivo', text: 'Reacciono antes de pensar', colorClass: 'Rojo Suave', hexColor: '#f87171' },
      { score: 3, label: 'Consciente', text: 'A veces logro manejarlo', colorClass: 'Amarillo', hexColor: '#f59e0b' },
      { score: 4, label: 'Intencional', text: 'Estoy aprendiendo a responder mejor', colorClass: 'Azul', hexColor: '#3b82f6' },
      { score: 5, label: 'Pleno', text: 'Ya lo hago naturalmente', colorClass: 'Verde', hexColor: '#10b981' }
    ];
  }
}
