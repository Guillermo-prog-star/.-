import { Component, OnInit, inject, signal, computed, ChangeDetectionStrategy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { HttpClient } from '@angular/common/http';
import { AuthService } from '../../core/services/auth.service';
import { FamilyStateService } from '../../core/services/family-state.service';
import { TransformationFlowService } from '../../core/services/transformation-flow.service';
import { ApiService } from '../../core/services/api.service';
import { catchError, of, forkJoin } from 'rxjs';

interface UserProfile {
  id: number;
  email: string;
  fullName: string;
  role: string;
  familyId: number | null;
  familyName: string | null;
}

@Component({
  selector: 'app-profile-page',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [CommonModule, RouterLink],
  styles: [`
    :host { display: block; }

    .profile-header {
      margin-bottom: 32px;
    }
    .profile-header h1 {
      font-size: 28px;
      font-weight: 800;
      color: #fff;
      letter-spacing: -0.02em;
      margin: 0 0 6px;
    }
    .profile-header p {
      color: rgba(255,255,255,0.4);
      font-size: 14px;
      margin: 0;
    }

    .profile-grid {
      display: grid;
      grid-template-columns: 340px 1fr;
      gap: 24px;
      align-items: start;
    }

    .glass-card {
      background: rgba(255,255,255,0.03);
      border: 1px solid rgba(255,255,255,0.08);
      border-radius: 24px;
      padding: 28px;
      backdrop-filter: blur(20px);
    }

    /* ── Identity card ── */
    .identity-card {
      display: flex;
      flex-direction: column;
      align-items: center;
      gap: 16px;
      text-align: center;
    }

    .avatar {
      width: 88px;
      height: 88px;
      border-radius: 50%;
      background: radial-gradient(circle at 30% 30%, #818cf8, #4f46e5);
      display: flex;
      align-items: center;
      justify-content: center;
      font-size: 30px;
      font-weight: 800;
      color: #fff;
      box-shadow: 0 0 32px rgba(99,102,241,0.35);
      letter-spacing: -0.02em;
      flex-shrink: 0;
    }

    .user-name {
      font-size: 20px;
      font-weight: 700;
      color: #fff;
      margin: 0;
      letter-spacing: -0.02em;
    }

    .role-badge {
      display: inline-flex;
      align-items: center;
      gap: 6px;
      padding: 4px 14px;
      border-radius: 99px;
      font-size: 11px;
      font-weight: 700;
      text-transform: uppercase;
      letter-spacing: 0.06em;
    }
    .badge-admin {
      background: rgba(251,191,36,0.12);
      color: #fbbf24;
      border: 1px solid rgba(251,191,36,0.25);
    }
    .badge-user {
      background: rgba(99,102,241,0.12);
      color: #818cf8;
      border: 1px solid rgba(99,102,241,0.25);
    }

    .info-list {
      width: 100%;
      display: flex;
      flex-direction: column;
      gap: 12px;
    }
    .info-row {
      display: flex;
      justify-content: space-between;
      align-items: center;
      padding: 10px 14px;
      background: rgba(255,255,255,0.03);
      border-radius: 12px;
      border: 1px solid rgba(255,255,255,0.05);
      font-size: 13px;
    }
    .info-label {
      color: rgba(255,255,255,0.4);
      font-weight: 600;
    }
    .info-value {
      color: rgba(255,255,255,0.85);
      font-weight: 600;
      text-align: right;
      max-width: 180px;
      overflow: hidden;
      text-overflow: ellipsis;
      white-space: nowrap;
    }

    /* ── Details panel ── */
    .details-panel {
      display: flex;
      flex-direction: column;
      gap: 20px;
    }

    .section-title {
      font-size: 12px;
      font-weight: 700;
      color: rgba(255,255,255,0.3);
      text-transform: uppercase;
      letter-spacing: 0.08em;
      margin: 0 0 16px;
    }

    /* Security status */
    .security-status {
      display: flex;
      align-items: center;
      gap: 16px;
      padding: 16px 20px;
      border-radius: 16px;
      border-left: 3px solid;
    }
    .security-status.secure {
      background: rgba(52,211,153,0.05);
      border-color: #34d399;
    }
    .security-status.insecure {
      background: rgba(239,68,68,0.05);
      border-color: #f87171;
    }
    .security-icon { font-size: 28px; flex-shrink: 0; }
    .security-text strong {
      display: block;
      font-size: 14px;
      font-weight: 700;
      color: #fff;
      margin-bottom: 2px;
    }
    .security-text span {
      font-size: 12px;
      color: rgba(255,255,255,0.4);
    }

    /* Family context */
    .family-block {
      display: flex;
      align-items: center;
      gap: 16px;
      padding: 16px 20px;
      background: rgba(99,102,241,0.06);
      border-radius: 16px;
      border: 1px solid rgba(99,102,241,0.15);
    }
    .family-icon { font-size: 32px; }
    .family-text strong {
      display: block;
      font-size: 15px;
      font-weight: 700;
      color: #fff;
      margin-bottom: 2px;
    }
    .family-text span {
      font-size: 12px;
      color: rgba(255,255,255,0.4);
    }
    .no-family {
      color: rgba(255,255,255,0.3);
      font-size: 13px;
      font-style: italic;
    }

    /* Actions */
    .btn-logout {
      width: 100%;
      padding: 12px 20px;
      background: rgba(239,68,68,0.08);
      border: 1px solid rgba(239,68,68,0.2);
      border-radius: 12px;
      color: #f87171;
      font-size: 14px;
      font-weight: 700;
      cursor: pointer;
      transition: all 0.2s;
      letter-spacing: 0.01em;
    }
    .btn-logout:hover {
      background: rgba(239,68,68,0.15);
      border-color: rgba(239,68,68,0.35);
      transform: translateY(-1px);
    }
    .logout-inline { display: flex; flex-direction: column; gap: 10px; }
    .confirm-msg { font-size: 13px; color: rgba(255,255,255,0.7); margin: 0; }
    .confirm-actions { display: flex; gap: 10px; }
    .btn-confirm-yes { flex: 1; padding: 10px; background: rgba(239,68,68,0.15); border: 1px solid rgba(239,68,68,0.3); border-radius: 10px; color: #f87171; font-size: 13px; font-weight: 700; cursor: pointer; transition: all 0.2s; }
    .btn-confirm-yes:hover { background: rgba(239,68,68,0.3); color: #fff; }
    .btn-confirm-no { flex: 1; padding: 10px; background: rgba(255,255,255,0.04); border: 1px solid rgba(255,255,255,0.1); border-radius: 10px; color: rgba(255,255,255,0.55); font-size: 13px; font-weight: 700; cursor: pointer; transition: all 0.2s; }
    .btn-confirm-no:hover { background: rgba(255,255,255,0.08); color: rgba(255,255,255,0.85); }

    /* Loading skeleton */
    .skeleton {
      background: linear-gradient(90deg,
        rgba(255,255,255,0.04) 25%,
        rgba(255,255,255,0.08) 50%,
        rgba(255,255,255,0.04) 75%);
      background-size: 200% 100%;
      animation: shimmer 1.5s infinite;
      border-radius: 8px;
    }
    @keyframes shimmer {
      0%   { background-position: 200% 0; }
      100% { background-position: -200% 0; }
    }
    .skeleton-line { height: 14px; margin-bottom: 8px; }
    .skeleton-line.lg { height: 20px; width: 60%; }
    .skeleton-line.sm { height: 12px; width: 40%; }

    /* Transformation journey */
    .transform-journey { display: flex; flex-direction: column; gap: 14px; }
    .tj-row { display: flex; justify-content: space-between; align-items: flex-start; gap: 12px; }
    .tj-pillar { display: flex; gap: 12px; align-items: flex-start; }
    .tj-pillar-icon { font-size: 26px; flex-shrink: 0; }
    .tj-pillar-label { font-size: 8px; font-weight: 800; color: rgba(255,255,255,0.3); letter-spacing: .12em; text-transform: uppercase; }
    .tj-pillar-name { font-size: 14px; font-weight: 800; color: #fff; }
    .tj-pillar-range { font-size: 11px; color: rgba(255,255,255,0.4); }
    .tj-month-box { text-align: center; background: rgba(99,102,241,0.12); border: 1px solid rgba(99,102,241,0.25); border-radius: 10px; padding: 8px 14px; flex-shrink: 0; }
    .tj-month-num { font-size: 20px; font-weight: 900; color: #818cf8; line-height: 1; }
    .tj-month-label { font-size: 8px; font-weight: 700; color: rgba(255,255,255,0.3); text-transform: uppercase; }
    .tj-phase { font-size: 12px; color: rgba(255,255,255,0.45); font-style: italic; }
    .tj-bar-row { display: flex; align-items: center; gap: 10px; }
    .tj-bar-wrap { flex: 1; height: 5px; background: rgba(255,255,255,0.07); border-radius: 3px; overflow: hidden; }
    .tj-bar-fill { height: 100%; background: linear-gradient(90deg, #6366f1, #818cf8); border-radius: 3px; transition: width .5s; }
    .tj-bar-pct { font-size: 11px; font-weight: 700; color: #818cf8; flex-shrink: 0; }
    .tj-sprint-row { display: flex; align-items: center; gap: 8px; }
    .tj-sprint-icon { font-size: 14px; }
    .tj-sprint-label { font-size: 12px; color: rgba(255,255,255,0.5); font-weight: 600; }
    .tj-mission-badge { font-size: 10px; font-weight: 700; background: rgba(16,185,129,0.12); border: 1px solid rgba(16,185,129,0.25); color: #10b981; padding: 2px 8px; border-radius: 5px; }
    .tj-badges { display: flex; flex-wrap: wrap; gap: 8px; }
    .tj-badge { display: flex; align-items: center; gap: 5px; background: rgba(255,255,255,0.04); border: 1px solid rgba(255,255,255,0.08); border-radius: 8px; padding: 5px 10px; }
    .tj-badge-icon { font-size: 14px; }
    .tj-badge-name { font-size: 11px; color: rgba(255,255,255,0.6); font-weight: 600; }
    .tj-badges-empty { font-size: 12px; color: rgba(255,255,255,0.25); font-style: italic; }
    .tj-links { display: flex; gap: 8px; flex-wrap: wrap; padding-top: 4px; border-top: 1px solid rgba(255,255,255,0.05); }
    .tj-link { font-size: 12px; font-weight: 700; color: rgba(255,255,255,0.5); background: rgba(255,255,255,0.04); border: 1px solid rgba(255,255,255,0.08); border-radius: 8px; padding: 5px 12px; text-decoration: none; transition: all .2s; }
    .tj-link:hover { background: rgba(255,255,255,0.08); color: #fff; border-color: rgba(255,255,255,0.15); }
    /* Stats grid */
    .stats-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 10px; }
    .stat-item { background: rgba(255,255,255,0.02); border: 1px solid rgba(255,255,255,0.06); border-radius: 10px; padding: 12px; text-align: center; }
    .stat-num { font-size: 22px; font-weight: 900; color: #818cf8; }
    .stat-lbl { font-size: 10px; color: rgba(255,255,255,0.35); font-weight: 600; text-transform: uppercase; letter-spacing: .05em; margin-top: 2px; }

    /* Error state */
    .error-note {
      font-size: 12px;
      color: #fbbf24;
      background: rgba(251,191,36,0.08);
      border: 1px solid rgba(251,191,36,0.2);
      border-radius: 10px;
      padding: 10px 14px;
      margin-top: 8px;
    }

    @media (max-width: 900px) {
      .profile-grid { grid-template-columns: 1fr; }
    }

    /* ── WhatsApp section ─────────────────────────────────────── */
    .wa-block { margin-bottom: 12px; }
    .wa-active, .wa-inactive {
      display: flex; align-items: center; gap: 12px;
      padding: 12px 14px; border-radius: 14px;
    }
    .wa-active  { background: rgba(34,197,94,0.08); border: 1px solid rgba(34,197,94,0.2); }
    .wa-inactive{ background: rgba(255,255,255,0.04); border: 1px solid rgba(255,255,255,0.08); }
    .wa-icon { font-size: 22px; flex-shrink: 0; }
    .wa-text { flex: 1; min-width: 0; }
    .wa-text strong { display: block; font-size: 13px; font-weight: 700; color: #f1f5f9; }
    .wa-text span   { font-size: 12px; color: #64748b; }
    .btn-wa-edit, .btn-wa-add {
      flex-shrink: 0; padding: 6px 14px; border-radius: 10px; border: none; cursor: pointer;
      font-size: 12px; font-weight: 700; transition: opacity .2s;
    }
    .btn-wa-edit { background: rgba(255,255,255,0.1); color: #94a3b8; }
    .btn-wa-add  { background: rgba(37,211,102,0.2); color: #4ade80; }

    .wa-edit-form { padding: 12px 0 4px; }
    .wa-input-row { display: flex; align-items: center; gap: 8px; margin-bottom: 6px; }
    .wa-prefix { font-size: 13px; color: #94a3b8; font-weight: 600; }
    .wa-inp {
      flex: 1; background: rgba(255,255,255,0.06); border: 1px solid rgba(255,255,255,0.12);
      border-radius: 10px; padding: 8px 12px; color: #f1f5f9; font-size: 14px; outline: none;
    }
    .wa-hint  { font-size: 11px; color: #475569; margin: 0 0 8px; }
    .wa-error { font-size: 12px; color: #f87171; margin: 0 0 8px; }
    .wa-form-actions { display: flex; gap: 8px; }
    .btn-wa-save {
      padding: 8px 18px; border-radius: 10px; border: none; cursor: pointer;
      background: rgba(37,211,102,0.25); color: #4ade80; font-weight: 700; font-size: 13px;
    }
    .btn-wa-save:disabled { opacity: .5; cursor: not-allowed; }
    .btn-wa-cancel {
      padding: 8px 14px; border-radius: 10px; border: none; cursor: pointer;
      background: rgba(255,255,255,0.06); color: #64748b; font-size: 13px;
    }

    .wa-types { margin-top: 14px; }
    .wa-types-title { font-size: 11px; color: #475569; margin: 0 0 8px; text-transform: uppercase; letter-spacing: .05em; }
    .wa-chips { display: flex; flex-wrap: wrap; gap: 6px; }
    .wa-chip {
      padding: 3px 10px; border-radius: 99px; font-size: 11px; font-weight: 600;
    }
    .wa-chip--red    { background: rgba(239,68,68,0.12);   color: #f87171; }
    .wa-chip--orange { background: rgba(249,115,22,0.12);  color: #fb923c; }
    .wa-chip--yellow { background: rgba(234,179,8,0.12);   color: #fbbf24; }
    .wa-chip--blue   { background: rgba(99,102,241,0.12);  color: #818cf8; }
    .wa-chip--gray   { background: rgba(148,163,184,0.10); color: #94a3b8; }
  `],
  template: `
    <div class="profile-header">
      <h1>Mi Perfil</h1>
      <p>Identidad, sesión y contexto familiar del consultor activo.</p>
    </div>

    <div class="profile-grid">

      <!-- ── Identity card ── -->
      <div class="glass-card identity-card">
        @if (loading()) {
          <div class="skeleton" style="width:88px;height:88px;border-radius:50%"></div>
          <div class="skeleton skeleton-line lg"></div>
          <div class="skeleton skeleton-line sm"></div>
        } @else {
          <div class="avatar">{{ initials() }}</div>
          <p class="user-name">{{ displayName() }}</p>
          <span class="role-badge" [ngClass]="roleColorClass()">
            {{ roleLabel() }}
          </span>
        }

        <div class="info-list">
          <div class="info-row">
            <span class="info-label">Email</span>
            @if (loading()) {
              <span class="skeleton skeleton-line" style="width:120px"></span>
            } @else {
              <span class="info-value" [title]="displayEmail()">{{ displayEmail() }}</span>
            }
          </div>
          <div class="info-row">
            <span class="info-label">ID de usuario</span>
            @if (loading()) {
              <span class="skeleton skeleton-line" style="width:60px"></span>
            } @else {
              <span class="info-value">#{{ profile()?.id ?? '—' }}</span>
            }
          </div>
          <div class="info-row">
            <span class="info-label">Familia</span>
            @if (loading()) {
              <span class="skeleton skeleton-line" style="width:90px"></span>
            } @else {
              <span class="info-value">{{ profile()?.familyName ?? 'Sin familia' }}</span>
            }
          </div>
        </div>

        @if (hasBackendError()) {
          <p class="error-note">
            ⚠️ No se pudo verificar el perfil con el servidor. Mostrando datos en caché.
          </p>
        }
      </div>

      <!-- ── Details panel ── -->
      <div class="details-panel">

        <!-- Security -->
        <div class="glass-card">
          <p class="section-title">Estado de Sesión</p>
          <div class="security-status" [class.secure]="isTokenActive()" [class.insecure]="!isTokenActive()">
            <span class="security-icon">{{ isTokenActive() ? '🛡️' : '⚠️' }}</span>
            <div class="security-text">
              <strong>{{ isTokenActive() ? 'Conexión Segura Activa' : 'Sesión no autenticada' }}</strong>
              <span>
                {{ isTokenActive()
                  ? 'Token JWT activo. Interceptores de red vinculados y peticiones protegidas.'
                  : 'No hay token activo. Por favor, inicia sesión nuevamente.' }}
              </span>
            </div>
          </div>
        </div>

        <!-- Family context -->
        <div class="glass-card">
          <p class="section-title">Contexto Familiar</p>
          @if (profile()?.familyId) {
            <div class="family-block">
              <span class="family-icon">👨‍👩‍👧‍👦</span>
              <div class="family-text">
                <strong>{{ profile()?.familyName }}</strong>
                <span>ID de familia: #{{ profile()?.familyId }}</span>
              </div>
            </div>
          } @else if (!loading()) {
            <p class="no-family">Este usuario no está asociado a ningún núcleo familiar.</p>
          } @else {
            <div class="skeleton skeleton-line" style="height:60px;border-radius:16px"></div>
          }
        </div>

        <!-- WhatsApp Notifications -->
        <div class="glass-card">
          <p class="section-title">📱 Notificaciones WhatsApp</p>
          @if (waLoading()) {
            <div class="skeleton skeleton-line" style="height:48px;border-radius:12px"></div>
          } @else {
            <div class="wa-block">
              @if (waNumber()) {
                <div class="wa-active">
                  <span class="wa-icon">✅</span>
                  <div class="wa-text">
                    <strong>WhatsApp activo</strong>
                    <span>+57 {{ waNumber() }}</span>
                  </div>
                  <button class="btn-wa-edit" (click)="editingWa.set(true)">Cambiar</button>
                </div>
              } @else {
                <div class="wa-inactive">
                  <span class="wa-icon">💬</span>
                  <div class="wa-text">
                    <strong>Sin número configurado</strong>
                    <span>Activa alertas críticas por WhatsApp</span>
                  </div>
                  <button class="btn-wa-add" (click)="editingWa.set(true)">Activar</button>
                </div>
              }
            </div>

            @if (editingWa()) {
              <div class="wa-edit-form">
                <div class="wa-input-row">
                  <span class="wa-prefix">+57</span>
                  <input class="wa-inp" type="tel" maxlength="10" placeholder="300 123 4567"
                         [value]="waInput()" (input)="onWaInput($any($event.target).value)"
                         (keydown.enter)="saveWaNumber()" />
                </div>
                <p class="wa-hint">Recibirás alertas de riesgo crítico, recaídas en trayectorias y planes asignados.</p>
                @if (waError()) { <p class="wa-error">{{ waError() }}</p> }
                <div class="wa-form-actions">
                  <button class="btn-wa-save" (click)="saveWaNumber()" [disabled]="waSaving()">
                    {{ waSaving() ? 'Guardando…' : '✓ Guardar' }}
                  </button>
                  <button class="btn-wa-cancel" (click)="editingWa.set(false); waError.set('')">Cancelar</button>
                </div>
              </div>
            }

            <div class="wa-types">
              <p class="wa-types-title">Tipos de alerta que llegan por WhatsApp:</p>
              <div class="wa-chips">
                <span class="wa-chip wa-chip--red">🚨 Riesgo crítico</span>
                <span class="wa-chip wa-chip--orange">🗺️ Recaída trayectoria</span>
                <span class="wa-chip wa-chip--yellow">⚠️ Alerta Sentinel</span>
                <span class="wa-chip wa-chip--blue">📋 Plan asignado</span>
                <span class="wa-chip wa-chip--gray">🔍 Sugerencias urgentes</span>
              </div>
            </div>
          }
        </div>

        <!-- Transformation Journey -->
        <div class="glass-card">
          <p class="section-title">Viaje de Transformación</p>
          <div class="transform-journey">

            <!-- Pillar + Month -->
            <div class="tj-row">
              <div class="tj-pillar">
                <span class="tj-pillar-icon">
                  {{ flow.currentPillar() === 'reconocimiento' ? '💛' : flow.currentPillar() === 'amor' ? '❤️' : '💙' }}
                </span>
                <div>
                  <div class="tj-pillar-label">PILAR ACTIVO</div>
                  <div class="tj-pillar-name">{{ flow.pillarLabel() }}</div>
                  <div class="tj-pillar-range">{{ flow.pillarMonthRange() }}</div>
                </div>
              </div>
              <div class="tj-month-box">
                <div class="tj-month-num">{{ flow.milestoneLabel() }}</div>
                <div class="tj-month-label">Hito</div>
              </div>
            </div>

            <!-- Phase -->
            <div class="tj-phase">{{ flow.currentPhaseLabel() }}</div>

            <!-- Progress bar -->
            <div class="tj-bar-row">
              <div class="tj-bar-wrap">
                <div class="tj-bar-fill" [style.width.%]="flow.progressPercent()"></div>
              </div>
              <span class="tj-bar-pct">{{ flow.progressPercent() }}% de 36 meses</span>
            </div>

            <!-- Sprint -->
            <div class="tj-sprint-row">
              <span class="tj-sprint-icon">⚡</span>
              <span class="tj-sprint-label">Sprint #{{ flow.currentSprintNumber() }}</span>
              @if (flow.activeMissionId()) {
                <span class="tj-mission-badge">Misión activa</span>
              }
            </div>

            <!-- Badges -->
            <div class="tj-badges">
              @for (badge of earnedBadges(); track badge.id) {
                <div class="tj-badge" [title]="badge.description">
                  <span class="tj-badge-icon">{{ badge.icon }}</span>
                  <span class="tj-badge-name">{{ badge.name }}</span>
                </div>
              }
              @if (earnedBadges().length === 0) {
                <div class="tj-badges-empty">Completa misiones para ganar insignias</div>
              }
            </div>

            <!-- Quick links -->
            <div class="tj-links">
              <a routerLink="/transformation/route" class="tj-link">🗺️ Ver ruta</a>
              <a routerLink="/evaluations/start"    class="tj-link">◈ Diagnóstico</a>
              <a routerLink="/legado"               class="tj-link">🏛️ Legado</a>
            </div>

          </div>
        </div>

        <!-- Stats card -->
        @if (familyStats()) {
          <div class="glass-card">
            <p class="section-title">Estadísticas Familiares</p>
            <div class="stats-grid">
              <div class="stat-item">
                <div class="stat-num">{{ familyStats()!.totalEvaluations ?? 0 }}</div>
                <div class="stat-lbl">Diagnósticos</div>
              </div>
              <div class="stat-item">
                <div class="stat-num">{{ familyStats()!.completedPlanTasks ?? 0 }}</div>
                <div class="stat-lbl">Misiones completadas</div>
              </div>
              <div class="stat-item">
                <div class="stat-num">{{ familyStats()!.latestGlobalScore ?? 0 | number:'1.0-0' }}</div>
                <div class="stat-lbl">ICF actual</div>
              </div>
              <div class="stat-item">
                <div class="stat-num">{{ familyStats()!.activeCrisesCount ?? 0 }}</div>
                <div class="stat-lbl">Crisis activas</div>
              </div>
            </div>
          </div>
        }

        <!-- Actions -->
        <div class="glass-card">
          <p class="section-title">Acciones de Cuenta</p>
          @if (showLogoutConfirm()) {
            <div class="logout-inline">
              <p class="confirm-msg">¿Cerrar sesión de forma segura?</p>
              <div class="confirm-actions">
                <button class="btn-confirm-yes" (click)="confirmLogout()">Sí, salir</button>
                <button class="btn-confirm-no" (click)="cancelLogout()">Cancelar</button>
              </div>
            </div>
          } @else {
            <button class="btn-logout" (click)="logout()">
              🚪 Cerrar Sesión de Forma Segura
            </button>
          }
        </div>

      </div>
    </div>
  `
})
export class ProfilePageComponent implements OnInit {
  private auth        = inject(AuthService);
  private http        = inject(HttpClient);
  private api         = inject(ApiService);
  private familyState = inject(FamilyStateService);
  readonly flow       = inject(TransformationFlowService);

  readonly profile         = signal<UserProfile | null>(null);
  readonly loading         = signal(true);
  readonly hasBackendError = signal(false);
  readonly familyStats     = signal<any>(null);

  // ── WhatsApp signals ──────────────────────────────────────────────────────
  readonly waNumber   = signal<string>('');
  readonly waLoading  = signal(true);
  readonly editingWa  = signal(false);
  readonly waInput    = signal('');
  readonly waSaving   = signal(false);
  readonly waError    = signal('');

  private readonly localUser = this.auth.user;

  // ── Insignias ganadas basadas en el estado de transformación ──────────────
  readonly earnedBadges = computed(() => {
    const badges: Array<{id:string; icon:string; name:string; description:string}> = [];
    const pct   = this.flow.progressPercent();
    const month = this.flow.currentMonth();
    const step  = this.flow.onboardingStep();
    const stats = this.familyStats();

    if (step === 'completed')         badges.push({ id:'setup',    icon:'🏠', name:'Nido Activo',       description:'Familia configurada' });
    if (month >= 1)                   badges.push({ id:'m1',       icon:'🌱', name:'Primer Paso',       description:'Mes 1 iniciado' });
    if (month >= 6)                   badges.push({ id:'pillar1',  icon:'💛', name:'Reconocimiento',    description:'Pilar 1 completado' });
    if (month >= 18)                  badges.push({ id:'pillar2',  icon:'❤️', name:'Amor',              description:'Pilar 2 completado' });
    if (month >= 36)                  badges.push({ id:'pillar3',  icon:'💙', name:'Entrega',           description:'Transformación completa' });
    if (pct >= 25)                    badges.push({ id:'25pct',    icon:'⚡', name:'Primer Cuarto',     description:'25% del camino recorrido' });
    if (pct >= 50)                    badges.push({ id:'50pct',    icon:'🔥', name:'Mitad del Camino',  description:'50% completado' });
    if (pct >= 75)                    badges.push({ id:'75pct',    icon:'🌟', name:'Casi Llegamos',     description:'75% completado' });
    if (pct >= 100)                   badges.push({ id:'100pct',   icon:'🏆', name:'Transformación',    description:'36 meses completados' });
    if (stats?.totalEvaluations >= 1) badges.push({ id:'diag1',   icon:'◈',  name:'Autoconocimiento', description:'Primer diagnóstico realizado' });
    if (stats?.totalEvaluations >= 5) badges.push({ id:'diag5',   icon:'🔬', name:'Analítico',         description:'5 diagnósticos realizados' });
    if ((stats?.completedPlanTasks ?? 0) >= 5) badges.push({ id:'miss5', icon:'🎯', name:'Ejecutor',   description:'5 misiones completadas' });

    return badges;
  });

  ngOnInit(): void {
    this.flow.loadFromBackend(this.familyState.currentFamilyId());
    this.loadProfile();
    this.loadFamilyStats();
    this.loadWhatsAppNumber();
  }

  loadFamilyStats(): void {
    const fid = this.familyState.currentFamilyId();
    if (!fid) return;
    this.http.get<any>(`${this.api.base}/analytics/dashboard/family/${fid}`)
      .pipe(catchError(() => of(null)))
      .subscribe(res => this.familyStats.set(res?.data ?? res));
  }

  loadWhatsAppNumber(): void {
    const fid = this.familyState.getSelectedFamilyId();
    if (!fid) { this.waLoading.set(false); return; }
    this.http.get<any>(`${this.api.base}/families/${fid}`)
      .pipe(catchError(() => of(null)))
      .subscribe(res => {
        const family = res?.data ?? res;
        this.waNumber.set(family?.whatsapp ?? '');
        this.waInput.set(family?.whatsapp ?? '');
        this.waLoading.set(false);
      });
  }

  onWaInput(value: string): void {
    this.waInput.set(value.replace(/\D/g, ''));
  }

  saveWaNumber(): void {
    const num = this.waInput().replace(/\D/g, '');
    if (num.length < 10) { this.waError.set('Ingresa un número de 10 dígitos.'); return; }
    const fid = this.familyState.getSelectedFamilyId();
    if (!fid) return;
    this.waSaving.set(true);
    this.waError.set('');
    this.http.get<any>(`${this.api.base}/families/${fid}`).pipe(catchError(() => of(null))).subscribe(res => {
      const familyData = res?.data ?? res;
      if (!familyData) { this.waSaving.set(false); this.waError.set('Error al cargar datos de la familia.'); return; }
      familyData.whatsapp = num;
      this.http.put<any>(`${this.api.base}/families/${fid}`, familyData)
        .pipe(catchError(() => of(null)))
        .subscribe(upd => {
          this.waSaving.set(false);
          if (upd) {
            this.waNumber.set(num);
            this.editingWa.set(false);
          } else {
            this.waError.set('Error al guardar. Intenta de nuevo.');
          }
        });
    });
  }

  loadProfile(): void {
    this.loading.set(true);
    this.hasBackendError.set(false);

    this.auth.getAuthenticatedProfile().pipe(
      catchError(() => of(null))
    ).subscribe(data => {
      if (data) {
        this.profile.set(data);
      } else {
        const u = this.localUser();
        if (u) {
          this.profile.set({
            id: 0, email: u.email, fullName: u.fullName,
            role: u.role, familyId: u.familyId ?? null, familyName: u.familyName ?? null,
          });
        }
        this.hasBackendError.set(true);
      }
      this.loading.set(false);
    });
  }

  readonly initials = computed(() => {
    const name = this.profile()?.fullName || this.localUser()?.fullName || '';
    return name.trim().split(/\s+/).map(n => n[0]).join('').toUpperCase().substring(0, 2) || '?';
  });

  readonly displayName = computed(() =>
    this.profile()?.fullName || this.localUser()?.fullName || 'Usuario'
  );

  readonly displayEmail = computed(() =>
    this.profile()?.email || this.localUser()?.email || '—'
  );

  readonly isTokenActive = computed(() => !!this.auth.getToken());

  readonly roleLabel = computed(() => {
    const role = this.profile()?.role || this.localUser()?.role || '';
    if (role.includes('ADMIN') || role.includes('SENTINEL')) return 'Administrador';
    return 'Miembro Familiar';
  });

  readonly roleColorClass = computed(() => {
    const role = this.profile()?.role || this.localUser()?.role || '';
    return (role.includes('ADMIN') || role.includes('SENTINEL')) ? 'badge-admin' : 'badge-user';
  });

  readonly showLogoutConfirm = signal(false);

  logout(): void {
    this.showLogoutConfirm.set(true);
  }

  confirmLogout(): void {
    this.auth.logout();
  }

  cancelLogout(): void {
    this.showLogoutConfirm.set(false);
  }
}
