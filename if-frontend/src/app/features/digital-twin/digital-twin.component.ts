import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import {
  DigitalTwinService, DigitalTwinDto,
  PredictionDto, DetectedPattern, Correlation
} from '../../core/services/digital-twin.service';
import { FamilyStateService } from '../../core/services/family-state.service';
import { catchError, of } from 'rxjs';

const PRED_CFG: Record<string, { icon: string; color: string; bg: string }> = {
  TENSION_RISK:          { icon: '⚠️', color: '#ef4444', bg: 'rgba(239,68,68,0.08)'   },
  GROWTH_OPPORTUNITY:    { icon: '🌱', color: '#22c55e', bg: 'rgba(34,197,94,0.08)'   },
  COMMUNICATION_ALERT:   { icon: '💬', color: '#f97316', bg: 'rgba(249,115,22,0.08)'  },
  RITUAL_READINESS:      { icon: '🕯️', color: '#6366f1', bg: 'rgba(99,102,241,0.08)' },
  EVALUATION_DUE:        { icon: '🎯', color: '#f59e0b', bg: 'rgba(245,158,11,0.08)'  },
};

const RICHNESS_LABELS: Record<string, { label: string; color: string }> = {
  LOW:    { label: 'Datos iniciales',   color: '#94a3b8' },
  MEDIUM: { label: 'Datos moderados',   color: '#f59e0b' },
  HIGH:   { label: 'Datos ricos',       color: '#22c55e' },
  EXPERT: { label: 'Datos expertos',    color: '#6366f1' },
};

@Component({
  selector: 'app-digital-twin',
  standalone: true,
  imports: [CommonModule, RouterLink],
  template: `
    <div class="dt-page">

      <!-- Header -->
      <div class="dt-header">
        <div class="dt-icon">🪞</div>
        <div>
          <h1 class="dt-title">Gemelo Digital Familiar</h1>
          <p class="dt-sub">El sistema aprende cómo opera tu familia y anticipa lo que viene</p>
        </div>
        <button class="btn-compute" (click)="compute()" [disabled]="computing()">
          {{ computing() ? '⏳' : '🔄' }} Actualizar
        </button>
      </div>

      <!-- Cargando -->
      @if (loading()) {
        <div class="dt-loading">
          <div class="dt-mirror"></div>
          <p>El sistema está leyendo la historia de tu familia...</p>
        </div>
      }

      <!-- Sin gemelo -->
      @if (!loading() && !twin() && !computing()) {
        <div class="dt-empty">
          <div class="ei">🪞</div>
          <h2>El Gemelo Digital aún no ha sido inicializado</h2>
          <p>El sistema analizará todos los patrones, correlaciones e historia de tu familia para crear su espejo digital.</p>
          <button class="btn-create" (click)="compute()">✨ Crear Gemelo Digital</button>
        </div>
      }

      <!-- Computando -->
      @if (computing()) {
        <div class="dt-computing">
          <div class="comp-animation">
            <div class="comp-ring comp-ring-1"></div>
            <div class="comp-ring comp-ring-2"></div>
            <div class="comp-ring comp-ring-3"></div>
            <span class="comp-icon">🪞</span>
          </div>
          <div class="comp-title">Analizando el ADN conductual de tu familia...</div>
          <div class="comp-subs">
            <div>Detectando patrones de comportamiento</div>
            <div>Calculando correlaciones causales</div>
            <div>Generando predicciones</div>
            <div>Sintetizando la firma conductual</div>
          </div>
        </div>
      }

      <!-- Gemelo disponible -->
      @if (twin() && !computing()) {
        <div class="dt-content">

          <!-- Riqueza de datos -->
          <div class="richness-bar">
            <span class="rb-label">Calidad del análisis:</span>
            <span class="rb-badge"
                  [style.color]="richnessColor(twin()!.dataRichness)"
                  [style.border-color]="richnessColor(twin()!.dataRichness) + '40'">
              {{ richnessLabel(twin()!.dataRichness) }}
            </span>
            @if (twin()!.dataRichness === 'LOW') {
              <span class="rb-hint">— más actividad enriquecerá el análisis</span>
            }
          </div>

          <!-- Firma conductual -->
          @if (twin()!.behavioralSignature) {
            <div class="signature-card">
              <div class="sc-header">
                <span class="sc-icon">🧬</span>
                <span class="sc-label">Firma conductual única</span>
              </div>
              <p class="sc-text">{{ twin()!.behavioralSignature }}</p>
            </div>
          }

          <!-- Métricas de huella -->
          <div class="metrics-grid">
            <div class="metric-card">
              <div class="mc-icon">💪</div>
              <div class="mc-label">Fortaleza dominante</div>
              <div class="mc-value">{{ twin()!.dominantStrength || '—' }}</div>
            </div>
            <div class="metric-card">
              <div class="mc-icon">🌊</div>
              <div class="mc-label">Vulnerabilidad principal</div>
              <div class="mc-value">{{ twin()!.dominantVulnerability || '—' }}</div>
            </div>
            <div class="metric-card">
              <div class="mc-icon">⚡</div>
              <div class="mc-label">Índice de resiliencia</div>
              <div class="mc-value mc-value--big">{{ twin()!.resilienceIndex.toFixed(0) }}<span class="mc-unit">/100</span></div>
            </div>
            <div class="metric-card">
              <div class="mc-icon">🗓️</div>
              <div class="mc-label">Día más activo</div>
              <div class="mc-value">{{ twin()!.peakActivityDay || '—' }}</div>
            </div>
            @if (twin()!.avgDaysBetweenCrises) {
              <div class="metric-card">
                <div class="mc-icon">🔄</div>
                <div class="mc-label">Días promedio entre crisis</div>
                <div class="mc-value mc-value--big">{{ twin()!.avgDaysBetweenCrises }}<span class="mc-unit">d</span></div>
              </div>
            }
            @if (twin()!.avgRecoveryDays) {
              <div class="metric-card">
                <div class="mc-icon">🌅</div>
                <div class="mc-label">Días de recuperación</div>
                <div class="mc-value mc-value--big">{{ twin()!.avgRecoveryDays }}<span class="mc-unit">d</span></div>
              </div>
            }
          </div>

          <!-- Predicciones activas -->
          @if (twin()!.activePredictions.length) {
            <div class="section">
              <div class="section-title">🔮 Predicciones activas</div>
              <div class="predictions-list">
                @for (pred of twin()!.activePredictions; track pred.id) {
                  <div class="pred-card" [style.background]="predBg(pred.predictionType)"
                                         [style.border-color]="predColor(pred.predictionType) + '40'">
                    <div class="pc-header">
                      <span class="pc-icon">{{ predIcon(pred.predictionType) }}</span>
                      <div class="pc-meta">
                        <div class="pc-title">{{ pred.title }}</div>
                        @if (pred.timeHorizon) {
                          <div class="pc-horizon">{{ pred.timeHorizon }}</div>
                        }
                      </div>
                      <div class="pc-confidence" [style.color]="predColor(pred.predictionType)">
                        {{ pred.confidence }}%
                      </div>
                    </div>
                    @if (pred.description) {
                      <p class="pc-desc">{{ pred.description }}</p>
                    }
                    @if (pred.recommendedAction) {
                      <div class="pc-action">
                        <span class="pca-arrow" [style.color]="predColor(pred.predictionType)">→</span>
                        {{ pred.recommendedAction }}
                      </div>
                    }
                    <!-- Barra de confianza -->
                    <div class="pc-conf-bar">
                      <div class="pcb-fill" [style.width.%]="pred.confidence"
                                            [style.background]="predColor(pred.predictionType)"></div>
                    </div>
                  </div>
                }
              </div>
            </div>
          }

          <!-- Patrones detectados -->
          @if (twin()!.detectedPatterns.length) {
            <div class="section">
              <div class="section-title">🔁 Patrones conductuales detectados</div>
              <div class="patterns-list">
                @for (p of twin()!.detectedPatterns; track p.pattern) {
                  <div class="pattern-item">
                    <div class="pi-header">
                      <span class="pi-conf">{{ p.confidence }}% certeza</span>
                      <div class="pi-bar">
                        <div class="pib-fill" [style.width.%]="p.confidence"></div>
                      </div>
                    </div>
                    <p class="pi-desc">{{ p.description }}</p>
                  </div>
                }
              </div>
            </div>
          }

          <!-- Correlaciones -->
          @if (twin()!.correlations.length) {
            <div class="section">
              <div class="section-title">🔗 Correlaciones identificadas</div>
              @for (c of twin()!.correlations; track c.trigger) {
                <div class="corr-item">
                  <div class="ci-left">
                    <div class="ci-trigger">{{ c.trigger }}</div>
                    <div class="ci-arrow">→ {{ c.lagDays }} días después →</div>
                    <div class="ci-effect">{{ c.effect }}</div>
                  </div>
                  <div class="ci-conf">{{ c.confidence }}%</div>
                </div>
              }
            </div>
          }

          <!-- Sin patrones -->
          @if (!twin()!.detectedPatterns.length && !twin()!.activePredictions.length) {
            <div class="dt-empty dt-empty--sm">
              <div class="ei" style="font-size: 36px">📊</div>
              <p>El gemelo está aprendiendo. Con más actividad registrada aparecerán patrones y predicciones.</p>
              <a routerLink="/evidence/capture" class="btn-activity">📸 Registrar actividad</a>
            </div>
          }

          <!-- Footer -->
          <div class="dt-footer">
            <span class="dt-meta">Actualizado {{ formatDate(twin()!.computedAt) }}</span>
          </div>
        </div>
      }

      @if (error()) {
        <div class="dt-error">⚠️ {{ error() }}</div>
      }

    </div>
  `,
  styles: [`
    .dt-page {
      max-width: 760px; margin: 0 auto;
      padding: 24px 20px 60px;
      font-family: inherit;
      color: var(--if-text-primary, #e0e0e0);
    }

    .dt-header { display: flex; align-items: center; gap: 14px; margin-bottom: 28px; }
    .dt-icon   { font-size: 38px; }
    .dt-title  { font-size: 24px; font-weight: 800; margin: 0 0 4px; }
    .dt-sub    { font-size: 13px; color: var(--if-text-secondary, #888); margin: 0; }
    .btn-compute {
      margin-left: auto; padding: 7px 14px; border-radius: 9px;
      background: rgba(255,255,255,0.06); border: 1px solid rgba(255,255,255,0.1);
      color: var(--if-text-secondary, #aaa); font-size: 12px; font-weight: 600;
      cursor: pointer; transition: all 0.2s; white-space: nowrap;
    }
    .btn-compute:disabled { opacity: 0.5; cursor: not-allowed; }

    /* Loading */
    .dt-loading { text-align: center; padding: 52px 20px; color: var(--if-text-secondary, #888); }
    .dt-mirror {
      width: 60px; height: 80px; border-radius: 8px; margin: 0 auto 16px;
      background: linear-gradient(135deg, rgba(99,102,241,0.2), rgba(139,92,246,0.1));
      border: 1px solid rgba(99,102,241,0.3);
      animation: mirror-pulse 2s ease-in-out infinite;
    }
    @keyframes mirror-pulse { 0%,100%{opacity:0.5} 50%{opacity:1} }

    /* Vacío */
    .dt-empty { text-align: center; padding: 52px 20px; }
    .dt-empty--sm { padding: 28px; }
    .ei { font-size: 48px; margin-bottom: 12px; }
    .dt-empty h2 { font-size: 18px; font-weight: 700; margin: 0 0 10px; }
    .dt-empty p  { font-size: 13px; color: var(--if-text-secondary, #999); max-width: 400px; margin: 0 auto 20px; line-height: 1.6; }
    .btn-create {
      background: linear-gradient(135deg, #6366f1, #4f46e5);
      border: none; color: white; padding: 12px 28px;
      border-radius: 11px; font-size: 14px; font-weight: 700; cursor: pointer;
    }
    .btn-activity {
      display: inline-block; margin-top: 12px;
      background: rgba(99,102,241,0.12); border: 1px solid rgba(99,102,241,0.3);
      color: #a5b4fc; padding: 8px 18px; border-radius: 9px;
      font-size: 13px; font-weight: 600; text-decoration: none;
    }

    /* Computando */
    .dt-computing { text-align: center; padding: 52px 20px; }
    .comp-animation { position: relative; width: 80px; height: 80px; margin: 0 auto 20px; display: flex; align-items: center; justify-content: center; }
    .comp-ring { position: absolute; border-radius: 50%; border: 2px solid rgba(99,102,241,0.3); animation: ring-expand 2s linear infinite; }
    .comp-ring-1 { width: 40px; height: 40px; }
    .comp-ring-2 { width: 60px; height: 60px; animation-delay: 0.4s; }
    .comp-ring-3 { width: 80px; height: 80px; animation-delay: 0.8s; }
    @keyframes ring-expand { 0%{opacity:1;transform:scale(0.8)} 100%{opacity:0;transform:scale(1.2)} }
    .comp-icon { font-size: 30px; z-index: 1; }
    .comp-title { font-size: 16px; font-weight: 700; color: #a5b4fc; margin-bottom: 14px; }
    .comp-subs { display: flex; flex-direction: column; gap: 6px; max-width: 280px; margin: 0 auto; }
    .comp-subs div { font-size: 12px; color: var(--if-text-secondary, #777); padding: 4px 0; border-bottom: 1px solid rgba(255,255,255,0.04); }

    /* Riqueza */
    .richness-bar { display: flex; align-items: center; gap: 10px; margin-bottom: 18px; }
    .rb-label { font-size: 12px; color: var(--if-text-secondary, #888); }
    .rb-badge { font-size: 11px; font-weight: 700; padding: 3px 10px; border-radius: 99px; border: 1px solid; }
    .rb-hint  { font-size: 11px; color: var(--if-text-secondary, #666); font-style: italic; }

    /* Firma conductual */
    .signature-card {
      background: rgba(99,102,241,0.06);
      border: 1px solid rgba(99,102,241,0.2);
      border-radius: 16px; padding: 20px;
      margin-bottom: 20px;
    }
    .sc-header { display: flex; align-items: center; gap: 8px; margin-bottom: 10px; }
    .sc-icon   { font-size: 20px; }
    .sc-label  { font-size: 11px; font-weight: 700; text-transform: uppercase; letter-spacing: 0.08em; color: #a5b4fc; }
    .sc-text   { font-size: 14px; line-height: 1.75; color: var(--if-text-primary, #ddd); margin: 0; font-style: italic; }

    /* Métricas */
    .metrics-grid { display: grid; grid-template-columns: repeat(3, 1fr); gap: 12px; margin-bottom: 24px; }
    @media (max-width: 540px) { .metrics-grid { grid-template-columns: repeat(2, 1fr); } }
    .metric-card {
      background: rgba(255,255,255,0.04);
      border: 1px solid rgba(255,255,255,0.07);
      border-radius: 14px; padding: 16px;
      text-align: center;
    }
    .mc-icon   { font-size: 22px; margin-bottom: 6px; }
    .mc-label  { font-size: 10px; color: var(--if-text-secondary, #888); text-transform: uppercase; letter-spacing: 0.06em; margin-bottom: 6px; }
    .mc-value  { font-size: 14px; font-weight: 700; color: var(--if-text-primary, #ccc); }
    .mc-value--big { font-size: 26px; font-weight: 900; color: #c4b5fd; }
    .mc-unit   { font-size: 13px; font-weight: 400; color: var(--if-text-secondary, #888); }

    /* Secciones */
    .section { margin-bottom: 24px; }
    .section-title { font-size: 14px; font-weight: 700; margin-bottom: 14px; color: var(--if-text-primary, #ddd); }

    /* Predicciones */
    .predictions-list { display: flex; flex-direction: column; gap: 12px; }
    .pred-card {
      border: 1px solid rgba(255,255,255,0.08);
      border-radius: 14px; padding: 16px;
      transition: transform 0.18s;
    }
    .pred-card:hover { transform: translateX(3px); }
    .pc-header { display: flex; align-items: flex-start; gap: 12px; margin-bottom: 10px; }
    .pc-icon   { font-size: 22px; flex-shrink: 0; margin-top: 2px; }
    .pc-meta   { flex: 1; }
    .pc-title  { font-size: 14px; font-weight: 700; margin-bottom: 2px; }
    .pc-horizon{ font-size: 11px; color: var(--if-text-secondary, #888); }
    .pc-confidence { font-size: 20px; font-weight: 900; font-variant-numeric: tabular-nums; flex-shrink: 0; }
    .pc-desc   { font-size: 13px; color: var(--if-text-secondary, #bbb); margin: 0 0 10px; line-height: 1.5; }
    .pc-action { font-size: 13px; color: var(--if-text-secondary, #aaa); display: flex; gap: 8px; margin-bottom: 10px; font-style: italic; }
    .pca-arrow { font-style: normal; font-weight: 700; }
    .pc-conf-bar { height: 3px; border-radius: 99px; background: rgba(255,255,255,0.07); }
    .pcb-fill    { height: 100%; border-radius: 99px; transition: width 0.5s; }

    /* Patrones */
    .patterns-list { display: flex; flex-direction: column; gap: 10px; }
    .pattern-item { background: rgba(255,255,255,0.03); border: 1px solid rgba(255,255,255,0.06); border-radius: 12px; padding: 14px; }
    .pi-header { display: flex; align-items: center; gap: 10px; margin-bottom: 8px; }
    .pi-conf   { font-size: 11px; font-weight: 700; color: #a5b4fc; white-space: nowrap; }
    .pi-bar    { flex: 1; height: 3px; background: rgba(255,255,255,0.07); border-radius: 99px; }
    .pib-fill  { height: 100%; background: #6366f1; border-radius: 99px; }
    .pi-desc   { font-size: 13px; color: var(--if-text-secondary, #bbb); margin: 0; line-height: 1.5; }

    /* Correlaciones */
    .corr-item { display: flex; align-items: center; gap: 14px; padding: 12px 0; border-bottom: 1px solid rgba(255,255,255,0.05); }
    .corr-item:last-child { border-bottom: none; }
    .ci-left   { flex: 1; }
    .ci-trigger{ font-size: 13px; font-weight: 700; color: var(--if-text-primary, #ccc); }
    .ci-arrow  { font-size: 11px; color: var(--if-text-secondary, #888); margin: 2px 0; }
    .ci-effect { font-size: 13px; color: #86efac; }
    .ci-conf   { font-size: 13px; font-weight: 700; color: var(--if-text-secondary, #888); white-space: nowrap; }

    /* Footer */
    .dt-footer { margin-top: 20px; text-align: right; }
    .dt-meta   { font-size: 11px; color: var(--if-text-secondary, #666); }
    .dt-error  { background: rgba(239,68,68,0.1); border: 1px solid rgba(239,68,68,0.3); border-radius: 10px; padding: 12px 16px; font-size: 13px; color: #fca5a5; }
  `]
})
export class DigitalTwinComponent implements OnInit {
  private readonly twinSvc     = inject(DigitalTwinService);
  private readonly familyState = inject(FamilyStateService);

  readonly familyId  = this.familyState.currentFamilyId;
  readonly twin      = signal<DigitalTwinDto | null>(null);
  readonly loading   = signal(false);
  readonly computing = signal(false);
  readonly error     = signal<string | null>(null);

  ngOnInit(): void {
    const id = this.familyId();
    if (!id) return;
    this.load(id);
  }

  private load(id: number): void {
    this.loading.set(true);
    this.twinSvc.get(id).pipe(
      catchError(err => {
        if (err.status === 404) return of(null);
        this.error.set('No se pudo cargar el Gemelo Digital.');
        return of(null);
      })
    ).subscribe(data => {
      this.twin.set(data);
      this.loading.set(false);
    });
  }

  compute(): void {
    const id = this.familyId();
    if (!id) return;
    this.computing.set(true);
    this.error.set(null);
    this.twinSvc.compute(id).pipe(
      catchError(() => {
        this.error.set('Error al actualizar el Gemelo Digital.');
        this.computing.set(false);
        return of(null);
      })
    ).subscribe(data => {
      if (data) this.twin.set(data);
      this.computing.set(false);
    });
  }

  predIcon (type: string): string { return PRED_CFG[type]?.icon  ?? '●'; }
  predColor(type: string): string { return PRED_CFG[type]?.color ?? '#6366f1'; }
  predBg   (type: string): string { return PRED_CFG[type]?.bg    ?? 'rgba(99,102,241,0.08)'; }

  richnessLabel(r: string): string { return RICHNESS_LABELS[r]?.label ?? r; }
  richnessColor(r: string): string { return RICHNESS_LABELS[r]?.color ?? '#888'; }

  formatDate(iso: string): string {
    try {
      return new Date(iso).toLocaleString('es-CO', {
        day: '2-digit', month: 'short', year: 'numeric',
        hour: '2-digit', minute: '2-digit'
      });
    } catch { return iso; }
  }
}
