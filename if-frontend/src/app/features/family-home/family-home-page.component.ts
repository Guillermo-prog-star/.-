import { ChangeDetectionStrategy, Component, OnInit, computed, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { HttpClient } from '@angular/common/http';
import { Router, RouterLink, RouterLinkActive } from '@angular/router';
import { catchError, of } from 'rxjs';
import { ApiService } from '../../core/services/api.service';
import { FamilyStateService } from '../../core/services/family-state.service';
import { MilestoneAdvancementService, AdvancementEvaluation } from '../../core/services/milestone-advancement.service';
import {
  Command,
  FamilyHomeStoreService,
  isActiveHomeView,
  isAssessmentHomeView,
  isOnboardingHomeView,
  isReturnStageHomeView,
} from '../../core/family-home';
import { FAMILY_HOME_SHARED_STYLES } from './family-home-shared.styles';
import { OnboardingHomeViewComponent } from './views/onboarding-home-view.component';
import { AssessmentHomeViewComponent } from './views/assessment-home-view.component';
import { ReturnStageHomeViewComponent } from './views/return-stage-home-view.component';
import { ActiveHomeViewComponent } from './views/active-home-view.component';

/**
 * Contenedor del Hogar Digital Familiar (IFRM-D, Hito 7). Ruta nueva —
 * no reemplaza /dashboard. Cubre las 4 vistas que el backend puede producir
 * hoy (Onboarding, Assessment, ReturnStage, ActiveHome); PausedHomeView queda
 * fuera de alcance porque no existe todavía un mecanismo de pausa persistido
 * (ver notas de Hito 5 sobre confirm-resume/resume-journey).
 */
@Component({
  selector: 'app-family-home-page',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    CommonModule,
    RouterLink,
    RouterLinkActive,
    OnboardingHomeViewComponent,
    AssessmentHomeViewComponent,
    ReturnStageHomeViewComponent,
    ActiveHomeViewComponent,
  ],
  styles: [FAMILY_HOME_SHARED_STYLES, `
    .fh-header { margin-bottom: 24px; }
    .fh-header h1 { font-size: 28px; font-weight: 800; color: #fff; letter-spacing: -0.02em; margin: 0 0 6px; }
    .fh-header p { color: rgba(255,255,255,0.4); font-size: 14px; margin: 0; }
    .fh-empty { text-align: center; padding: 48px 24px; color: rgba(255,255,255,0.4); }
    .fh-retry { margin-top: 12px; }
    a.fh-link { color: #a5b4fc; text-decoration: none; font-weight: 700; }
    a.fh-link:hover { text-decoration: underline; }

    .fh-quicknav { display: flex; flex-wrap: wrap; gap: 8px; margin-bottom: 28px; }
    .fh-quicknav-item {
      padding: 8px 16px; border-radius: 20px; font-size: 13px; font-weight: 600;
      color: rgba(255,255,255,0.6); background: rgba(255,255,255,0.03);
      border: 1px solid rgba(255,255,255,0.08); text-decoration: none; cursor: pointer;
      transition: all 0.2s;
    }
    .fh-quicknav-item:hover { color: #fff; background: rgba(255,255,255,0.06); }
    .fh-quicknav-item.active { color: #a5b4fc; background: rgba(99,102,241,0.12); border-color: rgba(99,102,241,0.3); }

    .fh-advancement { margin-bottom: 24px; }
    .fh-adv-title { font-size: 15px; font-weight: 700; color: #fff; margin: 0 0 4px; }
    .fh-adv-sub   { font-size: 13px; color: rgba(255,255,255,0.4); margin: 0 0 16px; }
    .fh-adv-rows  { display: flex; flex-direction: column; gap: 10px; }
    .fh-adv-row   { display: flex; align-items: center; gap: 10px; font-size: 13px; color: rgba(255,255,255,0.7); }
    .fh-adv-check { width: 18px; height: 18px; border-radius: 50%; flex-shrink: 0; display: flex; align-items: center; justify-content: center; font-size: 10px; }
    .fh-adv-check.done    { background: rgba(34,197,94,0.15); color: #4ade80; border: 1px solid rgba(34,197,94,0.3); }
    .fh-adv-check.pending { background: rgba(255,255,255,0.05); color: rgba(255,255,255,0.3); border: 1px solid rgba(255,255,255,0.1); }
    .fh-adv-footer { margin-top: 16px; font-size: 12px; color: rgba(255,255,255,0.4); line-height: 1.5; }
  `],
  template: `
    <div class="fh-header">
      <h1>Hogar Digital Familiar</h1>
      <p>{{ familyState.currentFamilyName() || 'Tu espacio de acompañamiento diario' }}</p>
    </div>

    <nav class="fh-quicknav">
      @for (item of quickNav; track item.label) {
        @if (item.route) {
          <a class="fh-quicknav-item" [routerLink]="item.route" routerLinkActive="active">{{ item.label }}</a>
        } @else {
          <span class="fh-quicknav-item active">{{ item.label }}</span>
        }
      }
    </nav>

    @if (advancement(); as a) {
      <div class="glass-card fh-advancement">
        @if (a.terminal) {
          <p class="fh-adv-title">🏛️ Han completado el viaje de 36 meses</p>
          <p class="fh-adv-sub">Este es el hito final de la ruta — su historia sigue, pero ya no hay un "próximo hito" que perseguir.</p>
        } @else if (a.canAdvance) {
          <p class="fh-adv-title">🎉 Listos para el hito {{ nextMilestoneLabel(a) }}</p>
          <p class="fh-adv-sub">El sistema los avanzará automáticamente — no tienen que hacer nada más que seguir a su ritmo.</p>
        } @else {
          <p class="fh-adv-title">🧭 Camino hacia el hito {{ nextMilestoneLabel(a) }}</p>
          <p class="fh-adv-sub">Cuando lleguen a los tres, avanzan solos. Esto es lo que ya tienen y lo que sigue construyéndose:</p>
          <div class="fh-adv-rows">
            <div class="fh-adv-row">
              <span class="fh-adv-check" [class.done]="a.timeMet" [class.pending]="!a.timeMet">{{ a.timeMet ? '✓' : '' }}</span>
              <span>{{ a.daysElapsed }} de {{ a.minDays }} días en este hito</span>
            </div>
            <div class="fh-adv-row">
              <span class="fh-adv-check" [class.done]="a.icfMet" [class.pending]="!a.icfMet">{{ a.icfMet ? '✓' : '' }}</span>
              <span>
                @if (a.icfAvg > 0) { Evaluación de esta etapa en marcha }
                @else { Todavía sin evaluaciones registradas en este hito }
              </span>
            </div>
            <div class="fh-adv-row">
              <span class="fh-adv-check" [class.done]="a.tasksMet" [class.pending]="!a.tasksMet">{{ a.tasksMet ? '✓' : '' }}</span>
              <span>
                @if (a.totalTasks > 0) { {{ a.completedTasks }} de {{ a.totalTasks }} tareas completadas }
                @else { Aún no hay tareas asignadas en este hito }
              </span>
            </div>
          </div>
          <p class="fh-adv-footer">No es una carrera — cada familia avanza a su propio ritmo.</p>
        }
      </div>
    }

    @if (resolvingFamily() || store.loading()) {
      <div class="glass-card">
        <div class="skeleton skeleton-line lg"></div>
        <div class="skeleton skeleton-line" style="width: 80%"></div>
        <div class="skeleton skeleton-line" style="width: 40%"></div>
      </div>
    } @else if (!hasHomeId()) {
      <div class="glass-card fh-empty">
        <p>No pudimos identificar el Hogar Digital de tu familia todavía.</p>
        <a class="fh-link" routerLink="/families">Ir a Mis Familias</a>
      </div>
    } @else {
      @if (store.error(); as err) {
        <div class="glass-card fh-empty">
          <p>{{ err.detail }}</p>
          <button class="fh-cta fh-retry" (click)="reload()">Reintentar</button>
        </div>
      }
      @if (!store.error() && store.view(); as v) {
        @if (v.safetyPresentation.mode !== 'NONE') {
          <div class="fh-safety-banner" [class]="v.safetyPresentation.mode.toLowerCase()">
            <span class="fh-safety-icon">⚠️</span>
            <div class="fh-safety-text">
              <strong>{{ v.safetyPresentation.title }}</strong>
              <span>{{ v.safetyPresentation.message }}</span>
            </div>
          </div>
        }

        @if (unsupportedAction(); as msg) {
          <div class="fh-toast">{{ msg }}</div>
        }

        @if (isOnboardingHomeView(v)) {
          <app-onboarding-home-view [view]="v" (command)="onCommand($event)" />
        } @else if (isAssessmentHomeView(v)) {
          <app-assessment-home-view [view]="v" (command)="onCommand($event)" />
        } @else if (isReturnStageHomeView(v)) {
          <app-return-stage-home-view [view]="v" [actionPending]="store.actionPending()" (command)="onCommand($event)" />
        } @else if (isActiveHomeView(v)) {
          <app-active-home-view [view]="v" (command)="onCommand($event)" />
        } @else {
          <div class="glass-card fh-empty">
            <p>Esta vista del Hogar Digital todavía no está disponible en este panel.</p>
          </div>
        }
      }
    }
  `,
})
export class FamilyHomePageComponent implements OnInit {
  private readonly http = inject(HttpClient);
  private readonly api = inject(ApiService);
  private readonly router = inject(Router);
  readonly familyState = inject(FamilyStateService);
  readonly store = inject(FamilyHomeStoreService);
  private readonly advancementService = inject(MilestoneAdvancementService);

  readonly resolvingFamily = signal(false);
  readonly unsupportedAction = signal<string | null>(null);
  readonly advancement = signal<AdvancementEvaluation | null>(null);

  // Los 5 accesos permanentes del HUD Adaptativo, absorbidos aquí como el menú
  // fijo del Hogar Digital (ver ADR-002) — "Hoy" es esta misma página, el resto
  // apunta a páginas reales ya existentes en la app, no a contenido nuevo.
  readonly quickNav: { label: string; route: string | null }[] = [
    { label: 'Hoy', route: null },
    { label: 'Crecemos', route: '/transformation/route' },
    { label: 'Recordamos', route: '/family-timeline' },
    { label: 'Somos', route: '/family-dna' },
    { label: 'Conversamos', route: '/chat' },
  ];

  readonly hasHomeId = computed(() => !!this.familyState.currentHomeId());

  readonly isOnboardingHomeView = isOnboardingHomeView;
  readonly isAssessmentHomeView = isAssessmentHomeView;
  readonly isReturnStageHomeView = isReturnStageHomeView;
  readonly isActiveHomeView = isActiveHomeView;

  ngOnInit(): void {
    this.ensureHomeId();
    this.loadAdvancement();
  }

  nextMilestoneLabel(a: AdvancementEvaluation): string {
    return this.advancementService.nextMilestone(a.currentMilestone) ?? a.currentMilestone;
  }

  private loadAdvancement(): void {
    const familyId = this.familyState.getSelectedFamilyId();
    if (!familyId) return;
    this.advancementService.getStatus(familyId).subscribe(status => this.advancement.set(status));
  }

  reload(): void {
    const homeId = this.familyState.getSelectedHomeId();
    if (homeId) {
      this.store.load(homeId);
    }
  }

  onCommand(command: Command): void {
    if (!command.enabled) return;
    this.unsupportedAction.set(null);

    if (command.type === 'NAVIGATE') {
      this.router.navigateByUrl(command.target);
      return;
    }

    if (command.type === 'SUBMIT_ACTION' && command.target === 'accept-first-sprint') {
      const homeId = this.familyState.getSelectedHomeId();
      if (homeId) this.store.acceptFirstSprint(homeId);
      return;
    }

    this.unsupportedAction.set(`"${command.label}" todavía no está disponible en el Hogar Digital.`);
  }

  /**
   * homeId se persiste al elegir/crear familia (ver FamilyStateService.setFamily),
   * pero si el usuario aterriza aquí sin haber pasado por ese flujo, se resuelve
   * una vez contra /families/mine — mismo protocolo de auto-conexión usado en
   * DashboardPageComponent y EvaluationComponent.
   */
  private ensureHomeId(): void {
    const homeId = this.familyState.getSelectedHomeId();
    if (homeId) {
      this.store.load(homeId);
      return;
    }

    this.resolvingFamily.set(true);
    this.http.get<any>(`${this.api.base}/families/mine`)
      .pipe(catchError(() => of(null)))
      .subscribe((res) => {
        this.resolvingFamily.set(false);
        const family = res?.data ?? res;
        if (family?.id) {
          this.familyState.setFamily(family);
          const resolvedHomeId = this.familyState.getSelectedHomeId();
          if (resolvedHomeId) this.store.load(resolvedHomeId);
          this.loadAdvancement();
        }
      });
  }
}
