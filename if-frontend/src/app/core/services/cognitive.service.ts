import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, of } from 'rxjs';
import { map, catchError } from 'rxjs/operators';
import {
  CognitiveSnapshot, NarrativeResponse,
  GraphResponse, ReflectionResponse, IcfHistoryPoint, MemoryResponse
} from '../models/cognitive.model';

interface ApiResponse<T> { success: boolean; data: T; message: string; }

@Injectable({ providedIn: 'root' })
export class CognitiveService {
  private readonly http = inject(HttpClient);
  private readonly base = '/api/cognitive';

  /** Snapshot completo — una sola llamada para el dashboard */
  getSnapshot(familyId: number): Observable<CognitiveSnapshot | null> {
    return this.http.get<ApiResponse<CognitiveSnapshot>>(`${this.base}/${familyId}/snapshot`).pipe(
      map(r => r.data),
      catchError(() => of(null))
    );
  }

  /** Historia completa en capítulos */
  getNarrative(familyId: number): Observable<NarrativeResponse | null> {
    return this.http.get<ApiResponse<NarrativeResponse>>(`${this.base}/${familyId}/narrative`).pipe(
      map(r => r.data),
      catchError(() => of(null))
    );
  }

  /** Grafo de dinámicas entre miembros */
  getGraph(familyId: number): Observable<GraphResponse | null> {
    return this.http.get<ApiResponse<GraphResponse>>(`${this.base}/${familyId}/graph`).pipe(
      map(r => r.data),
      catchError(() => of(null))
    );
  }

  /** Memorias activas por tipo: episódicas, semánticas y procedurales */
  getMemory(familyId: number): Observable<MemoryResponse | null> {
    return this.http.get<ApiResponse<MemoryResponse>>(`${this.base}/${familyId}/memory`).pipe(
      map(r => r.data),
      catchError(() => of(null))
    );
  }

  /** Historial ICF de todas las evaluaciones finalizadas — para gráfico de tendencia */
  getIcfHistory(familyId: number): Observable<IcfHistoryPoint[]> {
    return this.http.get<ApiResponse<IcfHistoryPoint[]>>(
      `/api/analytics/family/${familyId}/icf-history`
    ).pipe(
      map(r => r.data),
      catchError(() => of([]))
    );
  }

  /**
   * Última reflexión calculada — read-only, sin side-effects de escritura.
   * Usar para carga del dashboard (banner de riesgo de abandono).
   */
  getLatestReflection(familyId: number): Observable<ReflectionResponse | null> {
    return this.http.get<ApiResponse<ReflectionResponse>>(
      `${this.base}/${familyId}/reflection/latest`
    ).pipe(
      map(r => r.data),
      catchError(() => of(null))
    );
  }

  /**
   * Ejecutar ciclo completo de reflexión autónoma.
   * Persiste lecciones y actualiza narrativa — usar sólo en acciones explícitas.
   */
  triggerReflection(familyId: number): Observable<ReflectionResponse | null> {
    return this.http.post<ApiResponse<ReflectionResponse>>(`${this.base}/${familyId}/reflect`, {}).pipe(
      map(r => r.data),
      catchError(() => of(null))
    );
  }
}
