import { Injectable, signal, effect, inject } from '@angular/core';
import { ScrollPolicy } from './scroll-policy.directive';
import { SentinelCoreService } from '../../core/services/sentinel-core.service';

@Injectable({ providedIn: 'root' })
export class ScrollPolicyService {
  private sentinel = inject(SentinelCoreService);

  readonly policy   = signal<ScrollPolicy>('manual');
  readonly critical = signal(false);

  constructor() {
    // Dispara auto-scroll cuando Sentinel detecta alerta crítica y el módulo activo la escucha
    effect(() => {
      if (this.sentinel.hasCriticalAlert() && this.policy() === 'critical-alert') {
        this.critical.set(true);
        setTimeout(() => this.critical.set(false), 500);
      }
    }, { allowSignalWrites: true });
  }

  set(policy: ScrollPolicy) {
    this.policy.set(policy);
    if (policy !== 'critical-alert') this.critical.set(false);
  }

  reset() { this.set('manual'); }
}
