import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, map } from 'rxjs';
import { ApiService } from './api.service';
import { ApiResponse } from '../models/api-response.model';
import {
  TrajectoryBankResponse,
  TrajectoryBankItem,
  FamilyTrajectoryDto,
  TrajectoryTimelineDto,
  TrajectoryImpactDto,
  TrajectorySuggestion,
  AssignTrajectoryRequest,
  TimelineEventRequest,
  IndicatorRequest,
  RiskMacrodomain,
} from '../models/trajectory.model';

@Injectable({ providedIn: 'root' })
export class TrajectoryService {
  private readonly http = inject(HttpClient);
  private readonly api  = inject(ApiService);

  getBank(): Observable<TrajectoryBankResponse> {
    return this.http.get<ApiResponse<TrajectoryBankResponse>>(
      `${this.api.base}/trajectories/bank`
    ).pipe(map(r => r.data!));
  }

  getBankByMacrodomain(macrodomain: RiskMacrodomain): Observable<TrajectoryBankItem[]> {
    return this.http.get<ApiResponse<TrajectoryBankItem[]>>(
      `${this.api.base}/trajectories/bank/${macrodomain}`
    ).pipe(map(r => r.data ?? []));
  }

  getFamilyTrajectories(familyId: number): Observable<FamilyTrajectoryDto[]> {
    return this.http.get<ApiResponse<FamilyTrajectoryDto[]>>(
      `${this.api.base}/trajectories/family/${familyId}`
    ).pipe(map(r => r.data ?? []));
  }

  assignTrajectory(familyId: number, request: AssignTrajectoryRequest): Observable<FamilyTrajectoryDto> {
    return this.http.post<ApiResponse<FamilyTrajectoryDto>>(
      `${this.api.base}/trajectories/family/${familyId}/assign`,
      request
    ).pipe(map(r => r.data!));
  }

  updateStatus(id: number, status: string, notes?: string): Observable<void> {
    return this.http.patch<ApiResponse<void>>(
      `${this.api.base}/trajectories/family/${id}/status`,
      { status, notes }
    ).pipe(map(() => void 0));
  }

  getTimeline(id: number): Observable<TrajectoryTimelineDto[]> {
    return this.http.get<ApiResponse<TrajectoryTimelineDto[]>>(
      `${this.api.base}/trajectories/family/${id}/timeline`
    ).pipe(map(r => r.data ?? []));
  }

  addTimelineEvent(id: number, event: TimelineEventRequest): Observable<TrajectoryTimelineDto> {
    return this.http.post<ApiResponse<TrajectoryTimelineDto>>(
      `${this.api.base}/trajectories/family/${id}/timeline`,
      event
    ).pipe(map(r => r.data!));
  }

  getImpactSummary(id: number): Observable<TrajectoryImpactDto[]> {
    return this.http.get<ApiResponse<TrajectoryImpactDto[]>>(
      `${this.api.base}/trajectories/family/${id}/impact`
    ).pipe(map(r => r.data ?? []));
  }

  upsertIndicator(id: number, indicator: IndicatorRequest): Observable<TrajectoryImpactDto> {
    return this.http.post<ApiResponse<TrajectoryImpactDto>>(
      `${this.api.base}/trajectories/family/${id}/indicator`,
      indicator
    ).pipe(map(r => r.data!));
  }

  getSuggestions(familyId: number): Observable<TrajectorySuggestion[]> {
    return this.http.get<ApiResponse<TrajectorySuggestion[]>>(
      `${this.api.base}/trajectories/family/${familyId}/suggestions`
    ).pipe(map(r => r.data ?? []));
  }
}
