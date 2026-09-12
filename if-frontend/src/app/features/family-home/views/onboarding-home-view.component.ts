import { ChangeDetectionStrategy, Component, EventEmitter, Input, Output } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Command, OnboardingHomeView } from '../../../core/family-home';
import { FAMILY_HOME_SHARED_STYLES } from '../family-home-shared.styles';

@Component({
  selector: 'app-onboarding-home-view',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [CommonModule],
  styles: [FAMILY_HOME_SHARED_STYLES],
  template: `
    <div class="glass-card">
      <p class="section-title">Bienvenida — {{ view.family.displayName }}</p>
      <p class="fh-narrative">
        Estamos preparando el perfil de tu familia. Completa los siguientes pasos
        para desbloquear tu primera evaluación.
      </p>

      <div class="fh-progress-wrap">
        <div class="fh-progress-bar">
          <div class="fh-progress-fill" [style.width.%]="view.journey.progress.percentage"></div>
        </div>
        <span class="fh-progress-pct">
          {{ view.journey.progress.completed }}/{{ view.journey.progress.total }}
        </span>
      </div>

      @if (view.journey.nextCommand; as cmd) {
        <button class="fh-cta" [disabled]="!cmd.enabled" (click)="command.emit(cmd)">
          {{ cmd.label }}
        </button>
      }
    </div>
  `,
})
export class OnboardingHomeViewComponent {
  @Input({ required: true }) view!: OnboardingHomeView;
  @Output() command = new EventEmitter<Command>();
}
