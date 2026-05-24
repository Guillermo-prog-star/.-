import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, of } from 'rxjs';
import { map, catchError } from 'rxjs/operators';
import { ApiResponse } from '../models/api-response.model';

export interface EmotionalStimulus {
  id: number;
  type: string;
  title: string;
  mediaUrl: string;
  category: string;
  targetRole: string;
  createdAt: string;
}

export interface ReflectionRequest {
  familyId: number;
  memberId: number;
  reflection: string;
  emotionalScore: number;
}

export interface EmotionalInferenceDto {
  empathy: number;
  avoidance: number;
  disconnection: number;
  activePresence: number;
  reactivity: number;
  feedback: string;
  recommendedAction: string;
}

export interface FamilyEmotionalStats {
  ioc: number;
  totalReflections: number;
  averageEmpathy: number;
  averagePresence: number;
  averageReactivity: number;
}

@Injectable({ providedIn: 'root' })
export class EmotionalEngineService {
  private readonly http = inject(HttpClient);
  private readonly base = '/api/emotional-engine';

  /** Obtiene el estímulo reflexivo activo (el video) */
  getActiveStimulus(): Observable<EmotionalStimulus | null> {
    return this.http.get<ApiResponse<EmotionalStimulus>>(`${this.base}/active`).pipe(
      map(r => r.data),
      catchError(() => of(null))
    );
  }

  /** Procesa la reflexión introspectiva de un miembro de la familia usando Claude */
  submitReflection(stimulusId: number, payload: ReflectionRequest): Observable<EmotionalInferenceDto | null> {
    return this.http.post<ApiResponse<EmotionalInferenceDto>>(`${this.base}/${stimulusId}/reflect`, payload).pipe(
      map(r => r.data),
      catchError(() => of(null))
    );
  }

  /** Obtiene las estadísticas acumuladas del Índice de Observación Consciente (IOC) */
  getFamilyStats(familyId: number): Observable<FamilyEmotionalStats | null> {
    return this.http.get<ApiResponse<FamilyEmotionalStats>>(`${this.base}/stats/family/${familyId}`).pipe(
      map(r => r.data),
      catchError(() => of(null))
    );
  }
}
