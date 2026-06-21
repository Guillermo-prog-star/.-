import { Injectable, inject, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { BehaviorSubject, Observable, map, tap, catchError, of, forkJoin, timeout } from 'rxjs';
import { DashboardDTO } from '../../../core/models/dashboard.model';
import { environment } from '../../../../environments/environment';

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
    const base = environment.apiBaseUrl;
    if (!id) return of(null);
    return forkJoin({
      dashboard: this.http.get<any>(`${base}/analytics/dashboard/family/${id}`).pipe(timeout(8000), catchError(() => of(null))),
      advanceStatus: this.http.get<any>(`${base}/milestones/family/${id}/advancement-status`).pipe(
        timeout(8000),
        map(res => res?.data?.canAdvance ?? false),
        catchError(() => of(false))
      ),
      progress: this.http.get<any>(`${base}/analytics/family/${id}/progress`).pipe(
        timeout(8000),
        map(res => res?.data ?? null),
        catchError(() => of(null))
      )
    }).pipe(
      map(({ dashboard, advanceStatus, progress }) => {
        const rawData = dashboard?.data ?? {};
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
        this.dashboardStateSignal.set(data);
      }),
      catchError(() => {
        // Si falla todo, emitir objeto vacío para que el dashboard cargue sin spinner infinito
        const empty = {} as DashboardDTO;
        this.dashboardState$.next(empty);
        this.dashboardStateSignal.set(empty);
        return of(empty);
      })
    );
  }

  // FIX TS2339: Implementación requerida por evolution-radar.component.ts
  getRadarData$(): Observable<any[]> {
    return this.http.get<any>(`${environment.apiBaseUrl}/analytics/radar`).pipe(
      map(res => res && res.data ? (Array.isArray(res.data) ? res.data : [res.data]) : []),
      catchError(() => of([]))
    );
  }

  completeTask(taskId: string, completed: boolean = true): Observable<void> {
    return this.http.put<void>(`${environment.apiBaseUrl}/plans/tasks/${taskId}/complete`, { completed });
  }
}