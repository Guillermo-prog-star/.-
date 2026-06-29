import {
  Component, OnInit, ChangeDetectionStrategy, signal, inject, computed
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, RouterModule } from '@angular/router';
import { FamilyStateService } from '../../core/services/family-state.service';
import { SubtleSignalRadarComponent } from '../dashboard/components/subtle-signal-radar/subtle-signal-radar.component';
import { SubtleSignalRadarService, RadarResponse } from '../../core/services/subtle-signal-radar.service';
import { catchError, of } from 'rxjs';

@Component({
  selector: 'app-radar-page',
  standalone: true,
  imports: [CommonModule, RouterModule, SubtleSignalRadarComponent],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <div class="radar-page">

      <!-- HEADER DE PÁGINA -->
      <div class="rp-header">
        <button class="rp-back" (click)="router.navigate(['/dashboard'])">
          ← Dashboard
        </button>
        <div class="rp-title-wrap">
          <div class="rp-pulse"></div>
          <div>
            <h1 class="rp-title">Radar de Señales Sutiles</h1>
            <p class="rp-subtitle">
              Análisis longitudinal de microseñales · Proyección de escenarios · Narrativa evolutiva IA
            </p>
          </div>
        </div>

        @if (familyId() > 0) {
          <button class="rp-alert-btn" (click)="sendAlert()" [disabled]="sendingAlert()">
            @if (sendingAlert()) {
              <span class="btn-spinner"></span> Verificando…
            } @else {
              🔔 Verificar alerta WhatsApp
            }
          </button>
        }
      </div>

      <!-- ALERT FEEDBACK -->
      @if (alertMsg()) {
        <div class="rp-alert-feedback" [class.sent]="alertSent()">
          {{ alertMsg() }}
        </div>
      }

      <!-- CONTENIDO PRINCIPAL -->
      @if (familyId() > 0) {
        <div class="rp-content">

          <!-- RESUMEN RÁPIDO (cargado por separado del widget) -->
          @if (summary()) {
            <div class="rp-summary-cards">
              <div class="rp-stat-card">
                <div class="rp-stat-value">{{ summary()!.evaluationsAnalyzed }}</div>
                <div class="rp-stat-label">Evaluaciones analizadas</div>
              </div>
              <div class="rp-stat-card">
                <div class="rp-stat-value" [class.has-signals]="highCount() > 0">
                  {{ highCount() }}
                </div>
                <div class="rp-stat-label">Señales alta intensidad</div>
              </div>
              <div class="rp-stat-card">
                <div class="rp-stat-value">{{ summary()!.confidenceScore }}%</div>
                <div class="rp-stat-label">Confianza del análisis</div>
              </div>
              <div class="rp-stat-card">
                <div class="rp-stat-value" [style.color]="phaseColor(summary()!.icfOverall?.evolutionPhase)">
                  {{ summary()!.icfOverall?.evolutionPhase ?? '—' }}
                </div>
                <div class="rp-stat-label">Fase evolutiva</div>
              </div>
            </div>
          }

          <!-- WIDGET COMPLETO DEL RADAR -->
          <app-subtle-signal-radar [familyId]="familyId()"/>

        </div>
      } @else {
        <div class="rp-no-family">
          <span>📡</span>
          <p>Selecciona una familia desde el dashboard para activar el radar.</p>
          <button class="rp-back-btn" (click)="router.navigate(['/dashboard'])">
            Ir al Dashboard
          </button>
        </div>
      }
    </div>
  `,
  styles: [`
    .radar-page {
      min-height: 100vh;
      padding: 1.5rem;
      max-width: 1100px;
      margin: 0 auto;
    }

    /* ── Header ── */
    .rp-header {
      display: flex;
      align-items: center;
      gap: 1rem;
      margin-bottom: 1.5rem;
      flex-wrap: wrap;
    }
    .rp-back {
      background: rgba(255,255,255,0.05);
      border: 1px solid rgba(255,255,255,0.1);
      color: rgba(255,255,255,0.5);
      border-radius: 0.6rem;
      padding: 0.4rem 0.9rem;
      font-size: 0.75rem;
      cursor: pointer;
      white-space: nowrap;
      transition: all 0.2s;
    }
    .rp-back:hover { color: rgba(255,255,255,0.85); border-color: rgba(255,255,255,0.25); }

    .rp-title-wrap {
      display: flex; align-items: center; gap: 0.75rem; flex: 1;
    }
    .rp-pulse {
      width: 10px; height: 10px; border-radius: 50%;
      background: #818cf8; box-shadow: 0 0 10px #818cf8;
      animation: pulse 2s ease-in-out infinite; flex-shrink: 0;
    }
    @keyframes pulse { 0%,100%{opacity:1;transform:scale(1)} 50%{opacity:.5;transform:scale(1.5)} }

    .rp-title {
      font-size: 1.2rem; font-weight: 800; letter-spacing: .06em;
      text-transform: uppercase; color: rgba(255,255,255,.9); margin: 0;
    }
    .rp-subtitle {
      font-size: 0.7rem; color: rgba(255,255,255,.3); margin: 0.2rem 0 0;
      font-weight: 500; letter-spacing: .03em;
    }

    .rp-alert-btn {
      display: flex; align-items: center; gap: 0.4rem;
      background: rgba(129,140,248,0.1);
      border: 1px solid rgba(129,140,248,0.25);
      color: #a5b4fc; border-radius: 0.6rem;
      padding: 0.45rem 1rem; font-size: 0.78rem; font-weight: 700;
      cursor: pointer; transition: all 0.2s; white-space: nowrap;
    }
    .rp-alert-btn:hover:not(:disabled) {
      background: rgba(129,140,248,0.18); border-color: rgba(129,140,248,0.45);
    }
    .rp-alert-btn:disabled { opacity: 0.5; cursor: not-allowed; }

    .btn-spinner {
      display: inline-block; width: 12px; height: 12px;
      border: 2px solid rgba(165,180,252,0.3); border-top-color: #a5b4fc;
      border-radius: 50%; animation: spin 0.8s linear infinite;
    }
    @keyframes spin { to { transform: rotate(360deg); } }

    /* ── Alert feedback ── */
    .rp-alert-feedback {
      margin-bottom: 1rem; padding: 0.75rem 1.25rem;
      border-radius: 0.75rem; font-size: 0.8rem; font-weight: 600;
      background: rgba(251,191,36,0.1); border: 1px solid rgba(251,191,36,0.2);
      color: #fde68a;
    }
    .rp-alert-feedback.sent {
      background: rgba(52,211,153,0.1); border-color: rgba(52,211,153,0.2);
      color: #6ee7b7;
    }

    /* ── Summary cards ── */
    .rp-summary-cards {
      display: grid; grid-template-columns: repeat(4, 1fr);
      gap: 0.75rem; margin-bottom: 1.25rem;
    }
    @media (max-width: 640px) {
      .rp-summary-cards { grid-template-columns: repeat(2, 1fr); }
    }
    .rp-stat-card {
      background: rgba(255,255,255,0.03);
      border: 1px solid rgba(255,255,255,0.07);
      border-radius: 1rem; padding: 1rem 1.25rem;
      text-align: center;
    }
    .rp-stat-value {
      font-size: 1.6rem; font-weight: 900; color: rgba(255,255,255,0.85);
      text-transform: capitalize;
    }
    .rp-stat-value.has-signals { color: #f87171; }
    .rp-stat-label {
      font-size: 0.65rem; color: rgba(255,255,255,0.3);
      font-weight: 600; letter-spacing: .05em; text-transform: uppercase;
      margin-top: 0.25rem;
    }

    /* ── No family ── */
    .rp-no-family {
      display: flex; flex-direction: column; align-items: center;
      gap: 1rem; padding: 5rem 2rem; color: rgba(255,255,255,0.3); text-align: center;
    }
    .rp-no-family span { font-size: 3rem; }
    .rp-back-btn {
      background: rgba(129,140,248,0.12); border: 1px solid rgba(129,140,248,0.3);
      color: #a5b4fc; border-radius: 0.75rem; padding: 0.6rem 1.5rem;
      font-size: 0.85rem; font-weight: 700; cursor: pointer;
    }
  `]
})
export class RadarPageComponent implements OnInit {

  readonly router     = inject(Router);
  readonly familySvc  = inject(FamilyStateService);
  readonly radarSvc   = inject(SubtleSignalRadarService);

  readonly familyId   = this.familySvc.currentFamilyId;
  readonly summary    = signal<RadarResponse | null>(null);
  readonly sendingAlert = signal(false);
  readonly alertMsg   = signal('');
  readonly alertSent  = signal(false);

  readonly highCount = computed(() =>
    (this.summary()?.microSignals ?? []).filter(s => s.severity === 'HIGH').length
  );

  ngOnInit(): void {
    const id = this.familyId();
    if (id > 0) {
      this.radarSvc.getRadar(id).pipe(catchError(() => of(null)))
        .subscribe(r => this.summary.set(r));
    }
  }

  sendAlert(): void {
    const id = this.familyId();
    if (!id) return;
    this.sendingAlert.set(true);
    this.alertMsg.set('');
    this.radarSvc.triggerAlert(id).subscribe({
      next: msg => {
        this.alertMsg.set(msg);
        this.alertSent.set(msg.toLowerCase().includes('enviada'));
        this.sendingAlert.set(false);
      },
      error: () => {
        this.alertMsg.set('Error al verificar la alerta. Intenta de nuevo.');
        this.alertSent.set(false);
        this.sendingAlert.set(false);
      }
    });
  }

  phaseColor(phase: string | null | undefined): string {
    const m: Record<string, string> = {
      pleno: '#34d399', consciente: '#60a5fa',
      reactivo: '#fbbf24', inconsciente: '#f87171'
    };
    return m[phase?.toLowerCase() ?? ''] ?? 'rgba(255,255,255,0.5)';
  }
}
