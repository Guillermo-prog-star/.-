import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, map } from 'rxjs';
import { ApiService } from './api.service';
import { Question } from '../models/question.model';
import { ApiResponse } from '../models/api-response.model';

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
   * 2. Diagnóstico: Obtiene 20 preguntas aleatorias filtradas por la etapa de la familia (6 meses, 2 años, etc).
   */
  getRandomQuestions(familyId: number): Observable<Question[]> {
    return this.http.get<ApiResponse<Question[]>>(`${this.api.base}/assessments/random?familyId=${familyId}`)
      .pipe(map(response => response.data));
  }

  /**
   * 3. Persistencia: Envía el mapa de respuestas a MySQL vía Spring Boot.
   * @param payload { familyId: number, responses: Map<string, number> }
   */
  submitEvaluation(payload: any): Observable<any> {
    return this.http.post(`${this.api.base}/assessments/submit`, payload);
  }

  /**
   * 4. Finalización: Envía las respuestas finales y dispara RabbitMQ.
   */
  finalizeEvaluation(id: number, payload: any): Observable<any> {
    return this.http.post(`${this.api.base}/evaluations/${id}/finalize`, payload);
  }
}