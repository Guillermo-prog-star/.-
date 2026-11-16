import { ChangeDetectionStrategy, Component, EventEmitter, Input, Output } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActiveHomeView, Command } from '../../../core/family-home';
import { FAMILY_HOME_SHARED_STYLES } from '../family-home-shared.styles';

const DIMENSION_LABELS: Record<string, string> = {
  emotions: 'Emociones',
  communication: 'Comunicación',
  habits: 'Hábitos',
  sharedTime: 'Tiempos Compartidos',
};

@Component({
  selector: 'app-active-home-view',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [CommonModule],
  styles: [FAMILY_HOME_SHARED_STYLES],
  template: `
    @if (view.today; as today) {
      <div class="glass-card" style="margin-bottom: 20px;">
        <p class="section-title">Hoy — {{ view.family.displayName }}</p>
        <p class="fh-narrative">{{ today.narrativeBlock.text }}</p>
        <button class="fh-cta" [disabled]="!today.primaryCommand.enabled" (click)="command.emit(today.primaryCommand)">
          {{ today.primaryCommand.label }}
        </button>
      </div>
    }

    @if (view.activeSprint; as sprint) {
      <div class="glass-card" style="margin-bottom: 20px;">
        <p class="section-title">Sprint Familiar</p>
        <div class="fh-sprint-block">
          <span class="fh-sprint-title">{{ sprint.title }}</span>
          <div class="fh-progress-wrap" style="margin: 0;">
            <div class="fh-progress-bar">
              <div class="fh-progress-fill" [style.width.%]="sprint.progress.percentage"></div>
            </div>
            <span class="fh-progress-pct">
              {{ sprint.progress.completed }}/{{ sprint.progress.total }}
            </span>
          </div>
        </div>
        @if (sprint.todayMission; as mission) {
          <button class="fh-cta" [disabled]="!mission.enabled" (click)="command.emit(mission)">
            {{ mission.label }}
          </button>
        }
      </div>
    }

    @if (view.resumeBlock; as resume) {
      <div class="glass-card" style="margin-bottom: 20px;">
        <p class="section-title">Retomando el Ritmo Familiar</p>
        <p class="fh-narrative">
          Han pasado unos días de pausa. Cuando estén listos, retomen sus actividades juntos.
        </p>
        <button class="fh-cta" [disabled]="!resume.confirmCommand.enabled" (click)="command.emit(resume.confirmCommand)">
          {{ resume.confirmCommand.label }}
        </button>
      </div>
    }

    <div class="glass-card">
      <p class="section-title">Dimensiones ICF</p>
      <div class="fh-dimensions-grid">
        @for (entry of dimensionEntries(); track entry.key) {
          <div class="fh-dimension-chip">
            <span class="fh-dimension-dot" [class]="entry.value.status.toLowerCase()"></span>
            <span class="fh-dimension-label">{{ dimensionLabel(entry.key) }}</span>
          </div>
        }
      </div>
    </div>
  `,
})
export class ActiveHomeViewComponent {
  @Input({ required: true }) view!: ActiveHomeView;
  @Output() command = new EventEmitter<Command>();

  dimensionEntries(): Array<{ key: string; value: ActiveHomeView['dimensions'][string] }> {
    return Object.entries(this.view.dimensions ?? {}).map(([key, value]) => ({ key, value }));
  }

  dimensionLabel(key: string): string {
    return DIMENSION_LABELS[key] ?? key;
  }
}
