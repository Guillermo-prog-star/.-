import { Component, inject, computed, signal, OnInit } from '@angular/core';
import { RouterLink, RouterLinkActive, Router, NavigationEnd } from '@angular/router';
import { CommonModule } from '@angular/common';
import { AuthService } from '../../core/services/auth.service';
import { FamilyStateService } from '../../core/services/family-state.service';
import { TransformationFlowService } from '../../core/services/transformation-flow.service';
import { UserNotificationService } from '../../core/services/user-notification.service';
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

        <!-- ── BLOQUE 1: CONFIGURACIÓN — Azul (infraestructura) ──── -->
        <div class="nav-section nav-section--system">
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
          <a routerLink="/family-dna" class="nav-item nav-dna" routerLinkActive="active">
            <span class="nav-icon">🧬</span><span class="nav-text">ADN Familiar</span>
          </a>
        </div>

        <div class="divider"></div>

        <!-- ── BLOQUE 2: DIAGNÓSTICO — Turquesa (observación) ─────── -->
        <div class="nav-section nav-section--diagnosis">
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
        </div>

        <div class="divider"></div>

        <!-- ── BLOQUE 2b: ÍNDICES — Capital y Fortalecimiento ────── -->
        <div class="nav-section nav-section--indices">
          <div class="section-label">ÍNDICES</div>
          <a routerLink="/capital" class="nav-item nav-capital" routerLinkActive="active">
            <span class="nav-icon">💎</span><span class="nav-text">Capital Familiar — ICaF</span>
          </a>
          <a routerLink="/smff" class="nav-item nav-smff" routerLinkActive="active">
            <span class="nav-icon">📐</span><span class="nav-text">Fortalecimiento — SMFF</span>
          </a>
        </div>

        <div class="divider"></div>

        <!-- ── BLOQUE 3: PLAN & RUTA — Verde (crecimiento) ─────── -->
        <div class="nav-section nav-section--evolution">
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
        </div>

        <div class="divider"></div>

        <!-- ── BLOQUE 4: TRANSFORMACIÓN DIARIA — Naranja (acción) ── -->
        <div class="nav-section nav-section--family">
          <div class="section-label">TRANSFORMACIÓN DIARIA</div>
          <a routerLink="/logbook"                     class="nav-item" routerLinkActive="active">
            <span class="nav-icon">📔</span><span class="nav-text">Bitácora &amp; Daily</span>
          </a>
          <a routerLink="/transformation/weekly-plan"  class="nav-item" routerLinkActive="active">
            <span class="nav-icon">📅</span><span class="nav-text">Planeación Mensual</span>
          </a>
          <a routerLink="/checklist"         class="nav-item" routerLinkActive="active">
            <span class="nav-icon">📸</span><span class="nav-text">Evidencias</span>
          </a>
          <a routerLink="/evidence/capture" class="nav-item nav-capsule" routerLinkActive="active">
            <span class="nav-icon">🎴</span><span class="nav-text">Cápsula Familiar</span>
          </a>
          <a routerLink="/transformation/error-protocol" class="nav-item" routerLinkActive="active">
            <span class="nav-icon">🔄</span><span class="nav-text">Gestión de Errores</span>
          </a>
        </div>

        <div class="divider"></div>

        <!-- ── BLOQUE 5: APOYO — semántica por sub-módulo ────────── -->
        <div class="nav-section nav-section--support">
          <div class="section-label">APOYO</div>
          <a routerLink="/crisis"    class="nav-item nav-crisis" routerLinkActive="active">
            <span class="nav-icon">🆘</span><span class="nav-text">Crisis Familiar</span>
          </a>
          <a routerLink="/chat"      class="nav-item nav-intel" routerLinkActive="active">
            <span class="nav-icon">✨</span><span class="nav-text">Consultor IA</span>
          </a>
          <a routerLink="/cognitive" class="nav-item nav-intel" routerLinkActive="active">
            <span class="nav-icon">🧠</span><span class="nav-text">Sistema Cognitivo</span>
          </a>
          <a routerLink="/documentation" class="nav-item nav-intel" routerLinkActive="active">
            <span class="nav-icon">📚</span><span class="nav-text">Documentación</span>
          </a>
          <a routerLink="/trajectory" class="nav-item nav-traj" routerLinkActive="active">
            <span class="nav-icon">🗺️</span><span class="nav-text">Trayectorias de Riesgo</span>
          </a>
        </div>

        <div class="divider"></div>

        <!-- ── BLOQUE 6: LEGADO — Dorado (trascendencia) ─────────── -->
        <div class="nav-section nav-section--legacy">
          <div class="section-label">LEGADO</div>
          <a routerLink="/rituals"   class="nav-item nav-ritual" routerLinkActive="active">
            <span class="nav-icon">🕯️</span><span class="nav-text">Motor de Rituales</span>
          </a>
          <a routerLink="/documentary-maker" class="nav-item nav-ritual" routerLinkActive="active">
            <span class="nav-icon">🎬</span><span class="nav-text">Ensamblaje Documental</span>
          </a>
          <a routerLink="/gratitude" class="nav-item" routerLinkActive="active">
            <span class="nav-icon">💖</span><span class="nav-text">Gratitud Familiar</span>
          </a>
          <a routerLink="/my-space"  class="nav-item" routerLinkActive="active">
            <span class="nav-icon">🔒</span><span class="nav-text">Mi Espacio</span>
          </a>
          <a routerLink="/legado"          class="nav-item" routerLinkActive="active">
            <span class="nav-icon">🏛️</span><span class="nav-text">Legado Familiar</span>
          </a>
          <a routerLink="/family-movie"   class="nav-item nav-movie" routerLinkActive="active">
            <span class="nav-icon">🎬</span><span class="nav-text">Película Familiar</span>
          </a>
          <a routerLink="/family-timeline" class="nav-item nav-timeline" routerLinkActive="active">
            <span class="nav-icon">📜</span><span class="nav-text">Historia Familiar</span>
          </a>
          <a routerLink="/family-tree"    class="nav-item nav-tree" routerLinkActive="active">
            <span class="nav-icon">🌳</span><span class="nav-text">Árbol Generacional</span>
          </a>
          <a routerLink="/lineage" class="nav-item nav-lineage" routerLinkActive="active">
            <span class="nav-icon">🌿</span><span class="nav-text">Linaje Generacional</span>
          </a>
        </div>

        <div class="divider"></div>

        <!-- ── BLOQUE 7: SISTEMA — Azul (infraestructura) ────────── -->
        <div class="nav-section nav-section--system">
          <div class="section-label">SISTEMA</div>
          <a routerLink="/digital-twin"  class="nav-item nav-twin" routerLinkActive="active">
            <span class="nav-icon">🪞</span><span class="nav-text">Gemelo Digital</span>
          </a>
          <a routerLink="/family-council" class="nav-item nav-council" routerLinkActive="active">
            <span class="nav-icon">⚜️</span><span class="nav-text">Consejo Familiar</span>
          </a>
          <a routerLink="/family-pulse" class="nav-item nav-pulse" routerLinkActive="active">
            <span class="nav-icon">💓</span><span class="nav-text">Pulso Familiar</span>
          </a>
          <a routerLink="/dashboard" class="nav-item" routerLinkActive="active">
            <span class="nav-icon">📊</span><span class="nav-text">Panel Analítico</span>
          </a>
          <a routerLink="/portal"    class="nav-item" routerLinkActive="active">
            <span class="nav-icon">📱</span><span class="nav-text">Portal Móvil</span>
          </a>
          <a routerLink="/profile"   class="nav-item" routerLinkActive="active">
            <span class="nav-icon">👤</span><span class="nav-text">Mi Perfil</span>
          </a>
        </div>

        @if (user()?.role === 'ADMIN') {
          <div class="divider"></div>
          <div class="nav-section nav-section--admin">
            <div class="section-label">ADMINISTRACIÓN</div>
            <a routerLink="/admin/stats"         class="nav-item" routerLinkActive="active"><span class="nav-icon">⚙️</span><span class="nav-text">Estadísticas</span></a>
            <a routerLink="/admin/eedsl"         class="nav-item" routerLinkActive="active"><span class="nav-icon">🔧</span><span class="nav-text">Reglas EEDSL</span></a>
            <a routerLink="/admin/sandbox"       class="nav-item" routerLinkActive="active"><span class="nav-icon">🧪</span><span class="nav-text">Sandbox</span></a>
            <a routerLink="/admin/voice-monitor" class="nav-item" routerLinkActive="active"><span class="nav-icon">🎙️</span><span class="nav-text">Monitor Voz</span></a>
          </div>
        }

      </nav>

      <!-- ─── NOTIFICATION PANEL ───────────────────── -->
      @if (showNotifications()) {
        <div class="notif-panel">
          <div class="notif-header">
            <span class="notif-title">Notificaciones</span>
            @if (notifService.unreadCount() > 0) {
              <button class="notif-mark-read" (click)="markAllRead()">Marcar leídas</button>
            }
          </div>
          <div class="notif-list">
            @if (notifService.notifications().length === 0) {
              <p class="notif-empty">Sin notificaciones recientes</p>
            }
            @for (n of notifService.notifications(); track n.id) {
              <div class="notif-item" [class.notif-unread]="!n.viewed">
                <span class="notif-icon">{{ notifService.typeIcon(n.type) }}</span>
                <div class="notif-body">
                  <p class="notif-item-title">{{ n.title }}</p>
                  <p class="notif-msg">{{ n.message }}</p>
                </div>
              </div>
            }
          </div>
        </div>
      }

      <!-- ─── FAMILIA BOX ─────────────────────────── -->
      <div class="family-box">
        <div class="f-row">
          <div class="f-info">
            <div class="f-name">{{ user()?.fullName }}</div>
            <div class="f-role">{{ user()?.role }}</div>
          </div>
          <button class="notif-bell" (click)="toggleNotifications()" title="Notificaciones">
            🔔
            @if (notifService.unreadCount() > 0) {
              <span class="notif-badge">{{ notifService.unreadCount() }}</span>
            }
          </button>
        </div>
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

    /* Evolution card — naranja (vida familiar activa) + barra verde (progreso) */
    .evolution-card { margin: 0 16px 12px; padding: 10px 14px; background: linear-gradient(135deg, rgba(245,158,11,0.07), rgba(212,175,55,0.04)); border: 1px solid rgba(245,158,11,0.2); border-radius: 10px; }
    .ev-code  { font-size: 9px; color: var(--if-family-bright); font-weight: 800; letter-spacing: 0.06em; text-transform: uppercase; }
    .ev-pillar { font-size: 12px; font-weight: 700; color: #fff; margin: 3px 0 2px; }
    .ev-meta  { display: flex; gap: 8px; margin-bottom: 6px; flex-wrap: wrap; }
    .ev-month { font-size: 10px; color: rgba(255,255,255,0.5); background: rgba(255,255,255,0.06); padding: 1px 6px; border-radius: 4px; }
    .ev-phase { font-size: 10px; color: rgba(255,255,255,0.4); }
    .ev-bar-wrap { height: 3px; background: rgba(255,255,255,0.06); border-radius: 2px; overflow: hidden; margin-bottom: 3px; }
    .ev-bar  { height: 100%; background: linear-gradient(90deg, var(--if-evolution), var(--if-evolution-bright)); border-radius: 2px; transition: width 0.4s ease; }
    .ev-pct  { font-size: 9px; color: rgba(255,255,255,0.3); text-align: right; }

    nav { flex: 1; padding: 0 12px; overflow-y: auto; scrollbar-width: thin; scrollbar-color: rgba(99,102,241,0.2) transparent; }
    nav::-webkit-scrollbar { width: 3px; }
    nav::-webkit-scrollbar-thumb { background: rgba(99,102,241,0.2); border-radius: 3px; }

    /* ── BASE nav-item (default) ─────────────────────────────────── */
    .section-label { font-size: 8px; font-weight: 800; color: rgba(255,255,255,0.18); letter-spacing: 0.12em; text-transform: uppercase; padding: 6px 8px 3px; margin-top: 4px; }

    .nav-item { display: flex; align-items: center; gap: 10px; padding: 9px 12px; border-radius: 9px; color: rgba(255,255,255,0.5); font-size: 13px; transition: all 0.2s; text-decoration: none; margin-bottom: 2px; cursor: pointer; border: 1px solid transparent; }
    .nav-item:hover { color: #fff; background: rgba(255,255,255,0.05); transform: translateX(3px); }

    .nav-text { flex: 1; font-weight: 500; }
    .nav-icon { font-size: 15px; width: 20px; text-align: center; flex-shrink: 0; }

    .step-dot { width: 20px; height: 20px; border-radius: 50%; background: rgba(255,255,255,0.07); border: 1px solid rgba(255,255,255,0.15); font-size: 9px; font-weight: 800; color: rgba(255,255,255,0.4); display: flex; align-items: center; justify-content: center; flex-shrink: 0; transition: all 0.3s; }
    .step-dot.neutral { background: transparent; border-color: rgba(255,255,255,0.1); color: rgba(255,255,255,0.25); font-size: 11px; }

    .badge-auto { font-size: 7px; font-weight: 900; padding: 1px 5px; border-radius: 4px; letter-spacing: 0.06em; text-transform: uppercase; }

    .group-header { background: none; border: none; width: 100%; text-align: left; cursor: pointer; font-family: inherit; }
    .chevron { font-size: 9px; transition: transform 0.25s; color: rgba(255,255,255,0.25); margin-left: auto; }
    .chevron.rotated { transform: rotate(90deg); }
    .sub-menu { margin: 2px 0 6px 30px; padding-left: 10px; border-left: 1px dashed rgba(255,255,255,0.07); display: flex; flex-direction: column; gap: 1px; }
    .nav-sub { padding: 7px 10px; font-size: 12px; color: rgba(255,255,255,0.45); border-radius: 6px; display: flex; align-items: center; gap: 6px; text-decoration: none; transition: all 0.2s; border: 1px solid transparent; }
    .nav-sub:hover { color: #fff; background: rgba(255,255,255,0.04); }

    .divider { height: 1px; background: rgba(255,255,255,0.04); margin: 8px 12px; }

    /* ── SISTEMA — Azul: Configuración, Dashboard, Admin ─────────── */
    .nav-section--system .section-label { color: var(--if-system-bright); }
    .nav-section--system .nav-item.active { background: var(--if-system-soft) !important; color: var(--if-system-bright) !important; border-color: var(--if-system-border) !important; }
    .nav-section--system .nav-item:hover { color: #93C5FD; }
    .nav-section--system .step-dot.done { background: rgba(59,130,246,0.2); border-color: var(--if-system); color: var(--if-system-bright); }
    .nav-section--system .guardian-nav { border-color: var(--if-system-soft); }
    .nav-section--system .guardian-nav:hover { border-color: var(--if-system-accent); }
    .nav-section--system .guardian-nav.active { border-color: var(--if-system-accent) !important; }

    /* ── DIAGNÓSTICO — Turquesa: observación, medición ───────────── */
    .nav-section--diagnosis .section-label { color: var(--if-diagnosis-bright); }
    .nav-section--diagnosis .nav-item.active { background: var(--if-diagnosis-soft) !important; color: var(--if-diagnosis-bright) !important; border-color: var(--if-diagnosis-border) !important; }
    .nav-section--diagnosis .nav-item:hover { color: #67E8F9; }
    .nav-section--diagnosis .step-dot.done { background: rgba(6,182,212,0.2); border-color: var(--if-diagnosis); color: var(--if-diagnosis-bright); }
    .nav-section--diagnosis .chevron.rotated { color: var(--if-diagnosis-bright); }
    .nav-section--diagnosis .group-active .group-header { color: var(--if-diagnosis-bright) !important; }
    .nav-section--diagnosis .nav-sub.active { background: rgba(6,182,212,0.08) !important; color: var(--if-diagnosis-bright) !important; border-color: rgba(6,182,212,0.2) !important; }

    /* ── ÍNDICES — Azul-violeta: ICaF + SMFF ────────────────────── */
    .nav-section--indices .section-label { color: #a78bfa; }
    .nav-capital { color: rgba(167,139,250,0.75) !important; }
    .nav-capital:hover { color: #c4b5fd !important; background: rgba(139,92,246,0.1) !important; }
    .nav-capital.active { background: rgba(139,92,246,0.15) !important; color: #ddd6fe !important; border-color: rgba(139,92,246,0.3) !important; }
    .nav-smff { color: rgba(107,140,255,0.8) !important; }
    .nav-smff:hover { color: #93c5fd !important; background: rgba(99,102,241,0.1) !important; }
    .nav-smff.active { background: rgba(99,102,241,0.15) !important; color: #bfdbfe !important; border-color: rgba(99,102,241,0.3) !important; }

    /* ── EVOLUCIÓN — Verde: Plan, Ruta, Crecimiento ──────────────── */
    .nav-section--evolution .section-label { color: var(--if-evolution-bright); }
    .nav-section--evolution .nav-item.active { background: rgba(34,197,94,0.12) !important; color: var(--if-evolution-bright) !important; border-color: rgba(34,197,94,0.25) !important; }
    .nav-section--evolution .nav-item:hover { color: #86EFAC; }
    .nav-section--evolution .step-dot.done { background: rgba(34,197,94,0.2); border-color: var(--if-evolution); color: var(--if-evolution-bright); }
    .nav-section--evolution .badge-auto { background: rgba(34,197,94,0.15); border: 1px solid rgba(34,197,94,0.3); color: var(--if-evolution-bright); }

    /* ── FAMILIA — Naranja: Transformación, Misiones, Acción ─────── */
    .nav-section--family .section-label { color: var(--if-family-bright); }
    .nav-section--family .nav-item.active { background: rgba(245,158,11,0.12) !important; color: var(--if-family-bright) !important; border-color: rgba(245,158,11,0.25) !important; }
    .nav-section--family .nav-item:hover { color: var(--if-family-bright); background: rgba(245,158,11,0.06); }

    /* ── APOYO — semántica por sub-módulo ───────────────────────── */
    .nav-section--support .section-label { color: rgba(255,255,255,0.3); }
    /* Crisis: solo rojo */
    .nav-crisis { color: rgba(239,68,68,0.7) !important; }
    .nav-crisis:hover { color: var(--if-crisis) !important; background: rgba(239,68,68,0.08) !important; }
    .nav-crisis.active { background: rgba(239,68,68,0.12) !important; color: var(--if-crisis-bright) !important; border-color: rgba(239,68,68,0.25) !important; }
    /* Gemelo Digital: índigo profundo */
    .nav-twin { color: rgba(129,140,248,0.85) !important; }
    .nav-twin:hover { color: #a5b4fc !important; background: rgba(99,102,241,0.1) !important; }
    .nav-twin.active { background: rgba(99,102,241,0.16) !important; color: #c7d2fe !important; border-color: rgba(99,102,241,0.35) !important; }
    /* Consejo Familiar: dorado ceremonial */
    .nav-council { color: rgba(251,191,36,0.85) !important; }
    .nav-council:hover { color: #fcd34d !important; background: rgba(245,158,11,0.1) !important; }
    .nav-council.active { background: rgba(245,158,11,0.15) !important; color: #fef3c7 !important; border-color: rgba(245,158,11,0.35) !important; }
    /* Pulso Familiar: rojo-rosa vital */
    .nav-pulse { color: rgba(244,114,182,0.85) !important; }
    .nav-pulse:hover { color: #f9a8d4 !important; background: rgba(236,72,153,0.09) !important; }
    .nav-pulse.active { background: rgba(236,72,153,0.14) !important; color: #fbcfe8 !important; border-color: rgba(236,72,153,0.3) !important; }
    /* Árbol Generacional: verde naturaleza */
    .nav-tree { color: rgba(74,222,128,0.8) !important; }
    .nav-tree:hover { color: #86efac !important; background: rgba(34,197,94,0.08) !important; }
    .nav-tree.active { background: rgba(34,197,94,0.12) !important; color: #bbf7d0 !important; border-color: rgba(34,197,94,0.3) !important; }
    /* Motor de Rituales: índigo cálido */
    .nav-ritual { color: rgba(165,180,252,0.8) !important; }
    .nav-ritual:hover { color: #c7d2fe !important; background: rgba(99,102,241,0.09) !important; }
    .nav-ritual.active { background: rgba(99,102,241,0.15) !important; color: #e0e7ff !important; border-color: rgba(99,102,241,0.3) !important; }
    /* Cápsula Familiar: verde esmeralda */
    .nav-capsule { color: rgba(16,185,129,0.8) !important; }
    .nav-capsule:hover { color: #6ee7b7 !important; background: rgba(16,185,129,0.08) !important; }
    .nav-capsule.active { background: rgba(16,185,129,0.12) !important; color: #a7f3d0 !important; border-color: rgba(16,185,129,0.3) !important; }
    /* Película Familiar: violeta cinematográfico */
    .nav-movie { color: rgba(196,181,253,0.85) !important; }
    .nav-movie:hover { color: #ddd6fe !important; background: rgba(124,58,237,0.09) !important; }
    .nav-movie.active { background: rgba(124,58,237,0.15) !important; color: #ede9fe !important; border-color: rgba(124,58,237,0.3) !important; }
    /* Historia Familiar: dorado-ámbar */
    .nav-timeline { color: rgba(245,158,11,0.8) !important; }
    .nav-timeline:hover { color: #fcd34d !important; background: rgba(245,158,11,0.08) !important; }
    .nav-timeline.active { background: rgba(245,158,11,0.12) !important; color: #fde68a !important; border-color: rgba(245,158,11,0.3) !important; }
    /* ADN Familiar: violeta-índigo */
    .nav-dna { color: rgba(167,139,250,0.8) !important; }
    .nav-dna:hover { color: #c4b5fd !important; background: rgba(139,92,246,0.1) !important; }
    .nav-dna.active { background: rgba(139,92,246,0.15) !important; color: #ddd6fe !important; border-color: rgba(139,92,246,0.3) !important; }
    /* Inteligencia: violeta */
    .nav-intel { color: rgba(139,92,246,0.75) !important; }
    .nav-intel:hover { color: var(--if-intel-bright) !important; background: var(--if-intel-faint) !important; }
    .nav-intel.active { background: var(--if-intel-soft) !important; color: var(--if-intel-bright) !important; border-color: var(--if-intel-border) !important; }
    .nav-traj { color: rgba(251,146,60,0.75) !important; }
    .nav-traj:hover { color: #fb923c !important; background: rgba(251,146,60,0.08) !important; }
    .nav-traj.active { background: rgba(251,146,60,0.12) !important; color: #fdba74 !important; border-color: rgba(251,146,60,0.25) !important; }

    /* ── LEGADO — Dorado: Gratitud, Constitución, Historia ──────── */
    .nav-section--legacy .section-label { color: var(--if-legacy); }
    .nav-section--legacy .nav-item { color: rgba(212,175,55,0.65); }
    .nav-section--legacy .nav-item:hover { color: var(--if-legacy); background: rgba(212,175,55,0.07); }
    .nav-section--legacy .nav-item.active { background: var(--if-legacy-soft) !important; color: var(--if-legacy) !important; border-color: var(--if-legacy-border) !important; }

    /* ── ADMINISTRACIÓN — Azul (sistema técnico) ─────────────────── */
    .nav-section--admin .section-label { color: var(--if-system-bright); }
    .nav-section--admin .nav-item { font-size: 12px; color: rgba(96,165,250,0.6); border: 1px dashed rgba(59,130,246,0.2); }
    .nav-section--admin .nav-item.active { background: rgba(59,130,246,0.1) !important; color: var(--if-system-bright) !important; border-color: rgba(59,130,246,0.3) !important; }

    .family-box { margin: 12px 16px 0; padding: 12px 14px; background: rgba(255,255,255,0.02); border-radius: 10px; border: 1px solid rgba(255,255,255,0.05); flex-shrink: 0; }
    .f-row   { display: flex; align-items: center; justify-content: space-between; margin-bottom: 6px; }
    .f-info  { min-width: 0; flex: 1; }
    .f-name  { color: #fff; font-size: 12px; font-weight: 700; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
    .f-role  { color: #6366f1; font-size: 9px; text-transform: uppercase; font-weight: 800; letter-spacing: 0.06em; }
    .notif-bell  { position: relative; background: none; border: none; cursor: pointer; font-size: 16px; padding: 2px 4px; line-height: 1; flex-shrink: 0; }
    .notif-badge { position: absolute; top: -2px; right: -4px; background: #ef4444; color: #fff; font-size: 8px; font-weight: 800; border-radius: 8px; min-width: 14px; height: 14px; display: flex; align-items: center; justify-content: center; padding: 0 2px; }
    .notif-panel { margin: 0 8px 4px; background: #111114; border: 1px solid rgba(255,255,255,0.08); border-radius: 12px; overflow: hidden; flex-shrink: 0; max-height: 280px; display: flex; flex-direction: column; }
    .notif-header { display: flex; align-items: center; justify-content: space-between; padding: 10px 14px 6px; border-bottom: 1px solid rgba(255,255,255,0.05); }
    .notif-title  { font-size: 11px; font-weight: 700; color: rgba(255,255,255,0.7); }
    .notif-mark-read { background: none; border: none; color: #6366f1; font-size: 9px; font-weight: 700; cursor: pointer; padding: 0; }
    .notif-list   { overflow-y: auto; flex: 1; padding: 6px 0; scrollbar-width: thin; scrollbar-color: rgba(99,102,241,0.2) transparent; }
    .notif-empty  { color: rgba(255,255,255,0.25); font-size: 11px; text-align: center; padding: 16px; }
    .notif-item   { display: flex; gap: 10px; padding: 8px 14px; border-bottom: 1px solid rgba(255,255,255,0.04); }
    .notif-item:last-child { border-bottom: none; }
    .notif-unread { background: rgba(99,102,241,0.06); }
    .notif-icon   { font-size: 14px; flex-shrink: 0; margin-top: 1px; }
    .notif-body   { min-width: 0; }
    .notif-item-title { font-size: 11px; font-weight: 600; color: rgba(255,255,255,0.8); margin: 0 0 2px; }
    .notif-msg    { font-size: 10px; color: rgba(255,255,255,0.4); margin: 0; line-height: 1.3; overflow: hidden; display: -webkit-box; -webkit-line-clamp: 2; -webkit-box-orient: vertical; }
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

  readonly familyState  = inject(FamilyStateService);
  readonly flow         = inject(TransformationFlowService);
  readonly notifService = inject(UserNotificationService);

  readonly user               = this.auth.user;
  readonly showLogoutConfirm  = signal(false);
  readonly showNotifications  = signal(false);

  toggleNotifications(): void {
    this.showNotifications.update(v => !v);
  }

  markAllRead(): void {
    this.notifService.markAllRead();
  }

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
