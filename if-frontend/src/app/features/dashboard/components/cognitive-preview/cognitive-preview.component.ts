import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { CognitiveService } from '../../../../core/services/cognitive.service';
import { FamilyStateService } from '../../../../core/services/family-state.service';
import { CognitiveSnapshot } from '../../../../core/models/cognitive.model';

/**
 * Card de vista previa del sistema cognitivo para el dashboard principal.
 * Muestra: etapa evolutiva, capítulo activo, salud del grafo y botón de acceso.
 */
@Component({
  selector: 'app-cognitive-preview',
  standalone: true,
  imports: [CommonModule, RouterLink],
  template: `
    <div class="glass-card rounded-3xl p-6 border border-white/5
                hover:border-violet-500/20 transition-all group relative overflow-hidden">

      <!-- Fondo decorativo -->
      <div class="absolute -right-8 -top-8 text-[8rem] opacity-[0.03]
                  group-hover:opacity-[0.06] transition-opacity">🧠</div>

      <!-- Header -->
      <div class="flex items-center justify-between mb-5">
        <div class="flex items-center gap-3">
          <div class="w-9 h-9 bg-violet-500/15 rounded-xl flex items-center justify-center text-lg">🧠</div>
          <div>
            <h3 class="font-bold text-white/90 text-sm">Sistema Cognitivo</h3>
            <span class="text-[9px] text-white/30 uppercase tracking-[0.15em]">Memoria · Narrativa · Identidad</span>
          </div>
        </div>
        <a routerLink="/cognitive"
           class="text-[10px] font-black uppercase tracking-widest text-violet-400/60
                  hover:text-violet-400 transition-colors flex items-center gap-1">
          Ver todo <span class="text-xs">→</span>
        </a>
      </div>

      <!-- Loading -->
      @if (loading()) {
      <div class="space-y-3">
        <div class="h-3 bg-white/5 rounded-full w-3/4 animate-pulse"></div>
        <div class="h-3 bg-white/5 rounded-full w-1/2 animate-pulse"></div>
      </div>
      }

      <!-- Datos cognitivos -->
      @if (!loading() && snapshot()) {
        <!-- Etapa + Capítulo -->
        <div class="grid grid-cols-2 gap-3 mb-4">
          <div class="p-3 bg-white/[0.02] rounded-2xl border border-white/5">
            <span class="text-[9px] text-white/25 uppercase tracking-widest block mb-1">Etapa</span>
            <span class="text-xs font-black text-violet-400 uppercase">
              {{ snapshot()!.identityProfile.evolutionStage }}
            </span>
          </div>
          <div class="p-3 bg-white/[0.02] rounded-2xl border border-white/5">
            <span class="text-[9px] text-white/25 uppercase tracking-widest block mb-1">Ciclos</span>
            <span class="text-xs font-black text-white/80">
              {{ snapshot()!.identityProfile.completedCycles }}
            </span>
          </div>
        </div>

        <!-- Capítulo activo -->
        @if (snapshot()!.currentChapter) {
        <div class="mb-4 p-3 bg-indigo-500/5 rounded-2xl border border-indigo-500/10">
          <div class="flex items-center justify-between mb-1">
            <span class="text-[9px] text-indigo-400/60 uppercase tracking-widest">
              Cap. {{ snapshot()!.currentChapter!.chapterNumber }} · {{ snapshot()!.currentChapter!.phase }}
            </span>
            @if (snapshot()!.currentChapter!.turningPoint) {
            <span class="text-[9px] text-amber-400">⚡ Inflexión</span>
            }
          </div>
          <p class="text-xs font-bold text-white/80">{{ snapshot()!.currentChapter!.title }}</p>
        </div>
        }

        <!-- Salud del grafo -->
        <div class="flex items-center justify-between text-[10px]">
          <span class="text-white/30 uppercase tracking-widest">Grafo relacional</span>
          <div class="flex items-center gap-2">
            <span [ngClass]="snapshot()!.graphSummary.healthy ? 'text-emerald-400' : 'text-amber-400'">
              {{ snapshot()!.graphSummary.healthy ? '✓ Saludable' : '⚠ Atención' }}
            </span>
            <span class="text-white/20">·</span>
            <span class="text-white/40">{{ snapshot()?.graphSummary?.totalDyads ?? 0 }} díadas</span>
          </div>
        </div>

        <!-- Turning points badge -->
        @if (snapshot()!.turningPoints > 0) {
        <div class="mt-3 flex items-center gap-2">
          <span class="text-[9px] text-amber-400/60 uppercase tracking-widest">
            ⚡ {{ snapshot()!.turningPoints }} punto(s) de inflexión en la historia
          </span>
        </div>
        }
      }

      <!-- Sin datos -->
      @if (!loading() && !snapshot()) {
      <div class="text-center py-4">
        <p class="text-white/20 text-xs">Sin datos cognitivos aún.</p>
        <a routerLink="/cognitive"
           class="text-violet-400/60 text-[10px] uppercase tracking-widest mt-2 block hover:text-violet-400">
          Inicializar →
        </a>
      </div>
      }
    </div>
  `,
  styles: [`.glass-card { background: rgba(255,255,255,0.02); backdrop-filter: blur(20px); }`]
})
export class CognitivePreviewComponent implements OnInit {
  private readonly cognitiveService = inject(CognitiveService);
  private readonly familyState      = inject(FamilyStateService);

  loading  = signal(true);
  snapshot = signal<CognitiveSnapshot | null>(null);

  ngOnInit(): void {
    const id = this.familyState.getSelectedFamilyId();
    if (!id) { this.loading.set(false); return; }
    this.cognitiveService.getSnapshot(id).subscribe({
      next: s => { this.snapshot.set(s); this.loading.set(false); },
      error: () => this.loading.set(false)
    });
  }
}
