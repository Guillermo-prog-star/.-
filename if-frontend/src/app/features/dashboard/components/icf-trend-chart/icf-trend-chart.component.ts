import {
  Component, OnInit, inject, signal, computed, ChangeDetectionStrategy
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { CognitiveService } from '../../../../core/services/cognitive.service';
import { FamilyStateService } from '../../../../core/services/family-state.service';
import { IcfHistoryPoint } from '../../../../core/models/cognitive.model';

/**
 * SDD Analytics v2 — Gráfico de tendencia ICF.
 * SVG sparkline puro (sin librerías externas) + badge de riesgo/crisis.
 */
@Component({
  selector: 'app-icf-trend-chart',
  standalone: true,
  imports: [CommonModule],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <div class="glass-card rounded-3xl p-6 border border-white/5
                hover:border-indigo-500/20 transition-all relative overflow-hidden">

      <!-- Fondo decorativo -->
      <div class="absolute -right-6 -top-6 text-[7rem] opacity-[0.03]">📈</div>

      <!-- Header -->
      <div class="flex items-center justify-between mb-5">
        <div class="flex items-center gap-3">
          <div class="w-9 h-9 bg-indigo-500/15 rounded-xl flex items-center justify-center text-lg">📈</div>
          <div>
            <h3 class="font-bold text-white/90 text-sm">Tendencia ICF</h3>
            <span class="text-[9px] text-white/30 uppercase tracking-[0.15em]">Evolución longitudinal</span>
          </div>
        </div>
        <!-- Delta badge -->
        <div *ngIf="deltaIcf() !== null"
             class="text-xs font-black px-2.5 py-1 rounded-full"
             [ngClass]="deltaIcf()! >= 0
               ? 'bg-emerald-500/10 text-emerald-400'
               : 'bg-red-500/10 text-red-400'">
          {{ deltaIcf()! >= 0 ? '+' : '' }}{{ deltaIcf()! | number:'1.1-1' }} pts
        </div>
      </div>

      <!-- Loading -->
      <div *ngIf="loading()" class="h-24 flex items-center justify-center">
        <div class="w-6 h-6 border-2 border-indigo-500/30 border-t-indigo-500 rounded-full animate-spin"></div>
      </div>

      <!-- Sin datos -->
      <div *ngIf="!loading() && points().length === 0"
           class="h-24 flex items-center justify-center">
        <p class="text-white/20 text-xs">Sin evaluaciones finalizadas.</p>
      </div>

      <!-- Gráfico SVG -->
      <ng-container *ngIf="!loading() && points().length > 0">
        <svg [attr.viewBox]="'0 0 ' + W + ' ' + H" class="w-full" [attr.height]="H"
             style="overflow: visible;">

          <!-- Zona de relleno bajo la línea -->
          <defs>
            <linearGradient id="icfGrad" x1="0" y1="0" x2="0" y2="1">
              <stop offset="0%"   stop-color="#6366f1" stop-opacity="0.25"/>
              <stop offset="100%" stop-color="#6366f1" stop-opacity="0"/>
            </linearGradient>
          </defs>
          <polygon [attr.points]="fillPolygon()" fill="url(#icfGrad)"/>

          <!-- Línea principal -->
          <polyline [attr.points]="linePoints()"
                    fill="none" stroke="#6366f1" stroke-width="2"
                    stroke-linejoin="round" stroke-linecap="round"/>

          <!-- Línea de referencia: ICF = 70 (zona verde) -->
          <line [attr.x1]="0" [attr.y1]="yFor(70)"
                [attr.x2]="W" [attr.y2]="yFor(70)"
                stroke="#10b981" stroke-width="1" stroke-dasharray="4 3" opacity="0.4"/>
          <!-- Línea de referencia: ICF = 40 (zona roja) -->
          <line [attr.x1]="0" [attr.y1]="yFor(40)"
                [attr.x2]="W" [attr.y2]="yFor(40)"
                stroke="#ef4444" stroke-width="1" stroke-dasharray="4 3" opacity="0.4"/>

          <!-- Puntos con tooltip-like labels -->
          <g *ngFor="let p of points(); let i = index">
            <!-- Crisis dot: rojo pulsante -->
            <circle *ngIf="p.hasCrisis"
                    [attr.cx]="xFor(i)" [attr.cy]="yFor(p.icf)"
                    r="5" fill="#ef4444" opacity="0.8"/>
            <!-- Normal dot -->
            <circle *ngIf="!p.hasCrisis"
                    [attr.cx]="xFor(i)" [attr.cy]="yFor(p.icf)"
                    r="4" fill="#6366f1" stroke="#312e81" stroke-width="1.5"/>
            <!-- Etiqueta ICF sobre cada punto -->
            <text *ngIf="points().length <= 8 || i === 0 || i === points().length - 1"
                  [attr.x]="xFor(i)" [attr.y]="yFor(p.icf) - 8"
                  text-anchor="middle" font-size="8" fill="rgba(255,255,255,0.5)"
                  font-family="monospace">{{ p.icf | number:'1.0-0' }}</text>
          </g>
        </svg>

        <!-- Leyenda inferior -->
        <div class="flex items-center justify-between mt-3 text-[9px] text-white/25 uppercase tracking-widest">
          <span>Eval. 1</span>
          <div class="flex items-center gap-3">
            <span class="flex items-center gap-1">
              <span class="w-3 h-px bg-emerald-500 inline-block"></span> ICF≥70
            </span>
            <span class="flex items-center gap-1">
              <span class="w-3 h-px bg-red-500 inline-block"></span> ICF≤40
            </span>
            <span class="flex items-center gap-1">
              <span class="w-2 h-2 bg-red-500 rounded-full inline-block"></span> Crisis
            </span>
          </div>
          <span>Eval. {{ points().length }}</span>
        </div>

        <!-- Último valor grande -->
        <div class="mt-4 flex items-end gap-3">
          <div>
            <span class="text-[9px] text-white/25 uppercase tracking-widest block mb-1">ICF Actual</span>
            <span class="text-3xl font-black"
                  [ngClass]="lastIcf() >= 70 ? 'text-emerald-400' :
                              lastIcf() >= 40 ? 'text-amber-400' : 'text-red-400'">
              {{ lastIcf() | number:'1.1-1' }}
            </span>
          </div>
          <div *ngIf="lastCrisis()" class="mb-1">
            <span class="text-[9px] bg-red-500/10 text-red-400 px-2 py-1 rounded-full uppercase tracking-widest">
              ⚠ Crisis activa
            </span>
          </div>
          <div class="ml-auto text-right">
            <span class="text-[9px] text-white/25 uppercase tracking-widest block mb-1">Evaluaciones</span>
            <span class="text-lg font-black text-white/60">{{ points().length }}</span>
          </div>
        </div>
      </ng-container>
    </div>
  `,
  styles: [`.glass-card { background: rgba(255,255,255,0.02); backdrop-filter: blur(20px); }`]
})
export class IcfTrendChartComponent implements OnInit {
  private readonly cognitiveService = inject(CognitiveService);
  private readonly familyState      = inject(FamilyStateService);

  readonly W = 300;
  readonly H = 80;
  readonly PAD_X = 12;
  readonly PAD_Y = 8;

  loading = signal(true);
  points  = signal<IcfHistoryPoint[]>([]);

  readonly lastIcf   = computed(() => this.points().at(-1)?.icf   ?? 0);
  readonly lastCrisis = computed(() => this.points().at(-1)?.hasCrisis ?? false);

  readonly deltaIcf = computed((): number | null => {
    const pts = this.points();
    if (pts.length < 2) return null;
    const delta = pts[pts.length - 1].icf - pts[pts.length - 2].icf;
    return Math.round(delta * 10) / 10;
  });

  ngOnInit(): void {
    const id = this.familyState.getSelectedFamilyId();
    if (!id) { this.loading.set(false); return; }
    this.cognitiveService.getIcfHistory(id).subscribe({
      next: pts => { this.points.set(pts); this.loading.set(false); },
      error: ()  => this.loading.set(false)
    });
  }

  // ─── Helpers de coordenadas SVG ──────────────────────────────────────────

  xFor(index: number): number {
    const n = this.points().length;
    if (n <= 1) return this.W / 2;
    return this.PAD_X + (index / (n - 1)) * (this.W - this.PAD_X * 2);
  }

  yFor(icf: number): number {
    // ICF 0–100 mapeado a [H-PAD_Y, PAD_Y]
    const clamped = Math.max(0, Math.min(100, icf));
    return this.H - this.PAD_Y - (clamped / 100) * (this.H - this.PAD_Y * 2);
  }

  linePoints(): string {
    return this.points()
      .map((p, i) => `${this.xFor(i)},${this.yFor(p.icf)}`)
      .join(' ');
  }

  fillPolygon(): string {
    const pts = this.points();
    if (!pts.length) return '';
    const line = pts.map((p, i) => `${this.xFor(i)},${this.yFor(p.icf)}`).join(' ');
    const base = `${this.xFor(pts.length - 1)},${this.H} ${this.xFor(0)},${this.H}`;
    return `${line} ${base}`;
  }
}
