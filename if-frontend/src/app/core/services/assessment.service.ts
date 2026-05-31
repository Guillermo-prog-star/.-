import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, map } from 'rxjs';
import { ApiService } from './api.service';
import { Question, SaveAnswerRequest, AnswerProgressResponse, FinalizeRequest, PillarProgressResult } from '../models/question.model';
import { ApiResponse } from '../models/api-response.model';
import { EvaluationHistory, TimelineEntryDto } from '../models/models';

/**
 * QuestionStat: Interface para el control de calidad.
 */
export interface QuestionStat {
  dimension: string;
  area: string;
  count: number;
}

@Injectable({
  providedIn: 'root'
})
export class AssessmentService {
  private http = inject(HttpClient);
  private api = inject(ApiService);

  /**
   * 1. Auditoría: Obtiene el resumen de carga del banco de preguntas.
   */
  getQuestionStats(): Observable<QuestionStat[]> {
    return this.http.get<ApiResponse<QuestionStat[]>>(`${this.api.base}/assessments/questions/stats`)
      .pipe(map(response => response.data));
  }

  /**
   * 2. Diagnóstico: Obtiene 20 preguntas adaptativas filtradas por familia y hito.
   *    - familyId:      adapta reactivos al riesgo y dimensión crítica detectada
   *    - milestoneCode: hito explícito (W1-M36); si se omite, usa el hito actual de la familia
   */
  getRandomQuestions(familyId: number, milestoneCode?: string, pillarName?: string): Observable<Question[]> {
    let url = `${this.api.base}/assessments/random?familyId=${familyId}`;
    if (milestoneCode?.trim()) {
      url += `&milestoneCode=${encodeURIComponent(milestoneCode.trim())}`;
    }
    if (pillarName?.trim()) {
      url += `&pillarName=${encodeURIComponent(pillarName.trim())}`;
    }
    return this.http.get<ApiResponse<Question[]>>(url)
      .pipe(map(response => response.data));
  }

  /**
   * Progreso de sesiones por pilar para una familia.
   * GET /api/assessments/pillar-progress?familyId={id}
   */
  getPillarProgress(familyId: number): Observable<PillarProgressResult> {
    return this.http
      .get<ApiResponse<PillarProgressResult>>(
        `${this.api.base}/assessments/pillar-progress?familyId=${familyId}`
      )
      .pipe(map(r => r.data));
  }

  /**
   * 3. Guardado incremental (mobile-first):
   *    Persiste una o más respuestas de forma idempotente (upsert).
   *    Si la app se cierra, el usuario puede retomar donde se quedó.
   *    POST /api/assessments/{evalId}/answers
   */
  saveAnswers(evalId: number, answers: SaveAnswerRequest[]): Observable<AnswerProgressResponse> {
    return this.http.post<ApiResponse<AnswerProgressResponse>>(
      `${this.api.base}/assessments/${evalId}/answers`,
      answers
    ).pipe(map(response => response.data));
  }

  /**
   * 4. Progreso del cuestionario:
   *    Devuelve cuántas preguntas fueron respondidas y si ya se puede finalizar.
   *    GET /api/assessments/{evalId}/answers
   */
  getProgress(evalId: number): Observable<AnswerProgressResponse> {
    return this.http.get<ApiResponse<AnswerProgressResponse>>(
      `${this.api.base}/assessments/${evalId}/answers`
    ).pipe(map(response => response.data));
  }

  /**
   * 5. Finalización: Dispara RISK_ALGO_V1 y publica el evento de RabbitMQ.
   *    - Flujo mobile-first: body vacío ({}); el backend carga respuestas desde BD.
   *    - Flujo clásico:      body con answers[]; el backend las usa directamente.
   *    POST /api/assessments/{id}/finalize  ← AssessmentController (devuelve EvaluationResultResponse)
   *    NOTA: No usar /api/evaluations/{id}/finalize — ese endpoint devuelve sólo el ID (Long)
   *    y tiene @Valid que rechaza el body vacío del flujo mobile-first.
   */
  finalizeEvaluation(id: number, payload: FinalizeRequest): Observable<any> {
    return this.http.post(`${this.api.base}/assessments/${id}/finalize`, payload);
  }

  /**
   * 6. Historial completo de evaluaciones de la familia.
   *    GET /api/assessments/family/{familyId}/history
   */
  getHistory(familyId: number): Observable<EvaluationHistory[]> {
    return this.http.get<ApiResponse<EvaluationHistory[]>>(
      `${this.api.base}/assessments/family/${familyId}/history`
    ).pipe(map(r => r.data ?? []));
  }

  /**
   * 7. Timeline evolutivo: ICF + riesgo por evaluación finalizada.
   *    GET /api/assessments/family/{familyId}/timeline
   */
  getTimeline(familyId: number): Observable<TimelineEntryDto[]> {
    return this.http.get<ApiResponse<TimelineEntryDto[]>>(
      `${this.api.base}/assessments/family/${familyId}/timeline`
    ).pipe(map(r => r.data ?? []));
  }

  /**
   * 8. Historial de puntuaciones por dimensión (normalizadas).
   *    GET /api/analytics/family/${familyId}/dimension-history
   */
  getDimensionHistory(familyId: number): Observable<any[]> {
    return this.http.get<ApiResponse<any[]>>(
      `${this.api.base}/analytics/family/${familyId}/dimension-history`
    ).pipe(map(r => r.data ?? []));
  }
}
