import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, map } from 'rxjs';
import { ApiService } from './api.service';
import { ApiResponse } from '../models/api-response.model';
import { InferenceRecordDto, OperationalStateDto, EmotionalRuleDto, EmotionalRuleRequest, FamilyAlertDto } from '../models/models';

@Injectable({ providedIn: 'root' })
export class ScannerService {
  private readonly http = inject(HttpClient);
  private readonly api  = inject(ApiService);

  getInferences(familyId: number): Observable<InferenceRecordDto[]> {
    return this.http.get<ApiResponse<InferenceRecordDto[]>>(
      `${this.api.base}/scanner/family/${familyId}/inferences`
    ).pipe(map(r => r.data ?? []));
  }

  getOperationalState(familyId: number): Observable<OperationalStateDto> {
    return this.http.get<ApiResponse<OperationalStateDto>>(
      `${this.api.base}/scanner/family/${familyId}/state`
    ).pipe(map(r => r.data!));
  }

  // ── EEDSL Admin ───────────────────────────────────────────────────────────

  getRules(activeOnly = false): Observable<EmotionalRuleDto[]> {
    const params = activeOnly ? '?activeOnly=true' : '';
    return this.http.get<ApiResponse<EmotionalRuleDto[]>>(
      `${this.api.base}/admin/eedsl${params}`
    ).pipe(map(r => r.data ?? []));
  }

  toggleRule(id: number): Observable<EmotionalRuleDto> {
    return this.http.patch<ApiResponse<EmotionalRuleDto>>(
      `${this.api.base}/admin/eedsl/${id}/toggle`, {}
    ).pipe(map(r => r.data!));
  }

  createRule(req: EmotionalRuleRequest): Observable<EmotionalRuleDto> {
    return this.http.post<ApiResponse<EmotionalRuleDto>>(
      `${this.api.base}/admin/eedsl`, req
    ).pipe(map(r => r.data!));
  }

  updateRule(id: number, req: EmotionalRuleRequest): Observable<EmotionalRuleDto> {
    return this.http.put<ApiResponse<EmotionalRuleDto>>(
      `${this.api.base}/admin/eedsl/${id}`, req
    ).pipe(map(r => r.data!));
  }

  // ── IF-ALT Alerts ─────────────────────────────────────────────────────────

  getAlerts(familyId: number, includeResolved = false): Observable<FamilyAlertDto[]> {
    return this.http.get<ApiResponse<FamilyAlertDto[]>>(
      `${this.api.base}/scanner/family/${familyId}/alerts?includeResolved=${includeResolved}`
    ).pipe(map(r => r.data ?? []));
  }

  resolveAlert(familyId: number, alertId: number): Observable<void> {
    return this.http.patch<ApiResponse<void>>(
      `${this.api.base}/scanner/family/${familyId}/alerts/${alertId}/resolve`, {}
    ).pipe(map(() => void 0));
  }
}
