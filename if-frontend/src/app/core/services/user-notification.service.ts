import { Injectable, signal, computed, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { interval, switchMap, catchError, of, startWith } from 'rxjs';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';

export interface UserNotification {
  id: number;
  type: string;
  title: string;
  message: string;
  viewed: boolean;
  sentAt: string;
}

@Injectable({ providedIn: 'root' })
export class UserNotificationService {
  private http = inject(HttpClient);

  private _notifications = signal<UserNotification[]>([]);

  readonly notifications  = this._notifications.asReadonly();
  readonly unreadCount    = computed(() => this._notifications().filter(n => !n.viewed).length);

  constructor() {
    interval(30000)
      .pipe(
        startWith(0),
        takeUntilDestroyed(),
        switchMap(() =>
          this.http.get<any>('/api/notifications/mine').pipe(catchError(() => of(null)))
        )
      )
      .subscribe(res => {
        if (res?.data) this._notifications.set(res.data);
      });
  }

  markAllRead(): void {
    this.http.put('/api/notifications/mine/mark-all-read', {}).subscribe({
      next: () => {
        this._notifications.update(list => list.map(n => ({ ...n, viewed: true })));
      }
    });
  }

  typeIcon(type: string): string {
    return type === 'EVIDENCE_VALIDATED' ? '✅'
         : type === 'EVIDENCE_REJECTED'  ? '❌'
         : type === 'MILESTONE_UP'        ? '🎉'
         : type === 'PLAN_ASSIGNED'       ? '📋'
         : type === 'CRISIS_ALERT'          ? '🚨'
         : type === 'SPRINT_COMPLETED'      ? '🎖️'
         : type === 'CAPSULA_CREATED'       ? '💾'
         : type === 'MISSION_COMPLETED'     ? '🏆'
         : type === 'DIMENSION_UNLOCKED'    ? '🔓'
         : type === 'TRAJECTORY_DETECTED'   ? '🗺️'
         : type === 'TRAJECTORY_RELAPSED'   ? '⚠️'
         : type === 'TRAJECTORY_RESOLVED'   ? '✅'
         : '🔔';
  }
}
