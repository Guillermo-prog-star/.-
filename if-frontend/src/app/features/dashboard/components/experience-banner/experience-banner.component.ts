import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { ExperienceEngineService } from '../../../../core/services/experience-engine.service';

@Component({
  selector: 'app-experience-banner',
  standalone: true,
  imports: [CommonModule, RouterLink],
  template: `
    @if (exp.profile().showBanner && !dismissed()) {
      <div
        class="exp-banner"
        [style.border-color]="exp.profile().accentBorder"
        [style.background]="'linear-gradient(135deg, ' + exp.profile().accentFaint + ', transparent)'"
      >
        <!-- Línea de acento superior -->
        <div class="exp-line" [style.background]="exp.profile().accentColor"></div>

        <!-- Contenido principal -->
        <div class="eb-body">
          <div class="eb-icon" [style.filter]="'drop-shadow(0 0 8px ' + exp.profile().accentColor + '40)'">
            {{ exp.profile().icon }}
          </div>

          <div class="eb-content">
            <div class="eb-headline" [style.color]="exp.profile().accentColor">
              {{ exp.profile().headline }}
            </div>
            <div class="eb-subline">{{ exp.profile().subline }}</div>

            <!-- Chips de estado -->
            <div class="eb-chips">
              @if (exp.profile().streakMessage) {
                <span class="eb-chip" [style.border-color]="exp.profile().accentBorder"
                                      [style.color]="exp.profile().accentColor">
                  {{ exp.profile().streakMessage }}
                </span>
              }
              @if (exp.profile().alertCount > 0) {
                <a routerLink="/family-pulse" class="eb-chip eb-chip--alert">
                  ⚠️ {{ exp.profile().alertCount }} señal{{ exp.profile().alertCount > 1 ? 'es' : '' }} de atención
                </a>
              }
            </div>

            <!-- Recomendación principal -->
            @if (exp.profile().recommendations.length > 0) {
              <div class="eb-rec">
                <span class="eb-rec-arrow" [style.color]="exp.profile().accentColor">→</span>
                {{ exp.profile().recommendations[0] }}
              </div>
            }
          </div>

          <!-- Acciones -->
          <div class="eb-actions">
            @if (exp.profile().mood === 'EN_CRISIS') {
              <a routerLink="/crisis" class="eb-action-btn eb-action-btn--crisis">
                Ir a Crisis
              </a>
            } @else if (exp.profile().mood === 'CELEBRANDO' || exp.profile().mood === 'CRECIENDO') {
              <a routerLink="/family-pulse" class="eb-action-btn"
                 [style.border-color]="exp.profile().accentBorder"
                 [style.color]="exp.profile().accentColor">
                Ver pulso
              </a>
            } @else {
              <a routerLink="/family-pulse" class="eb-action-btn"
                 [style.border-color]="exp.profile().accentBorder"
                 [style.color]="exp.profile().accentColor">
                Ver estado
              </a>
            }
            <button class="eb-dismiss" (click)="dismiss()" title="Cerrar">✕</button>
          </div>
        </div>
      </div>
    }
  `,
  styles: [`
    .exp-banner {
      position: relative;
      border: 1px solid rgba(255,255,255,0.08);
      border-radius: 16px;
      margin-bottom: 24px;
      overflow: hidden;
      transition: all 0.4s ease;
      animation: bannerIn 0.5s ease-out;
    }
    @keyframes bannerIn {
      from { opacity: 0; transform: translateY(-8px); }
      to   { opacity: 1; transform: translateY(0); }
    }

    .exp-line {
      position: absolute;
      top: 0; left: 0; right: 0;
      height: 2px;
      opacity: 0.7;
    }

    .eb-body {
      display: flex;
      align-items: flex-start;
      gap: 16px;
      padding: 20px 20px 20px 24px;
    }

    .eb-icon { font-size: 36px; flex-shrink: 0; margin-top: 2px; }

    .eb-content { flex: 1; min-width: 0; }

    .eb-headline {
      font-size: 16px; font-weight: 800;
      margin-bottom: 3px; line-height: 1.3;
    }

    .eb-subline {
      font-size: 13px;
      color: rgba(255,255,255,0.55);
      margin-bottom: 10px;
      line-height: 1.4;
    }

    .eb-chips {
      display: flex; flex-wrap: wrap; gap: 6px;
      margin-bottom: 10px;
    }

    .eb-chip {
      font-size: 11px; font-weight: 700;
      padding: 3px 10px; border-radius: 99px;
      border: 1px solid rgba(255,255,255,0.15);
      background: rgba(255,255,255,0.05);
      color: rgba(255,255,255,0.7);
      text-decoration: none;
      cursor: default;
    }
    .eb-chip--alert {
      color: #fca5a5;
      border-color: rgba(239,68,68,0.3);
      background: rgba(239,68,68,0.08);
      cursor: pointer;
    }
    .eb-chip--alert:hover { background: rgba(239,68,68,0.14); }

    .eb-rec {
      font-size: 13px;
      color: rgba(255,255,255,0.5);
      display: flex; gap: 6px; align-items: flex-start;
      font-style: italic;
    }
    .eb-rec-arrow { font-style: normal; font-weight: 700; flex-shrink: 0; }

    /* Acciones */
    .eb-actions {
      display: flex; flex-direction: column;
      align-items: flex-end; gap: 8px;
      flex-shrink: 0;
    }

    .eb-action-btn {
      font-size: 12px; font-weight: 700;
      padding: 6px 14px; border-radius: 8px;
      border: 1px solid rgba(255,255,255,0.15);
      color: rgba(255,255,255,0.7);
      text-decoration: none; white-space: nowrap;
      background: rgba(255,255,255,0.05);
      transition: all 0.18s; cursor: pointer;
    }
    .eb-action-btn:hover { background: rgba(255,255,255,0.1); }

    .eb-action-btn--crisis {
      background: rgba(239,68,68,0.12);
      border-color: rgba(239,68,68,0.3);
      color: #fca5a5;
    }
    .eb-action-btn--crisis:hover { background: rgba(239,68,68,0.2); }

    .eb-dismiss {
      background: transparent; border: none;
      color: rgba(255,255,255,0.25); font-size: 13px;
      cursor: pointer; padding: 2px 6px;
      border-radius: 6px; transition: all 0.15s;
    }
    .eb-dismiss:hover { color: rgba(255,255,255,0.6); background: rgba(255,255,255,0.06); }
  `]
})
export class ExperienceBannerComponent implements OnInit {
  readonly exp = inject(ExperienceEngineService);
  readonly dismissed = signal(false);

  ngOnInit(): void {
    this.exp.load();
  }

  dismiss(): void {
    this.dismissed.set(true);
  }
}
