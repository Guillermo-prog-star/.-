import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { map } from 'rxjs/operators';
import { ApiService } from './api.service';

export interface SprintMission {
  id: number;
  description: string;
  status: string;
  completedAt: string | null;
}

export interface SprintDaily {
  id: number;
  memberName: string;
  checkinDate: string;
  yesterdayText: string;
  todayText: string;
  blockagesText: string;
  resolutionText: string;
  emotionalIndicator: string;
  createdAt: string;
}

export interface SprintRetrospective {
  id: number;
  whatWentWell: string;
  whatWasDifficult: string;
  whatLearned: string;
  whatToAdjust: string;
  tensionLevel: number;
  mindfulCompliance: number;
  sharedTime: number;
  positiveInteractions: number;
  emotionalPersistence: number;
  consistencyScore: number;
  aiFeedback: string;
  createdAt: string;
}

export interface Sprint {
  id: number;
  familyId: number;
  objective: string;
  riskDimension: string;
  durationDays: number;
  startDate: string;
  endDate: string;
  status: string;
  missions: SprintMission[];
  dailies: SprintDaily[];
  retrospective: SprintRetrospective | null;
  createdAt: string;
  missionTaskId: number | null;
}

export interface CreateSprintRequest {
  objective: string;
  riskDimension: string;
  durationDays: number;
  missions: string[];
}

export interface CreateDailyRequest {
  yesterdayText: string;
  todayText: string;
  blockagesText: string;
  resolutionText: string;
  emotionalIndicator: string;
  memberName: string;
}

export interface CloseSprintRequest {
  whatWentWell: string;
  whatWasDifficult: string;
  whatLearned: string;
  whatToAdjust: string;
  tensionLevel: number;
  mindfulCompliance: number;
  sharedTime: number;
  positiveInteractions: number;
  emotionalPersistence: number;
}

@Injectable({ providedIn: 'root' })
export class SprintService {
  private http = inject(HttpClient);
  private api  = inject(ApiService);

  private base = () => `${this.api.base}/sprints`;

  getActive(familyId: number): Observable<Sprint | null> {
    return this.http.get<any>(`${this.base()}/active?familyId=${familyId}`)
      .pipe(map(r => r.data ?? null));
  }

  getHistory(familyId: number): Observable<Sprint[]> {
    return this.http.get<any>(`${this.base()}/history?familyId=${familyId}`)
      .pipe(map(r => r.data ?? []));
  }

  create(familyId: number, req: CreateSprintRequest): Observable<Sprint> {
    return this.http.post<any>(`${this.base()}?familyId=${familyId}`, req)
      .pipe(map(r => r.data));
  }

  toggleMission(sprintId: number, missionId: number): Observable<Sprint> {
    return this.http.put<any>(`${this.base()}/${sprintId}/missions/${missionId}/toggle`, {})
      .pipe(map(r => r.data));
  }

  submitDaily(sprintId: number, req: CreateDailyRequest): Observable<SprintDaily> {
    return this.http.post<any>(`${this.base()}/${sprintId}/dailies`, req)
      .pipe(map(r => r.data));
  }

  closeSprint(sprintId: number, req: CloseSprintRequest): Observable<Sprint> {
    return this.http.post<any>(`${this.base()}/${sprintId}/retrospective`, req)
      .pipe(map(r => r.data));
  }
}
