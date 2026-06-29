import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { map } from 'rxjs/operators';
import { environment } from '../../../environments/environment';

export interface FamilyHealthSummary {
  familyId: number;
  familyName: string;
  // ICF
  currentIcf: number | null;
  icfDelta30d: number | null;
  icfLabel: string;
  icfDirection: string;
  // Risk
  riskLevel: string;
  sentinelActive: boolean;
  // Radar
  evolutionPhase: string;
  highSignalCount: number;
  // Journey
  journeyCurrentLevel: number;
  journeyProgress: number;
  journeyNextAction: string;
  // Sprint
  hasActiveSprint: boolean;
  activeSprintStatus: string | null;
  activeSprintId: number | null;
  // Quick stats
  memberCount: number;
  evidenceCount: number;
  totalSprints: number;
  generatedAt: string;
}

@Injectable({ providedIn: 'root' })
export class FamilyHealthSummaryService {
  private readonly http = inject(HttpClient);
  private readonly base = environment.apiUrl;

  getSummary(familyId: number): Observable<FamilyHealthSummary> {
    return this.http
      .get<{ data: FamilyHealthSummary }>(`${this.base}/families/${familyId}/health-summary`)
      .pipe(map(r => r.data));
  }
}
