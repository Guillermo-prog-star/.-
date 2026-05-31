import { Component, inject, computed, signal, OnInit } from '@angular/core';
import { RouterLink, RouterLinkActive, Router, NavigationEnd } from '@angular/router';
import { CommonModule } from '@angular/common';
import { AuthService } from '../../core/services/auth.service';
import { FamilyStateService } from '../../core/services/family-state.service';
import { TransformationFlowService } from '../../core/services/transformation-flow.service';
import { filter } from 'rxjs/operators';

/**
 * SidebarComponent — Viaje de Transformación Familiar
 *
 * Rediseñado para reflejar el ciclo de transformación, no módulos aislados.
 * El orden de navegación sigue el flujo maestro de 36 meses.
 */
@Component({
  selector: 'app-sidebar',
  standalone: true,
  imports: [CommonModule, RouterLink, RouterLinkActive],
  template: `
    <div class="sidebar">
      <!-- ─── BRAND ──────────────────────────────── -->
      <div class="brand">
        <div class="sidebar-logo-container">
          <img src="assets/logo.svg" alt="Integrity Family" class="sidebar-logo" />
        </div>
        <div class="version-tag">SISTEMA OPERATIVO FAMILIAR</div>
      </div>

      <!-- ─── ESTADO EVOLUTIVO ──────────────────── -->
      @if (familyState.currentFamilyCode()) {
        <div class="evolution-card">
          <div class="ev-code">{{ familyState.currentFamilyCode() }}</div>
          <div class="ev-pillar">{{ flow.pillarLabel() }}</div>
          <div class="ev-meta">
            <span class="ev-month">{{ flow.milestoneLabel() }}</span>
            <span class="ev-phase">{{ flow.currentPhaseLabel() }}</span>
          </div>
          <div class="ev-bar-wrap">
            <div class="ev-bar" [style.width.%]="flow.progressPercent()"></div>
          </div>
          <div class="ev-pct">{{ flow.progressPercent() }}% completado</div>
        </div>
      }

      <nav>

        <!-- ── BLOQUE 1: CONFIGURACIÓN INICIAL ──── -->
        <div class="section-label">CONFIGURACIÓN</div>

        <a routerLink="/families" class="nav-item" routerLinkActive="active">
          <span class="step-dot" [class.done]="isSetupDone('family')">{{ isSetupDone('family') ? '✓' : '1' }}</span>
          <span class="nav-text">Familia</span>
        </a>
        <a routerLink="/members" class="nav-item" routerLinkActive="active">
          <span class="step-dot" [class.done]="isSetupDone('members')">{{ isSetupDone('members') ? '✓' : '2' }}</span>
          <span class="nav-text">Miembros</span>
        </a>
        <a [routerLink]="guardianRoute()" class="nav-item guardian-nav" routerLinkActive="active">
          <span class="step-dot" [class.done]="isSetupDone('guardian')">{{ isSetupDone('guardian') ? '✓' : '3' }}</span>
          <span class="nav-text">Guardián Familiar</span>
        </a>

        <div class="divider"></div>

        <!-- ── BLOQUE 2: DIAGNÓSTICO ─────────────── -->
        <div class="section-label">DIAGNÓSTICO</div>

        <div class="nav-group" [class.expanded]="diagExpanded" [class.group-active]="isDiagActive()">
          <button type="button" class="nav-item group-header" (click)="toggleDiag($event)">
            <span class="step-dot" [class.done]="isSetupDone('diagnosis')">{{ isSetupDone('diagnosis') ? '✓' : '4' }}</span>
            <span class="nav-text">Diagnóstico Familiar</span>
            <span class="chevron" [class.rotated]="diagExpanded">▶</span>
          </button>
          @if (diagExpanded) {
            <div class="sub-menu">
              <a routerLink="/evaluations/start"     class="nav-sub" routerLinkActive="active">🔍 Iniciar evaluación</a>
              <a routerLink="/evaluations/history"   class="nav-sub" routerLinkActive="active">📋 Historial</a>
              <a routerLink="/evaluations/evolution" class="nav-sub" routerLinkActive="active">📈 Evolución</a>
              <a routerLink="/evaluations/analytics" class="nav-sub" routerLinkActive="active">🧪 Panel Clínico</a>
            </div>
          }
        </div>

        <div class="divider"></div>

        <!-- ── BLOQUE 3: PLAN AUTO-GENERADO ─────── -->
        <div class="section-label">PLAN &amp; RUTA</div>

        <a routerLink="/plans" class="nav-item" routerLinkActive="active">
          <span class="step-dot" [class.done]="isSetupDone('plan')">{{ isSetupDone('plan') ? '✓' : '5' }}</span>
          <span class="nav-text">Plan Familiar</span>
          <span class="badge-auto">AUTO</span>
        </a>
        <a routerLink="/transformation/route" class="nav-item" routerLinkActive="active">
          <span class="step-dot neutral">◈</span>
          <span class="nav-text">Ruta de 36 Meses</span>
        </a>

        <div class="divider"></div>

        <!-- ── BLOQUE 4: TRANSFORMACIÓN DIARIA ──── -->
        <div class="section-label">TRANSFORMACIÓN DIARIA</div>

        <a routerLink="/logbook"                     class="nav-item" routerLinkActive="active">
          <span class="nav-icon">📔</span><span class="nav-text">Bitácora &amp; Daily</span>
        </a>
        <a routerLink="/transformation/weekly-plan"  class="nav-item" routerLinkActive="active">
          <span class="nav-icon">📅</span><span class="nav-text">Planeación Semanal</span>
        </a>
        <a routerLink="/checklist"                   class="nav-item" routerLinkActive="active">
          <span class="nav-icon">📸</span><span class="nav-text">Evidencias</span>
        </a>
        <a routerLink="/transformation/error-protocol" class="nav-item" routerLinkActive="active">
          <span class="nav-icon">🔄</span><span class="nav-text">Gestión de Errores</span>
        </a>

        <div class="divider"></div>

        <!-- ── BLOQUE 5: APOYO Y CRISIS ──────────── -->
        <div class="section-label">APOYO</div>

        <a routerLink="/crisis"    class="nav-item crisis-btn" routerLinkActive="active">
          <span class="nav-icon">🆘</span><span class="nav-text">Crisis Familiar</span>
        </a>
        <a routerLink="/chat"      class="nav-item" routerLinkActive="active">
          <span class="nav-icon">✨</span><span class="nav-text">Consultor IA</span>
        </a>
        <a routerLink="/cognitive" class="nav-item" routerLinkActive="active">
          <span class="nav-icon">🧠</span><span class="nav-text">Sistema Cognitivo</span>
        </a>

        <div class="divider"></div>

        <!-- ── BLOQUE 6: LEGADO ───────────────────── -->
        <div class="section-label">LEGADO</div>

        <a routerLink="/gratitude" class="nav-item" routerLinkActive="active">
          <span class="nav-icon">💖</span><span class="nav-text">Gratitud Familiar</span>
        </a>
        <a routerLink="/my-space"  class="nav-item" routerLinkActive="active">
          <span class="nav-icon">🔒</span><span class="nav-text">Mi Espacio</span>
        </a>
        <a routerLink="/legado"    class="nav-item legacy-btn" routerLinkActive="active">
          <span class="nav-icon">🏛️</span><span class="nav-text">Legado Familiar</span>
        </a>

        <div class="divider"></div>

        <!-- ── BLOQUE 7: SISTEMA ─────────────────── -->
        <div class="section-label">SISTEMA</div>

        <a routerLink="/dashboard" class="nav-item" routerLinkActive="active">
          <span class="nav-icon">📊</span><span class="nav-text">Panel Analítico</span>
        </a>
        <a routerLink="/portal"    class="nav-item" routerLinkActive="active">
          <span class="nav-icon">📱</span><span class="nav-text">Portal Móvil</span>
        </a>
        <a routerLink="/profile"   class="nav-item" routerLinkActive="active">
          <span class="nav-icon">👤</span><span class="nav-text">Mi Perfil</span>
        </a>

        @if (user()?.role === 'ADMIN') {
          <div class="divider"></div>
          <div class="section-label">ADMINISTRACIÓN</div>
          <a routerLink="/admin/stats"        class="nav-item admin-item" routerLinkActive="active"><span class="nav-icon">⚙️</span> Estadísticas</a>
          <a routerLink="/admin/eedsl"        class="nav-item admin-item" routerLinkActive="active"><span class="nav-icon">🔧</span> Reglas EEDSL</a>
          <a routerLink="/admin/sandbox"      class="nav-item admin-item" routerLinkActive="active"><span class="nav-icon">🧪</span> Sandbox</a>
          <a routerLink="/admin/voice-monitor" class="nav-item admin-item" routerLinkActive="active"><span class="nav-icon">🎙️</span> Monitor Voz</a>
        }

      </nav>

      <!-- ─── FAMILIA BOX ─────────────────────────── -->
      <div class="family-box">
        <div class="f-name">{{ user()?.fullName }}</div>
        <div class="f-role">{{ user()?.role }}</div>
        @if (showLogoutConfirm()) {
          <div class="logout-confirm">
            <span class="confirm-text">¿Finalizar sesión?</span>
            <div class="confirm-actions">
              <button (click)="confirmLogout()" class="confirm-yes">Sí</button>
              <button (click)="cancelLogout()"  class="confirm-no">No</button>
            </div>
          </div>
        } @else {
          <button (click)="handleLogout()" class="logout-link">Cerrar Sesión</button>
        }
      </div>
    </div>
  `,
  styles: [`
    .sidebar {
      width: 280px; background: #0a0a0c; height: 100vh; padding: 24px 0;
      display: flex; flex-direction: column;
      position: fixed; top: 0; left: 0;
      border-right: 1px solid rgba(255,255,255,0.05); z-index: 1000;
      font-family: 'Inter', sans-serif;
    }
    .brand { display: flex; flex-direction: column; align-items: center; padding: 0 20px 16px; border-bottom: 1px solid rgba(255,255,255,0.06); margin-bottom: 12px; }
    .sidebar-logo-container { width: 80px; height: 96px; border-radius: 16px; overflow: hidden; box-shadow: 0 8px 24px rgba(0,0,0,0.5); border: 1px solid rgba(255,255,255,0.05); transition: transform 0.3s; }
    .sidebar-logo-container:hover { transform: scale(1.05); }
    .sidebar-logo { width: 100%; height: 100%; object-fit: cover; }
    .version-tag { font-size: 7px; color: #818cf8; font-weight: 800; background: rgba(99,102,241,0.12); padding: 3px 10px; border-radius: 20px; margin-top: 8px; border: 1px solid rgba(99,102,241,0.2); letter-spacing: 0.06em; text-transform: uppercase; }

    .evolution-card { margin: 0 16px 12px; padding: 10px 14px; background: linear-gradient(135deg, rgba(99,102,241,0.08), rgba(139,92,246,0.06)); border: 1px solid rgba(99,102,241,0.18); border-radius: 10px; }
    .ev-code  { font-size: 9px; color: #818cf8; font-weight: 800; letter-spacing: 0.06em; text-transform: uppercase; }
    .ev-pillar { font-size: 12px; font-weight: 700; color: #fff; margin: 3px 0 2px; }
    .ev-meta  { display: flex; gap: 8px; margin-bottom: 6px; flex-wrap: wrap; }
    .ev-month { font-size: 10px; color: rgba(255,255,255,0.5); background: rgba(255,255,255,0.06); padding: 1px 6px; border-radius: 4px; }
    .ev-phase { font-size: 10px; color: rgba(255,255,255,0.4); }
    .ev-bar-wrap { height: 3px; background: rgba(255,255,255,0.06); border-radius: 2px; overflow: hidden; margin-bottom: 3px; }
    .ev-bar  { height: 100%; background: linear-gradient(90deg, #6366f1, #818cf8); border-radius: 2px; transition: width 0.4s ease; }
    .ev-pct  { font-size: 9px; color: rgba(255,255,255,0.3); text-align: right; }

    nav { flex: 1; padding: 0 12px; overflow-y: auto; scrollbar-width: thin; scrollbar-color: rgba(99,102,241,0.2) transparent; }
    nav::-webkit-scrollbar { width: 3px; }
    nav::-webkit-scrollbar-thumb { background: rgba(99,102,241,0.2); border-radius: 3px; }

    .section-label { font-size: 8px; font-weight: 800; color: rgba(255,255,255,0.2); letter-spacing: 0.12em; text-transform: uppercase; padding: 6px 8px 3px; margin-top: 4px; }

    .nav-item { display: flex; align-items: center; gap: 10px; padding: 9px 12px; border-radius: 9px; color: rgba(255,255,255,0.5); font-size: 13px; transition: all 0.2s; text-decoration: none; margin-bottom: 2px; cursor: pointer; }
    .nav-item:hover { color: #fff; background: rgba(255,255,255,0.05); transform: translateX(3px); }
    .nav-item.active { background: rgba(99,102,241,0.12) !important; color: #818cf8 !important; border: 1px solid rgba(99,102,241,0.2); }

    .nav-text { flex: 1; font-weight: 500; }
    .nav-icon { font-size: 15px; width: 20px; text-align: center; flex-shrink: 0; }

    .step-dot { width: 20px; height: 20px; border-radius: 50%; background: rgba(255,255,255,0.07); border: 1px solid rgba(255,255,255,0.15); font-size: 9px; font-weight: 800; color: rgba(255,255,255,0.4); display: flex; align-items: center; justify-content: center; flex-shrink: 0; transition: all 0.3s; }
    .step-dot.done { background: rgba(99,102,241,0.25); border-color: #6366f1; color: #818cf8; }
    .step-dot.neutral { background: transparent; border-color: rgba(255,255,255,0.1); color: rgba(255,255,255,0.25); font-size: 11px; }

    .badge-auto { font-size: 7px; font-weight: 900; background: rgba(16,185,129,0.15); border: 1px solid rgba(16,185,129,0.3); color: #10b981; padding: 1px 5px; border-radius: 4px; letter-spacing: 0.06em; text-transform: uppercase; }

    .group-header { background: none; border: none; width: 100%; text-align: left; cursor: pointer; font-family: inherit; }
    .chevron { font-size: 9px; transition: transform 0.25s; color: rgba(255,255,255,0.25); margin-left: auto; }
    .chevron.rotated { transform: rotate(90deg); color: #818cf8; }
    .group-active .group-header { color: #818cf8 !important; }
    .sub-menu { margin: 2px 0 6px 30px; padding-left: 10px; border-left: 1px dashed rgba(255,255,255,0.07); display: flex; flex-direction: column; gap: 1px; }
    .nav-sub { padding: 7px 10px; font-size: 12px; color: rgba(255,255,255,0.45); border-radius: 6px; display: flex; align-items: center; gap: 6px; text-decoration: none; transition: all 0.2s; }
    .nav-sub:hover { color: #fff; background: rgba(255,255,255,0.04); }
    .nav-sub.active { background: rgba(99,102,241,0.08) !important; color: #818cf8 !important; border: 1px solid rgba(99,102,241,0.15); }

    .divider { height: 1px; background: rgba(255,255,255,0.04); margin: 8px 12px; }
    .guardian-nav { border: 1px solid rgba(129,140,248,0.12); }
    .guardian-nav:hover { border-color: rgba(129,140,248,0.35); }
    .guardian-nav.active { border-color: rgba(129,140,248,0.35) !important; }
    .crisis-btn { color: rgba(255,68,68,0.65); }
    .crisis-btn:hover { color: #ff4444; background: rgba(255,68,68,0.08); }
    .legacy-btn { color: rgba(251,191,36,0.65); }
    .legacy-btn:hover { color: #fbbf24; background: rgba(251,191,36,0.06); }
    .admin-item { font-size: 12px; color: rgba(99,102,241,0.6); border: 1px dashed rgba(99,102,241,0.2); }

    .family-box { margin: 12px 16px 0; padding: 12px 14px; background: rgba(255,255,255,0.02); border-radius: 10px; border: 1px solid rgba(255,255,255,0.05); flex-shrink: 0; }
    .f-name  { color: #fff; font-size: 12px; font-weight: 700; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
    .f-role  { color: #6366f1; font-size: 9px; text-transform: uppercase; font-weight: 800; margin-bottom: 6px; letter-spacing: 0.06em; }
    .logout-link { background: none; border: none; color: #ff4444; font-size: 10px; font-weight: 700; cursor: pointer; padding: 0; text-transform: uppercase; }
    .logout-confirm { display: flex; flex-direction: column; gap: 4px; }
    .confirm-text { font-size: 10px; color: rgba(255,255,255,0.5); }
    .confirm-actions { display: flex; gap: 6px; }
    .confirm-yes { background: rgba(239,68,68,0.15); border: 1px solid rgba(239,68,68,0.3); color: #f87171; font-size: 10px; font-weight: 700; padding: 3px 8px; border-radius: 5px; cursor: pointer; }
    .confirm-yes:hover { background: rgba(239,68,68,0.3); color: #fff; }
    .confirm-no  { background: rgba(255,255,255,0.04); border: 1px solid rgba(255,255,255,0.1); color: rgba(255,255,255,0.4); font-size: 10px; font-weight: 700; padding: 3px 8px; border-radius: 5px; cursor: pointer; }
  `]
})
export class SidebarComponent implements OnInit {
  private router  = inject(Router);
  protected auth  = inject(AuthService);

  readonly familyState = inject(FamilyStateService);
  readonly flow        = inject(TransformationFlowService);

  readonly user              = this.auth.user;
  readonly showLogoutConfirm = signal(false);

  diagExpanded = false;

  readonly guardianRoute = computed(() =>
    ['/guardian', String(this.familyState.currentFamilyId()), 'election']
  );

  ngOnInit() {
    this.checkActiveRoute();
    this.router.events.pipe(
      filter((e): e is NavigationEnd => e instanceof NavigationEnd)
    ).subscribe(() => this.checkActiveRoute());
  }

  private checkActiveRoute() {
    if (this.router.url.includes('/evaluations')) this.diagExpanded = true;
  }

  isDiagActive(): boolean { return this.router.url.includes('/evaluations'); }

  toggleDiag(e: MouseEvent) {
    e.preventDefault();
    this.diagExpanded = !this.diagExpanded;
  }

  isSetupDone(step: 'family' | 'members' | 'guardian' | 'diagnosis' | 'plan'): boolean {
    const fid = this.familyState.currentFamilyId();
    if (!fid || fid === 0) return false;

    const onb = this.flow.onboardingStep();
    const order: Record<string, number> = {
      'create-family': 1, 'add-members': 2, 'choose-guardian': 3,
      'diagnosis': 4, 'plan-generated': 5, 'completed': 6,
    };
    const current = order[onb] ?? 0;
    const needed: Record<typeof step, number> = {
      family: 1, members: 2, guardian: 3, diagnosis: 4, plan: 5,
    };
    return current > needed[step];
  }

  handleLogout()   { this.showLogoutConfirm.set(true); }
  confirmLogout()  { this.auth.logout(); }
  cancelLogout()   { this.showLogoutConfirm.set(false); }
}
