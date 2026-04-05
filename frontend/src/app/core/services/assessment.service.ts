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
   * Útil para el Dashboard de William en el Nodo Armenia.
   */
  getQuestionStats(): Observable<QuestionStat[]> {
    return this.http.get<ApiResponse<QuestionStat[]>>(`${this.api.base}/assessment/questions/stats`)
      .pipe(map(response => response.data));
  }

  /**
   * 2. Diagnóstico: Obtiene 20 preguntas aleatorias procesadas desde el banco.
   */
  getRandomQuestions(): Observable<Question[]> {
    return this.http.get<Question[]>(`${this.api.base}/assessment/random`);
  }

  /**
   * 3. Persistencia: Envía el mapa de respuestas a MySQL vía Spring Boot.
   * @param payload { familyId: number, responses: Map<string, number> }
   */
  submitEvaluation(payload: any): Observable<any> {
    return this.http.post(`${this.api.base}/assessment/submit`, payload);
  }
}