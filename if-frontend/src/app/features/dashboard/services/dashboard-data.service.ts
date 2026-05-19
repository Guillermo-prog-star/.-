import { Injectable, inject, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { BehaviorSubject, Observable, map, tap, catchError, of, forkJoin } from 'rxjs';
import { DashboardDTO } from '../../../core/models/dashboard.model';

@Injectable({ providedIn: 'root' })
export class DashboardDataService {
  constructor(private http: HttpClient) {}
  private dashboardState$ = new BehaviorSubject<DashboardDTO | null>(null);
  
  // Señal requerida por ScenariosGridComponent
  public dashboardStateSignal = signal<DashboardDTO | null>(null);

  // FIX TS2339: El componente espera este método específico
  getDashboardState$(): Observable<DashboardDTO | null> {
    return this.dashboardState$.asObservable();
  }

  fetchData(familyId?: number): Observable<DashboardDTO | null> {
    const id = familyId;
    if (!id) return of(null);
    return forkJoin({
      dashboard: this.http.get<any>(`/api/analytics/dashboard/family/${id}`),
      advanceStatus: this.http.get<boolean>(`/api/milestones/family/${id}/check-advance`),
      progress: this.http.get<any>(`/api/analytics/family/${id}/progress`).pipe(
        map(res => res.data),
        catchError(() => of(null))
      )
    }).pipe(
      map(({ dashboard, advanceStatus, progress }) => {
        const rawData = dashboard.data;
        return {
          ...rawData,
          readyToAdvance: advanceStatus,
          awarenessGrowth: rawData.awarenessGrowth ?? rawData.pillarProgress ?? 0,
          totalEvaluations: rawData.totalEvaluations ?? rawData.totalPlanTasks ?? 0,
          progress: progress
        } as DashboardDTO;
      }),
      tap(data => {
        this.dashboardState$.next(data);
        this.dashboardStateSignal.set(data); // Sincronizar señal
      }),
      catchError(() => of(null))
    );
  }

  // FIX TS2339: Implementación requerida por evolution-radar.component.ts
  getRadarData$(): Observable<any[]> {
    return this.http.get<any>('/api/analytics/radar').pipe(
      map(res => res && res.data ? (Array.isArray(res.data) ? res.data : [res.data]) : []),
      catchError(() => of([]))
    );
  }

  // FIX TS2339: Implementación requerida por ai-plan-timeline.component.ts
  completeTask(taskId: string): Observable<void> {
    return this.http.post<void>(`/api/tasks/${taskId}/complete`, {});
  }
}