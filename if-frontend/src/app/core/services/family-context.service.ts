import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

export interface FamilyContextDto {
  familyId: number;
  familyName: string;
  connectionLevel: 'ALTA' | 'MEDIA' | 'BAJA';
  stressLevel: 'BAJO' | 'MODERADO' | 'ALTO' | 'CRITICO';
  communicationTrend: 'MEJORANDO' | 'ESTABLE' | 'DETERIORANDO';
  participationLevel: 'ALTA' | 'MEDIA' | 'BAJA';
  overallTrend: 'ASCENDENTE' | 'ESTABLE' | 'DESCENDENTE' | 'CRITICA';
  overallMood: 'CELEBRANDO' | 'CRECIENDO' | 'SERENO' | 'TENSO' | 'EN_CRISIS';
  icfCurrent: number | null;
  riskLevel: string | null;
  daysWithoutActivity: number;
  currentStreak: number;
  activeRitualsCount: number;
  sprintProgress: number | null;
  alerts: string[];
  recommendations: string[];
  computedAt: string;
  fresh: boolean;
}

@Injectable({ providedIn: 'root' })
export class FamilyContextService {
  private readonly http = inject(HttpClient);

  get(familyId: number): Observable<FamilyContextDto> {
    return this.http.get<FamilyContextDto>(`/api/families/${familyId}/context`);
  }

  refresh(familyId: number): Observable<FamilyContextDto> {
    return this.http.post<FamilyContextDto>(`/api/families/${familyId}/context/refresh`, {});
  }
}
