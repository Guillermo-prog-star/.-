import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { map } from 'rxjs/operators';
import { environment } from '../../../environments/environment';

export type AdjustmentStatus = 'PROPOSED' | 'APPROVED' | 'APPLIED' | 'REJECTED';
export type AdaptiveRuleType = 'REDUCE_LOAD' | 'SOFT_RESET' | 'GUIDED_LISTENING' | 'PAUSE_NON_CRITICAL';

export interface AdaptiveAdjustmentEntity {
  id: string;
  familyId: number;
  ruleType: AdaptiveRuleType;
  reason: string;
  status: AdjustmentStatus;
  createdAt: string;
  approvedAt: string | null;
  appliedAt: string | null;
  approvedBy: string | null;
}

@Injectable({ providedIn: 'root' })
export class AdaptivePlanService {
  private http = inject(HttpClient);
  private base = environment.apiBaseUrl;

  list(familyId: number): Observable<AdaptiveAdjustmentEntity[]> {
    return this.http.get<any>(`${this.base}/families/${familyId}/adaptive/adjustments`)
      .pipe(map(r => r.data ?? r));
  }

  evaluate(familyId: number): Observable<AdaptiveAdjustmentEntity[]> {
    return this.http.post<any>(`${this.base}/families/${familyId}/adaptive/evaluate`, {})
      .pipe(map(r => r.data ?? r));
  }

  approve(id: string, approvedBy: string): Observable<AdaptiveAdjustmentEntity> {
    return this.http.post<any>(
      `${this.base}/adaptive-adjustments/${id}/approve?approvedBy=${encodeURIComponent(approvedBy)}`, {}
    ).pipe(map(r => r.data ?? r));
  }

  apply(id: string): Observable<AdaptiveAdjustmentEntity> {
    return this.http.post<any>(`${this.base}/adaptive-adjustments/${id}/apply`, {})
      .pipe(map(r => r.data ?? r));
  }

  reject(id: string): Observable<AdaptiveAdjustmentEntity> {
    return this.http.post<any>(`${this.base}/adaptive-adjustments/${id}/reject`, {})
      .pipe(map(r => r.data ?? r));
  }
}
