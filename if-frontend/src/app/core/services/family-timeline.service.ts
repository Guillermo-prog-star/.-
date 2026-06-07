import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

export type EventType =
  | 'EVALUATION'
  | 'GRATITUDE'
  | 'LOGBOOK'
  | 'EVIDENCE'
  | 'CRISIS'
  | 'MISSION'
  | 'DNA'
  | 'MEMBER_JOINED';

export interface TimelineEvent {
  id: number;
  type: EventType;
  title: string;
  description: string | null;
  actor: string | null;
  emotion: string | null;
  metadata: string | null;
  occurredAt: string;
}

@Injectable({ providedIn: 'root' })
export class FamilyTimelineService {
  private readonly http = inject(HttpClient);

  get(familyId: number): Observable<TimelineEvent[]> {
    return this.http.get<TimelineEvent[]>(`/api/families/${familyId}/timeline`);
  }
}
