import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { map } from 'rxjs/operators';
import { environment } from '../../../environments/environment';

export type JourneyStatus = 'COMPLETE' | 'IN_PROGRESS' | 'NEXT' | 'LOCKED';

export interface JourneyLevel {
  level: number;
  name: string;
  description: string;
  status: JourneyStatus;
  statusLabel: string;
  icon: string;
  route: string;
  metric: string | null;
}

export interface SnapshotPoint {
  date: string;
  level: number;
  progress: number;
  levelUp: boolean;
  previousLevel: number | null;
}

export interface JourneyHistoryResponse {
  familyId: number;
  familyName: string;
  points: SnapshotPoint[];
  totalLevelUps: number;
  firstSnapshotDate: string | null;
  lastSnapshotDate: string | null;
}

export interface FamilyJourneyResponse {
  familyId: number;
  familyName: string;
  currentLevel: number;
  journeyProgress: number;
  levels: JourneyLevel[];
  nextAction: string;
  nextLevel: number;
}

@Injectable({ providedIn: 'root' })
export class FamilyJourneyService {
  private readonly http = inject(HttpClient);
  private readonly base = environment.apiUrl;

  getJourney(familyId: number): Observable<FamilyJourneyResponse> {
    return this.http.get<{ data: FamilyJourneyResponse }>(`${this.base}/families/${familyId}/journey`)
      .pipe(map(r => r.data));
  }

  getHistory(familyId: number): Observable<JourneyHistoryResponse> {
    return this.http.get<{ data: JourneyHistoryResponse }>(`${this.base}/families/${familyId}/journey/history`)
      .pipe(map(r => r.data));
  }

  takeSnapshot(familyId: number): Observable<boolean> {
    return this.http.post<{ data: boolean }>(`${this.base}/families/${familyId}/journey/snapshot`, {})
      .pipe(map(r => r.data));
  }
}
