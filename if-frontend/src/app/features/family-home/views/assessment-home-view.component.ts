import { ChangeDetectionStrategy, Component, EventEmitter, Input, Output } from '@angular/core';
import { CommonModule } from '@angular/common';
import { AssessmentHomeView, Command } from '../../../core/family-home';
import { FAMILY_HOME_SHARED_STYLES } from '../family-home-shared.styles';

@Component({
  selector: 'app-assessment-home-view',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [CommonModule],
  styles: [FAMILY_HOME_SHARED_STYLES],
  template: `
    <div class="glass-card">
      <p class="section-title">Evaluación en curso — {{ view.family.displayName }}</p>
      <p class="fh-narrative">
        Cada pregunta que respondes construye el diagnóstico ICF de tu familia.
        Continúa cuando estés lista.
      </p>

      <div class="fh-progress-wrap">
        <div class="fh-progress-bar">
          <div class="fh-progress-fill" [style.width.%]="view.journey.progress.percentage"></div>
        </div>
        <span class="fh-progress-pct">{{ view.journey.progress.percentage }}%</span>
      </div>

      @if (view.journey.nextCommand; as cmd) {
        <button class="fh-cta" [disabled]="!cmd.enabled" (click)="command.emit(cmd)">
          {{ cmd.label }}
        </button>
      }
    </div>
  `,
})
export class AssessmentHomeViewComponent {
  @Input({ required: true }) view!: AssessmentHomeView;
  @Output() command = new EventEmitter<Command>();
}
