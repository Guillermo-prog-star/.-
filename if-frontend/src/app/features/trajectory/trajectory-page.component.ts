import { Component, OnInit, inject, signal, computed } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { forkJoin, catchError, of } from 'rxjs';
import { TrajectoryService } from '../../core/services/trajectory.service';
import { FamilyStateService } from '../../core/services/family-state.service';
import {
  TrajectoryBankItem,
  FamilyTrajectoryDto,
  TrajectoryTimelineDto,
  TrajectoryImpactDto,
  TrajectoryStatus,
  SeverityLevel,
  TimelineEventRequest,
  IndicatorRequest,
  MACRODOMAIN_LABELS,
  SEVERITY_CONFIG,
  STATUS_CONFIG,
} from '../../core/models/trajectory.model';

type Tab = 'bank' | 'family' | 'impact';

@Component({
  selector: 'app-trajectory-page',
  standalone: true,
  imports: [CommonModule, FormsModule],
  template: `
    <div class="min-h-screen bg-gradient-to-br from-slate-900 via-blue-950 to-slate-900 text-white p-4 md:p-6">

      <!-- Header -->
      <div class="mb-6">
        <h1 class="text-2xl font-bold text-white mb-1">Banco de Trayectorias de Riesgo</h1>
        <p class="text-slate-400 text-sm">Seguimiento longitudinal de situaciones de riesgo familiar</p>
      </div>

      <!-- Tabs -->
      <div class="flex gap-2 mb-6 border-b border-white/10 pb-2">
        <button *ngFor="let tab of tabs" (click)="activeTab.set(tab.key)"
          class="px-4 py-2 rounded-t text-sm font-medium transition-colors"
          [class.bg-blue-600]="activeTab() === tab.key"
          [class.text-white]="activeTab() === tab.key"
          [class.text-slate-400]="activeTab() !== tab.key"
          [class.hover:text-white]="activeTab() !== tab.key">
          {{ tab.label }}
        </button>
      </div>

      <!-- Loading -->
      <div *ngIf="loading()" class="flex items-center justify-center py-20">
        <div class="animate-spin rounded-full h-8 w-8 border-b-2 border-blue-400"></div>
      </div>

      <!-- TAB: Banco de Trayectorias -->
      <div *ngIf="!loading() && activeTab() === 'bank'">
        <div class="text-slate-400 text-sm mb-4">
          {{ totalTrajectories() }} trayectorias en {{ macrodomainsCount() }} macrodominios
        </div>

        <div *ngFor="let domain of macrodomainsEntries()" class="mb-4">
          <button (click)="toggleDomain(domain[0])"
            class="w-full flex items-center justify-between p-3 bg-white/5 hover:bg-white/10 rounded-lg transition-colors">
            <span class="font-semibold text-white">{{ domainLabel(domain[0]) }}</span>
            <div class="flex items-center gap-2">
              <span class="text-xs text-slate-400">{{ domain[1].length }} trayectorias</span>
              <span class="text-slate-400">{{ expandedDomains().has(domain[0]) ? '▲' : '▼' }}</span>
            </div>
          </button>

          <div *ngIf="expandedDomains().has(domain[0])" class="mt-2 grid grid-cols-1 md:grid-cols-2 gap-3">
            <div *ngFor="let traj of domain[1]"
              class="p-4 bg-white/5 rounded-lg border border-white/10 hover:border-white/20 transition-all">
              <div class="flex items-start justify-between mb-2">
                <h3 class="font-medium text-white text-sm">{{ traj.name }}</h3>
                <span class="text-xs px-2 py-0.5 rounded-full ml-2 flex-shrink-0"
                  [class]="severityBadge(traj.severityDefault)">
                  {{ severityLabel(traj.severityDefault) }}
                </span>
              </div>
              <p class="text-slate-400 text-xs mb-3">{{ traj.description }}</p>

              <div *ngIf="parseSignals(traj.earlySignals).length > 0" class="mb-3">
                <p class="text-xs text-slate-500 mb-1 font-medium">Señales tempranas:</p>
                <div class="flex flex-wrap gap-1">
                  <span *ngFor="let s of parseSignals(traj.earlySignals)"
                    class="text-xs bg-yellow-900/30 text-yellow-300 px-2 py-0.5 rounded">{{ s }}</span>
                </div>
              </div>

              <button (click)="openAssignModal(traj)"
                class="w-full mt-2 py-1.5 text-xs bg-blue-600/30 hover:bg-blue-600/50 text-blue-300 rounded transition-colors">
                + Asignar a esta familia
              </button>
            </div>
          </div>
        </div>
      </div>

      <!-- TAB: Trayectorias de la Familia -->
      <div *ngIf="!loading() && activeTab() === 'family'">
        <div *ngIf="familyTrajectories().length === 0"
          class="text-center py-16 text-slate-400">
          <p class="text-4xl mb-3">🗺️</p>
          <p class="text-lg mb-1">Sin trayectorias registradas</p>
          <p class="text-sm">Asigna trayectorias desde el Banco para comenzar el seguimiento.</p>
        </div>

        <div class="space-y-3">
          <div *ngFor="let ft of familyTrajectories()"
            class="p-4 bg-white/5 rounded-xl border border-white/10">
            <div class="flex items-start justify-between mb-3">
              <div>
                <h3 class="font-semibold text-white">{{ ft.trajectory.name }}</h3>
                <p class="text-xs text-slate-500 mt-0.5">
                  {{ domainLabel(ft.trajectory.macrodomain) }} •
                  Detectado {{ formatDate(ft.detectedAt) }}
                </p>
              </div>
              <div class="flex items-center gap-2">
                <span class="text-xs font-medium" [class]="statusColor(ft.status)">
                  {{ statusLabel(ft.status) }}
                </span>
                <span class="text-xs px-2 py-0.5 rounded-full"
                  [class]="severityBadge(ft.trajectory.severityDefault)">
                  {{ severityLabel(ft.trajectory.severityDefault) }}
                </span>
              </div>
            </div>

            <p *ngIf="ft.notes" class="text-slate-400 text-xs mb-3">{{ ft.notes }}</p>

            <!-- Status update buttons -->
            <div class="flex gap-2 flex-wrap mb-3">
              <button *ngFor="let s of availableStatuses(ft.status)"
                (click)="changeStatus(ft, s)"
                class="text-xs px-3 py-1 rounded bg-white/10 hover:bg-white/20 text-slate-300 transition-colors">
                → {{ statusLabel(s) }}
              </button>
            </div>

            <!-- Actions -->
            <div class="flex gap-2">
              <button (click)="viewTimeline(ft)"
                class="text-xs px-3 py-1.5 bg-blue-600/30 hover:bg-blue-600/50 text-blue-300 rounded transition-colors">
                📅 Línea de tiempo
              </button>
              <button (click)="viewImpact(ft)"
                class="text-xs px-3 py-1.5 bg-purple-600/30 hover:bg-purple-600/50 text-purple-300 rounded transition-colors">
                📊 Indicadores
              </button>
            </div>
          </div>
        </div>
      </div>

      <!-- TAB: Impacto Global -->
      <div *ngIf="!loading() && activeTab() === 'impact'">
        <div *ngIf="familyTrajectories().length === 0"
          class="text-center py-16 text-slate-400">
          <p class="text-4xl mb-3">📈</p>
          <p>Asigna trayectorias y registra indicadores para ver el impacto.</p>
        </div>
        <div *ngIf="familyTrajectories().length > 0" class="space-y-4">
          <div *ngFor="let ft of familyTrajectories()"
            class="p-4 bg-white/5 rounded-xl border border-white/10">
            <div class="flex items-center justify-between mb-3">
              <h3 class="font-medium text-white text-sm">{{ ft.trajectory.name }}</h3>
              <span class="text-xs" [class]="statusColor(ft.status)">{{ statusLabel(ft.status) }}</span>
            </div>
            <div *ngIf="impactMap()[ft.id]?.length; else noIndicators">
              <div *ngFor="let ind of impactMap()[ft.id]"
                class="flex items-center justify-between py-1.5 border-b border-white/5 last:border-0">
                <span class="text-xs text-slate-300">{{ ind.indicatorName }}</span>
                <div class="flex items-center gap-2">
                  <span *ngIf="ind.baselineValue !== null" class="text-xs text-slate-500">
                    {{ ind.baselineValue }}{{ ind.unit }}
                  </span>
                  <span class="text-xs">→</span>
                  <span class="text-xs font-medium" [class]="impactColor(ind)">
                    {{ ind.currentValue }}{{ ind.unit }}
                  </span>
                  <span *ngIf="ind.improvementPct !== null"
                    class="text-xs px-1.5 py-0.5 rounded"
                    [class]="ind.improvementPct! >= 0 ? 'bg-green-900/30 text-green-400' : 'bg-red-900/30 text-red-400'">
                    {{ ind.improvementPct! >= 0 ? '+' : '' }}{{ ind.improvementPct!.toFixed(1) }}%
                  </span>
                </div>
              </div>
            </div>
            <ng-template #noIndicators>
              <p class="text-xs text-slate-500 py-2">Sin indicadores registrados.
                <button (click)="openIndicatorModal(ft)"
                  class="text-blue-400 hover:text-blue-300 ml-1">+ Agregar</button>
              </p>
            </ng-template>
            <button (click)="openIndicatorModal(ft)"
              class="mt-2 text-xs text-blue-400 hover:text-blue-300">
              + Agregar indicador
            </button>
          </div>
        </div>
      </div>

    </div>

    <!-- Modal: Asignar trayectoria -->
    <div *ngIf="assignModal()" class="fixed inset-0 bg-black/60 z-50 flex items-center justify-center p-4">
      <div class="bg-slate-800 rounded-2xl p-6 w-full max-w-md shadow-2xl">
        <h3 class="font-bold text-white mb-1">Asignar trayectoria</h3>
        <p class="text-slate-400 text-sm mb-4">{{ assignModal()!.name }}</p>
        <textarea [(ngModel)]="assignNotes" rows="3"
          placeholder="Notas iniciales (opcional)..."
          class="w-full bg-white/10 text-white text-sm rounded-lg p-3 mb-4 resize-none outline-none border border-white/10 focus:border-blue-500">
        </textarea>
        <div class="flex gap-2 justify-end">
          <button (click)="assignModal.set(null)"
            class="px-4 py-2 text-sm text-slate-400 hover:text-white transition-colors">
            Cancelar
          </button>
          <button (click)="confirmAssign()" [disabled]="assigning()"
            class="px-4 py-2 text-sm bg-blue-600 hover:bg-blue-700 text-white rounded-lg transition-colors disabled:opacity-50">
            {{ assigning() ? 'Asignando...' : 'Asignar' }}
          </button>
        </div>
      </div>
    </div>

    <!-- Modal: Indicador de impacto -->
    <div *ngIf="indicatorModal()" class="fixed inset-0 bg-black/60 z-50 flex items-center justify-center p-4">
      <div class="bg-slate-800 rounded-2xl p-6 w-full max-w-md shadow-2xl">
        <div class="flex items-center justify-between mb-4">
          <h3 class="font-bold text-white">Agregar indicador</h3>
          <button (click)="indicatorModal.set(null)" class="text-slate-400 hover:text-white text-xl">×</button>
        </div>
        <div class="space-y-2 mb-4">
          <input type="text" [(ngModel)]="newIndicator.indicatorName" placeholder="Nombre del indicador *"
            class="w-full bg-white/10 text-white text-sm rounded-lg p-2.5 outline-none border border-white/10 focus:border-blue-500">
          <input type="text" [(ngModel)]="newIndicator.indicatorKey" placeholder="Clave única (ej: conflictos_semana)"
            class="w-full bg-white/10 text-white text-sm rounded-lg p-2.5 outline-none border border-white/10 focus:border-blue-500">
          <div class="grid grid-cols-3 gap-2">
            <input type="number" [(ngModel)]="newIndicator.baselineValue" placeholder="Baseline"
              class="bg-white/10 text-white text-sm rounded-lg p-2.5 outline-none border border-white/10 focus:border-blue-500">
            <input type="number" [(ngModel)]="newIndicator.currentValue" placeholder="Actual"
              class="bg-white/10 text-white text-sm rounded-lg p-2.5 outline-none border border-white/10 focus:border-blue-500">
            <input type="text" [(ngModel)]="newIndicator.unit" placeholder="Unidad"
              class="bg-white/10 text-white text-sm rounded-lg p-2.5 outline-none border border-white/10 focus:border-blue-500">
          </div>
          <label class="flex items-center gap-2 text-sm text-slate-300">
            <input type="checkbox" [(ngModel)]="newIndicator.higherIsBetter" class="rounded">
            Más alto es mejor
          </label>
        </div>
        <div class="flex gap-2 justify-end">
          <button (click)="indicatorModal.set(null)" class="px-4 py-2 text-sm text-slate-400 hover:text-white">Cancelar</button>
          <button (click)="submitIndicator()" [disabled]="savingIndicator()"
            class="px-4 py-2 text-sm bg-purple-600 hover:bg-purple-700 text-white rounded-lg transition-colors disabled:opacity-50">
            {{ savingIndicator() ? 'Guardando...' : 'Guardar' }}
          </button>
        </div>
      </div>
    </div>

    <!-- Modal: Línea de tiempo -->
    <div *ngIf="timelineModal()" class="fixed inset-0 bg-black/60 z-50 flex items-center justify-center p-4">
      <div class="bg-slate-800 rounded-2xl p-6 w-full max-w-lg shadow-2xl max-h-[80vh] flex flex-col">
        <div class="flex items-center justify-between mb-4">
          <h3 class="font-bold text-white">Línea de tiempo</h3>
          <button (click)="timelineModal.set(null)" class="text-slate-400 hover:text-white text-xl">×</button>
        </div>
        <p class="text-sm text-slate-400 mb-4">{{ timelineModal()!.trajectory.name }}</p>

        <div class="flex-1 overflow-y-auto space-y-3 mb-4">
          <div *ngFor="let e of timeline()" class="flex gap-3">
            <div class="flex flex-col items-center">
              <div class="w-2 h-2 rounded-full mt-1 flex-shrink-0"
                [class]="riskDot(e.riskLevel)"></div>
              <div class="w-px flex-1 bg-white/10 mt-1"></div>
            </div>
            <div class="pb-3 flex-1">
              <div class="flex items-center gap-2 mb-1">
                <span class="text-xs text-slate-500">{{ e.eventDate }}</span>
                <span *ngIf="e.ageAtEvent" class="text-xs text-slate-600">• {{ e.ageAtEvent }} años</span>
              </div>
              <p class="text-sm text-white">{{ e.eventDescription }}</p>
              <p *ngIf="e.actionTaken" class="text-xs text-slate-400 mt-1">
                Acción: {{ e.actionTaken }}
              </p>
              <p *ngIf="e.result" class="text-xs text-green-400 mt-0.5">→ {{ e.result }}</p>
            </div>
          </div>
          <div *ngIf="timeline().length === 0" class="text-center py-6 text-slate-500 text-sm">
            Sin eventos registrados aún.
          </div>
        </div>

        <!-- Add event form -->
        <div class="border-t border-white/10 pt-4">
          <p class="text-xs text-slate-400 font-medium mb-2">Agregar evento</p>
          <div class="grid grid-cols-2 gap-2 mb-2">
            <input type="date" [(ngModel)]="newEvent.eventDate"
              class="col-span-1 bg-white/10 text-white text-xs rounded p-2 outline-none border border-white/10 focus:border-blue-500">
            <input type="number" [(ngModel)]="newEvent.ageAtEvent" placeholder="Edad (opcional)"
              class="col-span-1 bg-white/10 text-white text-xs rounded p-2 outline-none border border-white/10 focus:border-blue-500">
          </div>
          <textarea [(ngModel)]="newEvent.eventDescription" rows="2"
            placeholder="Descripción del evento *"
            class="w-full bg-white/10 text-white text-xs rounded p-2 mb-2 resize-none outline-none border border-white/10 focus:border-blue-500">
          </textarea>
          <div class="grid grid-cols-2 gap-2 mb-2">
            <input type="text" [(ngModel)]="newEvent.actionTaken" placeholder="Acción tomada"
              class="bg-white/10 text-white text-xs rounded p-2 outline-none border border-white/10 focus:border-blue-500">
            <input type="text" [(ngModel)]="newEvent.result" placeholder="Resultado"
              class="bg-white/10 text-white text-xs rounded p-2 outline-none border border-white/10 focus:border-blue-500">
          </div>
          <button (click)="submitEvent()" [disabled]="savingEvent()"
            class="w-full py-2 text-xs bg-blue-600 hover:bg-blue-700 text-white rounded transition-colors disabled:opacity-50">
            {{ savingEvent() ? 'Guardando...' : 'Agregar evento' }}
          </button>
        </div>
      </div>
    </div>
  `,
})
export class TrajectoryPageComponent implements OnInit {
  readonly trajectoryService = inject(TrajectoryService);
  private readonly familyState = inject(FamilyStateService);

  readonly tabs = [
    { key: 'bank' as Tab, label: 'Banco de Trayectorias' },
    { key: 'family' as Tab, label: 'Mi Familia' },
    { key: 'impact' as Tab, label: 'Impacto' },
  ];

  readonly activeTab = signal<Tab>('bank');
  readonly loading = signal(true);
  readonly assigning = signal(false);
  readonly savingEvent = signal(false);

  readonly bankData = signal<Record<string, TrajectoryBankItem[]>>({});
  readonly familyTrajectories = signal<FamilyTrajectoryDto[]>([]);
  readonly timeline = signal<TrajectoryTimelineDto[]>([]);

  readonly assignModal = signal<TrajectoryBankItem | null>(null);
  readonly timelineModal = signal<FamilyTrajectoryDto | null>(null);
  readonly indicatorModal = signal<FamilyTrajectoryDto | null>(null);
  readonly savingIndicator = signal(false);

  readonly impactMap = signal<Record<number, TrajectoryImpactDto[]>>({});

  assignNotes = '';
  newEvent: Partial<TimelineEventRequest> = { riskLevel: 'MEDIUM' };
  newIndicator: Partial<IndicatorRequest> = { higherIsBetter: true };

  readonly totalTrajectories = computed(() =>
    Object.values(this.bankData()).reduce((s, arr) => s + arr.length, 0)
  );
  readonly macrodomainsCount = computed(() => Object.keys(this.bankData()).length);
  readonly macrodomainsEntries = computed(() => Object.entries(this.bankData()));

  private readonly _expandedDomains = signal(new Set<string>());
  readonly expandedDomains = this._expandedDomains.asReadonly();

  ngOnInit(): void {
    const familyId = this.familyState.currentFamilyId();
    forkJoin({
      bank: this.trajectoryService.getBank().pipe(catchError(() => of(null))),
      family: familyId
        ? this.trajectoryService.getFamilyTrajectories(familyId).pipe(catchError(() => of([])))
        : of([]),
    }).subscribe(({ bank, family }) => {
      if (bank) this.bankData.set(bank.byMacrodomain);
      const trajs = family as FamilyTrajectoryDto[];
      this.familyTrajectories.set(trajs);
      this.loading.set(false);
      // Load impact data for all trajectories
      trajs.forEach(ft => this.loadImpact(ft.id));
    });
  }

  private loadImpact(familyTrajectoryId: number): void {
    this.trajectoryService.getImpactSummary(familyTrajectoryId)
      .pipe(catchError(() => of([])))
      .subscribe(indicators => {
        this.impactMap.update(m => ({ ...m, [familyTrajectoryId]: indicators }));
      });
  }

  impactColor(ind: TrajectoryImpactDto): string {
    if (ind.improvementPct === null || ind.improvementPct === undefined) return 'text-white';
    return ind.improvementPct >= 0 ? 'text-green-400' : 'text-red-400';
  }

  openIndicatorModal(ft: FamilyTrajectoryDto): void {
    this.newIndicator = { higherIsBetter: true };
    this.indicatorModal.set(ft);
  }

  submitIndicator(): void {
    const ft = this.indicatorModal();
    if (!ft || !this.newIndicator.indicatorName || !this.newIndicator.indicatorKey) return;
    this.savingIndicator.set(true);
    this.trajectoryService.upsertIndicator(ft.id, this.newIndicator as IndicatorRequest).subscribe({
      next: (ind) => {
        this.impactMap.update(m => ({
          ...m,
          [ft.id]: [...(m[ft.id] ?? []).filter(i => i.indicatorKey !== ind.indicatorKey), ind],
        }));
        this.indicatorModal.set(null);
        this.savingIndicator.set(false);
      },
      error: () => this.savingIndicator.set(false),
    });
  }

  toggleDomain(domain: string): void {
    const set = new Set(this._expandedDomains());
    set.has(domain) ? set.delete(domain) : set.add(domain);
    this._expandedDomains.set(set);
  }

  domainLabel(domain: string): string {
    return MACRODOMAIN_LABELS[domain] ?? domain;
  }

  parseSignals(json: string): string[] {
    try { return JSON.parse(json) ?? []; } catch { return []; }
  }

  severityBadge(sev: string): string {
    const cfg = SEVERITY_CONFIG[sev as keyof typeof SEVERITY_CONFIG];
    return cfg ? `${cfg.color} ${cfg.bg} px-2 py-0.5 rounded-full text-xs` : 'text-slate-400 text-xs';
  }

  severityLabel(sev: string): string {
    return SEVERITY_CONFIG[sev as keyof typeof SEVERITY_CONFIG]?.label ?? sev;
  }

  statusColor(status: TrajectoryStatus): string {
    return STATUS_CONFIG[status]?.color ?? 'text-slate-400';
  }

  statusLabel(status: TrajectoryStatus): string {
    return STATUS_CONFIG[status]?.label ?? status;
  }

  formatDate(iso: string): string {
    if (!iso) return '';
    return new Date(iso).toLocaleDateString('es-ES', { day: 'numeric', month: 'short', year: 'numeric' });
  }

  riskDot(level: string): string {
    const map: Record<string, string> = {
      LOW: 'bg-green-400', MEDIUM: 'bg-yellow-400',
      HIGH: 'bg-orange-400', CRITICAL: 'bg-red-500',
    };
    return map[level] ?? 'bg-slate-400';
  }

  availableStatuses(current: TrajectoryStatus): TrajectoryStatus[] {
    const all: TrajectoryStatus[] = ['DETECTED', 'IN_PROGRESS', 'RESOLVED', 'RELAPSED', 'CLOSED'];
    return all.filter(s => s !== current);
  }

  openAssignModal(traj: TrajectoryBankItem): void {
    this.assignNotes = '';
    this.assignModal.set(traj);
  }

  confirmAssign(): void {
    const traj = this.assignModal();
    const familyId = this.familyState.currentFamilyId();
    if (!traj || !familyId) return;
    this.assigning.set(true);
    this.trajectoryService.assignTrajectory(familyId, { code: traj.code, notes: this.assignNotes })
      .subscribe({
        next: (ft) => {
          this.familyTrajectories.update(list => [...list, ft]);
          this.assignModal.set(null);
          this.assigning.set(false);
        },
        error: () => this.assigning.set(false),
      });
  }

  changeStatus(ft: FamilyTrajectoryDto, status: TrajectoryStatus): void {
    this.trajectoryService.updateStatus(ft.id, status).subscribe(() => {
      this.familyTrajectories.update(list =>
        list.map(t => t.id === ft.id ? { ...t, status } : t)
      );
    });
  }

  viewTimeline(ft: FamilyTrajectoryDto): void {
    this.timelineModal.set(ft);
    this.newEvent = { riskLevel: 'MEDIUM' };
    this.trajectoryService.getTimeline(ft.id).subscribe(t => this.timeline.set(t));
  }

  viewImpact(ft: FamilyTrajectoryDto): void {
    this.activeTab.set('impact');
  }

  submitEvent(): void {
    const ft = this.timelineModal();
    if (!ft || !this.newEvent.eventDescription || !this.newEvent.eventDate) return;
    this.savingEvent.set(true);
    this.trajectoryService.addTimelineEvent(ft.id, this.newEvent as TimelineEventRequest).subscribe({
      next: (e) => {
        this.timeline.update(list => [...list, e]);
        this.newEvent = { riskLevel: 'MEDIUM' };
        this.savingEvent.set(false);
      },
      error: () => this.savingEvent.set(false),
    });
  }
}
