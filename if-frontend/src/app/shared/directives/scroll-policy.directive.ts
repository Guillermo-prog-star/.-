import {
  Directive, Input, ElementRef, OnInit, OnDestroy,
  AfterContentChecked, inject
} from '@angular/core';

export type ScrollPolicy =
  | 'auto-bottom'       // siempre baja al último elemento (chat, consultor IA)
  | 'scroll-to-new'     // baja solo si el usuario está cerca del final (bitácora, historial)
  | 'preserve-position' // nunca mueve (formularios, diagnóstico, paneles clínicos)
  | 'critical-alert'    // solo mueve ante alerta crítica (dashboard, portal)
  | 'manual';           // sin lógica automática

const NEAR_BOTTOM_THRESHOLD_PX = 120;

@Directive({
  selector: '[scrollPolicy]',
  standalone: true,
})
export class ScrollPolicyDirective implements OnInit, OnDestroy, AfterContentChecked {

  @Input('scrollPolicy') policy: ScrollPolicy = 'manual';

  // Para critical-alert: llamar triggerCriticalScroll() desde el componente
  @Input() criticalAlert = false;

  private el = inject(ElementRef<HTMLElement>);
  private prevScrollHeight = 0;
  private prevCritical = false;

  ngOnInit() {
    this.applyBaseStyles();
    this.prevScrollHeight = this.host.scrollHeight;
  }

  ngAfterContentChecked() {
    const host = this.host;
    const newScrollHeight = host.scrollHeight;
    const changed = newScrollHeight !== this.prevScrollHeight;

    if (changed) {
      switch (this.policy) {
        case 'auto-bottom':
          this.scrollToBottom();
          break;

        case 'scroll-to-new':
          if (this.isNearBottom()) {
            this.scrollToBottom();
          }
          break;

        case 'critical-alert':
          if (this.criticalAlert && !this.prevCritical) {
            this.scrollToBottom();
          }
          break;

        case 'preserve-position':
        case 'manual':
          break;
      }
      this.prevScrollHeight = newScrollHeight;
    }

    this.prevCritical = this.criticalAlert;
  }

  ngOnDestroy() {}

  private get host(): HTMLElement {
    return this.el.nativeElement;
  }

  private applyBaseStyles() {
    const host = this.host;
    if (getComputedStyle(host).overflowY === 'visible') {
      host.style.overflowY = 'auto';
    }
  }

  private isNearBottom(): boolean {
    const host = this.host;
    return host.scrollHeight - host.scrollTop - host.clientHeight <= NEAR_BOTTOM_THRESHOLD_PX;
  }

  private scrollToBottom() {
    this.host.scrollTop = this.host.scrollHeight;
  }
}
