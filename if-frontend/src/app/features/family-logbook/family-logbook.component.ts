import {
  Component, OnInit, inject, signal, computed, ChangeDetectionStrategy
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import {
  CreateFamilyLogbookEntryRequest,
  FamilyLogbookEntry,
  LogbookStatus
} from './family-logbook.model';
import { FamilyLogbookService } from './family-logbook.service';
import { AuthService } from '../../core/services/auth.service';
import { SprintService } from './sprint.service';
import { NarrativeCompanionComponent } from '../../shared/components/narrative-companion.component';
import { TransformationFlowService } from '../../core/services/transformation-flow.service';
import { HttpClient } from '@angular/common/http';
import { ApiService } from '../../core/services/api.service';
import { Router } from '@angular/router';
import { catchError, of } from 'rxjs';
import {
  SprintResponse,
  SprintDailyResponse,
  SprintMissionResponse,
  CreateSprintRequest,
  CreateDailyCheckinRequest,
  CloseSprintRequest
} from './sprint.model';

type TabMode = 'LOGBOOK' | 'SPRINT' | 'HISTORY';
type FilterMode = 'ALL' | LogbookStatus;

interface FormState {
  situation: string;
  difficultyDetected: string;
  emotionIdentified: string;
  understanding: string;
  correctionAction: string;
  familyAgreement: string;
  createdBy: string;
}

interface SprintFormState {
  objective: string;
  riskDimension: string;
  durationDays: number;
  newMission: string;
  missions: string[];
}

interface DailyFormState {
  yesterdayText: string;
  todayText: string;
  blockagesText: string;
  resolutionText: string;
  emotionalIndicator: string;
  memberName: string;
}

interface RetroFormState {
  whatWentWell: string;
  whatWasDifficult: string;
  whatLearned: string;
  whatToAdjust: string;
  tensionLevel: number;
  mindfulCompliance: number;
  sharedTime: number;
  positiveInteractions: number;
  emotionalPersistence: number;
}

const EMPTY_FORM: FormState = {
  situation: '',
  difficultyDetected: '',
  emotionIdentified: '',
  understanding: '',
  correctionAction: '',
  familyAgreement: '',
  createdBy: ''
};

const EMPTY_SPRINT_FORM: SprintFormState = {
  objective: '',
  riskDimension: 'comunicacion',
  durationDays: 7,
  newMission: '',
  missions: []
};

const EMPTY_DAILY_FORM: DailyFormState = {
  yesterdayText: '',
  todayText: '',
  blockagesText: '',
  resolutionText: '',
  emotionalIndicator: 'HAPPY',
  memberName: ''
};

const EMPTY_RETRO_FORM: RetroFormState = {
  whatWentWell: '',
  whatWasDifficult: '',
  whatLearned: '',
  whatToAdjust: '',
  tensionLevel: 5,
  mindfulCompliance: 8,
  sharedTime: 7,
  positiveInteractions: 8,
  emotionalPersistence: 7
};

/**
 * SDD: Bitácora de Transformación Familiar y Agilismo Consciente.
 * Rediseño Premium v3 — Tabs + OnPush + Sprints de 7/15 días + Daily de Conciencia.
 */
@Component({
  selector: 'app-family-logbook',
  standalone: true,
  imports: [CommonModule, RouterLink, NarrativeCompanionComponent],
  changeDetection: ChangeDetectionStrategy.OnPush,
  styles: [`
    .markdown-content ::ng-deep h3 { color: #818cf8; font-size: 1.25rem; font-weight: 800; margin: 1.5rem 0 0.75rem; border-bottom: 1px solid rgba(255,255,255,0.05); padding-bottom: 4px; }
    .markdown-content ::ng-deep h4 { color: #a5b4fc; font-size: 1.1rem; font-weight: 700; margin: 1.25rem 0 0.5rem; }
    .markdown-content ::ng-deep strong { color: #6366f1; font-weight: 800; }
    .markdown-content ::ng-deep p { margin: 0 0 1rem; line-height: 1.7; }
    .markdown-content ::ng-deep li { margin-left: 1.5rem; margin-bottom: 6px; list-style-type: disc; }
    @keyframes gradient-slide { 0% { background-position: 0% 50%; } 100% { background-position: 200% 50%; } }
    @keyframes spin { to { transform: rotate(360deg); } }
  `],
  template: `
<div class="min-h-screen p-4 lg:p-10 space-y-8">

  <!-- HEADER -->
  <header class="flex flex-col md:flex-row justify-between items-start md:items-center gap-6">
    <div>
      <a routerLink="/dashboard"
         class="inline-flex items-center gap-2 text-white/30 hover:text-white/70 text-[11px] uppercase tracking-widest font-black mb-3 transition-colors">
        ← Dashboard
      </a>
      <h1 class="text-4xl md:text-5xl font-bold bg-clip-text text-transparent bg-gradient-to-r from-white to-white/40 tracking-tight">
        Bitácora Evolutiva
      </h1>
      <p class="text-indigo-400 font-bold tracking-widest uppercase text-[10px] mt-2">
        Agilismo Familiar · Microcompromisos · Daily de Conciencia
      </p>
    </div>

    <!-- TABS MENU -->
    <div class="flex bg-black/40 border border-white/5 p-1 rounded-2xl">
      <button (click)="setTab('LOGBOOK')"
              class="px-4 py-2 rounded-xl text-[10px] font-black uppercase tracking-widest transition-all"
              [ngClass]="activeTab() === 'LOGBOOK' ? 'bg-indigo-600 text-white shadow-lg' : 'text-white/40 hover:text-white/70'">
        📔 Bitácora Tradicional
      </button>
      <button (click)="setTab('SPRINT')"
              class="px-4 py-2 rounded-xl text-[10px] font-black uppercase tracking-widest transition-all"
              [ngClass]="activeTab() === 'SPRINT' ? 'bg-indigo-600 text-white shadow-lg' : 'text-white/40 hover:text-white/70'">
        ⚡ Sprint de Evolución
      </button>
      <button (click)="setTab('HISTORY')"
              class="px-4 py-2 rounded-xl text-[10px] font-black uppercase tracking-widest transition-all"
              [ngClass]="activeTab() === 'HISTORY' ? 'bg-indigo-600 text-white shadow-lg' : 'text-white/40 hover:text-white/70'">
        📜 Historial Sprints
      </button>
    </div>
  </header>

  <!-- Narrative Guidance Engine -->
  <app-narrative-companion module="logbook"></app-narrative-companion>

  <!-- ERROR BANNER -->
  <div *ngIf="error()"
       class="glass-premium border border-red-500/30 bg-red-500/5 p-4 rounded-2xl flex items-center gap-3 text-red-400 text-sm font-medium">
    <span class="text-xl">⚠️</span>
    {{ error() }}
  </div>

  <!-- ==================== TAB: BITÁCORA TRADICIONAL ==================== -->
  <div *ngIf="activeTab() === 'LOGBOOK'" class="space-y-8 animate-in fade-in duration-300">
    <!-- AI CORRELATION PANEL -->
    <div *ngIf="correlation() || loadingCorrelation()"
         class="glass-premium p-6 rounded-[2rem] border border-indigo-500/20 bg-gradient-to-br from-indigo-500/5 to-purple-500/5">
      <details class="group">
        <summary class="flex justify-between items-center cursor-pointer list-none">
          <div class="flex items-center gap-4">
            <div class="w-11 h-11 bg-gradient-to-tr from-indigo-600 to-purple-600 rounded-2xl flex items-center justify-center text-lg shadow-lg shadow-indigo-500/20">
              🤖
            </div>
            <div>
              <span class="block text-[9px] uppercase tracking-widest text-indigo-400 font-black mb-0.5">Mentor de Integridad Familiar</span>
              <h3 class="font-bold text-white/90 text-sm">Análisis Cualitativo Clínico</h3>
            </div>
          </div>
          <div class="flex items-center gap-3">
            <div *ngIf="loadingCorrelation()"
                 class="w-4 h-4 rounded-full border-2 border-indigo-500/30 border-t-indigo-500 animate-spin"></div>
            <div class="w-7 h-7 bg-white/5 rounded-full flex items-center justify-center group-open:bg-white/10 transition-colors">
              <span class="text-white/50 text-[10px] group-open:rotate-180 transition-transform duration-300 block">▼</span>
            </div>
          </div>
        </summary>

        <div *ngIf="!loadingCorrelation() && correlation() as corr" class="mt-6 border-t border-white/5 pt-6">
          <div class="grid grid-cols-1 lg:grid-cols-[320px_1fr] gap-6">

            <!-- Temperature panel -->
            <div class="space-y-4">
              <div class="bg-white/[0.02] rounded-2xl border border-white/5 p-5 text-center">
                <span class="block text-[9px] uppercase tracking-widest text-white/40 font-black mb-1">Temperatura Emocional</span>
                <span class="block text-5xl font-black my-2"
                      [ngClass]="{
                        'text-emerald-400': corr.averageEmotionalScore > 0.2,
                        'text-red-400':     corr.averageEmotionalScore < -0.2,
                        'text-white/70':    corr.averageEmotionalScore >= -0.2 && corr.averageEmotionalScore <= 0.2
                      }">
                  {{ corr.averageEmotionalScore > 0 ? '+' : '' }}{{ corr.averageEmotionalScore | number:'1.2-2' }}
                </span>
                <span class="inline-block px-3 py-1 rounded-full text-[9px] font-black uppercase tracking-wider"
                      [ngClass]="{
                        'bg-emerald-500/10 text-emerald-400': corr.averageEmotionalScore > 0.2,
                        'bg-red-500/10 text-red-400':         corr.averageEmotionalScore < -0.2,
                        'bg-white/5 text-white/50':            corr.averageEmotionalScore >= -0.2 && corr.averageEmotionalScore <= 0.2
                      }">
                  {{ corr.generalLabel }}
                </span>
              </div>

              <!-- Dimension bars -->
              <div *ngIf="corr.dimensionCorrelations?.length" class="space-y-3">
                <h4 class="text-[9px] uppercase tracking-widest text-white/30 font-black">Sintonía por Dimensión</h4>
                <div *ngFor="let dc of corr.dimensionCorrelations" class="space-y-1">
                  <div class="flex justify-between text-xs font-semibold">
                    <span class="text-white/60">{{ dc.dimensionFriendlyName }}</span>
                    <span [ngClass]="dc.logbookSentimentScore >= 0 ? 'text-emerald-400' : 'text-red-400'">
                      {{ dc.logbookSentimentScore > 0 ? '+' : '' }}{{ dc.logbookSentimentScore | number:'1.1-1' }}
                    </span>
                  </div>
                  <div class="h-1.5 bg-white/5 rounded-full overflow-hidden">
                    <div class="h-full rounded-full transition-all duration-500"
                         [style.width.%]="(dc.logbookSentimentScore + 1) * 50"
                         [ngClass]="dc.logbookSentimentScore < -0.2 ? 'bg-red-500' : dc.logbookSentimentScore > 0.2 ? 'bg-emerald-500' : 'bg-indigo-500'">
                    </div>
                  </div>
                  <div *ngIf="dc.requiresPriorityShift"
                       class="text-[9px] font-black uppercase tracking-wider text-amber-400 bg-amber-500/10 px-2 py-0.5 rounded-md border border-amber-500/20 inline-block">
                    ⚠️ Ajuste de Prioridad Activo
                  </div>
                </div>
              </div>
            </div>

            <!-- Report panel -->
            <div class="markdown-content text-sm text-white/75 leading-relaxed bg-white/[0.02] rounded-2xl border border-white/5 p-5"
                 [innerHTML]="formatAiResponse(corr.adaptationRecommendation)">
            </div>
          </div>
        </div>
      </details>
    </div>

    <!-- NEW ENTRY BUTTON / FORM TOGGLE -->
    <div class="flex justify-end">
      <button (click)="toggleForm()"
              class="flex items-center gap-2 px-6 py-3 rounded-2xl font-black uppercase tracking-widest text-xs transition-all"
              [ngClass]="showForm()
                ? 'bg-white/10 text-white/60 hover:bg-white/15'
                : 'bg-indigo-600 hover:bg-indigo-500 text-white shadow-[0_8px_24px_rgba(99,102,241,0.3)] hover:scale-105 active:scale-95'">
        {{ showForm() ? '✕ Cancelar' : '+ Registrar Situación' }}
      </button>
    </div>

    <!-- NEW ENTRY FORM -->
    <div *ngIf="showForm()"
         class="glass-premium p-8 rounded-[2rem] border border-indigo-500/20 animate-in slide-in-from-top duration-300">
      <h2 class="text-lg font-black text-white/90 mb-6 flex items-center gap-3">
        <span class="w-8 h-8 bg-indigo-500/20 rounded-xl flex items-center justify-center text-base">📔</span>
        Registrar Nueva Situación
      </h2>

      <div class="grid grid-cols-1 md:grid-cols-2 gap-5">
        <div class="md:col-span-2">
          <label class="block text-[9px] uppercase tracking-widest text-white/40 font-black mb-2">Situación Vivida *</label>
          <textarea rows="3"
                    class="w-full bg-white/[0.03] border border-white/10 rounded-xl px-4 py-3 text-white/90 text-sm placeholder-white/20 focus:outline-none focus:border-indigo-500/50 focus:bg-white/[0.05] transition-all resize-none"
                    placeholder="Describe la situación que vivisteis..."
                    [value]="form().situation"
                    (input)="patchForm('situation', $any($event.target).value)"></textarea>
        </div>

        <div>
          <label class="block text-[9px] uppercase tracking-widest text-white/40 font-black mb-2">Dificultad / Error Detectado *</label>
          <textarea rows="3"
                    class="w-full bg-white/[0.03] border border-white/10 rounded-xl px-4 py-3 text-white/90 text-sm placeholder-white/20 focus:outline-none focus:border-indigo-500/50 focus:bg-white/[0.05] transition-all resize-none"
                    placeholder="¿Qué salió mal o fue difícil?"
                    [value]="form().difficultyDetected"
                    (input)="patchForm('difficultyDetected', $any($event.target).value)"></textarea>
        </div>

        <div>
          <label class="block text-[9px] uppercase tracking-widest text-white/40 font-black mb-2">Emoción Identificada *</label>
          <input type="text"
                 class="w-full bg-white/[0.03] border border-white/10 rounded-xl px-4 py-3 text-white/90 text-sm placeholder-white/20 focus:outline-none focus:border-indigo-500/50 focus:bg-white/[0.05] transition-all"
                 placeholder="Ej: Frustración, miedo, alegría..."
                 [value]="form().emotionIdentified"
                 (input)="patchForm('emotionIdentified', $any($event.target).value)" />
        </div>

        <div>
          <label class="block text-[9px] uppercase tracking-widest text-white/40 font-black mb-2">Qué Entendimos *</label>
          <textarea rows="3"
                    class="w-full bg-white/[0.03] border border-white/10 rounded-xl px-4 py-3 text-white/90 text-sm placeholder-white/20 focus:outline-none focus:border-indigo-500/50 focus:bg-white/[0.05] transition-all resize-none"
                    placeholder="¿Qué aprendizaje obtuvisteis?"
                    [value]="form().understanding"
                    (input)="patchForm('understanding', $any($event.target).value)"></textarea>
        </div>

        <div>
          <label class="block text-[9px] uppercase tracking-widest text-white/40 font-black mb-2">Qué Corregimos *</label>
          <textarea rows="3"
                    class="w-full bg-white/[0.03] border border-white/10 rounded-xl px-4 py-3 text-white/90 text-sm placeholder-white/20 focus:outline-none focus:border-indigo-500/50 focus:bg-white/[0.05] transition-all resize-none"
                    placeholder="Acción concreta de corrección..."
                    [value]="form().correctionAction"
                    (input)="patchForm('correctionAction', $any($event.target).value)"></textarea>
        </div>

        <div class="md:col-span-2">
          <label class="block text-[9px] uppercase tracking-widest text-white/40 font-black mb-2">Acuerdo Familiar *</label>
          <textarea rows="2"
                    class="w-full bg-white/[0.03] border border-white/10 rounded-xl px-4 py-3 text-white/90 text-sm placeholder-white/20 focus:outline-none focus:border-indigo-500/50 focus:bg-white/[0.05] transition-all resize-none"
                    placeholder="El acuerdo al que llegasteis como familia..."
                    [value]="form().familyAgreement"
                    (input)="patchForm('familyAgreement', $any($event.target).value)"></textarea>
        </div>

        <div>
          <label class="block text-[9px] uppercase tracking-widest text-white/40 font-black mb-2">Registrado por</label>
          <input type="text"
                 class="w-full bg-white/[0.03] border border-white/10 rounded-xl px-4 py-3 text-white/90 text-sm placeholder-white/20 focus:outline-none focus:border-indigo-500/50 focus:bg-white/[0.05] transition-all"
                 placeholder="Padre, madre, hijo, hija..."
                 [value]="form().createdBy"
                 (input)="patchForm('createdBy', $any($event.target).value)" />
        </div>

        <div class="md:col-span-2 flex justify-end">
          <button (click)="createEntry()"
                  [disabled]="saving()"
                  class="px-8 py-3.5 bg-indigo-600 hover:bg-indigo-500 disabled:opacity-50 disabled:cursor-not-allowed text-white font-black uppercase tracking-widest text-xs rounded-2xl transition-all hover:scale-105 active:scale-95 shadow-[0_8px_24px_rgba(99,102,241,0.3)] flex items-center gap-2">
            <span *ngIf="saving()" class="w-4 h-4 border-2 border-white/30 border-t-white rounded-full animate-spin"></span>
            {{ saving() ? 'Guardando...' : '💾 Guardar Aprendizaje' }}
          </button>
        </div>
      </div>
    </div>

    <!-- FILTER TABS + ENTRIES -->
    <div class="space-y-6">
      <div class="flex items-center gap-3 flex-wrap">
        <button *ngFor="let tab of filterTabs"
                (click)="setFilter(tab.value)"
                class="px-4 py-2 rounded-xl text-[10px] font-black uppercase tracking-widest transition-all"
                [ngClass]="filter() === tab.value
                  ? 'bg-white/10 text-white border border-white/20'
                  : 'text-white/30 hover:text-white/60 border border-transparent hover:border-white/10'">
          {{ tab.label }}
          <span class="ml-1.5 px-1.5 py-0.5 rounded-full text-[8px]"
                [ngClass]="filter() === tab.value ? 'bg-white/10' : 'bg-white/5'">
            {{ tab.count() }}
          </span>
        </button>
      </div>

      <!-- Loading state -->
      <div *ngIf="loading()" class="flex justify-center py-20">
        <div class="flex flex-col items-center gap-4">
          <div class="w-12 h-12 border-4 border-indigo-500/20 border-t-indigo-500 rounded-full animate-spin"></div>
          <p class="text-white/30 text-sm font-medium animate-pulse">Cargando bitácora...</p>
        </div>
      </div>

      <!-- Empty state -->
      <div *ngIf="!loading() && filteredEntries().length === 0"
           class="glass-premium p-16 rounded-[2rem] border border-white/5 text-center">
        <div class="text-6xl mb-4">📔</div>
        <p class="text-white/30 text-sm font-medium uppercase tracking-widest">
          {{ filter() === 'ALL' ? 'Sin entradas registradas todavía' : 'Sin entradas ' + (filter() === 'OPEN' ? 'abiertas' : 'resueltas') }}
        </p>
      </div>

      <!-- Entry cards -->
      <div *ngIf="!loading()" class="grid grid-cols-1 xl:grid-cols-2 gap-5">
        <article *ngFor="let entry of filteredEntries(); trackBy: trackById"
                 class="glass-premium rounded-[2rem] border overflow-hidden transition-all duration-300 group"
                 [ngClass]="entry.status === 'OPEN'
                   ? 'border-amber-500/15 hover:border-amber-500/30'
                   : 'border-emerald-500/15 hover:border-emerald-500/25'">

          <div class="p-6 pb-0 flex items-start justify-between gap-4">
            <div class="flex items-center gap-3">
              <div class="w-9 h-9 rounded-2xl flex items-center justify-center text-lg flex-shrink-0"
                   [ngClass]="entry.status === 'OPEN' ? 'bg-amber-500/10' : 'bg-emerald-500/10'">
                {{ entry.status === 'OPEN' ? '🚨' : '✅' }}
              </div>
              <span class="px-2.5 py-1 rounded-full text-[9px] font-black uppercase tracking-wider"
                    [ngClass]="entry.status === 'OPEN'
                      ? 'bg-amber-500/10 text-amber-400 border border-amber-500/20'
                      : 'bg-emerald-500/10 text-emerald-400 border border-emerald-500/20'">
                {{ entry.status === 'OPEN' ? 'Abierta' : 'Resuelta' }}
              </span>
            </div>
            <div class="text-right">
              <span class="text-[9px] text-white/30 font-medium">
                {{ entry.createdAt | date:'dd MMM yyyy' }}
              </span>
              <span *ngIf="entry.createdBy" class="block text-[9px] text-white/20 mt-0.5">
                por {{ entry.createdBy }}
              </span>
            </div>
          </div>

          <div class="px-6 pt-4 pb-2">
            <p class="text-white/85 font-semibold text-sm leading-relaxed line-clamp-2 group-hover:line-clamp-none transition-all">
              {{ entry.situation }}
            </p>
          </div>

          <details class="group/detail">
            <summary class="px-6 py-3 text-[9px] uppercase tracking-widest text-indigo-400/60 hover:text-indigo-400 font-black cursor-pointer list-none flex items-center gap-2 transition-colors">
              <span class="w-3 h-px bg-current"></span>
              Ver detalles
              <span class="group-open/detail:rotate-90 transition-transform duration-200 text-[10px]">›</span>
            </summary>

            <div class="px-6 pb-4 grid grid-cols-1 sm:grid-cols-2 gap-3">
              <div class="bg-white/[0.02] rounded-xl p-3 border border-white/5">
                <span class="block text-[9px] uppercase tracking-widest text-white/30 font-black mb-1">Dificultad</span>
                <p class="text-white/70 text-xs leading-relaxed">{{ entry.difficultyDetected }}</p>
              </div>
              <div class="bg-white/[0.02] rounded-xl p-3 border border-white/5">
                <span class="block text-[9px] uppercase tracking-widest text-indigo-400/60 font-black mb-1">Emoción</span>
                <p class="text-white/70 text-xs font-semibold">{{ entry.emotionIdentified }}</p>
              </div>
              <div class="bg-white/[0.02] rounded-xl p-3 border border-white/5">
                <span class="block text-[9px] uppercase tracking-widest text-white/30 font-black mb-1">Comprensión</span>
                <p class="text-white/70 text-xs leading-relaxed">{{ entry.understanding }}</p>
              </div>
              <div class="bg-white/[0.02] rounded-xl p-3 border border-white/5">
                <span class="block text-[9px] uppercase tracking-widest text-white/30 font-black mb-1">Corrección</span>
                <p class="text-white/70 text-xs leading-relaxed">{{ entry.correctionAction }}</p>
              </div>
              <div class="sm:col-span-2 bg-white/[0.02] rounded-xl p-3 border border-indigo-500/10">
                <span class="block text-[9px] uppercase tracking-widest text-indigo-400/60 font-black mb-1">Acuerdo Familiar</span>
                <p class="text-white/80 text-xs leading-relaxed italic">"{{ entry.familyAgreement }}"</p>
              </div>

              <div *ngIf="entry.status === 'RESOLVED' && entry.progressEvidence"
                   class="sm:col-span-2 bg-emerald-500/5 rounded-xl p-3 border border-emerald-500/20">
                <span class="block text-[9px] uppercase tracking-widest text-emerald-400/70 font-black mb-1">Evidencia de Avance</span>
                <p class="text-emerald-300/80 text-xs leading-relaxed">{{ entry.progressEvidence }}</p>
                <div class="mt-2 text-[9px] text-emerald-400/50 font-medium">
                  <span *ngIf="entry.resolvedBy">{{ entry.resolvedBy }}</span>
                  <span *ngIf="entry.resolvedAt"> · {{ entry.resolvedAt | date:'dd MMM yyyy' }}</span>
                </div>
              </div>
            </div>
          </details>

          <!-- Resolve box (open entries only) -->
          <div *ngIf="entry.status === 'OPEN'"
               class="mx-6 mb-6 p-4 bg-amber-500/5 rounded-2xl border border-amber-500/15">
            <label class="block text-[9px] uppercase tracking-widest text-amber-400/70 font-black mb-2">
              Evidencia de Avance para Cerrar
            </label>
            <textarea rows="2"
                      class="w-full bg-black/20 border border-amber-500/20 rounded-xl px-3 py-2.5 text-white/80 text-xs placeholder-white/20 focus:outline-none focus:border-amber-500/40 transition-all resize-none mb-3"
                      placeholder="Describe el progreso logrado..."
                      [value]="resolveEvidence()[entry.id] ?? ''"
                      (input)="patchEvidence(entry.id, $any($event.target).value)"></textarea>
            <button (click)="resolveEntry(entry)"
                    [disabled]="saving()"
                    class="w-full py-2.5 bg-amber-500/20 hover:bg-amber-500/30 disabled:opacity-50 text-amber-400 font-black uppercase tracking-widest text-[10px] rounded-xl border border-amber-500/20 transition-all hover:border-amber-500/40 flex items-center justify-center gap-2">
              <span *ngIf="saving()" class="w-3 h-3 border border-amber-400/30 border-t-amber-400 rounded-full animate-spin"></span>
              ✓ Cerrar con Evidencia
            </button>
          </div>
        </article>
      </div>
    </div>
  </div>


  <!-- ==================== TAB: SPRINT DE EVOLUCIÓN ==================== -->
  <div *ngIf="activeTab() === 'SPRINT'" class="space-y-8 animate-in fade-in duration-300">
    <div *ngIf="loadingSprint()" class="flex justify-center py-20">
      <div class="flex flex-col items-center gap-4">
        <div class="w-12 h-12 border-4 border-indigo-500/20 border-t-indigo-500 rounded-full animate-spin"></div>
        <p class="text-white/30 text-sm font-medium animate-pulse">Cargando sprint familiar...</p>
      </div>
    </div>

    <!-- CASE 2.1: NO ACTIVE SPRINT - FORM TO CREATE ONE -->
    <div *ngIf="!loadingSprint() && !activeSprint()"
         class="glass-premium p-8 rounded-[2rem] border border-indigo-500/20 max-w-3xl mx-auto space-y-6">

      <!-- ── BANNER SUGERENCIA DESDE MISIÓN ACTIVA ───────────────────────── -->
      <div *ngIf="sprintSuggestion() && !suggestionDismissed()"
           class="relative overflow-hidden rounded-2xl border p-5 mb-2"
           style="background:rgba(99,102,241,0.07);border-color:rgba(99,102,241,0.35);">
        <!-- Borde superior animado -->
        <div style="position:absolute;top:0;left:0;right:0;height:2px;background:linear-gradient(90deg,#6366f1,#818cf8,#6366f1);background-size:200%;animation:gradient-slide 2s linear infinite;"></div>

        <div class="flex items-start gap-4">
          <div class="w-10 h-10 rounded-xl flex items-center justify-content-center text-xl flex-shrink-0"
               style="background:rgba(99,102,241,0.15);display:flex;align-items:center;justify-content:center;">
            🎯
          </div>
          <div style="flex:1;min-width:0;">
            <div style="font-size:10px;font-weight:800;color:#818cf8;letter-spacing:.1em;text-transform:uppercase;margin-bottom:4px;">
              Misión activa detectada
            </div>
            <div style="font-size:14px;font-weight:700;color:#fff;margin-bottom:4px;white-space:nowrap;overflow:hidden;text-overflow:ellipsis;">
              {{ sprintSuggestion()!.missionTitle }}
            </div>
            <div style="font-size:12px;color:rgba(255,255,255,0.45);margin-bottom:12px;line-height:1.5;display:-webkit-box;-webkit-line-clamp:2;-webkit-box-orient:vertical;overflow:hidden;">
              {{ sprintSuggestion()!.missionDescription }}
            </div>
            <div style="display:flex;align-items:center;gap:8px;flex-wrap:wrap;">
              <span style="font-size:10px;background:rgba(255,255,255,0.05);border:1px solid rgba(255,255,255,0.08);padding:2px 8px;border-radius:5px;color:rgba(255,255,255,0.5);">
                ⏱️ {{ sprintSuggestion()!.durationDays }} días
              </span>
              <span style="font-size:10px;background:rgba(255,255,255,0.05);border:1px solid rgba(255,255,255,0.08);padding:2px 8px;border-radius:5px;color:rgba(255,255,255,0.5);">
                📐 {{ sprintSuggestion()!.dimension }}
              </span>
            </div>
          </div>
        </div>

        <div style="display:flex;gap:10px;margin-top:14px;">
          <button
            (click)="acceptAndCreateSprint()"
            [disabled]="savingSprint()"
            style="flex:1;padding:11px;background:linear-gradient(135deg,#6366f1,#818cf8);border:none;border-radius:10px;color:#fff;font-size:13px;font-weight:800;cursor:pointer;transition:all .2s;">
            <span *ngIf="savingSprint()">⏳ Creando sprint…</span>
            <span *ngIf="!savingSprint()">🚀 Crear sprint de esta misión</span>
          </button>
          <button
            (click)="acceptSuggestion()"
            style="padding:11px 16px;background:rgba(255,255,255,0.04);border:1px solid rgba(255,255,255,0.1);border-radius:10px;color:rgba(255,255,255,0.5);font-size:12px;cursor:pointer;">
            Personalizar
          </button>
          <button
            (click)="dismissSuggestion()"
            title="Ignorar sugerencia"
            style="padding:11px 14px;background:none;border:1px solid rgba(255,255,255,0.07);border-radius:10px;color:rgba(255,255,255,0.25);font-size:13px;cursor:pointer;">
            ✕
          </button>
        </div>
      </div>

      <!-- Cargando sugerencia -->
      <div *ngIf="loadingSuggestion()"
           style="display:flex;align-items:center;gap:10px;padding:12px 16px;background:rgba(255,255,255,0.02);border:1px solid rgba(255,255,255,0.06);border-radius:12px;color:rgba(255,255,255,0.35);font-size:12px;">
        <div style="width:14px;height:14px;border:2px solid rgba(99,102,241,0.3);border-top-color:#6366f1;border-radius:50%;animation:spin 0.7s linear infinite;"></div>
        Buscando misión activa del plan…
      </div>

      <div class="text-center space-y-2">
        <div class="w-16 h-16 bg-indigo-600/10 text-indigo-400 rounded-2xl flex items-center justify-center text-3xl mx-auto shadow-inner">
          ⚡
        </div>
        <h2 class="text-2xl font-black text-white/90">Iniciar Sprint de Evolución Familiar</h2>
        <p class="text-white/40 text-xs max-w-md mx-auto">
          Enfoca la transformación familiar en microciclos ágiles de 7 o 15 días. Sin burocracia, orientados 100% a la convivencia emocional y hábitos diarios.
        </p>
      </div>

      <div class="space-y-4 border-t border-white/5 pt-6">
        <!-- Objective -->
        <div>
          <label class="block text-[9px] uppercase tracking-widest text-indigo-300 font-black mb-2">Objetivo del Sprint *</label>
          <input type="text"
                 class="w-full bg-white/[0.03] border border-white/10 rounded-xl px-4 py-3.5 text-white/90 text-sm placeholder-white/20 focus:outline-none focus:border-indigo-500/50 focus:bg-white/[0.05] transition-all"
                 placeholder="Ej: Reducir discusiones en la cena o desconectar pantallas 2 horas juntas"
                 [value]="sprintForm().objective"
                 (input)="patchSprintForm('objective', $any($event.target).value)" />
        </div>

        <div class="grid grid-cols-1 sm:grid-cols-2 gap-4">
          <!-- Dimension -->
          <div>
            <label class="block text-[9px] uppercase tracking-widest text-white/40 font-black mb-2">Dimensión Afectada *</label>
            <select class="w-full bg-black/60 border border-white/10 rounded-xl px-4 py-3.5 text-white/80 text-sm focus:outline-none focus:border-indigo-500/50 transition-all"
                    [value]="sprintForm().riskDimension"
                    (change)="patchSprintForm('riskDimension', $any($event.target).value)">
              <option value="comunicacion">Comunicación Asertiva</option>
              <option value="emociones">Regulación & Clima Emocional</option>
              <option value="habitos">Hábitos & Convivencia Colectiva</option>
              <option value="tiempos">Tiempos de Conexión Activa</option>
            </select>
          </div>

          <!-- Duration -->
          <div>
            <label class="block text-[9px] uppercase tracking-widest text-white/40 font-black mb-2">Duración del Ciclo *</label>
            <select class="w-full bg-black/60 border border-white/10 rounded-xl px-4 py-3.5 text-white/80 text-sm focus:outline-none focus:border-indigo-500/50 transition-all"
                    [value]="sprintForm().durationDays"
                    (change)="patchSprintForm('durationDays', +$any($event.target).value)">
              <option [value]="7">Sprint de 7 días (Recomendado)</option>
              <option [value]="15">Sprint de 15 días (Profundo)</option>
            </select>
          </div>
        </div>

        <!-- Add Missions -->
        <div class="space-y-3">
          <label class="block text-[9px] uppercase tracking-widest text-white/40 font-black">Micro-Misiones del Sprint *</label>
          <div class="flex gap-2">
            <input type="text"
                   class="flex-1 bg-white/[0.03] border border-white/10 rounded-xl px-4 py-3 text-white/90 text-sm placeholder-white/20 focus:outline-none focus:border-indigo-500/50 transition-all"
                   placeholder="Añadir una misión. Ej: Cenar compartida 3 veces sin celulares..."
                   [value]="sprintForm().newMission"
                   (input)="patchSprintForm('newMission', $any($event.target).value)"
                   (keyup.enter)="addSprintMission()" />
            <button (click)="addSprintMission()"
                    class="px-5 py-3 bg-indigo-600/30 hover:bg-indigo-600/50 text-indigo-300 border border-indigo-500/20 text-xs font-black uppercase tracking-widest rounded-xl transition-all">
              + Añadir
            </button>
          </div>

          <!-- Missions List -->
          <div class="space-y-2 max-h-48 overflow-y-auto" *ngIf="sprintForm().missions.length > 0">
            <div *ngFor="let mission of sprintForm().missions; let i = index"
                 class="flex justify-between items-center bg-white/[0.02] border border-white/5 rounded-xl px-4 py-2.5">
              <span class="text-xs text-white/85 font-medium">{{ mission }}</span>
              <button (click)="removeSprintMission(i)"
                      class="text-red-400/50 hover:text-red-400 text-sm font-bold px-2 py-1 transition-colors">
                ✕
              </button>
            </div>
          </div>
        </div>

        <!-- Submit Button -->
        <div class="pt-4">
          <button (click)="createSprint()"
                  [disabled]="savingSprint() || !sprintForm().objective.trim() || sprintForm().missions.length === 0"
                  class="w-full py-4 bg-indigo-600 hover:bg-indigo-500 disabled:opacity-30 disabled:cursor-not-allowed text-white font-black uppercase tracking-widest text-xs rounded-2xl transition-all shadow-[0_8px_32px_rgba(99,102,241,0.2)] hover:scale-[1.01] active:scale-95 flex items-center justify-center gap-2">
            <span *ngIf="savingSprint()" class="w-4 h-4 border-2 border-white/30 border-t-white rounded-full animate-spin"></span>
            Iniciar Sprint Familiar
          </button>
        </div>
      </div>
    </div>

    <!-- CASE 2.2: SPRINT IS ACTIVE -->
    <div *ngIf="!loadingSprint() && activeSprint() as sprint" class="space-y-8">
      
      <!-- SPRINT HEADER / OBJECTIVE CARD -->
      <div class="glass-premium p-6 lg:p-8 rounded-[2.5rem] border border-indigo-500/20 bg-gradient-to-br from-indigo-950/20 via-slate-900/30 to-purple-950/20">
        <div class="flex flex-col md:flex-row justify-between items-start md:items-center gap-6">
          <div class="space-y-2">
            <div class="flex items-center gap-3">
              <span class="px-2.5 py-0.5 bg-indigo-500/10 text-indigo-400 text-[9px] uppercase tracking-widest font-black rounded-md border border-indigo-500/20">
                Sprint de {{ sprint.durationDays }} Días Activo
              </span>
              <span class="px-2.5 py-0.5 bg-emerald-500/10 text-emerald-400 text-[9px] uppercase tracking-widest font-black rounded-md border border-emerald-500/20">
                Dimensión: {{ getDimensionFriendlyName(sprint.riskDimension) }}
              </span>
            </div>
            <h2 class="text-2xl lg:text-3xl font-black text-white/90">
              🎯 Objetivo: {{ sprint.objective }}
            </h2>
            <p class="text-white/40 text-xs">
              Microciclo: {{ sprint.startDate | date:'dd MMM' }} al {{ sprint.endDate | date:'dd MMM yyyy' }}
            </p>
          </div>

          <!-- Progress / Actions -->
          <div class="flex flex-col sm:flex-row items-center gap-4 w-full md:w-auto">
            <div class="bg-black/30 border border-white/5 px-4 py-3 rounded-2xl text-center min-w-[120px] w-full sm:w-auto">
              <span class="block text-2xl font-black text-indigo-400">{{ getDaysRemaining(sprint.endDate) }}</span>
              <span class="text-[9px] uppercase tracking-widest text-white/30 font-black">Días Restantes</span>
            </div>

            <!-- Ir a Evidencias (si el sprint viene de una misión del plan) -->
            <button *ngIf="activeSprint()?.missionTaskId"
                    (click)="goToEvidence()"
                    class="w-full sm:w-auto px-5 py-4 bg-amber-600/20 hover:bg-amber-500/30 text-amber-300 border border-amber-500/25 font-black uppercase tracking-widest text-[10px] rounded-2xl transition-all hover:scale-105 flex items-center justify-center gap-2">
              📸 Agregar Evidencia
            </button>

            <!-- Complete/Close Sprint Button -->
            <button (click)="openRetrospectiveModal()"
                    class="w-full sm:w-auto px-6 py-4 bg-emerald-600 hover:bg-emerald-500 text-white font-black uppercase tracking-widest text-[10px] rounded-2xl shadow-lg transition-all hover:scale-105 active:scale-95 flex items-center justify-center gap-2">
              🏁 Completar Sprint & Retro
            </button>
          </div>
        </div>
      </div>

      <div class="grid grid-cols-1 lg:grid-cols-[1fr_400px] gap-8">
        
        <!-- LEFT COLUMN: DAILY DE CONCIENCIA -->
        <div class="space-y-6">
          <div class="flex justify-between items-center">
            <h3 class="text-lg font-black text-white/90 flex items-center gap-2">
              <span class="w-8 h-8 bg-indigo-500/10 rounded-lg flex items-center justify-center text-sm">🏡</span>
              El "Daily" en la sala de star (Check-In Diario)
            </h3>
            <button (click)="toggleDailyForm()"
                    class="px-4 py-2 bg-indigo-600/20 hover:bg-indigo-600/30 text-indigo-300 border border-indigo-500/20 rounded-xl text-[10px] font-black uppercase tracking-widest transition-all">
              {{ showDailyForm() ? '✕ Cancelar' : '📝 Registrar Mi Daily' }}
            </button>
          </div>

          <!-- DAILY FORM -->
          <div *ngIf="showDailyForm()"
               class="glass-premium p-6 rounded-[2rem] border border-white/10 bg-white/[0.01] animate-in slide-in-from-top duration-300 space-y-4">
            
            <div class="text-xs text-white/50 border-b border-white/5 pb-2">
              "No necesitas mirar al techo en el sofá; aprovecha los tiempos muertos para hacer tu propia reunión de sincronización diaria"
            </div>

            <div class="grid grid-cols-1 sm:grid-cols-2 gap-4">
              <div>
                <label class="block text-[9px] uppercase tracking-widest text-white/40 font-black mb-2">Tu Nombre *</label>
                <input type="text"
                       class="w-full bg-black/40 border border-white/10 rounded-xl px-4 py-3 text-white/90 text-sm focus:outline-none focus:border-indigo-500/50 transition-all"
                       placeholder="Ej: Papá, William, Sofía..."
                       [value]="dailyForm().memberName"
                       (input)="patchDailyForm('memberName', $any($event.target).value)" />
              </div>
              
              <!-- Mood indicator -->
              <div>
                <label class="block text-[9px] uppercase tracking-widest text-white/40 font-black mb-2">¿Cómo te sientes emocionalmente? *</label>
                <div class="flex gap-2">
                  <button *ngFor="let mood of moods"
                          (click)="patchDailyForm('emotionalIndicator', mood.value)"
                          type="button"
                          class="flex-1 py-2 rounded-xl text-base border transition-all flex items-center justify-center"
                          [ngClass]="dailyForm().emotionalIndicator === mood.value
                            ? 'bg-indigo-600/20 border-indigo-500 text-white shadow-md'
                            : 'bg-black/20 border-white/5 text-white/40 hover:text-white/70 hover:border-white/10'">
                    {{ mood.icon }}
                  </button>
                </div>
              </div>
            </div>

            <!-- Daily Fields: Ayer, Hoy, Bloqueos -->
            <div class="space-y-4">
              <div>
                <label class="block text-[9px] uppercase tracking-widest text-white/40 font-black mb-2">😊 Ayer: ¿Qué logré avanzar ayer? *</label>
                <textarea rows="2"
                          class="w-full bg-black/40 border border-white/10 rounded-xl px-4 py-3 text-white/90 text-xs focus:outline-none focus:border-indigo-500/50 transition-all resize-none"
                          placeholder="Describe brevemente tus vivencias y misiones de ayer..."
                          [value]="dailyForm().yesterdayText"
                          (input)="patchDailyForm('yesterdayText', $any($event.target).value)"></textarea>
              </div>

              <div>
                <label class="block text-[9px] uppercase tracking-widest text-indigo-300 font-black mb-2">🎯 Hoy: ¿Qué me propondré hoy? *</label>
                <textarea rows="2"
                          class="w-full bg-black/40 border border-white/10 rounded-xl px-4 py-3 text-white/90 text-xs focus:outline-none focus:border-indigo-500/50 transition-all resize-none"
                          placeholder="Ej: Me propondré 1 hora de estudio enfocada en la situación problema al llegar a casa."
                          [value]="dailyForm().todayText"
                          (input)="patchDailyForm('todayText', $any($event.target).value)"></textarea>
              </div>

              <div class="grid grid-cols-1 sm:grid-cols-2 gap-4">
                <div>
                  <label class="block text-[9px] uppercase tracking-widest text-white/40 font-black mb-2">⚠️ Bloqueos: ¿Qué dificultades tuve? *</label>
                  <textarea rows="2"
                            class="w-full bg-black/40 border border-white/10 rounded-xl px-4 py-3 text-white/90 text-xs focus:outline-none focus:border-indigo-500/50 transition-all resize-none"
                            placeholder="¿Qué te detuvo o causó tensión?"
                            [value]="dailyForm().blockagesText"
                            (input)="patchDailyForm('blockagesText', $any($event.target).value)"></textarea>
                </div>

                <div>
                  <label class="block text-[9px] uppercase tracking-widest text-white/40 font-black mb-2">💡 ¿Cómo las resolveré? *</label>
                  <textarea rows="2"
                            class="w-full bg-black/40 border border-white/10 rounded-xl px-4 py-3 text-white/90 text-xs focus:outline-none focus:border-indigo-500/50 transition-all resize-none"
                            placeholder="Acción consciente para superar el bloqueo..."
                            [value]="dailyForm().resolutionText"
                            (input)="patchDailyForm('resolutionText', $any($event.target).value)"></textarea>
                </div>
              </div>
            </div>

            <!-- Submit Check-In -->
            <div class="flex justify-end">
              <button (click)="submitDaily()"
                      [disabled]="savingDaily() || !dailyForm().memberName.trim() || !dailyForm().yesterdayText.trim() || !dailyForm().todayText.trim()"
                      class="px-6 py-3 bg-indigo-600 hover:bg-indigo-500 disabled:opacity-30 disabled:cursor-not-allowed text-white font-black uppercase tracking-widest text-[10px] rounded-xl shadow-md transition-all hover:scale-105 active:scale-95 flex items-center gap-2">
                <span *ngIf="savingDaily()" class="w-3 h-3 border border-white/30 border-t-white rounded-full animate-spin"></span>
                💾 Guardar Check-In Diario
              </button>
            </div>
          </div>

          <!-- LOGGED DAILIES LIST -->
          <div class="space-y-4">
            <h4 class="text-[10px] uppercase tracking-widest text-white/30 font-black">Historial del Daily en el Sprint</h4>
            
            <div *ngIf="sprint.dailies.length === 0"
                 class="bg-white/[0.01] border border-white/5 rounded-2xl p-8 text-center text-xs text-white/30">
              Aún no se han registrado Check-Ins diarios para este Sprint. ¡Sé el primero en sincronizar tu conciencia familiar!
            </div>

            <div class="space-y-4" *ngIf="sprint.dailies.length > 0">
              <div *ngFor="let d of sprint.dailies"
                   class="bg-white/[0.02] border border-white/5 rounded-2xl p-5 space-y-4 transition-all hover:border-white/10">
                
                <!-- Daily Header -->
                <div class="flex justify-between items-center">
                  <div class="flex items-center gap-2">
                    <div class="w-8 h-8 rounded-xl bg-indigo-600/10 flex items-center justify-center text-base">
                      {{ getMoodIcon(d.emotionalIndicator) }}
                    </div>
                    <div>
                      <span class="block text-xs font-bold text-white/95">{{ d.memberName }}</span>
                      <span class="block text-[9px] text-white/30">{{ d.checkinDate | date:'dd MMM yyyy' }}</span>
                    </div>
                  </div>
                  <span class="px-2 py-0.5 bg-indigo-500/10 text-indigo-400 text-[8px] uppercase tracking-widest font-black rounded border border-indigo-500/20">
                    Daily
                  </span>
                </div>

                <!-- Daily Content -->
                <div class="grid grid-cols-1 md:grid-cols-3 gap-4 text-xs">
                  <div class="bg-black/20 p-3 rounded-xl border border-white/5 space-y-1">
                    <span class="block text-[8px] uppercase tracking-widest text-white/40 font-black">😊 Ayer</span>
                    <p class="text-white/80 leading-relaxed">{{ d.yesterdayText }}</p>
                  </div>
                  <div class="bg-indigo-950/20 p-3 rounded-xl border border-indigo-500/10 space-y-1">
                    <span class="block text-[8px] uppercase tracking-widest text-indigo-300 font-black">🎯 Hoy</span>
                    <p class="text-white/85 font-semibold leading-relaxed">{{ d.todayText }}</p>
                  </div>
                  <div class="bg-red-950/10 p-3 rounded-xl border border-red-500/10 space-y-1">
                    <span class="block text-[8px] uppercase tracking-widest text-red-400 font-black">⚠️ Bloqueo y Solución</span>
                    <p class="text-red-300/80 leading-relaxed mb-1"><span class="font-bold">Dif:</span> {{ d.blockagesText }}</p>
                    <p class="text-emerald-300/80 leading-relaxed"><span class="font-bold">Sol:</span> {{ d.resolutionText }}</p>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>

        <!-- RIGHT COLUMN: SPRINT MISSIONS -->
        <div class="space-y-6">
          <h3 class="text-lg font-black text-white/90 flex items-center gap-2">
            <span class="w-8 h-8 bg-indigo-500/10 rounded-lg flex items-center justify-center text-sm">📋</span>
            Misiones del Sprint
          </h3>

          <div class="glass-premium p-6 rounded-[2.5rem] border border-white/5 bg-white/[0.01] space-y-4">
            <div class="text-[9px] uppercase tracking-widest text-white/30 font-black border-b border-white/5 pb-2">
              Cumplimiento semanal consciente
            </div>

            <!-- Missions Progress Bar -->
            <div class="space-y-1.5 pt-2">
              <div class="flex justify-between text-[10px] font-bold text-white/70">
                <span>Avance Misiones</span>
                <span>{{ getCompletedMissionsCount(sprint.missions) }} de {{ sprint.missions.length }} ({{ getMissionsProgressPercent(sprint.missions) }}%)</span>
              </div>
              <div class="h-2 bg-white/5 rounded-full overflow-hidden">
                <div class="h-full bg-indigo-500 transition-all duration-500 rounded-full"
                     [style.width.%]="getMissionsProgressPercent(sprint.missions)"></div>
              </div>
            </div>

            <!-- Checklist -->
            <div class="space-y-3 pt-4">
              <div *ngFor="let m of sprint.missions"
                   (click)="toggleMission(sprint.id, m)"
                   class="flex items-start gap-3 p-3.5 rounded-xl border transition-all cursor-pointer select-none group"
                   [ngClass]="m.status === 'COMPLETED'
                     ? 'bg-emerald-500/5 border-emerald-500/25 text-white/50'
                     : 'bg-black/30 border-white/5 text-white/85 hover:border-white/10'">
                
                <div class="w-5 h-5 rounded-md border flex items-center justify-center text-[10px] font-black transition-all"
                     [ngClass]="m.status === 'COMPLETED'
                       ? 'bg-emerald-500 border-emerald-500 text-white'
                       : 'border-white/20 group-hover:border-indigo-400'">
                  {{ m.status === 'COMPLETED' ? '✓' : '' }}
                </div>
                
                <div class="flex-1 text-xs">
                  <p class="font-medium leading-relaxed"
                     [ngClass]="m.status === 'COMPLETED' ? 'line-through text-white/40' : ''">
                    {{ m.description }}
                  </p>
                  <span *ngIf="m.status === 'COMPLETED' && m.completedAt"
                        class="block text-[8px] text-emerald-500/60 mt-1 uppercase tracking-wider">
                    Cumplido el {{ m.completedAt | date:'dd MMM HH:mm' }}
                  </span>
                </div>
              </div>
            </div>
          </div>
        </div>

      </div>

      <!-- RETROSPECTIVA MODAL / POPUP OVERLAY -->
      <div *ngIf="showRetroForm()"
           class="fixed inset-0 z-50 bg-black/80 backdrop-blur-md flex items-center justify-center p-4 overflow-y-auto">
        
        <div class="glass-premium max-w-2xl w-full p-6 md:p-8 rounded-[2.5rem] border border-white/10 bg-slate-950 space-y-6 max-h-[90vh] overflow-y-auto animate-in scale-in duration-300">
          
          <div class="flex justify-between items-center border-b border-white/5 pb-4">
            <div class="flex items-center gap-3">
              <span class="text-2xl">🏁</span>
              <div>
                <h3 class="text-xl font-black text-white/95">Cerrar Sprint y Retrospectiva</h3>
                <p class="text-white/30 text-[9px] uppercase tracking-widest">Inspección y Adaptación Familiar Continua</p>
              </div>
            </div>
            <button (click)="closeRetrospectiveModal()"
                    class="w-8 h-8 rounded-full bg-white/5 hover:bg-white/10 flex items-center justify-center text-white/50 hover:text-white transition-colors">
              ✕
            </button>
          </div>

          <div class="space-y-6 text-xs">
            
            <!-- RANGES: METRICAS HUMANAS -->
            <div class="space-y-4">
              <h4 class="text-[9px] uppercase tracking-widest text-indigo-300 font-black border-b border-white/5 pb-1">Métricas Relacionales y Emocionales (Escala 1-10)</h4>
              
              <div class="grid grid-cols-1 sm:grid-cols-2 gap-4">
                <!-- Tension Level -->
                <div class="space-y-1">
                  <div class="flex justify-between font-bold text-white/70">
                    <span>Nivel de Tensión Relacional</span>
                    <span class="text-indigo-400">{{ retroForm().tensionLevel }} / 10</span>
                  </div>
                  <input type="range" min="1" max="10"
                         class="w-full h-1 bg-white/10 rounded-lg appearance-none cursor-pointer accent-indigo-500"
                         [value]="retroForm().tensionLevel"
                         (input)="patchRetroForm('tensionLevel', +$any($event.target).value)" />
                  <span class="block text-[8px] text-white/30">(1: Calma Absoluta, 10: Tensión/Discusión Alta)</span>
                </div>

                <!-- Mindful Compliance -->
                <div class="space-y-1">
                  <div class="flex justify-between font-bold text-white/70">
                    <span>Cumplimiento Consciente</span>
                    <span class="text-indigo-400">{{ retroForm().mindfulCompliance }} / 10</span>
                  </div>
                  <input type="range" min="1" max="10"
                         class="w-full h-1 bg-white/10 rounded-lg appearance-none cursor-pointer accent-indigo-500"
                         [value]="retroForm().mindfulCompliance"
                         (input)="patchRetroForm('mindfulCompliance', +$any($event.target).value)" />
                  <span class="block text-[8px] text-white/30">(Compromiso real y honesto con las micro-misiones)</span>
                </div>

                <!-- Shared Time -->
                <div class="space-y-1">
                  <div class="flex justify-between font-bold text-white/70">
                    <span>Tiempo Compartido de Calidad</span>
                    <span class="text-indigo-400">{{ retroForm().sharedTime }} / 10</span>
                  </div>
                  <input type="range" min="1" max="10"
                         class="w-full h-1 bg-white/10 rounded-lg appearance-none cursor-pointer accent-indigo-500"
                         [value]="retroForm().sharedTime"
                         (input)="patchRetroForm('sharedTime', +$any($event.target).value)" />
                  <span class="block text-[8px] text-white/30">(Cenas compartidas, paseos o charlas cara a cara)</span>
                </div>

                <!-- Positive Interactions -->
                <div class="space-y-1">
                  <div class="flex justify-between font-bold text-white/70">
                    <span>Interacciones Positivas</span>
                    <span class="text-indigo-400">{{ retroForm().positiveInteractions }} / 10</span>
                  </div>
                  <input type="range" min="1" max="10"
                         class="w-full h-1 bg-white/10 rounded-lg appearance-none cursor-pointer accent-indigo-500"
                         [value]="retroForm().positiveInteractions"
                         (input)="patchRetroForm('positiveInteractions', +$any($event.target).value)" />
                  <span class="block text-[8px] text-white/30">(Abrazos, elogios, escucha activa y sonrisas)</span>
                </div>
              </div>
            </div>

            <!-- QUESTIONS -->
            <div class="space-y-4">
              <h4 class="text-[9px] uppercase tracking-widest text-indigo-300 font-black border-b border-white/5 pb-1">Retrospectiva Familiar Reflexiva</h4>

              <div>
                <label class="block text-[9px] uppercase tracking-widest text-white/40 font-black mb-1.5">😊 ¿Qué mejoró en nuestra convivencia este sprint? *</label>
                <textarea rows="2"
                          class="w-full bg-black/40 border border-white/10 rounded-xl px-4 py-3 text-white/90 focus:outline-none focus:border-indigo-500/50 transition-all resize-none"
                          placeholder="¿Logramos reír más? ¿Nos sentimos más apoyados?"
                          [value]="retroForm().whatWentWell"
                          (input)="patchRetroForm('whatWentWell', $any($event.target).value)"></textarea>
              </div>

              <div>
                <label class="block text-[9px] uppercase tracking-widest text-white/40 font-black mb-1.5">⚠️ ¿Qué sigue siendo difícil en nuestra relación? *</label>
                <textarea rows="2"
                          class="w-full bg-black/40 border border-white/10 rounded-xl px-4 py-3 text-white/90 focus:outline-none focus:border-indigo-500/50 transition-all resize-none"
                          placeholder="¿Qué patrones destructivos aparecieron?"
                          [value]="retroForm().whatWasDifficult"
                          (input)="patchRetroForm('whatWasDifficult', $any($event.target).value)"></textarea>
              </div>

              <div class="grid grid-cols-1 sm:grid-cols-2 gap-4">
                <div>
                  <label class="block text-[9px] uppercase tracking-widest text-white/40 font-black mb-1.5">💡 ¿Qué aprendimos como familia? *</label>
                  <textarea rows="2"
                            class="w-full bg-black/40 border border-white/10 rounded-xl px-4 py-3 text-white/90 focus:outline-none focus:border-indigo-500/50 transition-all resize-none"
                            placeholder="¿Qué entendimos sobre el otro?"
                            [value]="retroForm().whatLearned"
                            (input)="patchRetroForm('whatLearned', $any($event.target).value)"></textarea>
                </div>

                <div>
                  <label class="block text-[9px] uppercase tracking-widest text-white/40 font-black mb-1.5">🛠️ ¿Qué debemos ajustar para el próximo Sprint? *</label>
                  <textarea rows="2"
                            class="w-full bg-black/40 border border-white/10 rounded-xl px-4 py-3 text-white/90 focus:outline-none focus:border-indigo-500/50 transition-all resize-none"
                            placeholder="Microcompromisos o cambios concretos..."
                            [value]="retroForm().whatToAdjust"
                            (input)="patchRetroForm('whatToAdjust', $any($event.target).value)"></textarea>
                </div>
              </div>
            </div>

            <!-- Submit Retro -->
            <div class="flex justify-end gap-3 pt-4 border-t border-white/5">
              <button (click)="closeRetrospectiveModal()"
                      class="px-5 py-3 bg-white/5 hover:bg-white/10 text-white/60 font-black uppercase tracking-widest text-[9px] rounded-xl transition-all">
                Cancelar
              </button>
              <button (click)="closeSprint()"
                      [disabled]="savingRetro() || !retroForm().whatWentWell.trim() || !retroForm().whatWasDifficult.trim()"
                      class="px-7 py-3 bg-emerald-600 hover:bg-emerald-500 disabled:opacity-30 disabled:cursor-not-allowed text-white font-black uppercase tracking-widest text-[9px] rounded-xl shadow-md transition-all hover:scale-105 active:scale-95 flex items-center gap-2">
                <span *ngIf="savingRetro()" class="w-3 h-3 border border-white/30 border-t-white rounded-full animate-spin"></span>
                ✓ Finalizar Sprint Familiar
              </button>
            </div>
          </div>

        </div>
      </div>

    </div>
  </div>


  <!-- ==================== TAB: HISTORIAL DE SPRINTS ==================== -->
  <div *ngIf="activeTab() === 'HISTORY'" class="space-y-8 animate-in fade-in duration-300">
    <div *ngIf="loadingHistory()" class="flex justify-center py-20">
      <div class="flex flex-col items-center gap-4">
        <div class="w-12 h-12 border-4 border-indigo-500/20 border-t-indigo-500 rounded-full animate-spin"></div>
        <p class="text-white/30 text-sm font-medium animate-pulse">Cargando historial de sprints...</p>
      </div>
    </div>

    <!-- Empty History -->
    <div *ngIf="!loadingHistory() && sprintHistory().length === 0"
         class="glass-premium p-16 rounded-[2rem] border border-white/5 text-center">
      <div class="text-6xl mb-4">📜</div>
      <p class="text-white/30 text-sm font-medium uppercase tracking-widest">
        Aún no hay sprints familiares finalizados en tu historial.
      </p>
      <button (click)="setTab('SPRINT')"
              class="mt-6 px-6 py-2.5 bg-indigo-600 text-white font-black text-xs uppercase tracking-widest rounded-xl transition-all shadow-md">
        Comenzar tu primer Sprint
      </button>
    </div>

    <!-- History List -->
    <div *ngIf="!loadingHistory() && sprintHistory().length > 0" class="space-y-6">
      <h3 class="text-lg font-black text-white/90">Sprints Históricos de la Familia</h3>

      <div class="space-y-6">
        <article *ngFor="let s of sprintHistory()"
                 class="glass-premium rounded-[2.5rem] border border-white/5 overflow-hidden p-6 lg:p-8 space-y-6">
          
          <!-- History Header -->
          <div class="flex flex-col md:flex-row justify-between items-start md:items-center gap-4 border-b border-white/5 pb-4">
            <div class="space-y-1">
              <div class="flex items-center gap-3">
                <span class="px-2 py-0.5 bg-white/5 text-white/50 text-[9px] uppercase tracking-widest font-black rounded border border-white/10">
                  Sprint finalizado
                </span>
                <span class="text-xs text-indigo-400 font-bold">
                  Dimensión: {{ getDimensionFriendlyName(s.riskDimension) }}
                </span>
              </div>
              <h4 class="text-lg font-black text-white/90">🎯 Objetivo: {{ s.objective }}</h4>
              <p class="text-white/30 text-[10px] uppercase font-bold">
                Ciclo de {{ s.durationDays }} días · {{ s.startDate | date:'dd MMM yyyy' }} al {{ s.endDate | date:'dd MMM yyyy' }}
              </p>
            </div>

            <!-- Consistencia Score -->
            <div class="bg-indigo-600/10 border border-indigo-500/20 px-5 py-3 rounded-2xl text-center min-w-[130px]"
                 *ngIf="s.retrospective">
              <span class="block text-3xl font-black text-indigo-400">{{ s.retrospective.consistencyScore }} / 10</span>
              <span class="text-[9px] uppercase tracking-widest text-white/40 font-black">Consistencia Evolutiva</span>
            </div>
          </div>

          <!-- History Content (if Retro exists) -->
          <div class="grid grid-cols-1 lg:grid-cols-[1fr_320px] gap-8" *ngIf="s.retrospective">
            
            <!-- Reflections & AI Feedback -->
            <div class="space-y-6">
              <!-- IA Feedback -->
              <div class="bg-indigo-950/10 border border-indigo-500/10 rounded-2xl p-6 space-y-2">
                <span class="text-[9px] uppercase tracking-widest text-indigo-400 font-black block">Análisis Clínico y Retroalimentación IA</span>
                <div class="markdown-content text-xs text-white/80 leading-relaxed"
                     [innerHTML]="formatAiResponse(s.retrospective.aiFeedback)">
                </div>
              </div>

              <!-- Retrospective answers -->
              <div class="grid grid-cols-1 sm:grid-cols-2 gap-4 text-xs">
                <div class="bg-white/[0.01] border border-white/5 p-4 rounded-xl space-y-1">
                  <span class="block text-[8px] uppercase tracking-widest text-emerald-400 font-black">😊 ¿Qué mejoró?</span>
                  <p class="text-white/70">{{ s.retrospective.whatWentWell }}</p>
                </div>
                <div class="bg-white/[0.01] border border-white/5 p-4 rounded-xl space-y-1">
                  <span class="block text-[8px] uppercase tracking-widest text-red-400 font-black">⚠️ ¿Qué fue difícil?</span>
                  <p class="text-white/70">{{ s.retrospective.whatWasDifficult }}</p>
                </div>
                <div class="bg-white/[0.01] border border-white/5 p-4 rounded-xl space-y-1">
                  <span class="block text-[8px] uppercase tracking-widest text-indigo-400 font-black">💡 Aprendizajes</span>
                  <p class="text-white/70">{{ s.retrospective.whatLearned }}</p>
                </div>
                <div class="bg-white/[0.01] border border-white/5 p-4 rounded-xl space-y-1">
                  <span class="block text-[8px] uppercase tracking-widest text-indigo-400 font-black">🛠️ Ajustes planificados</span>
                  <p class="text-white/70">{{ s.retrospective.whatToAdjust }}</p>
                </div>
              </div>
            </div>

            <!-- Relational Metrics Graph -->
            <div class="space-y-4 bg-white/[0.02] border border-white/5 p-5 rounded-2xl h-fit">
              <span class="text-[9px] uppercase tracking-widest text-white/30 font-black block">Métricas del Hito</span>
              
              <div class="space-y-3 text-xs">
                <!-- Tension -->
                <div class="space-y-1">
                  <div class="flex justify-between font-bold text-white/60">
                    <span>Tensión Relacional</span>
                    <span class="text-red-400">{{ s.retrospective.tensionLevel }} / 10</span>
                  </div>
                  <div class="h-1 bg-white/5 rounded-full overflow-hidden">
                    <div class="h-full bg-red-500 rounded-full" [style.width.%]="s.retrospective.tensionLevel * 10"></div>
                  </div>
                </div>

                <!-- Compliance -->
                <div class="space-y-1">
                  <div class="flex justify-between font-bold text-white/60">
                    <span>Cumplimiento Consciente</span>
                    <span class="text-emerald-400">{{ s.retrospective.mindfulCompliance }} / 10</span>
                  </div>
                  <div class="h-1 bg-white/5 rounded-full overflow-hidden">
                    <div class="h-full bg-emerald-500 rounded-full" [style.width.%]="s.retrospective.mindfulCompliance * 10"></div>
                  </div>
                </div>

                <!-- Shared Time -->
                <div class="space-y-1">
                  <div class="flex justify-between font-bold text-white/60">
                    <span>Tiempo de Calidad</span>
                    <span class="text-indigo-400">{{ s.retrospective.sharedTime }} / 10</span>
                  </div>
                  <div class="h-1 bg-white/5 rounded-full overflow-hidden">
                    <div class="h-full bg-indigo-500 rounded-full" [style.width.%]="s.retrospective.sharedTime * 10"></div>
                  </div>
                </div>

                <!-- Positive Interactions -->
                <div class="space-y-1">
                  <div class="flex justify-between font-bold text-white/60">
                    <span>Interacciones Positivas</span>
                    <span class="text-indigo-400">{{ s.retrospective.positiveInteractions }} / 10</span>
                  </div>
                  <div class="h-1 bg-white/5 rounded-full overflow-hidden">
                    <div class="h-full bg-indigo-500 rounded-full" [style.width.%]="s.retrospective.positiveInteractions * 10"></div>
                  </div>
                </div>
              </div>
            </div>

          </div>
        </article>
      </div>
    </div>
  </div>

</div>
  `
})
export class FamilyLogbookComponent implements OnInit {
  private readonly service         = inject(FamilyLogbookService);
  private readonly sprintService   = inject(SprintService);
  private readonly authService     = inject(AuthService);
  private readonly flow            = inject(TransformationFlowService);
  private readonly http            = inject(HttpClient);
  private readonly api             = inject(ApiService);
  private readonly router          = inject(Router);

  // ─── State ──────────────────────────────────────────────────────────────────
  private familyId = 0;
  private authorName = '';

  readonly activeTab       = signal<TabMode>('LOGBOOK');
  
  // Traditional Logbook Signals
  readonly entries         = signal<FamilyLogbookEntry[]>([]);
  readonly loading         = signal(false);
  readonly saving          = signal(false);
  readonly error           = signal('');
  readonly filter          = signal<FilterMode>('ALL');
  readonly showForm        = signal(false);
  readonly form            = signal<FormState>({ ...EMPTY_FORM });
  readonly resolveEvidence = signal<Record<number, string | undefined>>({});
  readonly correlation     = signal<any>(null);
  readonly loadingCorrelation = signal(false);

  // Agile Sprints Signals
  readonly activeSprint    = signal<SprintResponse | null>(null);
  readonly loadingSprint   = signal(false);
  readonly savingSprint    = signal(false);
  readonly sprintForm      = signal<SprintFormState>({ ...EMPTY_SPRINT_FORM });

  // ── Auto-sprint desde misión activa ────────────────────────────────────────
  /** Sugerencia de sprint pre-cargada desde la misión activa del plan */
  readonly sprintSuggestion = signal<{
    missionTitle: string;
    missionDescription: string;
    dimension: string;
    durationDays: number;
    missionTaskId: number;
  } | null>(null);
  readonly loadingSuggestion = signal(false);
  readonly suggestionDismissed = signal(false);
  
  readonly showDailyForm   = signal(false);
  readonly dailyForm       = signal<DailyFormState>({ ...EMPTY_DAILY_FORM });
  readonly savingDaily     = signal(false);

  readonly showRetroForm   = signal(false);
  readonly retroForm       = signal<RetroFormState>({ ...EMPTY_RETRO_FORM });
  readonly savingRetro     = signal(false);

  readonly sprintHistory   = signal<SprintResponse[]>([]);
  readonly loadingHistory  = signal(false);

  readonly moods = [
    { value: 'HAPPY', icon: '😊 Alegre' },
    { value: 'CALM', icon: '🧘 Calmo' },
    { value: 'TIRED', icon: '🥱 Cansado' },
    { value: 'STRESSED', icon: '😡 Estresado' }
  ];

  // ─── Computed ───────────────────────────────────────────────────────────────
  readonly openCount     = computed(() => this.entries().filter(e => e.status === 'OPEN').length);
  readonly resolvedCount = computed(() => this.entries().filter(e => e.status === 'RESOLVED').length);
  readonly allCount      = computed(() => this.entries().length);

  readonly filteredEntries = computed(() => {
    const f = this.filter();
    if (f === 'ALL') return this.entries();
    return this.entries().filter(e => e.status === f);
  });

  readonly filterTabs = [
    { value: 'ALL'      as FilterMode, label: 'Todas',    count: this.allCount      },
    { value: 'OPEN'     as FilterMode, label: 'Abiertas', count: this.openCount     },
    { value: 'RESOLVED' as FilterMode, label: 'Resueltas',count: this.resolvedCount }
  ];

  // ─── Init ───────────────────────────────────────────────────────────────────
  ngOnInit(): void {
    const user = this.authService.user();
    if (user?.familyId) {
      this.familyId   = user.familyId;
      this.authorName = user.fullName ?? '';
      this.form.update(f => ({ ...f, createdBy: this.authorName }));
      this.dailyForm.update(d => ({ ...d, memberName: this.authorName }));
      
      this.loadEntries();
      this.loadCorrelation();
      this.loadActiveSprint();
      // Si viene con ?tab=SPRINT en la URL (desde plan/misión), activar ese tab
      const urlParams = new URLSearchParams(window.location.search);
      if (urlParams.get('tab') === 'SPRINT') this.activeTab.set('SPRINT');
    } else {
      this.error.set('No se encontró una familia asociada a tu cuenta.');
    }
  }

  // ─── Tabs Switcher ─────────────────────────────────────────────────────────
  setTab(tab: TabMode): void {
    this.activeTab.set(tab);
    if (tab === 'LOGBOOK') {
      this.loadEntries();
    } else if (tab === 'SPRINT') {
      this.loadActiveSprint();
    } else if (tab === 'HISTORY') {
      this.loadSprintHistory();
    }
  }

  // ─── traditional Logbook Actions ──────────────────────────────────────────
  loadEntries(): void {
    this.loading.set(true);
    this.error.set('');

    this.service.findByFamily(this.familyId).subscribe({
      next: list => { this.entries.set(list); this.loading.set(false); },
      error: ()   => { this.error.set('No fue posible cargar la bitácora familiar.'); this.loading.set(false); }
    });
  }

  loadCorrelation(): void {
    if (!this.familyId) return;
    this.loadingCorrelation.set(true);
    this.service.getCorrelation(this.familyId).subscribe({
      next: res  => { this.correlation.set(res?.data ?? null); this.loadingCorrelation.set(false); },
      error: ()  => this.loadingCorrelation.set(false)
    });
  }

  toggleForm(): void {
    this.showForm.update(v => !v);
    if (!this.showForm()) this.form.set({ ...EMPTY_FORM, createdBy: this.authorName });
  }

  setFilter(f: FilterMode): void { this.filter.set(f); }

  patchForm(field: keyof FormState, value: string): void {
    this.form.update(f => ({ ...f, [field]: value }));
  }

  patchEvidence(id: number, value: string): void {
    this.resolveEvidence.update(r => ({ ...r, [id]: value }));
  }

  createEntry(): void {
    const f = this.form();
    if (!f.situation.trim() || !f.difficultyDetected.trim() || !f.emotionIdentified.trim()
        || !f.understanding.trim() || !f.correctionAction.trim() || !f.familyAgreement.trim()) {
      this.error.set('Todos los campos marcados con * son obligatorios.');
      return;
    }

    const request: CreateFamilyLogbookEntryRequest = {
      familyId:          this.familyId,
      situation:         f.situation,
      difficultyDetected: f.difficultyDetected,
      emotionIdentified: f.emotionIdentified,
      understanding:     f.understanding,
      correctionAction:  f.correctionAction,
      familyAgreement:   f.familyAgreement,
      createdBy:         f.createdBy || this.authorName
    };

    this.saving.set(true);
    this.error.set('');

    this.service.create(request).subscribe({
      next: () => {
        this.saving.set(false);
        this.showForm.set(false);
        this.form.set({ ...EMPTY_FORM, createdBy: this.authorName });
        this.loadEntries();
        this.loadCorrelation();
      },
      error: () => {
        this.error.set('No fue posible crear la entrada de bitácora.');
        this.saving.set(false);
      }
    });
  }

  resolveEntry(entry: FamilyLogbookEntry): void {
    const evidence = (this.resolveEvidence()[entry.id] ?? '').trim();
    if (!evidence) {
      this.error.set('La evidencia de avance es obligatoria para cerrar la entrada.');
      return;
    }

    this.saving.set(true);
    this.error.set('');

    this.service.resolve(entry.id, {
      progressEvidence: evidence,
      resolvedBy: this.authorName || 'Familia'
    }).subscribe({
      next: () => {
        this.saving.set(false);
        this.patchEvidence(entry.id, '');
        this.loadEntries();
        this.loadCorrelation();
      },
      error: () => {
        this.error.set('No fue posible cerrar la entrada.');
        this.saving.set(false);
      }
    });
  }

  trackById(_: number, entry: FamilyLogbookEntry): number {
    return entry.id;
  }

  /** Navega a la sección de Evidencias con el task ID de la misión del sprint activo */
  goToEvidence(): void {
    const sprint = this.activeSprint();
    const taskId = sprint ? (sprint as any).missionTaskId : null;
    if (taskId) {
      this.router.navigate(['/plans/mission', taskId]);
    } else {
      this.router.navigate(['/checklist']);
    }
  }

  /** Navega a la ruta de 36 meses */
  goToTransformationRoute(): void {
    this.router.navigate(['/transformation/route']);
  }

  // ─── Agile Sprints Actions ────────────────────────────────────────────────
  loadActiveSprint(): void {
    if (!this.familyId) return;
    this.loadingSprint.set(true);
    this.error.set('');
    this.sprintService.getActiveSprint(this.familyId).subscribe({
      next: res => {
        this.activeSprint.set(res);
        this.loadingSprint.set(false);
        if (res) {
          // Sprint activo encontrado → mostrar tab SPRINT automáticamente
          // (solo si no vino redirigido expresamente a LOGBOOK)
          const urlParams = new URLSearchParams(window.location.search);
          if (!urlParams.get('tab')) this.activeTab.set('SPRINT');
          // Actualizar número de sprint en el flujo de transformación
          this.flow.setSprint((res as any).id ?? 1);
        } else {
          // Sin sprint → intentar sugerir desde misión activa
          this.tryLoadMissionSuggestion();
        }
      },
      error: () => {
        this.error.set('No se pudo cargar el sprint activo.');
        this.loadingSprint.set(false);
        this.tryLoadMissionSuggestion();
      }
    });
  }

  loadSprintHistory(): void {
    if (!this.familyId) return;
    this.loadingHistory.set(true);
    this.error.set('');
    this.sprintService.getSprintHistory(this.familyId).subscribe({
      next: res => {
        this.sprintHistory.set(res);
        this.loadingHistory.set(false);
      },
      error: () => {
        this.error.set('No se pudo cargar el historial de sprints.');
        this.loadingHistory.set(false);
      }
    });
  }

  patchSprintForm(field: keyof SprintFormState, value: any): void {
    this.sprintForm.update(f => ({ ...f, [field]: value }));
  }

  addSprintMission(): void {
    const desc = this.sprintForm().newMission.trim();
    if (desc) {
      this.sprintForm.update(f => ({
        ...f,
        missions: [...f.missions, desc],
        newMission: ''
      }));
    }
  }

  removeSprintMission(idx: number): void {
    this.sprintForm.update(f => ({
      ...f,
      missions: f.missions.filter((_, i) => i !== idx)
    }));
  }

  /**
   * Intenta cargar la misión activa del plan y pre-rellenar la sugerencia de sprint.
   * Solo actúa si hay un activeMissionId en TransformationFlowService y
   * la sugerencia no fue descartada por el usuario en esta sesión.
   */
  private tryLoadMissionSuggestion(): void {
    if (this.suggestionDismissed()) return;
    const missionId = this.flow.activeMissionId();
    if (!missionId) return;

    this.loadingSuggestion.set(true);
    this.http.get<any>(`${this.api.base}/plans/tasks/${missionId}`)
      .pipe(catchError(() => of(null)))
      .subscribe(res => {
        this.loadingSuggestion.set(false);
        const task = res?.data ?? res;
        if (!task?.title) return;

        // Mapear dimensión del task al riskDimension del sprint
        const dim = (task.dimension ?? 'comunicacion').toLowerCase();

        this.sprintSuggestion.set({
          missionTitle:       task.title,
          missionDescription: task.description ?? task.objective ?? task.title,
          dimension:          dim,
          durationDays:       task.sprintDays ?? 7,
          missionTaskId:      Number(missionId),
        });

        // Pre-rellenar el formulario de sprint
        this.sprintForm.update(f => ({
          ...f,
          objective:     `Misión: ${task.title}`,
          riskDimension: dim,
          durationDays:  task.sprintDays ?? 7,
          missions:      task.description ? [task.description] : [task.title],
        }));
      });
  }

  /** El usuario acepta la sugerencia → cambia a tab SPRINT y hace scroll al form */
  acceptSuggestion(): void {
    this.activeTab.set('SPRINT');
    this.suggestionDismissed.set(true);
  }

  /** El usuario descarta la sugerencia sin crear sprint */
  dismissSuggestion(): void {
    this.suggestionDismissed.set(true);
    this.sprintSuggestion.set(null);
    this.sprintForm.set({ ...EMPTY_SPRINT_FORM });
  }

  /**
   * Crea el sprint directamente desde la sugerencia (un solo clic).
   * Una vez creado, limpia la sugerencia y activa el tab SPRINT.
   */
  acceptAndCreateSprint(): void {
    const suggestion = this.sprintSuggestion();
    if (!suggestion) return;

    this.savingSprint.set(true);
    this.error.set('');

    const req: CreateSprintRequest = {
      objective:     `Misión: ${suggestion.missionTitle}`,
      riskDimension: suggestion.dimension,
      durationDays:  suggestion.durationDays,
      missions:      [suggestion.missionDescription],
      missionTaskId: suggestion.missionTaskId,
    };

    this.sprintService.createSprint(this.familyId, req).subscribe({
      next: res => {
        this.activeSprint.set(res);
        this.savingSprint.set(false);
        this.sprintSuggestion.set(null);
        this.suggestionDismissed.set(true);
        // Actualizar número de sprint en el flujo de transformación
        this.flow.setSprint((res as any).sprintNumber ?? 1);
        // Cambiar a tab SPRINT para mostrar el sprint recién creado
        this.activeTab.set('SPRINT');
      },
      error: err => {
        this.error.set(err?.error?.message || 'Error al crear el Sprint.');
        this.savingSprint.set(false);
      }
    });
  }

  createSprint(): void {
    const f = this.sprintForm();
    if (!f.objective.trim() || f.missions.length === 0) {
      this.error.set('Por favor, indica un objetivo e incorpora al menos una misión.');
      return;
    }

    this.savingSprint.set(true);
    this.error.set('');

    const req: CreateSprintRequest = {
      objective: f.objective,
      riskDimension: f.riskDimension,
      durationDays: f.durationDays,
      missions: f.missions
    };

    this.sprintService.createSprint(this.familyId, req).subscribe({
      next: res => {
        this.activeSprint.set(res);
        this.savingSprint.set(false);
        this.sprintForm.set({ ...EMPTY_SPRINT_FORM });
      },
      error: err => {
        this.error.set(err?.error?.message || 'Error al iniciar el Sprint.');
        this.savingSprint.set(false);
      }
    });
  }

  toggleMission(sprintId: number, mission: SprintMissionResponse): void {
    this.sprintService.toggleMission(sprintId, mission.id).subscribe({
      next: res => this.activeSprint.set(res),
      error: () => this.error.set('No se pudo cambiar el estado de la misión.')
    });
  }

  toggleDailyForm(): void {
    this.showDailyForm.update(v => !v);
    if (!this.showDailyForm()) {
      this.dailyForm.set({ ...EMPTY_DAILY_FORM, memberName: this.authorName });
    }
  }

  patchDailyForm(field: keyof DailyFormState, value: string): void {
    this.dailyForm.update(d => ({ ...d, [field]: value }));
  }

  submitDaily(): void {
    const active = this.activeSprint();
    if (!active) return;

    const d = this.dailyForm();
    if (!d.memberName.trim() || !d.yesterdayText.trim() || !d.todayText.trim()) {
      this.error.set('Por favor completa todos los campos del Daily.');
      return;
    }

    this.savingDaily.set(true);
    this.error.set('');

    const req: CreateDailyCheckinRequest = {
      yesterdayText: d.yesterdayText,
      todayText: d.todayText,
      blockagesText: d.blockagesText || 'Ninguno',
      resolutionText: d.resolutionText || 'Ninguna',
      emotionalIndicator: d.emotionalIndicator,
      memberName: d.memberName
    };

    this.sprintService.submitDaily(active.id, req).subscribe({
      next: () => {
        this.savingDaily.set(false);
        this.showDailyForm.set(false);
        this.dailyForm.set({ ...EMPTY_DAILY_FORM, memberName: this.authorName });
        this.loadActiveSprint();
      },
      error: err => {
        this.error.set(err?.error?.message || 'Ya registraste tu Daily el día de hoy.');
        this.savingDaily.set(false);
      }
    });
  }

  openRetrospectiveModal(): void {
    this.retroForm.set({ ...EMPTY_RETRO_FORM });
    this.showRetroForm.set(true);
  }

  closeRetrospectiveModal(): void {
    this.showRetroForm.set(false);
  }

  patchRetroForm(field: keyof RetroFormState, value: any): void {
    this.retroForm.update(r => ({ ...r, [field]: value }));
  }

  closeSprint(): void {
    const active = this.activeSprint();
    if (!active) return;

    const r = this.retroForm();
    if (!r.whatWentWell.trim() || !r.whatWasDifficult.trim()) {
      this.error.set('Por favor, indica lo que salió bien y las dificultades experimentadas.');
      return;
    }

    this.savingRetro.set(true);
    this.error.set('');

    const req: CloseSprintRequest = {
      whatWentWell: r.whatWentWell,
      whatWasDifficult: r.whatWasDifficult,
      whatLearned: r.whatLearned || 'Aprendimos el valor de la comunicación diaria.',
      whatToAdjust: r.whatToAdjust || 'Ajustaremos nuestros horarios.',
      tensionLevel: r.tensionLevel,
      mindfulCompliance: r.mindfulCompliance,
      sharedTime: r.sharedTime,
      positiveInteractions: r.positiveInteractions,
      emotionalPersistence: r.emotionalPersistence
    };

    this.sprintService.closeSprint(active.id, req).subscribe({
      next: () => {
        this.savingRetro.set(false);
        this.showRetroForm.set(false);
        this.activeSprint.set(null);
        this.retroForm.set({ ...EMPTY_RETRO_FORM });
        // Sprint completado → avanzar sprint en el flujo + ir a Gratitud
        this.flow.setSprint(this.flow.currentSprintNumber() + 1);
        this.loadSprintHistory();
        // Redirigir al Muro de Gratitud para cerrar el ciclo
        setTimeout(() => this.router.navigate(['/gratitude'], {
          state: { sprintCompleted: true }
        }), 400);
      },
      error: () => {
        this.error.set('Error al cerrar el sprint familiar.');
        this.savingRetro.set(false);
      }
    });
  }

  // ─── UI Helpers ────────────────────────────────────────────────────────────
  getDimensionFriendlyName(dim: string): string {
    switch (dim?.toLowerCase()) {
      case 'comunicacion': return 'Comunicación Asertiva';
      case 'emociones': return 'Regulación & Clima Emocional';
      case 'habitos': return 'Hábitos & Convivencia Colectiva';
      case 'tiempos': return 'Tiempos de Conexión Activa';
      default: return dim || 'Comunicación';
    }
  }

  getDaysRemaining(endDateStr: string): number {
    if (!endDateStr) return 0;
    const end = new Date(endDateStr);
    const now = new Date();
    const diff = end.getTime() - now.getTime();
    return Math.max(0, Math.ceil(diff / (1000 * 3600 * 24)));
  }

  getMoodIcon(indicator?: string): string {
    switch (indicator) {
      case 'HAPPY': return '😊';
      case 'CALM': return '🧘';
      case 'TIRED': return '🥱';
      case 'STRESSED': return '😡';
      default: return '😊';
    }
  }

  getCompletedMissionsCount(missions: SprintMissionResponse[]): number {
    return missions.filter(m => m.status === 'COMPLETED').length;
  }

  getMissionsProgressPercent(missions: SprintMissionResponse[]): number {
    if (!missions || missions.length === 0) return 0;
    const completed = this.getCompletedMissionsCount(missions);
    return Math.round((completed / missions.length) * 100);
  }

  formatAiResponse(text: string): string {
    if (!text) return '';
    return text
      .replace(/^### (.*)/gm, '<h4>$1</h4>')
      .replace(/^## (.*)/gm,  '<h3>$1</h3>')
      .replace(/^# (.*)/gm,   '<h2>$1</h2>')
      .replace(/\*\*(.*?)\*\*/g, '<strong>$1</strong>')
      .replace(/\*(.*?)\*/g,   '<em>$1</em>')
      .replace(/^\s*[-*]\s+(.*)/gm, '<li>$1</li>')
      .replace(/^\s*\d+\.\s+(.*)/gm, '<li>$1</li>')
      .replace(/\n/g, '<br>');
  }
}
