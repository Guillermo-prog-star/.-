import { Component, OnInit, inject, signal, computed } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { HttpClient } from '@angular/common/http';
import { forkJoin, catchError, of } from 'rxjs';
import { CognitiveService } from '../../core/services/cognitive.service';
import { FamilyStateService } from '../../core/services/family-state.service';
import { NarrativeCompanionComponent } from '../../shared/components/narrative-companion.component';
import {
  CognitiveSnapshot,
  NarrativeResponse, GraphResponse, ReflectionResponse, MemoryResponse, MemoryDto,
  NarrativeChapter, DyadDto, NarrativePhase,
  EffectivenessLevel, AbandonmentLevel
} from '../../core/models/cognitive.model';
import { ScrollPolicyService } from '../../shared/directives/scroll-policy.service';
import { ApiService } from '../../core/services/api.service';

// ── Copilot types ──────────────────────────────────────────────────────────
interface CopilotSuggestion {
  summary: string;
  priority: string;
  recommendedActions: string[];
  containmentSuggestion: string;
  followUpDays: number;
}
interface AiInferenceEntry {
  id: number;
  familyId: number;
  inputSummary: string;
  inferenceResult: string;
  priority: string;
  modelVersion: string;
  createdAt: string;
}
interface ActiveSessionInfo {
  sessionId: number;
  goal: string;
  emotionalArc: string;
  turnCount: number;
  startedAt: string;
}

@Component({
  selector: 'app-cognitive-page',
  standalone: true,
  imports: [CommonModule, RouterLink, NarrativeCompanionComponent],
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

      <!-- Narrative Guidance Engine -->
      <app-narrative-companion module="cognitive"></app-narrative-companion>

      <!-- LOADING -->
      @if (loading()) {
        <div class="flex items-center justify-center py-24">
          <div class="flex flex-col items-center gap-4 text-white/30">
            <div class="w-12 h-12 rounded-full border-2 border-violet-500/40
                        border-t-violet-500 animate-spin"></div>
            <span class="text-xs uppercase tracking-widest">Cargando sistema cognitivo...</span>
          </div>
        </div>
      }

      @if (!loading()) {

        <!-- ══ SECCIÓN 0: COPILOTO IA ════════════════════════════════════════ -->
        <section class="glass-card border-l-4 border-indigo-500 p-8 rounded-3xl space-y-6">

          <!-- Header copiloto -->
          <div class="flex items-start justify-between flex-wrap gap-4">
            <div class="flex items-center gap-3">
              <div class="w-10 h-10 bg-indigo-500/20 rounded-xl flex items-center justify-center text-xl">🤖</div>
              <div>
                <h2 class="font-bold text-white/90 text-lg">Copiloto IA</h2>
                <span class="text-[9px] text-white/30 uppercase tracking-[0.2em]">Inferencias Estructuradas · Sesión Activa · Estado Emocional</span>
              </div>
            </div>
            <div class="flex items-center gap-3">
              @if (copilot()) {
                <span class="px-3 py-1 rounded-full border text-[10px] font-black uppercase tracking-widest"
                      [ngClass]="priorityBg(copilot()!.priority)">
                  {{ priorityLabel(copilot()!.priority) }}
                </span>
              }
              <button (click)="triggerInference()" [disabled]="inferring()"
                      class="px-4 py-2 rounded-xl bg-indigo-500/20 border border-indigo-500/30
                             text-indigo-300 text-xs font-semibold uppercase tracking-wide
                             hover:bg-indigo-500/30 transition-colors disabled:opacity-40
                             flex items-center gap-2">
                @if (inferring()) {
                  <div class="w-3 h-3 border border-indigo-300/40 border-t-indigo-300 rounded-full animate-spin"></div>
                  Analizando…
                } @else {
                  ✦ Nueva inferencia
                }
              </button>
            </div>
          </div>

          <!-- Última inferencia + sesión activa (2 columnas) -->
          <div class="grid grid-cols-1 md:grid-cols-2 gap-5">

            <!-- Card: Última sugerencia del copiloto -->
            <div class="bg-white/[0.03] rounded-2xl p-5 border border-white/5 space-y-4">
              <span class="text-[9px] text-indigo-400/60 uppercase tracking-widest block">Última Sugerencia IA</span>

              @if (!copilot()) {
                <p class="text-white/30 text-xs italic">
                  Sin inferencia disponible. Genera una para obtener el análisis contextual de la familia.
                </p>
              } @else {
                <!-- Resumen -->
                <p class="text-white/75 text-sm leading-relaxed">{{ copilot()!.summary }}</p>

                <!-- Acciones recomendadas -->
                @if (copilot()!.recommendedActions?.length) {
                  <div class="space-y-2">
                    <span class="text-[9px] text-white/30 uppercase tracking-widest">Acciones recomendadas</span>
                    @for (action of copilot()!.recommendedActions; track action) {
                      <div class="flex items-start gap-2">
                        <span class="text-indigo-400 mt-0.5 flex-shrink-0">›</span>
                        <p class="text-white/65 text-xs leading-relaxed">{{ action }}</p>
                      </div>
                    }
                  </div>
                }

                <!-- Contención -->
                @if (copilot()!.containmentSuggestion) {
                  <div class="p-3 bg-indigo-500/5 rounded-xl border border-indigo-500/10">
                    <span class="text-[9px] text-indigo-400/60 uppercase tracking-widest block mb-1">Contención sugerida</span>
                    <p class="text-white/60 text-xs leading-relaxed">{{ copilot()!.containmentSuggestion }}</p>
                  </div>
                }

                <!-- Follow-up -->
                @if (copilot()!.followUpDays) {
                  <div class="flex items-center gap-2 text-xs text-white/40">
                    <span>🕐</span>
                    <span>Seguimiento recomendado en <strong class="text-white/70">{{ copilot()!.followUpDays }} días</strong></span>
                  </div>
                }
              }
            </div>

            <!-- Card: Sesión activa + estado emocional -->
            <div class="bg-white/[0.03] rounded-2xl p-5 border border-white/5 space-y-4">
              <span class="text-[9px] text-indigo-400/60 uppercase tracking-widest block">Sesión Conversacional</span>

              @if (!activeSession()) {
                <div class="flex flex-col items-center justify-center py-6 gap-2 text-white/25">
                  <span class="text-3xl">💬</span>
                  <p class="text-xs text-center">Sin sesión activa en las últimas 4 horas</p>
                  <a routerLink="/chat"
                     class="mt-2 px-3 py-1.5 text-[10px] uppercase tracking-wide font-semibold
                            bg-indigo-500/10 border border-indigo-500/20 text-indigo-400
                            rounded-lg hover:bg-indigo-500/20 transition-colors">
                    Ir al consultor IA
                  </a>
                </div>
              } @else {
                <!-- Arco emocional -->
                <div class="flex items-center gap-3">
                  <span class="text-3xl">{{ arcEmoji(activeSession()!.emotionalArc) }}</span>
                  <div>
                    <span class="px-2.5 py-0.5 rounded-full border text-[10px] font-semibold uppercase tracking-wide"
                          [ngClass]="arcBg(activeSession()!.emotionalArc)">
                      {{ arcLabel(activeSession()!.emotionalArc) }}
                    </span>
                    <p class="text-[9px] text-white/30 uppercase tracking-widest mt-1">Estado emocional del grupo</p>
                  </div>
                </div>

                <!-- Métricas de sesión -->
                <div class="grid grid-cols-2 gap-3">
                  <div class="bg-white/[0.02] rounded-xl p-3 border border-white/5">
                    <span class="text-[9px] text-white/30 uppercase tracking-widest block mb-1">Objetivo</span>
                    <span class="text-xs font-semibold text-indigo-300">{{ goalLabel(activeSession()!.goal) }}</span>
                  </div>
                  <div class="bg-white/[0.02] rounded-xl p-3 border border-white/5">
                    <span class="text-[9px] text-white/30 uppercase tracking-widest block mb-1">Turnos</span>
                    <span class="text-2xl font-black text-white/80">{{ activeSession()!.turnCount }}</span>
                  </div>
                </div>

                <!-- Inicio de sesión -->
                @if (activeSession()!.startedAt) {
                  <p class="text-[10px] text-white/30">
                    Iniciada: {{ inferenceDate(activeSession()!.startedAt) }}
                  </p>
                }
                <a routerLink="/chat"
                   class="block text-center px-3 py-1.5 text-[10px] uppercase tracking-wide font-semibold
                          bg-indigo-500/10 border border-indigo-500/20 text-indigo-400
                          rounded-lg hover:bg-indigo-500/20 transition-colors">
                  Continuar sesión →
                </a>
              }
            </div>
          </div>

          <!-- Historial de inferencias -->
          @if (copilotHistory().length) {
            <div class="space-y-3">
              <span class="text-[9px] text-white/30 uppercase tracking-widest block">Historial de Inferencias IA</span>
              <div class="space-y-2">
                @for (entry of copilotHistory(); track trackByInference($index, entry)) {
                  <div class="flex items-start gap-4 bg-white/[0.02] rounded-xl p-4 border border-white/5
                               hover:border-white/10 transition-colors">
                    <!-- Priority dot -->
                    <div class="flex-shrink-0 mt-1">
                      <div class="w-2 h-2 rounded-full"
                           [ngClass]="{ 'bg-red-400': entry.priority === 'HIGH',
                                        'bg-amber-400': entry.priority === 'MEDIUM',
                                        'bg-emerald-400': entry.priority === 'LOW',
                                        'bg-white/20': !entry.priority }">
                      </div>
                    </div>
                    <!-- Content -->
                    <div class="flex-1 min-w-0">
                      <div class="flex items-center justify-between gap-2 mb-1">
                        <span class="text-[10px] font-semibold uppercase tracking-wide"
                              [ngClass]="priorityColor(entry.priority)">
                          {{ priorityLabel(entry.priority) }}
                        </span>
                        <span class="text-[9px] text-white/25 flex-shrink-0">{{ inferenceDate(entry.createdAt) }}</span>
                      </div>
                      @if (entry.inputSummary) {
                        <p class="text-white/50 text-xs truncate">{{ entry.inputSummary }}</p>
                      }
                      @if (entry.modelVersion) {
                        <span class="text-[9px] text-white/20 mt-0.5 block">modelo: {{ entry.modelVersion }}</span>
                      }
                    </div>
                  </div>
                }
              </div>
            </div>
          }

        </section>

        <!-- ── SECCIÓN 1: IDENTIDAD ───────────────────────────────────────── -->
        @if (narrative()) {
        <section class="glass-card border-l-4 border-violet-500 p-8 rounded-3xl">
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
          @if (snapshot()?.identityProfile?.identityNarrative) {
            <div class="mt-6 p-5 bg-violet-500/5 rounded-2xl border border-violet-500/10">
              <span class="text-[9px] text-violet-400/60 uppercase tracking-widest block mb-2">Narrativa Identitaria</span>
              <p class="text-white/70 text-sm leading-relaxed">{{ snapshot()!.identityProfile.identityNarrative }}</p>
            </div>
          }
        </section>
        }

        <!-- ── SECCIÓN 2: NARRATIVA ───────────────────────────────────────── -->
        @if (narrative()) {
        <section class="glass-card border-l-4 border-indigo-500 p-8 rounded-3xl">
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
              @for (chapter of narrative()?.chapters; track chapter.chapterNumber) {
                <div class="relative">
                  <!-- Dot en la línea -->
                  <div class="absolute -left-[2.65rem] top-1.5 w-3 h-3 rounded-full border-2
                              transition-colors"
                       [ngClass]="chapter.open
                         ? 'bg-indigo-500 border-indigo-400 shadow-[0_0_10px_rgba(99,102,241,0.5)]'
                         : 'bg-white/10 border-white/20'">
                  </div>
                  <!-- Turning point indicator -->
                  @if (chapter.turningPoint) {
                    <div class="absolute -left-[3.2rem] top-0 text-xs">⚡</div>
                  }

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
                        @if (chapter.open) {
                          <span class="px-2 py-0.5 text-[9px] font-black uppercase tracking-widest
                                       rounded-full bg-emerald-500/10 text-emerald-400">
                            ACTIVO
                          </span>
                        }
                      </div>
                      @if (chapter.icfAtOpen) {
                        <span class="text-[10px] text-white/30">
                          ICF {{ chapter.icfAtOpen | number:'1.0-0' }}
                          @if (chapter.icfAtClose) {
                            <span>→ {{ chapter.icfAtClose | number:'1.0-0' }}</span>
                          }
                        </span>
                      }
                    </div>
                    <h3 class="font-bold text-white/90 mb-1">{{ chapter.title }}</h3>
                    <p class="text-white/50 text-xs leading-relaxed">{{ chapter.body }}</p>
                  </div>
                </div>
              }
            </div>
          </div>
        </section>
        }

        <!-- ── SECCIÓN 3: GRAFO RELACIONAL ───────────────────────────────── -->
        @if (graph()) {
        <section class="glass-card border-l-4 border-teal-500 p-8 rounded-3xl">
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
          @if ((graph()?.systemRoles?.length ?? 0) > 0) {
            <div class="mb-6">
              <span class="text-[9px] text-white/30 uppercase tracking-widest block mb-3">Roles Sistémicos</span>
              <div class="flex flex-wrap gap-2">
                @for (role of graph()!.systemRoles; track role.memberName) {
                  <div class="flex items-center gap-2 px-3 py-1.5 rounded-xl border text-xs font-bold"
                       [ngClass]="systemRoleClass(role.systemRole)">
                    <span>{{ roleEmoji(role.systemRole) }}</span>
                    <span class="text-white/70">{{ role.memberName }}</span>
                    <span class="uppercase text-[9px] tracking-widest opacity-60">{{ role.systemRole }}</span>
                  </div>
                }
              </div>
            </div>
          }

          <!-- Díadas -->
          <div class="space-y-3">
            @for (dyad of graph()!.dyads; track trackByDyad($index, dyad)) {
              <div class="p-5 bg-white/[0.02] rounded-2xl border border-white/5
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
            }

            <!-- Empty state -->
            @if (graph()!.dyads.length === 0) {
              <div class="text-center py-12 text-white/20 text-sm">
                La familia necesita al menos 2 miembros activos para generar el grafo.
              </div>
            }
          </div>
        </section>
        }

        <!-- ── SECCIÓN 4: MEMORIA COGNITIVA ─────────────────────────────── -->
        <section class="glass-card border-l-4 border-cyan-500 p-8 rounded-3xl">
          <div class="flex items-center gap-3 mb-6">
            <div class="w-10 h-10 bg-cyan-500/20 rounded-xl flex items-center justify-center text-xl">💾</div>
            <div>
              <h2 class="font-bold text-white/90 text-lg">Memoria Cognitiva</h2>
              <span class="text-[9px] text-white/30 uppercase tracking-[0.2em]">Episódica · Semántica · Procedural</span>
            </div>
          </div>

          @if (!memory()) {
            <div class="text-center py-8 text-white/20 text-sm">
              Sin memorias registradas aún.
            </div>
          }

          @if (memory()) {
            <div class="space-y-6">

              <!-- Tabs: 3 tipos de memoria -->
              <div class="flex gap-2 flex-wrap">
                @for (tab of memoryTabs; track tab.key) {
                  <button (click)="activeMemoryTab.set(tab.key)"
                          class="px-3 py-1.5 text-[10px] font-black uppercase tracking-widest rounded-xl transition-all"
                          [ngClass]="activeMemoryTab() === tab.key
                            ? tab.activeClass
                            : 'bg-white/5 text-white/30 hover:bg-white/10'">
                    {{ tab.emoji }} {{ tab.label }}
                    <span class="ml-1 opacity-60">({{ memoryCount(tab.key) }})</span>
                  </button>
                }
              </div>

              <!-- Lista de memorias activa -->
              <div class="space-y-2">
                @for (m of activeMemories(); track m.id) {
                  <div class="p-4 bg-white/[0.02] rounded-2xl border border-white/5 hover:bg-white/[0.04] transition-all">
                    <div class="flex items-start justify-between gap-4 mb-2">
                      <span class="text-[9px] font-black uppercase tracking-widest px-2 py-0.5 rounded-full"
                            [ngClass]="memoryTypeClass(m.memoryType)">
                        {{ m.memoryType }}
                      </span>
                      <div class="flex items-center gap-2 text-[9px] text-white/25">
                        <span>⭐ {{ m.importanceScore | number:'1.1-1' }}</span>
                        @if (m.semanticKey) {
                          <span class="text-white/20">· {{ m.semanticKey }}</span>
                        }
                      </div>
                    </div>
                    @if (parseMemoryContent(m.content); as parsed) {
                      @if (parsed.evaluationId) {
                        <div class="grid grid-cols-2 md:grid-cols-3 gap-2 mt-3">
                          <div class="bg-white/[0.03] p-2 rounded-lg border border-white/5">
                            <span class="block text-[9px] text-white/30 uppercase tracking-widest mb-1">Índice ICaF</span>
                            <span class="text-sm font-black text-indigo-400">{{ parsed.icf }}</span>
                          </div>
                          <div class="bg-white/[0.03] p-2 rounded-lg border border-white/5">
                            <span class="block text-[9px] text-white/30 uppercase tracking-widest mb-1">Nivel de Riesgo</span>
                            <span class="text-sm font-black"
                                  [ngClass]="parsed.riskLevel === 'ALTO' ? 'text-red-400' : (parsed.riskLevel === 'MODERADO' ? 'text-amber-400' : 'text-emerald-400')">
                              {{ parsed.riskLevel }}
                            </span>
                          </div>
                          <div class="bg-white/[0.03] p-2 rounded-lg border border-white/5 md:col-span-1 col-span-2">
                            <span class="block text-[9px] text-white/30 uppercase tracking-widest mb-1">Atención</span>
                            <span class="text-xs font-bold text-white/80 capitalize">{{ parsed.criticalDimension || 'Ninguna' }}</span>
                          </div>
                        </div>
                      } @else {
                        <pre class="text-white/60 text-[10px] bg-black/20 p-2 rounded-lg mt-2 overflow-x-auto">{{ parsed | json }}</pre>
                      }
                    } @else {
                      <p class="text-white/70 text-xs leading-relaxed">{{ m.content }}</p>
                    }
                    <div class="mt-3 text-[9px] text-white/20 uppercase tracking-widest">
                      {{ m.sourceType }} · {{ m.createdAt | date:'dd/MM/yy HH:mm' }}
                    </div>
                  </div>
                }

                @if (activeMemories().length === 0) {
                  <div class="text-center py-8 text-white/20 text-sm">
                    Sin memorias en esta categoría.
                  </div>
                }
              </div>
            </div>
          }
        </section>

        <!-- ── SECCIÓN 5: REFLEXIÓN AUTÓNOMA ─────────────────────────────────────── -->
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
              @if (reflecting()) {
                <span class="w-3 h-3 rounded-full border border-amber-400 border-t-transparent animate-spin"></span>
              }
              {{ reflecting() ? 'Analizando...' : '↻ Actualizar Reflexión' }}
            </button>
          </div>

          <!-- Estado: sin reflexión disponible -->
          @if (!reflection()) {
            <div class="text-center py-12">
              <p class="text-white/20 text-sm">Sin datos de reflexión todavía — realiza un diagnóstico primero.</p>
            </div>
          }

          <!-- Resultado de reflexión -->
          @if (reflection()) {
            <div class="space-y-6">
              <!-- Alerta urgente -->
              @if (reflection()!.requiresUrgentAttention) {
                <div class="flex items-center gap-3 p-4 bg-red-500/10 rounded-2xl border border-red-500/20">
                  <span class="text-xl">🚨</span>
                  <p class="text-red-400 text-sm font-bold">Atención urgente requerida — revisa abandono y efectividad.</p>
                </div>
              }

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
                  {{ reflection()!.icfTrend >= 0 ? '+' : '' }}{{ reflection()!.icfTrend | number:'1.0-0' }}
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
              @if ((reflection()?.abandonmentSignals?.length ?? 0) > 0) {
                <div class="p-4 bg-red-500/5 rounded-2xl border border-red-500/10">
                  <span class="text-[9px] text-red-400/60 uppercase tracking-widest block mb-2">Señales de riesgo detectadas</span>
                  <div class="flex flex-wrap gap-2">
                    @for (signal of reflection()!.abandonmentSignals; track signal) {
                      <span class="px-2 py-1 text-[10px] font-bold uppercase tracking-widest
                                   bg-red-500/10 text-red-400 rounded-lg border border-red-500/10">
                        {{ signal }}
                      </span>
                    }
                  </div>
                </div>
              }

              <!-- Lección aprendida -->
              @if (reflection()!.lessonLearned) {
                <div class="p-5 bg-amber-500/5 rounded-2xl border border-amber-500/10">
                  <span class="text-[9px] text-amber-400/60 uppercase tracking-widest block mb-2">💡 Lección Aprendida</span>
                  <p class="text-white/70 text-sm leading-relaxed italic">{{ reflection()!.lessonLearned }}</p>
                </div>
              }
            </div>
          }
        </section>

      }
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
  private readonly scrollPolicy    = inject(ScrollPolicyService);
  private readonly http            = inject(HttpClient);
  private readonly api             = inject(ApiService);

  private familyId = 0;

  // ─── State ──────────────────────────────────────────────────────────────
  loading    = signal(true);
  reflecting = signal(false);
  inferring  = signal(false);

  snapshot   = signal<CognitiveSnapshot | null>(null);
  narrative  = signal<NarrativeResponse | null>(null);
  graph      = signal<GraphResponse | null>(null);
  memory     = signal<MemoryResponse | null>(null);
  reflection = signal<ReflectionResponse | null>(null);

  // Copilot state
  copilot        = signal<CopilotSuggestion | null>(null);
  copilotHistory = signal<AiInferenceEntry[]>([]);
  activeSession  = signal<ActiveSessionInfo | null>(null);

  activeMemoryTab = signal<'episodic' | 'semantic' | 'procedural'>('episodic');

  readonly memoryTabs = [
    { key: 'episodic'   as const, label: 'Episódica',   emoji: '📅',
      activeClass: 'bg-cyan-500/10 text-cyan-400 border border-cyan-500/20' },
    { key: 'semantic'   as const, label: 'Semántica',   emoji: '🧩',
      activeClass: 'bg-violet-500/10 text-violet-400 border border-violet-500/20' },
    { key: 'procedural' as const, label: 'Procedural',  emoji: '⚙️',
      activeClass: 'bg-amber-500/10 text-amber-400 border border-amber-500/20' }
  ];

  activeMemories = computed((): MemoryDto[] => {
    const m = this.memory();
    if (!m) return [];
    return this.activeMemoryTab() === 'episodic'   ? m.episodic
         : this.activeMemoryTab() === 'semantic'   ? m.semantic
         :                                           m.procedural;
  });

  memoryCount(tab: 'episodic' | 'semantic' | 'procedural'): number {
    const m = this.memory();
    if (!m) return 0;
    return tab === 'episodic' ? m.episodic.length
         : tab === 'semantic' ? m.semantic.length
         :                      m.procedural.length;
  }

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
    this.scrollPolicy.set('scroll-to-new');
    const fid = this.familyState.getSelectedFamilyId();
    if (!fid) { this.loading.set(false); return; }
    this.familyId = fid;

    forkJoin({
      snapshot:   this.cognitiveService.getSnapshot(fid),
      narrative:  this.cognitiveService.getNarrative(fid),
      graph:      this.cognitiveService.getGraph(fid),
      memory:     this.cognitiveService.getMemory(fid),
      reflection: this.cognitiveService.getLatestReflection(fid)
    }).subscribe({
      next: ({ snapshot, narrative, graph, memory, reflection }) => {
        this.snapshot.set(snapshot);
        this.narrative.set(narrative);
        this.graph.set(graph);
        this.memory.set(memory);
        this.reflection.set(reflection);
        this.loading.set(false);
      },
      error: () => this.loading.set(false)
    });

    // Load copilot data in parallel (non-blocking)
    this.loadCopilot(fid);
  }

  private loadCopilot(fid: number): void {
    forkJoin({
      suggestion: this.http.get<any>(`${this.api.base}/copilot/family/${fid}`)
                    .pipe(catchError(() => of(null))),
      history:    this.http.get<any>(`${this.api.base}/copilot/history/${fid}`)
                    .pipe(catchError(() => of(null))),
      session:    this.http.get<any>(`${this.api.base}/chat/session/active?familyId=${fid}`)
                    .pipe(catchError(() => of(null)))
    }).subscribe(({ suggestion, history, session }) => {
      this.copilot.set(suggestion?.data ?? suggestion ?? null);
      const raw: AiInferenceEntry[] = history?.data ?? history ?? [];
      this.copilotHistory.set(Array.isArray(raw) ? raw.slice(0, 8) : []);
      this.activeSession.set(session?.data ?? session ?? null);
    });
  }

  triggerInference(): void {
    if (this.inferring() || !this.familyId) return;
    this.inferring.set(true);
    this.http.post<any>(`${this.api.base}/copilot/infer`,
        { familyId: this.familyId, triggerEvent: 'MANUAL' })
      .pipe(catchError(() => of(null)))
      .subscribe(res => {
        const r = res?.data ?? res ?? null;
        if (r) {
          this.copilot.set(r);
          // prepend synthetic entry to history
          this.copilotHistory.update(h => [{
            id: Date.now(), familyId: this.familyId,
            inputSummary: 'Inferencia manual', inferenceResult: '',
            priority: r.priority ?? 'MEDIUM',
            modelVersion: '—', createdAt: new Date().toISOString()
          } as AiInferenceEntry, ...h].slice(0, 8));
        }
        this.inferring.set(false);
      });
  }

  runReflection(): void {
    if (this.reflecting()) return;
    this.reflecting.set(true);
    this.cognitiveService.triggerReflection(this.familyId).subscribe({
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

  memoryTypeClass(type: string): string {
    return {
      EPISODIC:   'bg-cyan-500/10 text-cyan-400',
      SEMANTIC:   'bg-violet-500/10 text-violet-400',
      PROCEDURAL: 'bg-amber-500/10 text-amber-400',
      IDENTITY:   'bg-pink-500/10 text-pink-400'
    }[type] ?? 'bg-white/10 text-white/40';
  }

  trackByDyad(_: number, d: DyadDto) { return `${d.memberAId}-${d.memberBId}`; }
  trackByInference(_: number, i: AiInferenceEntry) { return i.id; }

  // ─── Copilot helpers ─────────────────────────────────────────────────────
  priorityColor(p: string): string {
    return { HIGH: 'text-red-400', MEDIUM: 'text-amber-400', LOW: 'text-emerald-400' }[p] ?? 'text-white/40';
  }
  priorityBg(p: string): string {
    return { HIGH: 'bg-red-500/10 border-red-500/20 text-red-400',
             MEDIUM: 'bg-amber-500/10 border-amber-500/20 text-amber-400',
             LOW:  'bg-emerald-500/10 border-emerald-500/20 text-emerald-400' }[p]
           ?? 'bg-white/5 border-white/10 text-white/40';
  }
  priorityLabel(p: string): string {
    return { HIGH: 'ALTA', MEDIUM: 'MEDIA', LOW: 'BAJA' }[p] ?? p ?? '—';
  }
  arcEmoji(arc: string): string {
    return { STABLE: '😌', MILD_TENSION: '😐', ESCALATING: '😟',
             ESCALATED: '😰', DE_ESCALATING: '😮‍💨',
             CALM: '😌', ANXIOUS: '😰', FRUSTRATED: '😤',
             HOPEFUL: '🙂', CONFUSED: '😕', ENGAGED: '😊' }[arc] ?? '❓';
  }
  arcLabel(arc: string): string {
    return { STABLE: 'Estable', MILD_TENSION: 'Tensión leve',
             ESCALATING: 'Escalando', ESCALATED: 'Escalado',
             DE_ESCALATING: 'Desescalando', CALM: 'Calmada',
             ANXIOUS: 'Ansiosa', FRUSTRATED: 'Frustrada',
             HOPEFUL: 'Esperanzada', CONFUSED: 'Confusa', ENGAGED: 'Comprometida' }[arc] ?? arc;
  }
  arcBg(arc: string): string {
    if (!arc) return 'bg-white/5 border-white/10 text-white/40';
    if (['ESCALATED','ANXIOUS','FRUSTRATED'].includes(arc))
      return 'bg-red-500/10 border-red-500/20 text-red-400';
    if (['ESCALATING','MILD_TENSION','CONFUSED'].includes(arc))
      return 'bg-amber-500/10 border-amber-500/20 text-amber-400';
    if (['DE_ESCALATING','HOPEFUL'].includes(arc))
      return 'bg-teal-500/10 border-teal-500/20 text-teal-400';
    return 'bg-emerald-500/10 border-emerald-500/20 text-emerald-400';
  }
  goalLabel(g: string): string {
    return { GENERAL: 'General', SUPPORT: 'Soporte', REFLECTION: 'Reflexión',
             PLANNING: 'Planificación', GUARDIAN_SYNC: 'Sincronización Guardián' }[g] ?? g;
  }
  inferenceDate(iso: string): string {
    if (!iso) return '—';
    try {
      const d = new Date(iso);
      return d.toLocaleDateString('es', { day: '2-digit', month: 'short' }) +
             ' ' + d.toLocaleTimeString('es', { hour: '2-digit', minute: '2-digit' });
    } catch { return iso; }
  }

  parseMemoryContent(content: string): any {
    try {
      if (content && typeof content === 'string' && content.trim().startsWith('{')) {
        return JSON.parse(content);
      }
    } catch (e) {
      // Ignorar, no es un JSON válido
    }
    return null;
  }
}
