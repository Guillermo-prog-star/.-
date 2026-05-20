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

const EMPTY_FORM: FormState = {
  situation: '',
  difficultyDetected: '',
  emotionIdentified: '',
  understanding: '',
  correctionAction: '',
  familyAgreement: '',
  createdBy: ''
};

/**
 * SDD: Bitácora de Transformación Familiar.
 * Rediseño Premium v2 — OnPush + Signals + Tailwind inline.
 */
@Component({
  selector: 'app-family-logbook',
  standalone: true,
  imports: [CommonModule, RouterLink],
  changeDetection: ChangeDetectionStrategy.OnPush,
  styles: [`
    .markdown-content ::ng-deep h3 { color: #60a5fa; font-size: 1.1rem; font-weight: 800; margin: 1.25rem 0 0.5rem; }
    .markdown-content ::ng-deep h4 { color: #93c5fd; font-size: 1rem; font-weight: 700; margin: 1rem 0 0.4rem; }
    .markdown-content ::ng-deep strong { color: #60a5fa; }
    .markdown-content ::ng-deep p { margin: 0 0 0.75rem; }
    .markdown-content ::ng-deep li { margin-left: 1.25rem; margin-bottom: 4px; }
  `],
  template: `
<div class="min-h-screen p-6 lg:p-12 space-y-8">

  <!-- HEADER -->
  <header class="flex flex-col md:flex-row justify-between items-start md:items-center gap-6">
    <div>
      <a routerLink="/dashboard"
         class="inline-flex items-center gap-2 text-white/30 hover:text-white/70 text-[11px] uppercase tracking-widest font-black mb-3 transition-colors">
        ← Dashboard
      </a>
      <h1 class="text-4xl md:text-5xl font-bold bg-clip-text text-transparent bg-gradient-to-r from-white to-white/40 tracking-tight">
        Bitácora Familiar
      </h1>
      <p class="text-white/40 font-medium tracking-widest uppercase text-[10px] mt-2">
        Registrar · Comprender · Corregir · Evidenciar
      </p>
    </div>

    <!-- Stats -->
    <div class="flex gap-4">
      <div class="glass-premium px-5 py-3 rounded-2xl text-center border border-white/5">
        <span class="block text-2xl font-black text-white/90">{{ entries().length }}</span>
        <span class="text-[9px] uppercase tracking-widest text-white/30">Total</span>
      </div>
      <div class="glass-premium px-5 py-3 rounded-2xl text-center border border-amber-500/20">
        <span class="block text-2xl font-black text-amber-400">{{ openCount() }}</span>
        <span class="text-[9px] uppercase tracking-widest text-amber-500/60">Abiertas</span>
      </div>
      <div class="glass-premium px-5 py-3 rounded-2xl text-center border border-emerald-500/20">
        <span class="block text-2xl font-black text-emerald-400">{{ resolvedCount() }}</span>
        <span class="text-[9px] uppercase tracking-widest text-emerald-500/60">Resueltas</span>
      </div>
    </div>
  </header>

  <!-- ERROR BANNER -->
  <div *ngIf="error()"
       class="glass-premium border border-red-500/30 bg-red-500/5 p-4 rounded-2xl flex items-center gap-3 text-red-400 text-sm font-medium">
    <span class="text-xl">⚠️</span>
    {{ error() }}
  </div>

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
      {{ showForm() ? '✕ Cancelar' : '+ Nueva Entrada' }}
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
    <!-- Filter pills -->
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
        <p class="text-white/30 text-sm font-medium animate-pulse">Cargando bitácora familiar...</p>
      </div>
    </div>

    <!-- Empty state -->
    <div *ngIf="!loading() && filteredEntries().length === 0"
         class="glass-premium p-16 rounded-[2rem] border border-white/5 text-center">
      <div class="text-6xl mb-4">📔</div>
      <p class="text-white/30 text-sm font-medium uppercase tracking-widest">
        {{ filter() === 'ALL' ? 'Sin entradas registradas todavía' : 'Sin entradas ' + (filter() === 'OPEN' ? 'abiertas' : 'resueltas') }}
      </p>
      <button *ngIf="filter() === 'ALL'" (click)="toggleForm()"
              class="mt-6 px-6 py-2.5 bg-indigo-600/20 hover:bg-indigo-600/40 text-indigo-400 font-black text-xs uppercase tracking-widest rounded-xl border border-indigo-500/20 transition-all">
        Crear primera entrada
      </button>
    </div>

    <!-- Entry cards -->
    <div *ngIf="!loading()" class="grid grid-cols-1 xl:grid-cols-2 gap-5">
      <article *ngFor="let entry of filteredEntries(); trackBy: trackById"
               class="glass-premium rounded-[2rem] border overflow-hidden transition-all duration-300 group"
               [ngClass]="entry.status === 'OPEN'
                 ? 'border-amber-500/15 hover:border-amber-500/30'
                 : 'border-emerald-500/15 hover:border-emerald-500/25'">

        <!-- Card header -->
        <div class="p-6 pb-0 flex items-start justify-between gap-4">
          <div class="flex items-center gap-3">
            <div class="w-9 h-9 rounded-2xl flex items-center justify-center text-lg flex-shrink-0"
                 [ngClass]="entry.status === 'OPEN' ? 'bg-amber-500/10' : 'bg-emerald-500/10'">
              {{ entry.status === 'OPEN' ? '🚨' : '✅' }}
            </div>
            <div>
              <span class="px-2.5 py-1 rounded-full text-[9px] font-black uppercase tracking-wider"
                    [ngClass]="entry.status === 'OPEN'
                      ? 'bg-amber-500/10 text-amber-400 border border-amber-500/20'
                      : 'bg-emerald-500/10 text-emerald-400 border border-emerald-500/20'">
                {{ entry.status === 'OPEN' ? 'Abierta' : 'Resuelta' }}
              </span>
            </div>
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

        <!-- Situation headline -->
        <div class="px-6 pt-4 pb-2">
          <p class="text-white/85 font-semibold text-sm leading-relaxed line-clamp-2 group-hover:line-clamp-none transition-all">
            {{ entry.situation }}
          </p>
        </div>

        <!-- Collapsible details -->
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

            <!-- Resolution evidence -->
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
  `
})
export class FamilyLogbookComponent implements OnInit {
  private readonly service    = inject(FamilyLogbookService);
  private readonly authService = inject(AuthService);

  // ─── State ──────────────────────────────────────────────────────────────────
  private familyId = 0;
  private authorName = '';

  readonly entries         = signal<FamilyLogbookEntry[]>([]);
  readonly loading         = signal(false);
  readonly saving          = signal(false);
  readonly error           = signal('');
  readonly filter          = signal<FilterMode>('ALL');
  readonly showForm        = signal(false);
  readonly form            = signal<FormState>({ ...EMPTY_FORM });
  readonly resolveEvidence = signal<Record<number, string>>({});
  readonly correlation     = signal<any>(null);
  readonly loadingCorrelation = signal(false);

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
      this.loadEntries();
      this.loadCorrelation();
    } else {
      this.error.set('No se encontró una familia asociada a tu cuenta.');
    }
  }

  // ─── Actions ────────────────────────────────────────────────────────────────
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
