import {
  Component, OnInit, inject, signal, computed, ChangeDetectionStrategy
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { CognitiveService } from '../../../../core/services/cognitive.service';
import { FamilyStateService } from '../../../../core/services/family-state.service';
import { DimensionHistoryPoint } from '../../../../core/models/cognitive.model';

// ─── Chart configuration ─────────────────────────────────────────────────────
const W = 340, H = 90, PAD_X = 14, PAD_Y = 10;
const MIN_ICF = 0, MAX_ICF = 100;

const DIMENSIONS = [
  { key: 'emociones',    label: 'Emociones',    color: '#fb7185', stroke: '#fb7185' },
  { key: 'comunicacion', label: 'Comunicación',  color: '#818cf8', stroke: '#818cf8' },
  { key: 'habitos',      label: 'Hábitos',       color: '#2dd4bf', stroke: '#2dd4bf' },
  { key: 'tiempos',      label: 'Tiempos',       color: '#fbbf24', stroke: '#fbbf24' }
] as const;

type DimKey = typeof DIMENSIONS[number]['key'];

interface PlottedLine {
  key:    DimKey;
  label:  string;
  color:  string;
  stroke: string;
  points: { x: number; y: number; value: number }[];
  polyline: string;
  last: number | null;
  lastX: number;
  lastY: number;
}

/**
 * SDD Analytics v2 — Gráfico de Evolución por Dimensiones.
 * Muestra 4 líneas SVG (Emociones, Comunicación, Hábitos, Tiempos) sobre el
 * historial de evaluaciones finalizadas. Pure SVG, sin librerías externas.
 */
@Component({
  selector: 'app-dimension-history-chart',
  standalone: true,
  imports: [CommonModule],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <div class="glass-premium p-6 rounded-[2rem] border border-white/5
                animate-in fade-in slide-in-from-bottom-4 duration-700 delay-600">

      <!-- Header -->
      <div class="flex items-center justify-between mb-4">
        <div>
          <h3 class="font-bold text-white/90 text-sm">Evolución por Dimensiones</h3>
          <p class="text-[9px] text-white/30 uppercase tracking-[0.15em] mt-0.5">
            Puntuaciones históricas · Emociones · Comunicación · Hábitos · Tiempos
          </p>
        </div>
        <span class="text-[9px] font-black uppercase tracking-widest px-2.5 py-1
                     rounded-full bg-white/5 text-white/30 border border-white/5">
          {{ points().length }} eval{{ points().length !== 1 ? 's' : '' }}
        </span>
      </div>

      <!-- Loading -->
      @if (loading()) {
      <div class="h-28 flex items-center justify-center">
        <div class="w-5 h-5 rounded-full border-2 border-indigo-500/30 border-t-indigo-500 animate-spin"></div>
      </div>
      }

      <!-- No data -->
      @if (!loading() && points().length === 0) {
      <div class="h-28 flex flex-col items-center justify-center gap-2 text-white/20">
        <span class="text-2xl">📊</span>
        <span class="text-[10px] uppercase tracking-widest">Sin evaluaciones todavía</span>
      </div>
      }

      <!-- SVG chart -->
      @if (!loading() && points().length > 0) {
      <div class="relative">
        <svg [attr.viewBox]="'0 0 ' + W + ' ' + H"
             class="w-full h-auto overflow-visible">

          <!-- Reference line at 70 -->
          <line [attr.x1]="PAD_X" [attr.y1]="refY(70)"
                [attr.x2]="W - PAD_X" [attr.y2]="refY(70)"
                stroke="#10b981" stroke-width="0.5" stroke-dasharray="3 3" opacity="0.3"/>
          <text [attr.x]="W - PAD_X + 2" [attr.y]="refY(70) + 3"
                fill="#10b981" font-size="5" opacity="0.5">70</text>

          <!-- Lines for each dimension -->
          @for (line of lines(); track line.key) {
            <polyline [attr.points]="line.polyline"
                      [attr.stroke]="line.stroke"
                      stroke-width="1.5"
                      fill="none"
                      stroke-linecap="round"
                      stroke-linejoin="round"
                      opacity="0.8"/>

            @if (line.last !== null) {
            <!-- End-of-line label -->
            <text [attr.x]="line.lastX + 4"
                  [attr.y]="line.lastY + 3"
                  [attr.fill]="line.color"
                  font-size="5.5" font-weight="bold" opacity="0.9">
              {{ line.last }}
            </text>
            <!-- Dot on last point -->
            <circle [attr.cx]="line.lastX"
                    [attr.cy]="line.lastY"
                    r="2.5"
                    [attr.fill]="line.color"
                    stroke="#0f172a" stroke-width="1"/>
            }
          }

        </svg>

        <!-- Legend -->
        <div class="flex items-center justify-center flex-wrap gap-x-4 gap-y-1.5 mt-3">
          @for (dim of DIMENSIONS; track dim.key) {
          <div class="flex items-center gap-1.5">
            <span class="w-3 h-0.5 rounded-full inline-block"
                  [style.background]="dim.color"></span>
            <span class="text-[9px] font-bold text-white/40 uppercase tracking-wider">
              {{ dim.label }}
            </span>
          </div>
          }
        </div>
      </div>
      }

      <!-- Last values row -->
      @if (!loading() && lastValues(); as lv) {
      <div class="mt-4 grid grid-cols-4 gap-2">
        @for (dim of DIMENSIONS; track dim.key) {
        <div class="text-center p-2 bg-white/[0.02] rounded-xl border border-white/5">
          <span class="text-[8px] uppercase tracking-widest block mb-0.5"
                [style.color]="dim.color" style="opacity:0.7">{{ dim.label }}</span>
          <span class="text-sm font-black text-white/80">
            {{ lv[dim.key] !== undefined ? lv[dim.key] : '—' }}
          </span>
        </div>
        }
      </div>
      }
    </div>
  `
})
export class DimensionHistoryChartComponent implements OnInit {
  private readonly cognitiveService = inject(CognitiveService);
  private readonly familyState      = inject(FamilyStateService);

  readonly W = W;
  readonly H = H;
  readonly PAD_X = PAD_X;
  readonly DIMENSIONS = DIMENSIONS;

  loading = signal(true);
  points  = signal<DimensionHistoryPoint[]>([]);

  readonly lines = computed<PlottedLine[]>(() => {
    const pts = this.points();
    if (pts.length === 0) return [];

    const xStep  = pts.length > 1 ? (W - PAD_X * 2) / (pts.length - 1) : 0;
    const yRange = MAX_ICF - MIN_ICF;

    return DIMENSIONS.map(dim => {
      const plotted = pts.map((p, i) => {
        const val = p.dimensions[dim.key];
        const x   = PAD_X + i * xStep;
        const y   = val !== undefined
          ? PAD_Y + (1 - (val - MIN_ICF) / yRange) * (H - PAD_Y * 2)
          : -1;
        return { x, y, value: val ?? -1 };
      }).filter(p => p.value >= 0);

      const polyline = plotted.map(p => `${p.x},${p.y}`).join(' ');
      const last = plotted.length > 0 ? plotted[plotted.length - 1] : null;

      return {
        key:      dim.key,
        label:    dim.label,
        color:    dim.color,
        stroke:   dim.stroke,
        points:   plotted,
        polyline,
        last:     last ? Math.round(last.value * 10) / 10 : null,
        lastX:    last?.x ?? 0,
        lastY:    last?.y ?? 0
      };
    });
  });

  readonly lastValues = computed(() => {
    const pts = this.points();
    if (pts.length === 0) return null;
    return pts[pts.length - 1].dimensions;
  });

  ngOnInit(): void {
    const id = this.familyState.getSelectedFamilyId();
    if (!id) { this.loading.set(false); return; }

    this.cognitiveService.getDimensionHistory(id).subscribe({
      next: pts => { this.points.set(pts); this.loading.set(false); },
      error: ()  => this.loading.set(false)
    });
  }

  refY(value: number): number {
    return PAD_Y + (1 - (value - MIN_ICF) / (MAX_ICF - MIN_ICF)) * (H - PAD_Y * 2);
  }
}
