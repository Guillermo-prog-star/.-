import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, of } from 'rxjs';
import { catchError, map } from 'rxjs/operators';
import { LongitudinalStateDTO, CausalInferenceDTO } from '../models/dashboard.model';

/**
 * LongitudinalStateService — Consume los nuevos endpoints del backend:
 *
 *   GET /api/families/{id}/longitudinal-state   → memoria estructural
 *   GET /api/families/{id}/causal-inference      → Motor Inferencial (R1-R7)
 *   GET /api/families/{id}/longitudinal-summary  → resumen compacto para polling
 *
 * Se usa en el Dashboard para alimentar:
 *   - EvolutionPathComponent (fase narrativa, tendencia)
 *   - DashboardDTO con campos longitudinales
 *   - Motor Inferencial display (reglas activas y explicaciones)
 */
@Injectable({ providedIn: 'root' })
export class LongitudinalStateService {
  private readonly http = inject(HttpClient);

  /** Estado longitudinal completo — toda la memoria estructural */
  getLongitudinalState(familyId: number): Observable<LongitudinalStateDTO | null> {
    return this.http.get<any>(`/api/families/${familyId}/longitudinal-state`).pipe(
      map(res => res?.data ?? res),
      catchError(err => {
        console.warn('[LongitudinalState] No disponible aún:', err?.status);
        return of(null);
      })
    );
  }

  /** Inferencia causal — qué reglas están activas y por qué */
  getCausalInference(familyId: number): Observable<CausalInferenceDTO | null> {
    return this.http.get<any>(`/api/families/${familyId}/causal-inference`).pipe(
      map(res => res?.data ?? res),
      catchError(err => {
        console.warn('[CausalEngine] No disponible aún:', err?.status);
        return of(null);
      })
    );
  }

  /** Resumen compacto — para polling liviano en el dashboard */
  getLongitudinalSummary(familyId: number): Observable<Partial<LongitudinalStateDTO> | null> {
    return this.http.get<any>(`/api/families/${familyId}/longitudinal-summary`).pipe(
      map(res => res?.data ?? res),
      catchError(() => of(null))
    );
  }

  /** Fuerza re-inferencia causal completa en el backend */
  triggerCausalInference(familyId: number): Observable<CausalInferenceDTO | null> {
    return this.http.post<any>(`/api/families/${familyId}/causal-inference`, {}).pipe(
      map(res => res?.data ?? res),
      catchError(() => of(null))
    );
  }

  /**
   * Convierte riskTrend del backend al formato del frontend:
   *   IMPROVING → UP
   *   DETERIORATING → DOWN
   *   CRITICAL → DOWN
   *   STABLE → STABLE
   */
  static normalizeTrend(trend: string | undefined): 'UP' | 'DOWN' | 'STABLE' {
    switch (trend) {
      case 'IMPROVING': return 'UP';
      case 'DETERIORATING':
      case 'CRITICAL': return 'DOWN';
      default: return 'STABLE';
    }
  }
}
