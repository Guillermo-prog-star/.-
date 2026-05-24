import { Component, OnInit, inject, ChangeDetectionStrategy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { HttpClient } from '@angular/common/http';
import { Router, RouterLink } from '@angular/router';
import { Observable, of } from 'rxjs';
import { map, shareReplay, catchError } from 'rxjs/operators';

// Capa de Servicios
import { DashboardDataService } from './services/dashboard-data.service';
import { FamilyStateService } from '../../core/services/family-state.service';
import { EmotionalEngineService } from '../../core/services/emotional-engine.service';

// Capa de Modelos (SDD: Single Source of Truth)
import { DashboardDTO, DimensionScore } from '../../core/models/dashboard.model';

// Componentes de Presentación
import { IcfStatCardComponent } from './components/icf-stat-card/icf-stat-card.component';
import { ConsciousnessOrbitComponent } from './components/consciousness-orbit/consciousness-orbit.component';
import { AiPlanTimelineComponent } from './components/ai-plan-timeline/ai-plan-timeline.component';
import { ScenariosGridComponent } from './components/scenarios-grid/scenarios-grid.component';
import { AiInsightPanelComponent } from './components/ai-insight-panel/ai-insight-panel.component';
import { SentinelAlertComponent } from './components/sentinel-alert/sentinel-alert.component';
import { CognitivePreviewComponent } from './components/cognitive-preview/cognitive-preview.component';
import { IcfTrendChartComponent } from './components/icf-trend-chart/icf-trend-chart.component';
import { AbandonmentRiskBannerComponent } from './components/abandonment-risk-banner/abandonment-risk-banner.component';
import { DimensionHistoryChartComponent } from './components/dimension-history-chart/dimension-history-chart.component';
import { NarrativeCompanionComponent } from '../../shared/components/narrative-companion.component';

/**
 * SDD: Dashboard Page Component
 * Postura Técnica: Reactividad declarativa con estrategia OnPush.
 * Sincronizado con DashboardDTO 2.0 (Capas de Estado y Seguridad).
 */
@Component({
  selector: 'app-dashboard-page',
  standalone: true,
  imports: [
    CommonModule,
    RouterLink,
    IcfStatCardComponent,
    ConsciousnessOrbitComponent,
    AiPlanTimelineComponent,
    ScenariosGridComponent,
    AiInsightPanelComponent,
    SentinelAlertComponent,
    CognitivePreviewComponent,
    IcfTrendChartComponent,
    AbandonmentRiskBannerComponent,
    DimensionHistoryChartComponent,
    NarrativeCompanionComponent
  ],
  templateUrl: './dashboard-page.component.html',
  styleUrls: ['./dashboard-page.component.css'],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class DashboardPageComponent implements OnInit {
  private readonly emotionalService = inject(EmotionalEngineService);
  private readonly router           = inject(Router);

  // [SDD] Inyección de Dependencias
  constructor(
    public readonly dashboardService: DashboardDataService,
    private readonly familyState: FamilyStateService,
    private readonly http: HttpClient
  ) {}

  /** * SDD: Stream Unificado. 
   * Se define explícitamente como DashboardDTO para resolver errores 'unknown'.
   */
  readonly state$: Observable<DashboardDTO | null> = this.dashboardService.getDashboardState$().pipe(
    shareReplay(1)
  );

  // SELECTORES REACTIVOS (Propiedades derivadas con Type-Safety)
  readonly summary$ = this.state$.pipe(
    map(state => state?.latestConsciousnessLabel ?? 'Iniciando Diagnóstico...')
  );

  readonly dimensions$: Observable<DimensionScore | null> = this.state$.pipe(
    map(state => state?.dimensionScores ?? null)
  );

  readonly readyToAdvance$: Observable<boolean> = this.state$.pipe(
    map(state => !!state?.readyToAdvance)
  );

  iocScore$: Observable<number> = of(50.0);

  get familyName(): string {
    return localStorage.getItem('selectedFamilyName') || 'Familia';
  }

  ngOnInit(): void {
    let familyId = this.familyState.getSelectedFamilyId();

    if (familyId === 0) {
      // [SDD Spec] Protocolo de Auto-Conexión:
      // Si el usuario no tiene familia seleccionada en localStorage, buscamos sus familias disponibles.
      this.http.get<any>('/api/families').subscribe({
        next: (res) => {
          const families = res?.data ?? res ?? [];
          if (Array.isArray(families) && families.length > 0) {
            const firstFamily = families[0];
            this.familyState.setFamily(firstFamily);
            familyId = firstFamily.id;

            this.dashboardService.fetchData(familyId).subscribe();

            this.iocScore$ = this.emotionalService.getFamilyStats(familyId).pipe(
              map(stats => stats?.ioc ?? 50.0),
              catchError(() => of(50.0)),
              shareReplay(1)
            );
          } else {
            // Si realmente no posee familias, redirigir a creación para desbloquear el onboarding
            this.router.navigate(['/families/create']);
          }
        },
        error: () => {
          this.router.navigate(['/families/create']);
        }
      });
    } else {
      // [SDD] Carga inicial del ecosistema cuando hay una familia activa
      this.dashboardService.fetchData(familyId).subscribe();

      this.iocScore$ = this.emotionalService.getFamilyStats(familyId).pipe(
        map(stats => stats?.ioc ?? 50.0),
        catchError(() => of(50.0)),
        shareReplay(1)
      );
    }
  }

  /**
   * [SDD Spec] Comando de Evolución: Avanza oficialmente el hito del núcleo familiar.
   * Tras el éxito, re-sincroniza el estado global.
   */
  advanceMilestone(): void {
    const familyId = this.familyState.getSelectedFamilyId();
    if (!familyId) return;

    this.http.post<any>(`/api/milestones/family/${familyId}/advance`, {}).subscribe({
      next: () => {
        console.log('🚀 [MILESTONE-EVOLVED] Nodo evolucionado.');
        this.dashboardService.fetchData(familyId).subscribe();
      },
      error: (err) => console.error('❌ [SDD-ERROR] Falla en evolución:', err)
    });
  }
}