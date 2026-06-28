import {
  Component, OnInit, OnDestroy, inject,
  ChangeDetectionStrategy, signal, computed
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { HttpClient } from '@angular/common/http';
import { Subject, takeUntil } from 'rxjs';
import { environment } from '../../../environments/environment';
import { SmffDataService } from './services/smff-data.service';
import {
  IndicatorResult, IndicatorsSnapshot,
  SMFF_GROUPS, GROUP_MAP, smffLevel, SmffGroup
} from './models/smff.model';

@Component({
  selector: 'app-smff-panel',
  standalone: true,
  imports: [CommonModule],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
<div class="smff-root">

  @if (loading()) {
    <div class="smff-loading">
      <div class="spinner"></div>
      <span>Calculando indicadores…</span>
    </div>
  } @else if (!familyId) {
    <div class="smff-empty">No tienes una familia registrada.</div>
  } @else if (!snap()) {
    <div class="smff-empty">No se pudieron cargar los indicadores.</div>
  } @else {
    <div class="smff-wrap">

      <!-- ── CABECERA: SCORE SMFF ────────────────────────────────────── -->
      <div class="smff-header">
        <div class="smff-score-ring">
          <svg viewBox="0 0 120 120" class="ring-svg">
            <circle cx="60" cy="60" r="50" fill="none"
                    stroke="rgba(255,255,255,.08)" stroke-width="10"/>
            <circle cx="60" cy="60" r="50" fill="none"
                    [attr.stroke]="smffColor()"
                    stroke-width="10" stroke-linecap="round"
                    stroke-dasharray="314"
                    [attr.stroke-dashoffset]="ringOffset()"
                    transform="rotate(-90 60 60)"/>
            <text x="60" y="55" text-anchor="middle"
                  fill="white" font-size="22" font-weight="700">
              {{ snap()!.smffScore | number:'1.0-0' }}
            </text>
            <text x="60" y="73" text-anchor="middle"
                  [attr.fill]="smffColor()" font-size="9" font-weight="600">
              {{ levelLabel() }}
            </text>
          </svg>
        </div>

        <div class="smff-header-info">
          <h2 class="smff-title">SMFF — Fortalecimiento Familiar</h2>
          <p class="smff-subtitle">
            {{ snap()!.totalReal }} de 20 indicadores con datos reales ·
            {{ snap()!.dataCompletePct | number:'1.0-0' }}% completitud
          </p>

          <!-- barra de completitud -->
          <div class="completitud-track">
            <div class="completitud-fill"
                 [style.width.%]="snap()!.dataCompletePct"
                 [style.background]="smffColor()">
            </div>
          </div>

          <!-- leyenda de grupos -->
          <div class="group-chips">
            @for (g of groups; track g.key) {
              <button class="group-chip"
                      [class.active]="activeGroup() === g.key"
                      [style.--c]="g.color"
                      (click)="setGroup(g.key)">
                {{ g.label }}
                <span class="group-avg">{{ groupAvg(g.key) | number:'1.0-0' }}</span>
              </button>
            }
            <button class="group-chip"
                    [class.active]="activeGroup() === null"
                    style="--c:#c0c4d8"
                    (click)="setGroup(null)">
              Todos
            </button>
          </div>
        </div>
      </div>

      <!-- ── AVISO ESTIMADOS ─────────────────────────────────────────── -->
      @if (snap()!.dataCompletePct < 50) {
        <div class="smff-alert">
          Más de la mitad de los indicadores son estimaciones. Responde los cuestionarios
          y registra misiones para obtener datos reales.
        </div>
      }

      <!-- ── GRID DE INDICADORES ────────────────────────────────────── -->
      <div class="ind-grid">
        @for (r of visibleIndicators(); track r.id) {
          <div class="ind-card" [class.estimated]="r.isEstimated">

            <div class="ind-top">
              <span class="ind-id">{{ r.id }}</span>
              <span class="ind-cls" [attr.data-cls]="r.cls">{{ r.cls }}</span>
              @if (r.isEstimated) {
                <span class="ind-est-badge">estimado</span>
              }
            </div>

            <div class="ind-name">{{ r.name }}</div>

            <div class="ind-value-row">
              <span class="ind-value" [style.color]="colorFor(r)">
                {{ r.value | number:'1.0-0' }}
              </span>
              <span class="ind-raw">
                {{ r.rawValue | number:'1.1-1' }} {{ r.rawUnit }}
              </span>
            </div>

            <!-- barra de progreso -->
            <div class="ind-bar-track">
              <div class="ind-bar-fill"
                   [style.width.%]="r.value"
                   [style.background]="colorFor(r)">
              </div>
            </div>

          </div>
        }
      </div>

      <p class="smff-ts">
        Calculado {{ snap()!.calculatedAt | date:'d MMM yyyy, HH:mm' }}
      </p>
    </div>
  }

</div>
  `,
  styles: [`
    .smff-root {
      font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', sans-serif;
      color: #e8eaf0;
      padding: 16px;
      max-width: 960px;
      margin: 0 auto;
    }

    /* ── loading / empty ─── */
    .smff-loading, .smff-empty {
      display: flex; align-items: center; justify-content: center;
      gap: 10px; min-height: 180px;
      color: #8b8fa8; font-size: 14px;
    }
    .spinner {
      width: 22px; height: 22px; border-radius: 50%;
      border: 3px solid rgba(255,255,255,.12);
      border-top-color: #6b8cff;
      animation: spin .7s linear infinite;
    }
    @keyframes spin { to { transform: rotate(360deg); } }

    /* ── header ─── */
    .smff-header {
      display: flex; gap: 20px; align-items: flex-start;
      margin-bottom: 14px;
      background: rgba(255,255,255,.04);
      border-radius: 10px; padding: 16px;
    }

    .smff-score-ring { width: 110px; flex-shrink: 0; }
    .ring-svg { width: 110px; height: 110px; }

    .smff-header-info { flex: 1; min-width: 0; }
    .smff-title { font-size: 15px; font-weight: 700; margin: 0 0 3px; }
    .smff-subtitle { font-size: 11px; color: #8b8fa8; margin: 0 0 10px; }

    .completitud-track {
      height: 4px; border-radius: 2px;
      background: rgba(255,255,255,.08); overflow: hidden;
      margin-bottom: 10px;
    }
    .completitud-fill { height: 100%; border-radius: 2px; transition: width .4s; }

    /* ── group chips ─── */
    .group-chips { display: flex; flex-wrap: wrap; gap: 5px; }
    .group-chip {
      padding: 3px 9px; border-radius: 14px; font-size: 10px; font-weight: 600;
      cursor: pointer; border: 1.5px solid rgba(255,255,255,.1);
      background: rgba(255,255,255,.04); color: var(--c, #c0c4d8);
      transition: all .15s; display: flex; align-items: center; gap: 5px;
    }
    .group-chip.active {
      background: color-mix(in srgb, var(--c) 15%, transparent);
      border-color: var(--c);
    }
    .group-avg {
      background: rgba(255,255,255,.08); padding: 0 4px;
      border-radius: 4px; font-size: 9px; color: var(--c);
    }

    /* ── alert ─── */
    .smff-alert {
      background: rgba(240, 160, 50, .1); border: 1px solid rgba(240,160,50,.3);
      border-radius: 7px; padding: 9px 12px; font-size: 11px;
      color: #f0c050; margin-bottom: 12px;
    }

    /* ── indicator grid ─── */
    .ind-grid {
      display: grid;
      grid-template-columns: repeat(auto-fill, minmax(200px, 1fr));
      gap: 8px;
    }

    .ind-card {
      background: rgba(255,255,255,.04);
      border-radius: 8px; padding: 10px 12px;
      border: 1px solid rgba(255,255,255,.07);
      transition: border-color .15s;
    }
    .ind-card.estimated { opacity: .7; border-style: dashed; }

    .ind-top {
      display: flex; align-items: center; gap: 5px; margin-bottom: 5px;
    }
    .ind-id { font-size: 9px; font-weight: 700; color: #8b8fa8; }
    .ind-cls {
      font-size: 8px; font-weight: 700; padding: 1px 4px; border-radius: 3px;
      background: rgba(255,255,255,.07); color: #8b8fa8;
    }
    .ind-cls[data-cls="RESULTADO"] { color: #7ecfb3; }
    .ind-cls[data-cls="PROCESO"]   { color: #f5a623; }
    .ind-cls[data-cls="ESTADO"]    { color: #a08ff0; }
    .ind-est-badge {
      font-size: 8px; color: #8b6030; background: rgba(240,160,50,.12);
      padding: 0 4px; border-radius: 3px; margin-left: auto;
    }

    .ind-name { font-size: 11px; font-weight: 600; margin-bottom: 7px; line-height: 1.3; }

    .ind-value-row { display: flex; align-items: baseline; gap: 6px; margin-bottom: 6px; }
    .ind-value { font-size: 22px; font-weight: 800; line-height: 1; }
    .ind-raw { font-size: 9px; color: #8b8fa8; }

    .ind-bar-track {
      height: 3px; border-radius: 2px;
      background: rgba(255,255,255,.08); overflow: hidden;
    }
    .ind-bar-fill { height: 100%; border-radius: 2px; transition: width .4s; }

    /* ── timestamp ─── */
    .smff-ts { font-size: 10px; color: #8b8fa8; margin-top: 10px; text-align: right; }

    @media (max-width: 600px) {
      .smff-header { flex-direction: column; }
      .ind-grid { grid-template-columns: 1fr 1fr; }
    }
  `]
})
export class SmffPanelComponent implements OnInit, OnDestroy {

  private readonly smffService = inject(SmffDataService);
  private readonly http        = inject(HttpClient);
  private readonly destroy$    = new Subject<void>();

  readonly loading = signal(true);
  readonly snap    = signal<IndicatorsSnapshot | null>(null);

  private readonly _activeGroup = signal<SmffGroup | null>(null);
  readonly activeGroup = this._activeGroup.asReadonly();

  readonly groups = SMFF_GROUPS;

  familyId = 0;

  // ── Computed ────────────────────────────────────────────────────────────────

  readonly smffColor = computed(() => {
    const s = this.snap();
    if (!s) return '#6b8cff';
    return smffLevel(s.smffScore).color;
  });

  readonly levelLabel = computed(() => {
    const s = this.snap();
    if (!s) return '';
    return smffLevel(s.smffScore).label;
  });

  readonly ringOffset = computed(() => {
    const s = this.snap();
    if (!s) return 314;
    return 314 - (s.smffScore / 100) * 314;
  });

  readonly visibleIndicators = computed(() => {
    const s = this.snap();
    if (!s) return [];
    const g = this._activeGroup();
    return g ? s.indicators.filter(r => r.group === g) : s.indicators;
  });

  // ── Lifecycle ───────────────────────────────────────────────────────────────

  ngOnInit(): void {
    this.http
      .get<any>(`${environment.apiBaseUrl}/families/mine`)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: res => {
          this.familyId = res?.data?.id ?? res?.id ?? 0;
          if (this.familyId) this.loadSnapshot();
          else this.loading.set(false);
        },
        error: () => this.loading.set(false)
      });
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  // ── Acciones ────────────────────────────────────────────────────────────────

  setGroup(g: SmffGroup | null): void {
    this._activeGroup.set(g);
  }

  groupAvg(key: SmffGroup): number {
    const s = this.snap();
    if (!s) return 0;
    const inds = s.indicators.filter(r => r.group === key);
    if (!inds.length) return 0;
    return inds.reduce((sum, r) => sum + r.value, 0) / inds.length;
  }

  colorFor(r: IndicatorResult): string {
    const g = GROUP_MAP.get(r.group);
    if (!g) return '#6b8cff';
    // oscurece si bajo de 40
    return r.value < 40 ? '#e06b8b' : g.color;
  }

  // ── Privado ─────────────────────────────────────────────────────────────────

  private loadSnapshot(): void {
    this.smffService.getSnapshot(this.familyId)
      .pipe(takeUntil(this.destroy$))
      .subscribe(s => {
        this.snap.set(s);
        this.loading.set(false);
      });
  }
}
