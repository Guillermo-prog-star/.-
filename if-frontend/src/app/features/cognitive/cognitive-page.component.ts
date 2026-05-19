import { Component, OnInit, inject, signal, computed } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { forkJoin } from 'rxjs';
import { CognitiveService } from '../../core/services/cognitive.service';
import { FamilyStateService } from '../../core/services/family-state.service';
import {
  NarrativeResponse, GraphResponse, ReflectionResponse,
  NarrativeChapter, DyadDto, NarrativePhase,
  EffectivenessLevel, AbandonmentLevel
} from '../../core/models/cognitive.model';

@Component({
  selector: 'app-cognitive-page',
  standalone: true,
  imports: [CommonModule, RouterLink],
  template: `
    <div class="min-h-screen p-6 lg:p-12 space-y-10">

      <!-- HEADER -->
      <header class="flex flex-col md:flex-row justify-between items-start md:items-center gap-4">
        <div>
          <h1 class="text-4xl md:text-5xl font-bold bg-clip-text text-transparent
                     bg-gradient-to-r from-violet-400 via-indigo-300 to-white tracking-tight">
            Sistema Cognitivo
          </h1>
          <p class="text-white/40 font-medium tracking-widest uppercase text-[10px] mt-2">
            Memoria · Narrativa · Identidad · Grafo Relacional
          </p>
        </div>
        <a routerLink="/dashboard"
           class="text-white/30 hover:text-white/60 text-xs uppercase tracking-widest
                  flex items-center gap-2 transition-colors">
          ← Volver al Panóptico
        </a>
      </header>

      <!-- LOADING -->
      <div *ngIf="loading()" class="flex items-center justify-center py-24">
        <div class="flex flex-col items-center gap-4 text-white/30">
          <div class="w-12 h-12 rounded-full border-2 border-violet-500/40
                      border-t-violet-500 animate-spin"></div>
          <span class="text-xs uppercase tracking-widest">Cargando sistema cognitivo...</span>
        </div>
      </div>

      <ng-container *ngIf="!loading()">

        <!-- ── SECCIÓN 1: IDENTIDAD ───────────────────────────────────────── -->
        <section *ngIf="narrative()" class="glass-card border-l-4 border-violet-500 p-8 rounded-3xl">
          <div class="flex items-center gap-3 mb-6">
            <div class="w-10 h-10 bg-violet-500/20 rounded-xl flex items-center justify-center text-xl">🪪</div>
            <div>
              <h2 class="font-bold text-white/90 text-lg">Perfil de Identidad</h2>
              <span class="text-[9px] text-white/30 uppercase tracking-[0.2em]">Etapa Evolutiva · Estilo · Adaptabilidad</span>
            </div>
          </div>

          <div class="grid grid-cols-1 md:grid-cols-3 gap-6">
            <!-- Etapa evolutiva -->
            <div class="bg-white/[0.03] rounded-2xl p-5 border border-white/5">
              <span class="text-[9px] text-white/30 uppercase tracking-widest block mb-3">Etapa Evolutiva</span>
              <div class="flex items-center gap-3">
                <span class="text-3xl">{{ stageEmoji }}</span>
                <div>
                  <p class="font-black text-white/90 uppercase tracking-wide text-sm">{{ snapshot()?.identityProfile?.evolutionStage }}</p>
                  <p class="text-white/40 text-xs">{{ stageName }}</p>
                </div>
              </div>
              <div class="mt-4 text-[10px] text-white/40 uppercase tracking-widest">
                {{ snapshot()?.identityProfile?.completedCycles ?? 0 }} ciclos completados
              </div>
            </div>

            <!-- Estilos -->
            <div class="bg-white/[0.03] rounded-2xl p-5 border border-white/5 space-y-3">
              <span class="text-[9px] text-white/30 uppercase tracking-widest block">Estilos Familiares</span>
              <div class="flex justify-between items-center">
                <span class="text-xs text-white/50">Comunicación</span>
                <span class="text-xs font-bold text-indigo-400 uppercase">
                  {{ snapshot()?.identityProfile?.communicationStyle ?? '—' }}
                </span>
              </div>
              <div class="flex justify-between items-center">
                <span class="text-xs text-white/50">Conflicto</span>
                <span class="text-xs font-bold text-violet-400 uppercase">
                  {{ snapshot()?.identityProfile?.conflictStyle ?? '—' }}
                </span>
              </div>
              <div class="flex justify-between items-center">
                <span class="text-xs text-white/50">Expresión emocional</span>
                <span class="text-xs font-bold text-pink-400 uppercase">
                  {{ snapshot()?.identityProfile?.emotionalExpression ?? '—' }}
                </span>
              </div>
            </div>

            <!-- Adaptabilidad -->
            <div class="bg-white/[0.03] rounded-2xl p-5 border border-white/5">
              <span class="text-[9px] text-white/30 uppercase tracking-widest block mb-3">Índice de Adaptabilidad</span>
              <div class="text-5xl font-black text-transparent bg-clip-text bg-gradient-to-r from-violet-400 to-indigo-300">
                {{ adaptabilityPct }}%
              </div>
              <div class="mt-3 h-1.5 bg-white/10 rounded-full overflow-hidden">
                <div class="h-full bg-gradient-to-r from-violet-500 to-indigo-500 rounded-full transition-all duration-1000"
                     [style.width.%]="snapshot()?.identityProfile?.adaptabilityIndex ? snapshot()!.identityProfile.adaptabilityIndex * 100 : 0">
                </div>
              </div>
            </div>
          </div>

          <!-- Narrativa identitaria -->
          <div *ngIf="snapshot()?.identityProfile?.identityNarrative"
               class="mt-6 p-5 bg-violet-500/5 rounded-2xl border border-violet-500/10">
            <span class="text-[9px] text-violet-400/60 uppercase tracking-widest block mb-2">Narrativa Identitaria</span>
            <p class="text-white/70 text-sm leading-relaxed">{{ snapshot()!.identityProfile.identityNarrative }}</p>
          </div>
        </section>

        <!-- ── SECCIÓN 2: NARRATIVA ───────────────────────────────────────── -->
        <section *ngIf="narrative()" class="glass-card border-l-4 border-indigo-500 p-8 rounded-3xl">
          <div class="flex items-center justify-between gap-3 mb-6">
            <div class="flex items-center gap-3">
              <div class="w-10 h-10 bg-indigo-500/20 rounded-xl flex items-center justify-center text-xl">📖</div>
              <div>
                <h2 class="font-bold text-white/90 text-lg">Historia Evolutiva</h2>
                <span class="text-[9px] text-white/30 uppercase tracking-[0.2em]">
                  {{ narrative()?.totalChapters }} capítulos ·
                  {{ narrative()?.turningPoints }} puntos de inflexión
                </span>
              </div>
            </div>
            <span class="px-3 py-1 text-[10px] font-black uppercase tracking-widest rounded-full
                         bg-indigo-500/10 text-indigo-400 border border-indigo-500/20">
              {{ narrative()?.currentPhase }}
            </span>
          </div>

          <!-- Story arc summary -->
          <p class="text-white/50 text-sm mb-8 leading-relaxed">{{ narrative()?.storyArcSummary }}</p>

          <!-- Timeline de capítulos -->
          <div class="relative">
            <div class="absolute left-5 top-0 bottom-0 w-px bg-gradient-to-b from-indigo-500/40 via-white/10 to-transparent"></div>
            <div class="space-y-6 ml-12">
              <div *ngFor="let chapter of narrative()?.chapters; trackBy: trackByChapter"
                   class="relative">
                <!-- Dot en la línea -->
                <div class="absolute -left-[2.65rem] top-1.5 w-3 h-3 rounded-full border-2
                            transition-colors"
                     [ngClass]="chapter.open
                       ? 'bg-indigo-500 border-indigo-400 shadow-[0_0_10px_rgba(99,102,241,0.5)]'
                       : 'bg-white/10 border-white/20'">
                </div>
                <!-- Turning point indicator -->
                <div *ngIf="chapter.turningPoint"
                     class="absolute -left-[3.2rem] top-0 text-xs">⚡</div>

                <div class="p-5 rounded-2xl border transition-all"
                     [ngClass]="chapter.open
                       ? 'bg-indigo-500/5 border-indigo-500/20 hover:bg-indigo-500/10'
                       : 'bg-white/[0.02] border-white/5 hover:bg-white/[0.04]'">
                  <div class="flex items-center justify-between mb-2">
                    <div class="flex items-center gap-2">
                      <span class="text-xs font-black text-white/30 uppercase tracking-widest">
                        Cap. {{ chapter.chapterNumber }}
                      </span>
                      <span class="px-2 py-0.5 text-[9px] font-black uppercase tracking-widest rounded-full"
                            [ngClass]="phaseClass(chapter.phase)">
                        {{ chapter.phase }}
                      </span>
                      <span *ngIf="chapter.open"
                            class="px-2 py-0.5 text-[9px] font-black uppercase tracking-widest
                                   rounded-full bg-emerald-500/10 text-emerald-400">
                        ACTIVO
                      </span>
                    </div>
                    <span *ngIf="chapter.icfAtOpen" class="text-[10px] text-white/30">
                      ICF {{ chapter.icfAtOpen | number:'1.0-0' }}
                      <span *ngIf="chapter.icfAtClose">→ {{ chapter.icfAtClose | number:'1.0-0' }}</span>
                    </span>
                  </div>
                  <h3 class="font-bold text-white/90 mb-1">{{ chapter.title }}</h3>
                  <p class="text-white/50 text-xs leading-relaxed">{{ chapter.body }}</p>
                </div>
              </div>
            </div>
          </div>
        </section>

        <!-- ── SECCIÓN 3: GRAFO RELACIONAL ───────────────────────────────── -->
        <section *ngIf="graph()" class="glass-card border-l-4 border-teal-500 p-8 rounded-3xl">
          <div class="flex items-center gap-3 mb-6">
            <div class="w-10 h-10 bg-teal-500/20 rounded-xl flex items-center justify-center text-xl">🕸️</div>
            <div>
              <h2 class="font-bold text-white/90 text-lg">Grafo de Identidad</h2>
              <span class="text-[9px] text-white/30 uppercase tracking-[0.2em]">
                {{ graph()?.dyads?.length }} díadas · Cohesión: {{ graph()?.cohesionDensity | number:'1.0-0' }} · Tensión: {{ graph()?.tensionDensity | number:'1.0-0' }}
              </span>
            </div>
          </div>

          <!-- Roles sistémicos -->
          <div *ngIf="(graph()?.systemRoles?.length ?? 0) > 0" class="mb-6">
            <span class="text-[9px] text-white/30 uppercase tracking-widest block mb-3">Roles Sistémicos</span>
            <div class="flex flex-wrap gap-2">
              <div *ngFor="let role of graph()!.systemRoles"
                   class="flex items-center gap-2 px-3 py-1.5 rounded-xl border text-xs font-bold"
                   [ngClass]="systemRoleClass(role.systemRole)">
                <span>{{ roleEmoji(role.systemRole) }}</span>
                <span class="text-white/70">{{ role.memberName }}</span>
                <span class="uppercase text-[9px] tracking-widest opacity-60">{{ role.systemRole }}</span>
              </div>
            </div>
          </div>

          <!-- Díadas -->
          <div class="space-y-3">
            <div *ngFor="let dyad of graph()!.dyads; trackBy: trackByDyad"
                 class="p-5 bg-white/[0.02] rounded-2xl border border-white/5
                        hover:bg-white/[0.04] transition-all">
              <!-- Header de la díada -->
              <div class="flex items-center justify-between mb-4">
                <div class="flex items-center gap-2">
                  <span class="font-bold text-white/90 text-sm">{{ dyad.memberAName }}</span>
                  <span class="text-white/20 text-xs">↔</span>
                  <span class="font-bold text-white/90 text-sm">{{ dyad.memberBName }}</span>
                  <span class="px-2 py-0.5 ml-1 text-[9px] font-black uppercase tracking-widest rounded-full"
                        [ngClass]="dynamicClass(dyad.dynamicType)">
                    {{ dyad.dynamicType }}
                  </span>
                </div>
                <span class="text-white/30 text-[10px] flex items-center gap-1">
                  {{ trendIcon(dyad.evolutionTrend) }} {{ dyad.evolutionTrend }}
                </span>
              </div>
              <!-- Barras de métricas -->
              <div class="space-y-2">
                <div class="flex items-center gap-3">
                  <span class="text-[10px] text-white/30 w-20 text-right">Cohesión</span>
                  <div class="flex-1 h-1.5 bg-white/10 rounded-full overflow-hidden">
                    <div class="h-full bg-emerald-500 rounded-full transition-all duration-700"
                         [style.width.%]="dyad.cohesionScore"></div>
                  </div>
                  <span class="text-[10px] text-white/50 w-8">{{ dyad.cohesionScore | number:'1.0-0' }}</span>
                </div>
                <div class="flex items-center gap-3">
                  <span class="text-[10px] text-white/30 w-20 text-right">Comunicación</span>
                  <div class="flex-1 h-1.5 bg-white/10 rounded-full overflow-hidden">
                    <div class="h-full bg-indigo-500 rounded-full transition-all duration-700"
                         [style.width.%]="dyad.communicationScore"></div>
                  </div>
                  <span class="text-[10px] text-white/50 w-8">{{ dyad.communicationScore | number:'1.0-0' }}</span>
                </div>
                <div class="flex items-center gap-3">
                  <span class="text-[10px] text-white/30 w-20 text-right">Tensión</span>
                  <div class="flex-1 h-1.5 bg-white/10 rounded-full overflow-hidden">
                    <div class="h-full rounded-full transition-all duration-700"
                         [ngClass]="dyad.tensionScore > 60 ? 'bg-red-500' : dyad.tensionScore > 35 ? 'bg-amber-500' : 'bg-teal-500'"
                         [style.width.%]="dyad.tensionScore"></div>
                  </div>
                  <span class="text-[10px] text-white/50 w-8">{{ dyad.tensionScore | number:'1.0-0' }}</span>
                </div>
              </div>
              <!-- Roles en esta díada -->
              <div class="flex gap-3 mt-3 pt-3 border-t border-white/5">
                <span class="text-[9px] text-white/20 uppercase tracking-widest">{{ dyad.memberAName }}: <strong class="text-white/50">{{ dyad.roleA }}</strong></span>
                <span class="text-white/10">·</span>
                <span class="text-[9px] text-white/20 uppercase tracking-widest">{{ dyad.memberBName }}: <strong class="text-white/50">{{ dyad.roleB }}</strong></span>
              </div>
            </div>

            <!-- Empty state -->
            <div *ngIf="graph()!.dyads?.length === 0"
                 class="text-center py-12 text-white/20 text-sm">
              La familia necesita al menos 2 miembros activos para generar el grafo.
            </div>
          </div>
        </section>

        <!-- ── SECCIÓN 4: REFLEXIÓN AUTÓNOMA ─────────────────────────────── -->
        <section class="glass-card border-l-4 border-amber-500 p-8 rounded-3xl">
          <div class="flex items-center justify-between gap-3 mb-6">
            <div class="flex items-center gap-3">
              <div class="w-10 h-10 bg-amber-500/20 rounded-xl flex items-center justify-center text-xl">🪞</div>
              <div>
                <h2 class="font-bold text-white/90 text-lg">Reflexión Autónoma</h2>
                <span class="text-[9px] text-white/30 uppercase tracking-[0.2em]">Autoevaluación del sistema · Efectividad · Riesgo</span>
              </div>
            </div>
            <button (click)="runReflection()"
                    [disabled]="reflecting()"
                    class="px-5 py-2.5 text-xs font-black uppercase tracking-widest rounded-xl
                           bg-amber-500/10 text-amber-400 border border-amber-500/20
                           hover:bg-amber-500/20 transition-all disabled:opacity-40
                           disabled:cursor-not-allowed flex items-center gap-2">
              <span *ngIf="reflecting()" class="w-3 h-3 rounded-full border border-amber-400 border-t-transparent animate-spin"></span>
              {{ reflecting() ? 'Analizando...' : '▶ Ejecutar Reflexión' }}
            </button>
          </div>

          <!-- Estado: sin reflexión aún -->
          <div *ngIf="!reflection()" class="text-center py-12">
            <p class="text-white/20 text-sm">Ejecuta la reflexión para ver el análisis de efectividad del sistema.</p>
          </div>

          <!-- Resultado de reflexión -->
          <div *ngIf="reflection()" class="space-y-6">
            <!-- Alerta urgente -->
            <div *ngIf="reflection()!.requiresUrgentAttention"
                 class="flex items-center gap-3 p-4 bg-red-500/10 rounded-2xl border border-red-500/20">
              <span class="text-xl">🚨</span>
              <p class="text-red-400 text-sm font-bold">Atención urgente requerida — revisa abandono y efectividad.</p>
            </div>

            <!-- Métricas -->
            <div class="grid grid-cols-2 md:grid-cols-4 gap-4">
              <div class="p-4 bg-white/[0.03] rounded-2xl border border-white/5 text-center">
                <span class="text-[9px] text-white/30 uppercase tracking-widest block mb-2">Efectividad</span>
                <span class="text-2xl font-black"
                      [ngClass]="effectivenessColor(reflection()!.effectivenessLevel)">
                  {{ effectivenessLabel(reflection()!.effectivenessLevel) }}
                </span>
              </div>
              <div class="p-4 bg-white/[0.03] rounded-2xl border border-white/5 text-center">
                <span class="text-[9px] text-white/30 uppercase tracking-widest block mb-2">Tendencia ICF</span>
                <span class="text-2xl font-black"
                      [ngClass]="reflection()!.icfTrend >= 0 ? 'text-emerald-400' : 'text-red-400'">
                  {{ reflection()!.icfTrend >= 0 ? '+' : '' }}{{ reflection()!.icfTrend | number:'1.1-1' }}
                </span>
              </div>
              <div class="p-4 bg-white/[0.03] rounded-2xl border border-white/5 text-center">
                <span class="text-[9px] text-white/30 uppercase tracking-widest block mb-2">Adherencia</span>
                <span class="text-2xl font-black text-indigo-400">
                  {{ reflection()!.avgAdherence | number:'1.0-0' }}%
                </span>
              </div>
              <div class="p-4 bg-white/[0.03] rounded-2xl border border-white/5 text-center">
                <span class="text-[9px] text-white/30 uppercase tracking-widest block mb-2">Abandono</span>
                <span class="text-2xl font-black"
                      [ngClass]="abandonmentColor(reflection()!.abandonmentLevel)">
                  {{ reflection()!.abandonmentLevel }}
                </span>
              </div>
            </div>

            <!-- Resumen de efectividad -->
            <div class="p-4 bg-white/[0.02] rounded-2xl border-l-2 border-amber-500/40">
              <span class="text-[9px] text-white/30 uppercase tracking-widest block mb-2">Análisis del sistema</span>
              <p class="text-white/60 text-sm">{{ reflection()!.effectivenessSummary }}</p>
            </div>

            <!-- Señales de abandono -->
            <div *ngIf="(reflection()?.abandonmentSignals?.length ?? 0) > 0"
                 class="p-4 bg-red-500/5 rounded-2xl border border-red-500/10">
              <span class="text-[9px] text-red-400/60 uppercase tracking-widest block mb-2">Señales de riesgo detectadas</span>
              <div class="flex flex-wrap gap-2">
                <span *ngFor="let signal of reflection()!.abandonmentSignals"
                      class="px-2 py-1 text-[10px] font-bold uppercase tracking-widest
                             bg-red-500/10 text-red-400 rounded-lg border border-red-500/10">
                  {{ signal }}
                </span>
              </div>
            </div>

            <!-- Lección aprendida -->
            <div *ngIf="reflection()!.lessonLearned"
                 class="p-5 bg-amber-500/5 rounded-2xl border border-amber-500/10">
              <span class="text-[9px] text-amber-400/60 uppercase tracking-widest block mb-2">💡 Lección Aprendida</span>
              <p class="text-white/70 text-sm leading-relaxed italic">{{ reflection()!.lessonLearned }}</p>
            </div>
          </div>
        </section>

      </ng-container>

      <!-- LOADING TEMPLATE -->
      <ng-template #loadingTemplate></ng-template>
    </div>
  `,
  styles: [`
    .glass-card {
      background: rgba(255,255,255,0.02);
      backdrop-filter: blur(20px);
      border: 1px solid rgba(255,255,255,0.05);
    }
  `]
})
export class CognitivePageComponent implements OnInit {
  private readonly cognitiveService = inject(CognitiveService);
  private readonly familyState     = inject(FamilyStateService);

  // ─── State ──────────────────────────────────────────────────────────────
  loading   = signal(true);
  reflecting = signal(false);

  snapshot  = signal<any | null>(null);
  narrative = signal<NarrativeResponse | null>(null);
  graph     = signal<GraphResponse | null>(null);
  reflection = signal<ReflectionResponse | null>(null);

  // ─── Computed helpers ────────────────────────────────────────────────────
  get stageEmoji(): string {
    const map: Record<string, string> = {
      INITIAL: '🌱', RECOGNITION: '👁️', ADJUSTMENT: '⚙️',
      CONSOLIDATION: '🏗️', AUTONOMOUS: '🚀'
    };
    return map[this.snapshot()?.identityProfile?.evolutionStage ?? ''] ?? '🌱';
  }

  get stageName(): string {
    const map: Record<string, string> = {
      INITIAL: 'Reconocimiento inicial', RECOGNITION: 'Reconocimiento',
      ADJUSTMENT: 'En ajuste activo', CONSOLIDATION: 'Consolidando logros',
      AUTONOMOUS: 'Autonomía operativa'
    };
    return map[this.snapshot()?.identityProfile?.evolutionStage ?? ''] ?? 'En proceso';
  }

  get adaptabilityPct(): string {
    const v = this.snapshot()?.identityProfile?.adaptabilityIndex ?? 0;
    return (v * 100).toFixed(0);
  }

  // ─── Lifecycle ───────────────────────────────────────────────────────────
  ngOnInit(): void {
    const familyId = this.familyState.getSelectedFamilyId();
    if (!familyId) { this.loading.set(false); return; }

    forkJoin({
      snapshot:  this.cognitiveService.getSnapshot(familyId),
      narrative: this.cognitiveService.getNarrative(familyId),
      graph:     this.cognitiveService.getGraph(familyId)
    }).subscribe({
      next: ({ snapshot, narrative, graph }) => {
        this.snapshot.set(snapshot);
        this.narrative.set(narrative);
        this.graph.set(graph);
        this.loading.set(false);
      },
      error: () => this.loading.set(false)
    });
  }

  runReflection(): void {
    const familyId = this.familyState.getSelectedFamilyId();
    if (!familyId || this.reflecting()) return;
    this.reflecting.set(true);
    this.cognitiveService.triggerReflection(familyId).subscribe({
      next: r => { this.reflection.set(r); this.reflecting.set(false); },
      error: ()  => this.reflecting.set(false)
    });
  }

  // ─── Style helpers ───────────────────────────────────────────────────────
  phaseClass(phase: NarrativePhase): string {
    const map: Record<NarrativePhase, string> = {
      AWAKENING:     'bg-indigo-500/10 text-indigo-400',
      DISCOVERY:     'bg-cyan-500/10 text-cyan-400',
      TRANSITION:    'bg-teal-500/10 text-teal-400',
      CONSOLIDATION: 'bg-emerald-500/10 text-emerald-400',
      CRISIS:        'bg-red-500/10 text-red-400',
      RECOVERY:      'bg-amber-500/10 text-amber-400',
      AUTONOMY:      'bg-violet-500/10 text-violet-400'
    };
    return map[phase] ?? 'bg-white/10 text-white/50';
  }

  dynamicClass(d: string): string {
    const map: Record<string, string> = {
      SUPPORTIVE:  'bg-emerald-500/10 text-emerald-400 border-emerald-500/20',
      BALANCED:    'bg-indigo-500/10 text-indigo-400 border-indigo-500/20',
      DISTANT:     'bg-white/5 text-white/40 border-white/10',
      CONFLICTIVE: 'bg-red-500/10 text-red-400 border-red-500/20'
    };
    return map[d] ?? 'bg-white/5 text-white/40';
  }

  systemRoleClass(role: string): string {
    const map: Record<string, string> = {
      ANCHOR:       'bg-violet-500/10 border-violet-500/20',
      PEACEMAKER:   'bg-emerald-500/10 border-emerald-500/20',
      ESCALATOR:    'bg-red-500/10 border-red-500/20',
      DISCONNECTED: 'bg-white/5 border-white/10',
      NEUTRAL:      'bg-white/[0.03] border-white/5'
    };
    return map[role] ?? 'bg-white/[0.03] border-white/5';
  }

  roleEmoji(role: string): string {
    return { ANCHOR: '⚓', PEACEMAKER: '🕊️', ESCALATOR: '🔥', DISCONNECTED: '🌑', NEUTRAL: '⚬' }[role] ?? '⚬';
  }

  trendIcon(trend: string): string {
    return { IMPROVING: '↑', STABLE: '→', DECLINING: '↓' }[trend] ?? '→';
  }

  effectivenessLabel(level: EffectivenessLevel): string {
    return { HIGH: 'ALTA', MODERATE: 'MEDIA', LOW: 'BAJA', REGRESSING: 'REGRESIÓN', INSUFFICIENT_DATA: 'S/D' }[level] ?? level;
  }

  effectivenessColor(level: EffectivenessLevel): string {
    return {
      HIGH: 'text-emerald-400', MODERATE: 'text-amber-400',
      LOW: 'text-orange-400', REGRESSING: 'text-red-400', INSUFFICIENT_DATA: 'text-white/30'
    }[level] ?? 'text-white/50';
  }

  abandonmentColor(level: AbandonmentLevel): string {
    return {
      LOW: 'text-emerald-400', MODERATE: 'text-amber-400',
      HIGH: 'text-orange-400', CRITICAL: 'text-red-400'
    }[level] ?? 'text-white/50';
  }

  // ─── TrackBy ─────────────────────────────────────────────────────────────
  trackByChapter(_: number, c: NarrativeChapter) { return c.chapterNumber; }
  trackByDyad(_: number, d: DyadDto) { return `${d.memberAId}-${d.memberBId}`; }
}
