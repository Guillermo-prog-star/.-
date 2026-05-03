import { Injectable, signal } from '@angular/core';

@Injectable({ providedIn: 'root' })
export class SentinelCoreService {
  private criticalAlertActive = signal<boolean>(false);
  hasCriticalAlert(): boolean { return this.criticalAlertActive(); }
  markAllAsViewed(): void { this.criticalAlertActive.set(false); }
  triggerEmergency(): void { this.criticalAlertActive.set(true); }
}
