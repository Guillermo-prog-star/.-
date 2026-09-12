import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, catchError, map, of } from 'rxjs';
import { ApiService } from './api.service';

export interface AdvancementEvaluation {
  familyId: number;
  currentMilestone: string;
  canAdvance: boolean;
  timeMet: boolean;
  icfMet: boolean;
  tasksMet: boolean;
  daysElapsed: number;
  minDays: number;
  icfAvg: number;
  icfMin: number;
  completedTasks: number;
  totalTasks: number;
  completionRatio: number;
  taskThreshold: number;
  riskLevel: string;
  terminal: boolean;
}

const MILESTONE_ORDER = ['W1', 'M1', 'M3', 'M6', 'M9', 'M12', 'M18', 'M24', 'M30', 'M36'];

@Injectable({ providedIn: 'root' })
export class MilestoneAdvancementService {
  private readonly http = inject(HttpClient);
  private readonly api = inject(ApiService);

  getStatus(familyId: number): Observable<AdvancementEvaluation | null> {
    return this.http
      .get<{ success: boolean; data: AdvancementEvaluation; message: string }>(
        `${this.api.base}/milestones/family/${familyId}/advancement-status`
      )
      .pipe(
        map(res => res.data),
        catchError(() => of(null))
      );
  }

  /** Próximo hito en la ruta W1→M36, o null si ya está en el terminal (M36). */
  nextMilestone(current: string): string | null {
    const idx = MILESTONE_ORDER.indexOf(current);
    if (idx === -1 || idx >= MILESTONE_ORDER.length - 1) return null;
    return MILESTONE_ORDER[idx + 1];
  }
}
