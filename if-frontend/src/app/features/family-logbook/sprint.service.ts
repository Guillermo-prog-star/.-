import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { map } from 'rxjs/operators';
import {
  CreateSprintRequest,
  CreateDailyCheckinRequest,
  CloseSprintRequest,
  SprintResponse,
  SprintDailyResponse
} from './sprint.model';

@Injectable({
  providedIn: 'root'
})
export class SprintService {

  private readonly baseUrl = '/api/sprints';

  constructor(private readonly http: HttpClient) {}

  getActiveSprint(familyId: number): Observable<SprintResponse | null> {
    return this.http.get<any>(`${this.baseUrl}/active`, { params: { familyId: familyId.toString() } }).pipe(
      map(res => res.data)
    );
  }

  getSprintHistory(familyId: number): Observable<SprintResponse[]> {
    return this.http.get<any>(`${this.baseUrl}/history`, { params: { familyId: familyId.toString() } }).pipe(
      map(res => res.data || [])
    );
  }

  createSprint(familyId: number, request: CreateSprintRequest): Observable<SprintResponse> {
    return this.http.post<any>(this.baseUrl, request, { params: { familyId: familyId.toString() } }).pipe(
      map(res => res.data)
    );
  }

  toggleMission(sprintId: number, missionId: number): Observable<SprintResponse> {
    return this.http.put<any>(`${this.baseUrl}/${sprintId}/missions/${missionId}/toggle`, {}).pipe(
      map(res => res.data)
    );
  }

  submitDaily(sprintId: number, request: CreateDailyCheckinRequest): Observable<SprintDailyResponse> {
    return this.http.post<any>(`${this.baseUrl}/${sprintId}/dailies`, request).pipe(
      map(res => res.data)
    );
  }

  closeSprint(sprintId: number, request: CloseSprintRequest): Observable<SprintResponse> {
    return this.http.post<any>(`${this.baseUrl}/${sprintId}/retrospective`, request).pipe(
      map(res => res.data)
    );
  }
}
