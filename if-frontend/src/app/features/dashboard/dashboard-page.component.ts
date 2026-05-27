import { Component, OnInit, OnDestroy, inject, ChangeDetectionStrategy, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { HttpClient } from '@angular/common/http';
import { Router, RouterLink } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { Observable, of, BehaviorSubject, Subject } from 'rxjs';
import { map, shareReplay, catchError, switchMap, filter, takeUntil } from 'rxjs/operators';

// Capa de Servicios
import { DashboardDataService } from './services/dashboard-data.service';
import { FamilyStateService } from '../../core/services/family-state.service';
import { EmotionalEngineService } from '../../core/services/emotional-engine.service';
import { ScannerService } from '../../core/services/scanner.service';

// Capa de Modelos (SDD: Single Source of Truth)
import { DashboardDTO, DimensionScore } from '../../core/models/dashboard.model';
import { OperationalStateDto, FamilyAlertDto } from '../../core/models/models';

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
import { GuardianPanelComponent } from '../guardian/guardian-panel.component';

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
    FormsModule,
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
    NarrativeCompanionComponent,
    GuardianPanelComponent
  ],
  templateUrl: './dashboard-page.component.html',
  styleUrls: ['./dashboard-page.component.css'],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class DashboardPageComponent implements OnInit, OnDestroy {
  private readonly emotionalService = inject(EmotionalEngineService);
  private readonly scannerService   = inject(ScannerService);
  private readonly router           = inject(Router);

  // Estado para modal de WhatsApp
  readonly showWhatsappModal = signal(false);
  whatsappNumber             = '';
  readonly savingWhatsapp    = signal(false);
  readonly whatsappError     = signal('');

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

  /** IF-TOS: Estado operacional actual, se alimenta una vez que ngOnInit resuelve el familyId */
  private readonly resolvedFamilyId$ = new BehaviorSubject<number>(0);
  private readonly destroy$ = new Subject<void>();

  readonly operationalState$: Observable<OperationalStateDto | null> =
    this.resolvedFamilyId$.pipe(
      filter(id => id > 0),
      switchMap(id => this.scannerService.getOperationalState(id).pipe(catchError(() => of(null)))),
      shareReplay(1)
    );

  /** IF-ALT: Alertas clínicas activas sin resolver — BehaviorSubject para actualizaciones optimistas */
  private readonly _alerts = new BehaviorSubject<FamilyAlertDto[]>([]);
  readonly alerts$ = this._alerts.asObservable();

  get familyName(): string {
    return this.familyState.currentFamilyName() || 'Familia';
  }

  get guardianFamilyId(): number {
    return this.familyState.getSelectedFamilyId() ?? 0;
  }

  /**
   * ID del miembro autenticado, leído desde la señal de FamilyStateService.
   */
  get guardianMemberId(): number | undefined {
    return this.familyState.currentMemberId() ?? undefined;
  }

  /** Resuelve y persiste el memberId del usuario autenticado dado un array de miembros. */
  private resolveCurrentMemberId(members: any[]): void {
    if (this.familyState.currentMemberId() != null) return; // ya resuelto
    try {
      const authUser = JSON.parse(localStorage.getItem('auth_user') || '{}');
      const matched = members.find((m: any) => m.email === authUser.email);
      if (matched?.id) this.familyState.setMemberId(matched.id);
    } catch { /* ignorar */ }
  }

  private checkWhatsappConfiguration(familyId: number): void {
    if (localStorage.getItem('whatsapp_dismissed') === 'true') {
      return;
    }
    this.http.get<any>('/api/families/mine').subscribe({
      next: (res) => {
        const family = res?.data ?? res;
        if (family && (!family.whatsapp || family.whatsapp.trim() === '')) {
          this.showWhatsappModal.set(true);
        }
      },
      error: (err) => console.warn('⚠️ [WHATSAPP-CHECK] Error consultando datos de familia:', err)
    });
  }

  activateWhatsapp(): void {
    const num = this.whatsappNumber.trim();
    if (num.length < 10) {
      this.whatsappError.set('Ingresa un número de celular válido de 10 dígitos.');
      return;
    }
    const familyId = this.familyState.getSelectedFamilyId();
    if (!familyId) return;

    this.savingWhatsapp.set(true);
    this.whatsappError.set('');

    this.http.get<any>(`/api/families/${familyId}`).subscribe({
      next: (res) => {
        const familyData = res?.data ?? res;
        familyData.whatsapp = num;

        this.http.put<any>(`/api/families/${familyId}`, familyData).subscribe({
          next: (updateRes) => {
            const updatedFamily = updateRes?.data ?? updateRes;
            this.familyState.setFamily(updatedFamily);
            this.savingWhatsapp.set(false);
            this.showWhatsappModal.set(false);
            console.log('✅ [WHATSAPP-ACTIVATED] WhatsApp activado con éxito.');
          },
          error: (err) => {
            console.error('❌ [WHATSAPP-ACTIVATION-FAILED] Error guardando WhatsApp:', err);
            this.savingWhatsapp.set(false);
            this.whatsappError.set('Error guardando la información. Intenta de nuevo.');
          }
        });
      },
      error: (err) => {
        console.error('❌ [WHATSAPP-CHECK-FAILED] Error recuperando datos de familia:', err);
        this.savingWhatsapp.set(false);
        this.whatsappError.set('Error al recuperar datos. Intenta de nuevo.');
      }
    });
  }

  dismissWhatsappModal(): void {
    localStorage.setItem('whatsapp_dismissed', 'true');
    this.showWhatsappModal.set(false);
    console.log('💤 [WHATSAPP-DISMISSED] El modal de WhatsApp ha sido pospuesto.');
  }

  ngOnInit(): void {
    // Suscripción reactiva: cuando se resuelve el familyId, carga las alertas IF-ALT.
    this.resolvedFamilyId$.pipe(
      filter(id => id > 0),
      switchMap(id => this.scannerService.getAlerts(id).pipe(catchError(() => of([])))),
      takeUntil(this.destroy$)
    ).subscribe(alerts => this._alerts.next(alerts));

    let familyId = this.familyState.getSelectedFamilyId();

    if (familyId === 0) {
      // [SDD Spec] Protocolo de Auto-Conexión:
      // Recupera la familia del usuario autenticado desde el backend (única fuente de verdad).
      this.http.get<any>('/api/families/mine').subscribe({
        next: (res) => {
          const family = res?.data ?? res;
          if (family && family.id) {
            this.familyState.setFamily(family);
            familyId = family.id;

            // Resolver memberId del usuario autenticado
            if (family.members?.length) this.resolveCurrentMemberId(family.members);

            this.resolvedFamilyId$.next(familyId);
            this.dashboardService.fetchData(familyId).subscribe();

            this.iocScore$ = this.emotionalService.getFamilyStats(familyId).pipe(
              map(stats => stats?.ioc ?? 50.0),
              catchError(() => of(50.0)),
              shareReplay(1)
            );
            this.checkWhatsappConfiguration(familyId);
          } else {
            this.router.navigate(['/families/create']);
          }
        },
        error: () => {
          // 404 = sin familia; cualquier otro error: redirigir igualmente a creación.
          this.router.navigate(['/families/create']);
        }
      });
    } else {
      // [SDD] Carga inicial del ecosistema cuando hay una familia activa
      // Resolver memberId si aún no está en cache
      if (!this.familyState.currentMemberId()) {
        this.http.get<any>('/api/families/mine').subscribe({
          next: (res) => {
            const family = res?.data ?? res;
            if (family?.members?.length) this.resolveCurrentMemberId(family.members);
          }
        });
      }
      this.resolvedFamilyId$.next(familyId);
      this.dashboardService.fetchData(familyId).subscribe();

      this.iocScore$ = this.emotionalService.getFamilyStats(familyId).pipe(
        map(stats => stats?.ioc ?? 50.0),
        catchError(() => of(50.0)),
        shareReplay(1)
      );
      this.checkWhatsappConfiguration(familyId);
    }
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  /**
   * [SDD Spec] Comando de Evolución: Avanza oficialmente el hito del núcleo familiar.
   * Tras el éxito, re-sincroniza el estado global.
   */
  // ── IF-TOS helpers ───────────────────────────────────────────────────────
  tosStateColor(s: string | null | undefined): string {
    const map: Record<string, string> = {
      EMERGING: '#94a3b8', STABLE: '#60a5fa', ESCALATING: '#f97316',
      CRITICAL: '#ef4444', RECOVERING: '#22d3ee', RESOLVED: '#34d399'
    };
    return map[s ?? ''] ?? '#64748b';
  }

  tosStateGlyph(s: string | null | undefined): string {
    const map: Record<string, string> = {
      EMERGING: '🌱', STABLE: '🔵', ESCALATING: '🔶',
      CRITICAL: '🔴', RECOVERING: '🔷', RESOLVED: '✅'
    };
    return map[s ?? ''] ?? '⬡';
  }

  tosStateClass(s: string | null | undefined): string {
    const map: Record<string, string> = {
      EMERGING: 'tos-emerging', STABLE: 'tos-stable', ESCALATING: 'tos-escalating',
      CRITICAL: 'tos-critical', RECOVERING: 'tos-recovering', RESOLVED: 'tos-resolved'
    };
    return map[s ?? ''] ?? 'tos-emerging';
  }

  alertSeverityColor(severity: string): string {
    const m: Record<string, string> = {
      LOW: '#60a5fa', MEDIUM: '#fbbf24', HIGH: '#f97316', CRITICAL: '#ef4444'
    };
    return m[severity] ?? '#94a3b8';
  }

  alertSeverityIcon(severity: string): string {
    const m: Record<string, string> = {
      LOW: '🔵', MEDIUM: '🟡', HIGH: '🟠', CRITICAL: '🔴'
    };
    return m[severity] ?? '⚪';
  }

  resolveAlert(alert: FamilyAlertDto): void {
    const familyId = this.familyState.getSelectedFamilyId();
    // Actualización optimista: eliminar de la lista inmediatamente.
    this._alerts.next(this._alerts.value.filter(a => a.id !== alert.id));
    this.scannerService.resolveAlert(familyId, alert.id).pipe(
      catchError(() => of(void 0))
    ).subscribe();
  }

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