import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { map } from 'rxjs/operators';
import {
  GuardianStatusResponse, VoteRequest, ActivateMissionRequest, MissionDto,
  GuardianBriefingResponse, ParticipationPulseResponse
} from '../models/models';
import { environment } from '../../../environments/environment';

interface ApiResponse<T> { data: T; message?: string; success?: boolean; }

@Injectable({ providedIn: 'root' })
export class GuardianService {

  private base = `${environment.apiBaseUrl}/families`;

  constructor(private http: HttpClient) {}

  getStatus(familyId: number, memberId?: number): Observable<GuardianStatusResponse> {
    let params = new HttpParams();
    if (memberId) params = params.set('memberId', memberId);
    return this.http
      .get<ApiResponse<GuardianStatusResponse>>(`${this.base}/${familyId}/guardian`, { params })
      .pipe(map(r => r.data));
  }

  vote(familyId: number, req: VoteRequest): Observable<GuardianStatusResponse> {
    return this.http
      .post<ApiResponse<GuardianStatusResponse>>(`${this.base}/${familyId}/guardian/vote`, req)
      .pipe(map(r => r.data));
  }

  confirmGuardian(familyId: number, memberId: number): Observable<GuardianStatusResponse> {
    const params = new HttpParams().set('memberId', memberId);
    return this.http
      .post<ApiResponse<GuardianStatusResponse>>(`${this.base}/${familyId}/guardian/confirm`, {}, { params })
      .pipe(map(r => r.data));
  }

  activateMission(familyId: number, req: ActivateMissionRequest): Observable<MissionDto> {
    return this.http
      .post<ApiResponse<MissionDto>>(`${this.base}/${familyId}/guardian/missions`, req)
      .pipe(map(r => r.data));
  }

  completeMission(familyId: number, missionId: number, guardianMemberId: number): Observable<MissionDto> {
    const params = new HttpParams().set('guardianMemberId', guardianMemberId);
    return this.http
      .post<ApiResponse<MissionDto>>(`${this.base}/${familyId}/guardian/missions/${missionId}/complete`, {}, { params })
      .pipe(map(r => r.data));
  }

  getMissions(familyId: number): Observable<MissionDto[]> {
    return this.http
      .get<ApiResponse<MissionDto[]>>(`${this.base}/${familyId}/guardian/missions`)
      .pipe(map(r => r.data));
  }

  getBriefing(familyId: number): Observable<GuardianBriefingResponse> {
    return this.http
      .get<ApiResponse<GuardianBriefingResponse>>(`${this.base}/${familyId}/guardian/briefing`)
      .pipe(map(r => r.data));
  }

  generateReengagement(familyId: number, targetMemberId: number): Observable<string> {
    return this.http
      .post<ApiResponse<string>>(`${this.base}/${familyId}/guardian/reengage/${targetMemberId}`, {})
      .pipe(map(r => r.data));
  }

  getParticipationPulse(familyId: number): Observable<ParticipationPulseResponse> {
    return this.http
      .get<ApiResponse<ParticipationPulseResponse>>(`${this.base}/${familyId}/participation/pulse`)
      .pipe(map(r => r.data));
  }
}
