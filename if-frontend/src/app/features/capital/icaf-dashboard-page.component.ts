import {
  Component, OnInit, OnDestroy, inject,
  ChangeDetectionStrategy, signal, computed
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, RouterLink } from '@angular/router';
import { HttpClient } from '@angular/common/http';
import { Subject, forkJoin, takeUntil } from 'rxjs';
import { FamilyTrajectoryDto } from '../../core/models/trajectory.model';
import { TrajectoryService } from '../../core/services/trajectory.service';
import { IcafDataService } from './services/icaf-data.service';
import {
  IcafDashboardResponse, IcafDomainScore,
  MADUREZ_CONFIG, DOMAIN_COLORS
} from '../../core/models/icaf.model';
import { environment } from '../../../environments/environment';

@Component({
  selector: 'app-icaf-dashboard-page',
  standalone: true,
  imports: [CommonModule, RouterLink],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
<div class="min-h-screen p-6 lg:p-10 space-y-8">

  <!-- HEADER -->
  <header class="flex items-center justify-between gap-4">
    <div>
      <h1 class="text-3xl font-bold text-white">Capital Familiar</h1>
      <p class="text-white/50 text-sm mt-1">Índice de Capital Familiar · ICaF</p>
    </div>
    <div class="flex gap-2">
      <a routerLink="/capital/observatory"
         class="px-4 py-2 rounded-xl text-sm font-medium transition-all
                bg-violet-500/10 hover:bg-violet-500/20 border border-violet-500/30
                text-violet-300 hover:text-violet-200">
        🔭 Observatorio
      </a>
      <a routerLink="/capital/questionnaire"
         class="px-4 py-2 rounded-xl text-sm font-medium transition-all
                bg-indigo-500/10 hover:bg-indigo-500/20 border border-indigo-500/30
                text-indigo-300 hover:text-indigo-200">
        📝 Cuestionario
      </a>
      <button
        (click)="refresh()"
        [disabled]="loading()"
        class="px-4 py-2 rounded-xl text-sm font-medium transition-all
               bg-white/5 hover:bg-white/10 border border-white/10
               text-white/70 hover:text-white disabled:opacity-40">
        {{ loading() ? 'Calculando…' : 'Recalcular' }}
      </button>
    </div>
  </header>

  @if (loading()) {
    <div class="flex items-center justify-center py-24">
      <div class="w-8 h-8 border-2 border-white/20 border-t-white/60 rounded-full animate-spin"></div>
    </div>
  }

  @if (!loading() && data()) {
    <!-- FILA 1: Gauge + Madurez + Tendencia -->
    <section class="grid grid-cols-1 md:grid-cols-3 gap-6">

      <!-- Gauge ICaF -->
      <div class="glass-card rounded-3xl p-6 flex flex-col items-center gap-3 md:col-span-1">
        <p class="text-white/50 text-xs font-semibold tracking-widest uppercase">ICaF Global</p>
        <div class="relative">
          <svg width="180" height="180" viewBox="0 0 180 180">
            <!-- Arco fondo -->
            <circle cx="90" cy="90" r="70"
              fill="none" stroke="rgba(255,255,255,0.06)"
              stroke-width="14" stroke-dasharray="330 440"
              stroke-dashoffset="-55"
              stroke-linecap="round"
              transform="rotate(90 90 90)"/>
            <!-- Arco valor -->
            <circle cx="90" cy="90" r="70"
              fill="none"
              [attr.stroke]="madurezColor()"
              stroke-width="14"
              [attr.stroke-dasharray]="gaugeArc() + ' 440'"
              stroke-dashoffset="-55"
              stroke-linecap="round"
              transform="rotate(90 90 90)"
              style="transition: stroke-dasharray 0.8s cubic-bezier(.4,0,.2,1)"/>
            <!-- Score -->
            <text x="90" y="86" text-anchor="middle"
              fill="white" font-size="36" font-weight="700"
              font-family="system-ui">{{ data()!.icaf | number:'1.0-0' }}</text>
            <text x="90" y="108" text-anchor="middle"
              fill="rgba(255,255,255,0.4)" font-size="11"
              font-family="system-ui">de 100</text>
          </svg>
        </div>
        <!-- Tendencia -->
        <div class="flex items-center gap-2 text-sm">
          <span class="text-lg">{{ trendIcon() }}</span>
          <span [class]="trendClass()">{{ trendLabel() }}</span>
        </div>
        @if (data()!.lastCalculatedAt) {
          <p class="text-white/25 text-xs">Calculado {{ data()!.lastCalculatedAt }}</p>
        }
      </div>

      <!-- Nivel de Madurez -->
      <div class="glass-card rounded-3xl p-6 flex flex-col justify-between md:col-span-1"
           [style.border-color]="madurezColor() + '40'">
        <div>
          <p class="text-white/50 text-xs font-semibold tracking-widest uppercase mb-3">Nivel de Madurez</p>
          <div class="flex items-center gap-3 mb-4">
            <span class="text-4xl font-black" [style.color]="madurezColor()">
              {{ data()!.madurezNivel }}
            </span>
            <div>
              <p class="text-white font-bold text-lg leading-tight">{{ data()!.madurezLabel }}</p>
              <p class="text-white/40 text-xs">de 5 niveles</p>
            </div>
          </div>
          <!-- Barra de niveles -->
          <div class="flex gap-1.5">
            @for (n of [1,2,3,4,5]; track n) {
              <div class="h-2 flex-1 rounded-full transition-all"
                   [style.background]="n <= data()!.madurezNivel ? madurezColor() : 'rgba(255,255,255,0.08)'">
              </div>
            }
          </div>
        </div>
        <p class="text-white/40 text-xs mt-4 leading-relaxed">{{ madurezDescription() }}</p>
      </div>

      <!-- Trayectoria longitudinal -->
      <div class="glass-card rounded-3xl p-6 md:col-span-1">
        <p class="text-white/50 text-xs font-semibold tracking-widest uppercase mb-4">Trayectoria</p>
        <div class="space-y-3">
          <div class="flex justify-between items-center">
            <span class="text-white/50 text-xs">Ahora</span>
            <div class="flex items-center gap-2">
              <div class="h-1 rounded-full bg-white/10 w-24 overflow-hidden">
                <div class="h-full rounded-full transition-all"
                     [style.width.%]="data()!.icaf"
                     [style.background]="madurezColor()"></div>
              </div>
              <span class="text-white font-bold text-sm w-10 text-right">
                {{ data()!.icaf | number:'1.0-1' }}
              </span>
            </div>
          </div>
          @if (data()!.icaf6mAgo !== null) {
            <div class="flex justify-between items-center">
              <span class="text-white/50 text-xs">6 meses</span>
              <div class="flex items-center gap-2">
                <div class="h-1 rounded-full bg-white/10 w-24 overflow-hidden">
                  <div class="h-full rounded-full bg-white/30 transition-all"
                       [style.width.%]="data()!.icaf6mAgo!"></div>
                </div>
                <span class="text-white/60 text-sm w-10 text-right">
                  {{ data()!.icaf6mAgo | number:'1.0-1' }}
                </span>
              </div>
            </div>
          }
          @if (data()!.icaf12mAgo !== null) {
            <div class="flex justify-between items-center">
              <span class="text-white/50 text-xs">12 meses</span>
              <div class="flex items-center gap-2">
                <div class="h-1 rounded-full bg-white/10 w-24 overflow-hidden">
                  <div class="h-full rounded-full bg-white/20 transition-all"
                       [style.width.%]="data()!.icaf12mAgo!"></div>
                </div>
                <span class="text-white/50 text-sm w-10 text-right">
                  {{ data()!.icaf12mAgo | number:'1.0-1' }}
                </span>
              </div>
            </div>
          }
          @if (data()!.icaf36mAgo !== null) {
            <div class="flex justify-between items-center">
              <span class="text-white/50 text-xs">36 meses</span>
              <div class="flex items-center gap-2">
                <div class="h-1 rounded-full bg-white/10 w-24 overflow-hidden">
                  <div class="h-full rounded-full bg-white/10 transition-all"
                       [style.width.%]="data()!.icaf36mAgo!"></div>
                </div>
                <span class="text-white/40 text-sm w-10 text-right">
                  {{ data()!.icaf36mAgo | number:'1.0-1' }}
                </span>
              </div>
            </div>
          }
          @if (data()!.icaf6mAgo === null) {
            <p class="text-white/25 text-xs pt-2">
              El historial longitudinal aparecerá después del primer recálculo periódico.
            </p>
          }
        </div>
      </div>
    </section>

    <!-- FILA 2: 11 Dominios -->
    <section class="glass-card rounded-3xl p-6">
      <div class="flex items-center justify-between mb-5">
        <h2 class="text-white font-bold">Los 11 Dominios del Capital Familiar</h2>
        <span class="text-white/30 text-xs">Peso · Score</span>
      </div>
      <div class="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4 gap-3">
        @for (domain of data()!.domains; track domain.key) {
          <div class="rounded-2xl p-4 border transition-all hover:scale-[1.02]"
               [style.border-color]="domainColor(domain.key) + '30'"
               [style.background]="domainColor(domain.key) + '08'">
            <div class="flex items-center justify-between mb-2">
              <span class="text-white/80 text-sm font-medium">{{ domain.label }}</span>
              <div class="flex items-center gap-1.5">
                @if (domain.isEstimated) {
                  <span title="Estimado" class="text-white/30 text-xs">~</span>
                }
                <span class="text-white/40 text-xs">{{ domain.weight * 100 | number:'1.0-0' }}%</span>
              </div>
            </div>
            <!-- Barra del dominio -->
            <div class="h-1.5 rounded-full bg-white/10 overflow-hidden mb-2">
              <div class="h-full rounded-full transition-all"
                   [style.width.%]="domain.score"
                   [style.background]="domainColor(domain.key)">
              </div>
            </div>
            <div class="flex items-center justify-between">
              <span class="font-bold text-lg" [style.color]="domainColor(domain.key)">
                {{ domain.score | number:'1.0-0' }}
              </span>
              <span class="text-white/25 text-xs">{{ domain.source }}</span>
            </div>
          </div>
        }
      </div>
      <!-- Leyenda estimados -->
      <p class="text-white/25 text-xs mt-4">
        ~ Dominios marcados con ~ usan estimaciones basadas en datos disponibles.
        Completa los cuestionarios de confianza y bienestar para datos reales.
      </p>
    </section>

    <!-- FILA 3: Eventos críticos -->
    <section class="grid grid-cols-1 md:grid-cols-2 gap-6">

      <!-- Métricas de resiliencia -->
      <div class="glass-card rounded-3xl p-6">
        <h2 class="text-white font-bold mb-5">Resiliencia ante Crisis</h2>
        <div class="grid grid-cols-2 gap-4">
          <div class="rounded-2xl bg-white/5 p-4 text-center">
            <p class="text-3xl font-black" [class]="data()!.activeEvents > 0 ? 'text-red-400' : 'text-emerald-400'">
              {{ data()!.activeEvents }}
            </p>
            <p class="text-white/50 text-xs mt-1">Crisis activas</p>
          </div>
          <div class="rounded-2xl bg-white/5 p-4 text-center">
            <p class="text-3xl font-black text-emerald-400">
              {{ data()!.resolutionRate | number:'1.0-0' }}%
            </p>
            <p class="text-white/50 text-xs mt-1">Tasa resolución</p>
          </div>
          <div class="rounded-2xl bg-white/5 p-4 text-center">
            <p class="text-3xl font-black text-blue-400">
              {{ data()!.avgDaysToResolution | number:'1.0-0' }}
            </p>
            <p class="text-white/50 text-xs mt-1">Días promedio</p>
          </div>
          <div class="rounded-2xl bg-white/5 p-4 text-center">
            <p class="text-3xl font-black"
               [class]="data()!.totalRelapses > 0 ? 'text-amber-400' : 'text-white/60'">
              {{ data()!.totalRelapses }}
            </p>
            <p class="text-white/50 text-xs mt-1">Recaídas</p>
          </div>
        </div>
        @if (data()!.activeEvents === 0 && data()!.resolvedEvents === 0) {
          <p class="text-white/30 text-xs mt-4 text-center">
            Sin eventos críticos registrados. Las crisis se detectan automáticamente
            cuando la familia registra incidentes.
          </p>
        }
      </div>

      <!-- Cuestionarios pendientes -->
      <div class="glass-card rounded-3xl p-6">
        <h2 class="text-white font-bold mb-5">Mejorar precisión del ICaF</h2>
        <div class="space-y-3">
          @for (domain of pendingDomains(); track domain.key) {
            <div class="flex items-center justify-between rounded-2xl bg-white/5 px-4 py-3">
              <div>
                <p class="text-white/80 text-sm font-medium">{{ domain.label }}</p>
                <p class="text-white/40 text-xs">
                  Actualmente estimado · peso {{ domain.weight * 100 | number:'1.0-0' }}%
                </p>
              </div>
              <button
                (click)="openQuestionnaire(domain.key)"
                class="px-3 py-1.5 rounded-xl text-xs font-medium
                       bg-indigo-500/20 hover:bg-indigo-500/30 border border-indigo-500/30
                       text-indigo-300 transition-all whitespace-nowrap">
                Responder
              </button>
            </div>
          }
          @if (pendingDomains().length === 0) {
            <div class="text-center py-6">
              <p class="text-emerald-400 font-medium text-sm">✓ Todos los cuestionarios completados</p>
              <p class="text-white/30 text-xs mt-1">El ICaF usa datos reales para todos los dominios disponibles.</p>
            </div>
          }
        </div>
      </div>
    </section>

    <!-- Trayectorias de Riesgo Activas -->
    @if (activeTrajectories().length > 0) {
      <section class="glass-card rounded-3xl p-6">
        <div class="flex items-center justify-between mb-5">
          <div>
            <h2 class="text-white font-bold">Trayectorias de Riesgo Activas</h2>
            <p class="text-white/40 text-xs mt-0.5">{{ activeTrajectories().length }} trayectoria(s) en seguimiento</p>
          </div>
          <div class="flex gap-2">
            <button (click)="downloadTrajectoryPdf()" [disabled]="downloadingPdf()"
              class="px-3 py-1.5 rounded-xl text-xs font-medium transition-all
                     bg-orange-500/10 hover:bg-orange-500/20 border border-orange-500/30
                     text-orange-300 hover:text-orange-200 disabled:opacity-40">
              {{ downloadingPdf() ? 'Generando…' : '📄 Exportar PDF' }}
            </button>
            <a routerLink="/trajectory"
              class="px-3 py-1.5 rounded-xl text-xs font-medium transition-all
                     bg-white/5 hover:bg-white/10 border border-white/10
                     text-white/60 hover:text-white">
              Ver todas →
            </a>
          </div>
        </div>
        <div class="space-y-2">
          @for (t of activeTrajectories(); track t.id) {
            <div class="flex items-center justify-between rounded-2xl bg-white/5 px-4 py-3">
              <div class="flex items-center gap-3 min-w-0">
                <span class="text-lg shrink-0">{{ severityIcon(t.trajectory.severityDefault) }}</span>
                <div class="min-w-0">
                  <p class="text-white/90 text-sm font-medium truncate">{{ t.trajectory.name }}</p>
                  <p class="text-white/40 text-xs">{{ macroDomainLabel(t.trajectory.macrodomain) }}</p>
                </div>
              </div>
              <span class="text-xs font-medium px-2.5 py-1 rounded-full shrink-0 ml-3"
                [class]="statusBadge(t.status)">
                {{ statusLabel(t.status) }}
              </span>
            </div>
          }
        </div>
      </section>
    }

    <!-- Banner de alerta: trayectorias en recaída -->
    @if (relapsedTrajectories().length > 0) {
      <div class="rounded-2xl border border-red-500/40 bg-red-500/08 p-4 flex gap-3">
        <span class="text-red-400 text-xl shrink-0">🚨</span>
        <div class="flex-1 min-w-0">
          <p class="text-red-300 font-semibold text-sm">Alerta de recaída activa</p>
          <p class="text-white/50 text-xs mt-1">
            {{ relapsedTrajectories().length }} trayectoria(s) han registrado una recaída:
            {{ relapsedNames() }}.
            Revisión urgente recomendada.
          </p>
        </div>
        <a routerLink="/trajectory"
           class="shrink-0 self-start px-3 py-1.5 rounded-xl text-xs font-medium
                  bg-red-500/20 hover:bg-red-500/30 border border-red-500/30
                  text-red-300 hover:text-red-200 transition-all">
          Ver →
        </a>
      </div>
    }

    <!-- Banner sin datos reales -->
    @if (!data()!.hasRealData) {
      <div class="rounded-2xl border border-amber-500/30 bg-amber-500/08 p-4 flex gap-3">
        <span class="text-amber-400 text-lg shrink-0">⚡</span>
        <div>
          <p class="text-amber-300 font-medium text-sm">ICaF en modo estimación</p>
          <p class="text-white/40 text-xs mt-1">
            Completa al menos una evaluación diagnóstica para que el ICaF refleje datos reales de tu familia.
          </p>
        </div>
      </div>
    }
  }

  @if (!loading() && !data()) {
    <div class="text-center py-24 text-white/30">
      <p class="text-4xl mb-3">📊</p>
      <p>No se pudo cargar el Capital Familiar.</p>
      <button (click)="refresh()" class="mt-4 text-indigo-400 text-sm hover:text-indigo-300">
        Reintentar
      </button>
    </div>
  }

</div>
  `,
  styles: [`
    :host { display: block; }
  `]
})
export class IcafDashboardPageComponent implements OnInit, OnDestroy {

  private readonly icafService       = inject(IcafDataService);
  private readonly trajectoryService = inject(TrajectoryService);
  private readonly router            = inject(Router);
  private readonly http              = inject(HttpClient);
  private readonly destroy$          = new Subject<void>();

  readonly loading         = signal(true);
  readonly data            = signal<IcafDashboardResponse | null>(null);
  readonly trajectories    = signal<FamilyTrajectoryDto[]>([]);
  readonly downloadingPdf  = signal(false);

  readonly activeTrajectories = computed(() =>
    this.trajectories().filter(t => t.status !== 'RESOLVED' && t.status !== 'CLOSED')
  );

  readonly relapsedTrajectories = computed(() =>
    this.trajectories().filter(t => t.status === 'RELAPSED')
  );

  readonly relapsedNames = computed(() => {
    const list = this.relapsedTrajectories();
    const names = list.slice(0, 2).map(t => t.trajectory.name).join(', ');
    return names + (list.length > 2 ? '…' : '');
  });

  private familyId = 0;

  // ── Computed ──────────────────────────────────────────────────────────────

  readonly madurezColor = computed(() => {
    const nivel = this.data()?.madurezNivel ?? 1;
    return MADUREZ_CONFIG[nivel]?.color ?? '#6366f1';
  });

  readonly gaugeArc = computed(() => {
    const score = this.data()?.icaf ?? 0;
    return Math.round((score / 100) * 330); // 330° = arco total del gauge
  });

  readonly trendIcon = computed(() => {
    const t = this.data()?.trend;
    if (t === 'IMPROVING') return '↑';
    if (t === 'DECLINING') return '↓';
    return '→';
  });

  readonly trendLabel = computed(() => {
    const t = this.data()?.trend;
    if (t === 'IMPROVING') return 'Mejorando';
    if (t === 'DECLINING') return 'Declinando';
    return 'Estable';
  });

  readonly trendClass = computed(() => {
    const t = this.data()?.trend;
    if (t === 'IMPROVING') return 'text-emerald-400 text-sm font-medium';
    if (t === 'DECLINING') return 'text-red-400 text-sm font-medium';
    return 'text-white/50 text-sm font-medium';
  });

  readonly madurezDescription = computed(() => {
    const nivel = this.data()?.madurezNivel ?? 1;
    const descriptions: Record<number, string> = {
      1: 'La familia está en modo supervivencia. Necesita estabilización urgente.',
      2: 'Responde a los problemas pero aún no los anticipa. Hay potencial de crecimiento.',
      3: 'Acuerdos y rutinas estables. La familia funciona con organización básica.',
      4: 'Metas compartidas y crecimiento activo. Se construye el futuro juntos.',
      5: 'La familia tiene impacto intergeneracional. Transmite legado y valores.',
    };
    return descriptions[nivel] ?? '';
  });

  readonly pendingDomains = computed(() => {
    return (this.data()?.domains ?? [])
      .filter(d => d.isEstimated && ['confianza', 'bienestar'].includes(d.key));
  });

  // ── Lifecycle ─────────────────────────────────────────────────────────────

  ngOnInit(): void {
    this.http
      .get<any>(`${environment.apiBaseUrl}/families/mine`)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: res => {
          this.familyId = res?.data?.id ?? res?.id ?? 0;
          if (this.familyId) this.loadDashboard();
          else this.loading.set(false);
        },
        error: () => this.loading.set(false)
      });
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  // ── Acciones ──────────────────────────────────────────────────────────────

  refresh(): void {
    if (!this.familyId) return;
    this.loading.set(true);
    this.icafService.recalculate(this.familyId)
      .pipe(takeUntil(this.destroy$))
      .subscribe(d => { this.data.set(d); this.loading.set(false); });
  }

  openQuestionnaire(domain: string): void {
    this.router.navigate(['/capital/questionnaire'], { queryParams: { domain } });
  }

  // ── Helpers de template ───────────────────────────────────────────────────

  domainColor(key: string): string {
    return DOMAIN_COLORS[key] ?? '#6366f1';
  }

  // ── Privados ──────────────────────────────────────────────────────────────

  downloadTrajectoryPdf(): void {
    if (!this.familyId || this.downloadingPdf()) return;
    this.downloadingPdf.set(true);
    this.http.get(
      `${environment.apiBaseUrl}/v1/reports/export/pdf/family/${this.familyId}/trajectories`,
      { responseType: 'blob' }
    ).subscribe({
      next: blob => {
        const url = URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = url;
        a.download = `IFE_Trayectorias_Familia_${this.familyId}.pdf`;
        a.click();
        URL.revokeObjectURL(url);
        this.downloadingPdf.set(false);
      },
      error: () => this.downloadingPdf.set(false)
    });
  }

  severityIcon(sev: string): string {
    if (sev === 'CRITICAL') return '🔴';
    if (sev === 'HIGH')     return '🟠';
    if (sev === 'MEDIUM')   return '🟡';
    return '🟢';
  }

  statusLabel(status: string): string {
    const labels: Record<string, string> = {
      DETECTED: 'Detectado', IN_PROGRESS: 'En progreso',
      RESOLVED: 'Resuelto', RELAPSED: 'Recaída', CLOSED: 'Cerrado'
    };
    return labels[status] ?? status;
  }

  statusBadge(status: string): string {
    if (status === 'RELAPSED')    return 'bg-orange-500/20 text-orange-300';
    if (status === 'IN_PROGRESS') return 'bg-blue-500/20 text-blue-300';
    if (status === 'DETECTED')    return 'bg-indigo-500/20 text-indigo-300';
    return 'bg-white/10 text-white/50';
  }

  macroDomainLabel(domain: string): string {
    const labels: Record<string, string> = {
      RELACIONES_PAREJA: 'Relaciones de Pareja',
      CRIANZA_ADOLESCENCIA: 'Crianza y Adolescencia',
      SALUD_MENTAL: 'Salud Mental',
      ADICCIONES: 'Adicciones',
      EDUCACION_DESARROLLO: 'Educación',
      ECONOMIA_FAMILIAR: 'Economía',
      GOBERNANZA: 'Gobernanza',
      ADULTO_MAYOR: 'Adulto Mayor',
      LEGADO: 'Legado',
    };
    return labels[domain] ?? domain;
  }

  private loadDashboard(): void {
    forkJoin({
      dashboard: this.icafService.getDashboard(this.familyId),
      trajectories: this.trajectoryService.getFamilyTrajectories(this.familyId)
    }).pipe(takeUntil(this.destroy$))
      .subscribe(({ dashboard, trajectories }) => {
        this.data.set(dashboard);
        this.trajectories.set(trajectories);
        this.loading.set(false);
      });
  }
}
