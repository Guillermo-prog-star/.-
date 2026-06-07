import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

export type RitualType =
  | 'CUMPLEANOS' | 'DOMINGO_FAMILIAR' | 'ANIVERSARIO'
  | 'LOGRO_CELEBRADO' | 'CRISIS_SUPERADA' | 'FIN_DE_MES'
  | 'SIN_ACTIVIDAD' | 'RACHA_POSITIVA' | 'PRIMER_ANO' | 'META_ALCANZADA';

export type RitualStatus = 'PENDING' | 'ACTIVE' | 'COMPLETED' | 'DISMISSED';

export interface RitualDto {
  id: number;
  familyId: number;
  ritualType: RitualType;
  status: RitualStatus;
  title: string;
  description: string | null;
  guidedSteps: string[];
  triggerContext: string | null;
  triggeredAt: string;
  completedAt: string | null;
}

@Injectable({ providedIn: 'root' })
export class RitualService {
  private readonly http = inject(HttpClient);

  getActive(familyId: number): Observable<RitualDto[]> {
    return this.http.get<RitualDto[]>(`/api/families/${familyId}/rituals/active`);
  }

  getHistory(familyId: number): Observable<RitualDto[]> {
    return this.http.get<RitualDto[]>(`/api/families/${familyId}/rituals/history`);
  }

  detect(familyId: number): Observable<{ status: string }> {
    return this.http.post<{ status: string }>(`/api/families/${familyId}/rituals/detect`, {});
  }

  activate(familyId: number, ritualId: number): Observable<RitualDto> {
    return this.http.post<RitualDto>(`/api/families/${familyId}/rituals/${ritualId}/activate`, {});
  }

  complete(familyId: number, ritualId: number): Observable<RitualDto> {
    return this.http.post<RitualDto>(`/api/families/${familyId}/rituals/${ritualId}/complete`, {});
  }

  dismiss(familyId: number, ritualId: number): Observable<void> {
    return this.http.post<void>(`/api/families/${familyId}/rituals/${ritualId}/dismiss`, {});
  }
}
