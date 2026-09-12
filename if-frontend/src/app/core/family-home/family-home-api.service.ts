import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Observable } from 'rxjs';
import { ApiService } from '../services/api.service';
import {
  AcceptFirstSprintRequest,
  FAMILY_HOME_CONTRACT_VERSION,
  FamilyActionResult,
  FamilyHomeView,
} from './family-home-view.model';

/**
 * Cliente HTTP del contrato IFRM-D FamilyHomeView (Hitos 4 y 5 del backend).
 * Ningún componente debe llamar HttpClient directamente contra estos
 * endpoints — todo pasa por aquí para mantener encabezados y rutas en un
 * solo lugar.
 */
@Injectable({ providedIn: 'root' })
export class FamilyHomeApiService {
  private readonly http = inject(HttpClient);
  private readonly api = inject(ApiService);

  private familiesUrl(familyId: string): string {
    return `${this.api.base}/v1/families/${familyId}`;
  }

  /** GET /api/v1/families/{familyId}/home */
  getHome(familyId: string, correlationId?: string): Observable<FamilyHomeView> {
    let headers = new HttpHeaders().set(
      'X-Family-Home-Contract-Version',
      FAMILY_HOME_CONTRACT_VERSION
    );
    if (correlationId) {
      headers = headers.set('X-Correlation-Id', correlationId);
    }
    return this.http.get<FamilyHomeView>(`${this.familiesUrl(familyId)}/home`, { headers });
  }

  /** POST /api/v1/families/{familyId}/actions/accept-first-sprint */
  acceptFirstSprint(
    familyId: string,
    request: AcceptFirstSprintRequest,
    idempotencyKey: string
  ): Observable<FamilyActionResult> {
    const headers = new HttpHeaders().set('Idempotency-Key', idempotencyKey);
    return this.http.post<FamilyActionResult>(
      `${this.familiesUrl(familyId)}/actions/accept-first-sprint`,
      request,
      { headers }
    );
  }
}
