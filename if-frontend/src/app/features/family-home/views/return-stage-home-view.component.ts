import { ChangeDetectionStrategy, Component, EventEmitter, Input, Output } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Command, ReturnStageHomeView } from '../../../core/family-home';
import { FAMILY_HOME_SHARED_STYLES } from '../family-home-shared.styles';

@Component({
  selector: 'app-return-stage-home-view',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [CommonModule],
  styles: [FAMILY_HOME_SHARED_STYLES],
  template: `
    <div class="glass-card">
      <p class="section-title">Diagnóstico completo — {{ view.family.displayName }}</p>
      <p class="fh-narrative">
        Tu familia ya tiene un diagnóstico ICF listo. El primer sprint traduce
        ese diagnóstico en misiones concretas para las próximas semanas.
      </p>

      @if (view.journey.nextCommand; as cmd) {
        <button class="fh-cta" [disabled]="!cmd.enabled || actionPending" (click)="command.emit(cmd)">
          {{ actionPending ? 'Creando sprint…' : cmd.label }}
        </button>
      }
    </div>
  `,
})
export class ReturnStageHomeViewComponent {
  @Input({ required: true }) view!: ReturnStageHomeView;
  @Input() actionPending = false;
  @Output() command = new EventEmitter<Command>();
}
