import { Component, OnInit, inject, signal, computed, effect } from '@angular/core';
import { RouterOutlet, RouterLink } from '@angular/router';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { HttpClient } from '@angular/common/http';
import { ApiService } from '../core/services/api.service';
import { catchError, of } from 'rxjs';
import { SidebarComponent } from '../shared/components/sidebar.component';
import { NavbarComponent } from '../shared/components/navbar.component';
import { FeedbackDialogComponent } from '../shared/components/feedback-dialog/feedback-dialog.component';
import { SentinelCoreService } from '../core/services/sentinel-core.service';
import { ScrollPolicyDirective } from '../shared/directives/scroll-policy.directive';
import { ScrollPolicyService } from '../shared/directives/scroll-policy.service';
import { ExperienceEngineService } from '../core/services/experience-engine.service';
import { FamilyStateService } from '../core/services/family-state.service';

export interface FamilyDataView {
  familyId: number | null;
  familyName: string;
  assignmentId: number;
  specialty: string;
  accessLevel: number;
  icfScore: number | null;
  icfLabel: string | null;
  icfDirection: string | null;
  riskLevel: string | null;
  sentinelActive: boolean | null;
  planSummaryAvailable: boolean | null;
  hasActiveSprint: boolean | null;
  activeSprintStatus: string | null;
  crisisHistoryAvailable: boolean | null;
}

/**
 * SDD: Shell Sentinel Core
 * Postura Técnica: Orquestación de layout con monitoreo reactivo de crisis.
 */
@Component({
  selector: 'app-shell',
  standalone: true,
  imports: [
    RouterOutlet,
    RouterLink,
    CommonModule,
    FormsModule,
    SidebarComponent,
    NavbarComponent,
    FeedbackDialogComponent,
    ScrollPolicyDirective
  ],
  template: `
    <div class="flex h-screen bg-[#0a0a0c] overflow-hidden">
      <app-sidebar class="hidden md:block w-[280px] fixed h-full border-r border-white/5" />
      
      <div class="flex-1 md:ml-[280px] flex flex-col min-w-0 h-full overflow-hidden">
        
        @if (sentinel.hasCriticalAlert()) {
          <div class="bg-red-500/10 backdrop-blur-md border-b border-red-500/20 text-white px-6 py-3 flex justify-between items-center z-[60] shadow-[0_4px_30px_rgba(239,68,68,0.15)] relative overflow-hidden">
             <div class="absolute bottom-0 left-0 h-[2px] w-full bg-gradient-to-r from-transparent via-red-500 to-transparent animate-pulse"></div>
             <div class="flex items-center gap-3">
                <span class="text-xl animate-bounce">🚨</span>
                <span class="text-xs font-extrabold uppercase tracking-widest text-red-200">
                  Alerta Sentinel: Se requiere intervención en red Alfa
                </span>
             </div>
             <a [routerLink]="['/admin/stats']"
                  class="text-[10px] font-black bg-gradient-to-r from-red-600 to-rose-600 text-white px-5 py-2 rounded-xl hover:shadow-[0_0_15px_rgba(239,68,68,0.5)] transform hover:scale-105 transition-all uppercase tracking-wider border border-red-500/30">
                Analizar Crisis →
             </a>
          </div>
        }

        @if (isObserving()) {
          <div class="bg-gradient-to-r from-[#1e1b4b] to-[#311042] border-b border-purple-500/20 text-white px-6 py-2.5 flex justify-between items-center z-[60] shadow-[0_4px_30px_rgba(139,92,246,0.15)] relative overflow-hidden">
             <div class="absolute bottom-0 left-0 h-[2px] w-full bg-gradient-to-r from-transparent via-purple-500 to-transparent animate-pulse"></div>
             <div class="flex items-center gap-3">
                <span class="text-xl">👁️</span>
                <span class="text-xs font-extrabold uppercase tracking-widest text-purple-200">
                  Modo Observador Clínico — Familia: <span class="text-white underline decoration-purple-400 font-black">{{ currentFamilyName() }}</span>
                </span>
             </div>
             
             <div class="flex items-center gap-3">
               <button (click)="toggleHud()"
                   class="text-[10px] font-black bg-gradient-to-r from-purple-600 to-pink-600 hover:from-purple-500 hover:to-pink-500 text-white px-4 py-1.5 rounded-lg hover:shadow-[0_0_15px_rgba(236,72,153,0.4)] transform hover:scale-105 transition-all uppercase tracking-wider border border-pink-500/30 cursor-pointer flex items-center gap-1">
                 <span>📋</span> <span>Resumen Clínico (HUD)</span>
               </button>
               
               <button (click)="exitObserverMode()"
                   class="text-[10px] font-black bg-gradient-to-r from-blue-600 to-purple-600 hover:from-blue-500 hover:to-purple-500 text-white px-4 py-1.5 rounded-lg hover:shadow-[0_0_15px_rgba(139,92,246,0.5)] transform hover:scale-105 transition-all uppercase tracking-wider border border-purple-500/30 cursor-pointer">
                 ← Volver al Panel
               </button>
             </div>
          </div>
        }

        <app-navbar />
        
        <main class="p-4 md:p-6 flex-1 overflow-y-auto min-h-0"
              [scrollPolicy]="scrollPolicy.policy()"
              [criticalAlert]="scrollPolicy.critical()">
          <router-outlet (activate)="scrollPolicy.reset()" />
        </main>
      </div>

      <app-feedback-dialog />

      <!-- ─── CLINICAL HUD DRAWER ─────────────────────────────────── -->
      @if (isObserving() && showHud()) {
        <div class="hud-backdrop" (click)="toggleHud()"></div>
        <div class="hud-drawer">
          <div class="hud-header">
            <div>
              <div class="hud-subtitle">RESUMEN CLÍNICO</div>
              <h3 class="hud-title">{{ currentFamilyName() }}</h3>
            </div>
            <button class="hud-close" (click)="toggleHud()">✕</button>
          </div>

          <div class="hud-body">
            @if (loadingView()) {
              <div class="hud-loading">Cargando datos autorizados...</div>
            } @else if (!dataView()) {
              <div class="hud-error">
                <span class="hud-error-icon">🔒</span>
                <p class="font-bold">Datos no disponibles</p>
                <p class="text-xs text-white/50 mt-1">El profesional de apoyo no cuenta con consentimiento activo para esta familia.</p>
              </div>
            } @else {
              <!-- ICF & RIESGO ROW -->
              <div class="hud-grid">
                <!-- ICF -->
                @if (dataView()!.icfScore !== null) {
                  <div class="hud-card">
                    <span class="hud-card-lbl">Índice Cohesión (ICaF)</span>
                    <div class="flex items-baseline gap-2 mt-1">
                      <span class="hud-card-val" [style.color]="icfColor()">{{ dataView()!.icfScore!.toFixed(1) }}</span>
                      <span class="hud-card-dir" [style.color]="icfColor()">{{ directionIcon(dataView()!.icfDirection) }}</span>
                    </div>
                    <div class="hud-card-sub" [style.color]="icfColor()">{{ dataView()!.icfLabel }}</div>
                  </div>
                }

                <!-- RIESGO -->
                @if (dataView()!.riskLevel) {
                  <div class="hud-card" [class.hud-card--danger]="dataView()!.riskLevel === 'ALTO' || dataView()!.sentinelActive">
                    <span class="hud-card-lbl">Nivel de Riesgo</span>
                    <div class="hud-card-val text-white mt-1">{{ dataView()!.riskLevel }}</div>
                    @if (dataView()!.sentinelActive) {
                      <div class="hud-card-sub text-red-400 font-bold">🔴 Centinela Activo</div>
                    } @else {
                      <div class="hud-card-sub text-white/40">Estable</div>
                    }
                  </div>
                }
              </div>

              <!-- SPRINT & PLAN -->
              <div class="hud-section-title">PROGRAMA DE INTERVENCIÓN</div>
              <div class="hud-list">
                <div class="hud-list-item">
                  <span class="hud-li-icon">⚡</span>
                  <div class="flex-1">
                    <div class="hud-li-title">Sprint Familiar</div>
                    <div class="hud-li-desc">
                      {{ dataView()!.hasActiveSprint ? 'Activo — Estado: ' + dataView()!.activeSprintStatus : 'Sin sprint activo' }}
                    </div>
                  </div>
                </div>

                <div class="hud-list-item">
                  <span class="hud-li-icon">📋</span>
                  <div class="flex-1">
                    <div class="hud-li-title">Plan de Mejora</div>
                    <div class="hud-li-desc">
                      {{ dataView()!.planSummaryAvailable ? 'Plan autorizado y en ejecución' : 'Sin plan autorizado' }}
                    </div>
                  </div>
                </div>

                <div class="hud-list-item">
                  <span class="hud-li-icon">⚠️</span>
                  <div class="flex-1">
                    <div class="hud-li-title">Historial de Crisis</div>
                    <div class="hud-li-desc">
                      {{ dataView()!.crisisHistoryAvailable ? 'Acceso disponible para revisión' : 'Sin acceso a crisis' }}
                    </div>
                  </div>
                </div>
              </div>

              <!-- NOTA CLÍNICA RÁPIDA -->
              <div class="hud-section-title">DEJAR NOTA CLÍNICA</div>
              <div class="hud-notes-box">
                <textarea class="hud-ta" rows="4" 
                    placeholder="Escribe una observación clínica de seguimiento..." 
                    [(ngModel)]="noteContent">
                </textarea>
                <div class="flex items-center justify-between mt-2.5">
                  <label class="hud-vis-toggle">
                    <input type="checkbox" [(ngModel)]="noteVisible">
                    <span>Visible para familia</span>
                  </label>
                  <button class="hud-save-btn" [disabled]="noteLoading() || !noteContent.trim()" (click)="saveNote()">
                    {{ noteLoading() ? 'Guardando...' : 'Guardar Nota' }}
                  </button>
                </div>
                @if (noteSuccess()) {
                  <div class="hud-note-ok">✅ Nota guardada correctamente.</div>
                }
              </div>
            }
          </div>
        </div>
      }
    </div>
  `,
  styles: [`
    .hud-backdrop {
      position: fixed; top: 0; left: 0; width: 100vw; height: 100vh;
      background: rgba(0,0,0,0.6); z-index: 1040; backdrop-filter: blur(4px);
    }
    .hud-drawer {
      position: fixed; top: 0; right: 0; width: 420px; height: 100vh;
      background: #0d0d11; border-left: 1px solid rgba(255,255,255,0.08);
      z-index: 1050; display: flex; flex-direction: column;
      font-family: 'Inter', sans-serif; box-shadow: -10px 0 40px rgba(0,0,0,0.6);
      animation: slideIn 0.25s cubic-bezier(0.16, 1, 0.3, 1) forwards;
    }
    @keyframes slideIn {
      from { transform: translateX(100%); }
      to { transform: translateX(0); }
    }
    .hud-header {
      padding: 24px; border-bottom: 1px solid rgba(255,255,255,0.06);
      display: flex; align-items: center; justify-content: space-between;
    }
    .hud-subtitle { font-size: 9px; font-weight: 800; color: #a78bfa; letter-spacing: 0.1em; }
    .hud-title { font-size: 18px; font-weight: 800; color: #fff; margin: 2px 0 0; }
    .hud-close { background: none; border: none; color: rgba(255,255,255,0.4); font-size: 18px; cursor: pointer; }
    .hud-close:hover { color: #fff; }
    .hud-body { padding: 24px; flex: 1; overflow-y: auto; display: flex; flex-direction: column; gap: 20px; }
    .hud-loading { color: rgba(255,255,255,0.4); font-size: 13px; text-align: center; padding: 40px 0; }
    .hud-error { text-align: center; padding: 40px 20px; background: rgba(255,255,255,0.02); border-radius: 12px; border: 1px dashed rgba(255,255,255,0.1); }
    .hud-error-icon { font-size: 28px; display: block; margin-bottom: 12px; }
    .hud-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 12px; }
    .hud-card { padding: 14px; background: rgba(255,255,255,0.03); border-radius: 10px; border: 1px solid rgba(255,255,255,0.06); }
    .hud-card--danger { background: rgba(239,68,68,0.05); border-color: rgba(239,68,68,0.2); }
    .hud-card-lbl { font-size: 10px; font-weight: 700; color: rgba(255,255,255,0.4); text-transform: uppercase; }
    .hud-card-val { font-size: 24px; font-weight: 900; }
    .hud-card-dir { font-size: 18px; font-weight: 700; }
    .hud-card-sub { font-size: 11px; margin-top: 2px; }
    .hud-section-title { font-size: 10px; font-weight: 800; color: rgba(255,255,255,0.3); text-transform: uppercase; letter-spacing: 0.05em; margin-bottom: -10px; }
    .hud-list { display: flex; flex-direction: column; gap: 10px; }
    .hud-list-item { display: flex; gap: 12px; padding: 12px; background: rgba(255,255,255,0.02); border-radius: 8px; border: 1px solid rgba(255,255,255,0.04); align-items: center; }
    .hud-li-icon { font-size: 16px; width: 24px; height: 24px; display: flex; align-items: center; justify-content: center; background: rgba(255,255,255,0.04); border-radius: 6px; }
    .hud-li-title { font-size: 12px; font-weight: 700; color: #fff; }
    .hud-li-desc { font-size: 11px; color: rgba(255,255,255,0.4); margin-top: 1px; }
    .hud-notes-box { padding: 14px; background: rgba(255,255,255,0.02); border: 1px solid rgba(255,255,255,0.06); border-radius: 10px; }
    .hud-ta { width: 100%; background: rgba(0,0,0,0.3); border: 1px solid rgba(255,255,255,0.1); border-radius: 6px; padding: 10px; color: #fff; font-size: 12px; font-family: inherit; resize: none; }
    .hud-ta:focus { outline: none; border-color: #a78bfa; }
    .hud-vis-toggle { display: flex; align-items: center; gap: 6px; font-size: 11px; color: rgba(255,255,255,0.5); cursor: pointer; }
    .hud-save-btn { background: #6366f1; color: #fff; font-size: 11px; font-weight: 700; padding: 6px 14px; border-radius: 6px; border: none; cursor: pointer; }
    .hud-save-btn:hover:not(:disabled) { background: #4f46e5; }
    .hud-save-btn:disabled { opacity: 0.5; cursor: not-allowed; }
    .hud-note-ok { font-size: 11px; color: #10b981; margin-top: 8px; font-weight: bold; }
  `]
})
export class ShellComponent implements OnInit {
  private api = inject(ApiService);
  private http = inject(HttpClient);

  readonly showHud = signal(false);
  readonly loadingView = signal(false);
  readonly dataView = signal<FamilyDataView | null>(null);

  noteContent = '';
  noteVisible = true;
  readonly noteLoading = signal(false);
  readonly noteSuccess = signal(false);

  constructor(
    public sentinel:     SentinelCoreService,
    public scrollPolicy: ScrollPolicyService,
    private expEngine:   ExperienceEngineService,
    private familySvc:   FamilyStateService,
  ) {
    // React to changes in observer session and fetch data
    effect(() => {
      const isObs = this.isObserving();
      const fid = this.familySvc.getSelectedFamilyId();
      const aid = this.familySvc.currentAssignmentId();
      
      if (isObs && fid && aid) {
        this.loadDataView(fid, aid);
      }
    });
  }

  isObserving(): boolean {
    return this.familySvc.isObserving();
  }

  currentFamilyName(): string {
    return this.familySvc.currentFamilyName();
  }

  exitObserverMode(): void {
    this.familySvc.clearFamily();
    window.location.href = '/professional';
  }

  toggleHud(): void {
    this.showHud.update(v => !v);
    if (this.showHud()) {
      this.noteSuccess.set(false);
      this.noteContent = '';
    }
  }

  private loadDataView(familyId: number, assignmentId: number) {
    this.loadingView.set(true);
    this.dataView.set(null);
    this.http.get<FamilyDataView>(`${this.api.base}/families/${familyId}/support/data-view?assignmentId=${assignmentId}`)
      .pipe(catchError(() => of(null)))
      .subscribe(v => {
        this.dataView.set(v);
        this.loadingView.set(false);
      });
  }

  icfColor = computed(() => {
    const s = this.dataView()?.icfScore;
    if (!s) return '#94a3b8';
    if (s >= 80) return '#10b981';
    if (s >= 60) return '#3b82f6';
    if (s >= 40) return '#f59e0b';
    return '#ef4444';
  });

  directionIcon(d: string | null | undefined): string {
    return ({ IMPROVING: '↑', STABLE: '→', DECLINING: '↓', CRITICAL_DECLINE: '↓↓' } as any)[d ?? ''] ?? '—';
  }

  icfLabel(): string {
    return this.dataView()?.icfLabel ?? '—';
  }

  saveNote(): void {
    const fid = this.familySvc.getSelectedFamilyId();
    const aid = this.familySvc.currentAssignmentId();
    if (!fid || !aid || !this.noteContent.trim()) return;

    this.noteLoading.set(true);
    this.noteSuccess.set(false);

    this.http.post<any>(`${this.api.base}/families/${fid}/support/notes`, {
      assignmentId: aid,
      content: this.noteContent,
      visibleToFamily: this.noteVisible
    }).pipe(catchError(() => of(null))).subscribe(r => {
      this.noteLoading.set(false);
      if (r) {
        this.noteSuccess.set(true);
        this.noteContent = '';
      }
    });
  }

  ngOnInit(): void {
    // Carga el contexto familiar al entrar al shell para que
    // la clase CSS del body se aplique antes de que renderice el dashboard
    if (this.familySvc.getSelectedFamilyId()) {
      this.expEngine.load();
    }
  }
}
