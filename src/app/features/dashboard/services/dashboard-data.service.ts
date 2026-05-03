import { Injectable, inject, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { BehaviorSubject, Observable, map, tap, catchError, of, forkJoin } from 'rxjs';
import { toSignal } from '@angular/core/rxjs-interop';
import { DashboardDTO } from '../../../core/models/dashboard.model';

@Injectable({ providedIn: 'root' })
export class DashboardDataService {
  private http = inject(HttpClient);
  private dashboardState$ = new BehaviorSubject<DashboardDTO | null>(null);
  public dashboardStateSignal = toSignal(this.dashboardState$);

  getDashboardState: Observable<DashboardDTO | null> {
    return this.dashboardState$.asObservable();
  }

  getRadarData: Observable<any> {
    return this.http.get<any>('/api/analytics/radar').pipe(
      catchError(() => of({ labels: [], datasets: [] }))
    );
  }

  completeTask(taskId: string): Observable<void> {
    return this.http.post<void>('/api/tasks/' + taskId + '/complete', {});
  }

  fetchData(familyId: number = 1): Observable<DashboardDTO | null> {
    return forkJoin({
      dashboard: this.http.get<any>('/api/analytics/dashboard/family/' + familyId),
      advanceStatus: this.http.get<boolean>('/api/milestones/family/' + familyId + '/check-advance')
    }).pipe(
      map(({ dashboard, advanceStatus }) => {
        const rawData = dashboard.data || {};
        return {
          ...rawData,
          readyToAdvance: advanceStatus,
          awarenessGrowth: rawData.awarenessGrowth ?? 0,
          totalEvaluations: rawData.totalEvaluations ?? 0,
          latestGlobalScore: rawData.latestGlobalScore ?? 0,
          pillarProgress: rawData.pillarProgress ?? 0,
          suggestedActions: rawData.suggestedActions ?? []
        } as DashboardDTO;
      }),
      tap(data => this.dashboardState$.next(data)),
      catchError(() => of(null))
    );
  }
}
