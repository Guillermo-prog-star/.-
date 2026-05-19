import { Component, OnInit, inject, ChangeDetectionStrategy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { HttpClient } from '@angular/common/http';
import { RouterLink } from '@angular/router';
import { Observable } from 'rxjs';
import { map, shareReplay } from 'rxjs/operators';

// Capa de Servicios
import { DashboardDataService } from './services/dashboard-data.service';
import { FamilyStateService } from '../../core/services/family-state.service';

// Capa de Modelos (SDD: Single Source of Truth)
import { DashboardDTO, DimensionScore } from '../../core/models/dashboard.model';

// Componentes de Presentación
import { IcfStatCardComponent } from './components/icf-stat-card/icf-stat-card.component';
import { EvolutionRadarComponent } from './components/evolution-radar/evolution-radar.component';
import { AiPlanTimelineComponent } from './components/ai-plan-timeline/ai-plan-timeline.component';
import { ScenariosGridComponent } from './components/scenarios-grid/scenarios-grid.component';
import { AiInsightPanelComponent } from './components/ai-insight-panel/ai-insight-panel.component';
import { SentinelAlertComponent } from './components/sentinel-alert/sentinel-alert.component';
import { CognitivePreviewComponent } from './components/cognitive-preview/cognitive-preview.component';

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
    EvolutionRadarComponent,
    AiPlanTimelineComponent,
    ScenariosGridComponent,
    AiInsightPanelComponent,
    SentinelAlertComponent,
    CognitivePreviewComponent
  ],
  templateUrl: './dashboard-page.component.html',
  styleUrls: ['./dashboard-page.component.css'],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class DashboardPageComponent implements OnInit {
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

  get familyName(): string {
    return localStorage.getItem('selectedFamilyName') || 'Familia';
  }

  ngOnInit(): void {
    const familyId = this.familyState.getSelectedFamilyId();
    // [SDD] Carga inicial del ecosistema
    this.dashboardService.fetchData(familyId).subscribe();
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