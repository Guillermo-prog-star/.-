import { Component, inject, signal, computed } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';
import { AuthService } from '../../core/services/auth.service';
import { FamilyStateService } from '../../core/services/family-state.service';
import { SentinelCoreService } from '../../core/services/sentinel-core.service';
import { UserNotificationService } from '../../core/services/user-notification.service';

/**
 * NavbarComponent: Barra de navegación con contexto familiar.
 * Resuelve el error TS2339 al acceder a auth.fullName y optimiza el cierre de sesión.
 */
@Component({
  selector: 'app-navbar',
  standalone: true,
  imports: [CommonModule],
  styles: [`
    .topbar {
      display: flex;
      justify-content: space-between;
      align-items: center;
      padding: 0 32px;
      background: var(--surface);
      border-bottom: 1px solid var(--border);
      height: 64px;
      font-family: 'Inter', sans-serif;
    }
    .brand-context { display: flex; flex-direction: column; gap: 2px; }
    .title-context { font-size: 14px; font-weight: 700; color: var(--primary); letter-spacing: -0.01em; }
    .f-context { font-size: 11px; color: var(--text-muted); font-weight: 600; display: flex; align-items: center; gap: 4px; }
    
    .user-area { display: flex; align-items: center; gap: 12px; }
    .chip {
      background: var(--surface-alt);
      border: 1px solid var(--border);
      border-radius: 999px;
      padding: 6px 16px;
      font-size: 13px;
      font-weight: 700;
      color: var(--primary);
      display: flex;
      align-items: center;
      gap: 8px;
      white-space: nowrap;
      text-overflow: ellipsis;
      overflow: hidden;
      max-width: 180px;
    }
    .btn-exit {
      font-size: 13px;
      padding: 8px 16px;
      background: var(--error-light);
      color: var(--error);
      border-radius: 10px;
      cursor: pointer;
      font-weight: 700;
      transition: all 0.2s;
    }
    .btn-exit:hover { background: var(--error); color: #fff; transform: scale(1.05); }
    .logout-inline { display: flex; align-items: center; gap: 8px; }
    .confirm-label { font-size: 12px; color: rgba(255,255,255,0.65); white-space: nowrap; }
    .confirm-yes { font-size: 12px; font-weight: 700; padding: 6px 14px; background: rgba(239,68,68,0.15); border: 1px solid rgba(239,68,68,0.3); color: #f87171; border-radius: 8px; cursor: pointer; transition: all 0.2s; }
    .confirm-yes:hover { background: rgba(239,68,68,0.35); color: #fff; }
    .confirm-no { font-size: 12px; font-weight: 700; padding: 6px 14px; background: rgba(255,255,255,0.04); border: 1px solid rgba(255,255,255,0.1); color: rgba(255,255,255,0.5); border-radius: 8px; cursor: pointer; transition: all 0.2s; }
    .confirm-no:hover { background: rgba(255,255,255,0.08); color: rgba(255,255,255,0.8); }

    .alert-bell {
      position: relative;
      display: flex;
      align-items: center;
      justify-content: center;
      width: 38px;
      height: 38px;
      border-radius: 10px;
      background: rgba(239,68,68,0.12);
      border: 1px solid rgba(239,68,68,0.3);
      cursor: pointer;
      transition: all 0.2s;
      font-size: 17px;
      flex-shrink: 0;
    }
    .alert-bell:hover { background: rgba(239,68,68,0.25); border-color: rgba(239,68,68,0.6); transform: scale(1.08); }
    .alert-bell.critical { animation: bell-pulse 1.6s ease-in-out infinite; }
    .alert-badge {
      position: absolute;
      top: -5px;
      right: -5px;
      min-width: 17px;
      height: 17px;
      padding: 0 4px;
      border-radius: 999px;
      background: #ef4444;
      color: #fff;
      font-size: 10px;
      font-weight: 800;
      display: flex;
      align-items: center;
      justify-content: center;
      line-height: 1;
      border: 1.5px solid var(--surface);
    }
    @keyframes bell-pulse {
      0%, 100% { box-shadow: 0 0 0 0 rgba(239,68,68,0.5); }
      50% { box-shadow: 0 0 0 6px rgba(239,68,68,0); }
    }

    .notif-wrap {
      position: relative;
    }
    .notif-bell {
      position: relative;
      display: flex;
      align-items: center;
      justify-content: center;
      width: 38px;
      height: 38px;
      border-radius: 10px;
      background: rgba(99,102,241,0.10);
      border: 1px solid rgba(99,102,241,0.28);
      cursor: pointer;
      transition: all 0.2s;
      font-size: 17px;
      flex-shrink: 0;
    }
    .notif-bell:hover { background: rgba(99,102,241,0.22); border-color: rgba(99,102,241,0.55); transform: scale(1.08); }
    .notif-bell.has-unread { animation: notif-pulse 2s ease-in-out infinite; }
    .notif-badge {
      position: absolute;
      top: -5px;
      right: -5px;
      min-width: 17px;
      height: 17px;
      padding: 0 4px;
      border-radius: 999px;
      background: #6366f1;
      color: #fff;
      font-size: 10px;
      font-weight: 800;
      display: flex;
      align-items: center;
      justify-content: center;
      line-height: 1;
      border: 1.5px solid var(--surface);
    }
    @keyframes notif-pulse {
      0%, 100% { box-shadow: 0 0 0 0 rgba(99,102,241,0.45); }
      50% { box-shadow: 0 0 0 5px rgba(99,102,241,0); }
    }
    .notif-dropdown {
      position: absolute;
      top: calc(100% + 8px);
      right: 0;
      width: 320px;
      background: #18181c;
      border: 1px solid rgba(255,255,255,0.08);
      border-radius: 14px;
      box-shadow: 0 16px 48px rgba(0,0,0,0.55);
      z-index: 200;
      overflow: hidden;
    }
    .notif-dropdown-header {
      display: flex;
      align-items: center;
      justify-content: space-between;
      padding: 12px 16px 10px;
      border-bottom: 1px solid rgba(255,255,255,0.06);
    }
    .notif-dropdown-title {
      font-size: 12px;
      font-weight: 800;
      color: rgba(255,255,255,0.7);
      text-transform: uppercase;
      letter-spacing: 0.06em;
    }
    .notif-mark-read {
      font-size: 11px;
      color: #6366f1;
      cursor: pointer;
      font-weight: 700;
      background: none;
      border: none;
      padding: 0;
    }
    .notif-mark-read:hover { color: #818cf8; }
    .notif-list { max-height: 320px; overflow-y: auto; }
    .notif-item {
      display: flex;
      gap: 10px;
      padding: 11px 16px;
      border-bottom: 1px solid rgba(255,255,255,0.04);
      transition: background 0.15s;
      cursor: default;
    }
    .notif-item:last-child { border-bottom: none; }
    .notif-item:hover { background: rgba(255,255,255,0.03); }
    .notif-item.unread { background: rgba(99,102,241,0.06); }
    .notif-item-icon { font-size: 18px; flex-shrink: 0; margin-top: 1px; }
    .notif-item-body { flex: 1; min-width: 0; }
    .notif-item-title { font-size: 12px; font-weight: 700; color: rgba(255,255,255,0.85); margin-bottom: 2px; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
    .notif-item-msg { font-size: 11px; color: rgba(255,255,255,0.45); line-height: 1.4; display: -webkit-box; -webkit-line-clamp: 2; -webkit-box-orient: vertical; overflow: hidden; }
    .notif-item-time { font-size: 10px; color: rgba(255,255,255,0.25); margin-top: 3px; }
    .notif-empty { padding: 24px 16px; text-align: center; font-size: 12px; color: rgba(255,255,255,0.3); }
    .notif-overlay { position: fixed; inset: 0; z-index: 199; }

    @media (max-width: 768px) {
      .topbar {
        padding: 0 12px;
      }
      .title-context {
        display: none;
      }
      .f-context {
        font-size: 11px;
      }
      .user-area {
        gap: 6px;
      }
      .chip {
        padding: 4px 10px;
        font-size: 11px;
        max-width: 120px;
      }
      .btn-exit {
        padding: 6px 12px;
        font-size: 11px;
      }
    }
  `],
  template: `
    <div class="topbar">
      <div class="brand-context">
        <div class="title-context">Bienestar, autonomía y progreso familiar</div>
        
        <div class="f-context">
          @if (familyName) {
            <span style="color: var(--accent);">📍</span> Familia: {{ familyName }}
          } @else {
            <span style="font-style: italic; opacity: 0.7;">Selecciona una familia para comenzar</span>
          }
        </div>
      </div>

      <div class="user-area">

        <!-- Family notification bell -->
        <div class="notif-wrap">
          @if (showNotifDropdown()) {
            <div class="notif-overlay" (click)="closeNotifs()"></div>
          }
          <button class="notif-bell" [class.has-unread]="notifUnread() > 0" (click)="toggleNotifs()" title="Notificaciones">
            🔔
            @if (notifUnread() > 0) {
              <span class="notif-badge">{{ notifUnread() > 9 ? '9+' : notifUnread() }}</span>
            }
          </button>
          @if (showNotifDropdown()) {
            <div class="notif-dropdown">
              <div class="notif-dropdown-header">
                <span class="notif-dropdown-title">Notificaciones</span>
                @if (notifUnread() > 0) {
                  <button class="notif-mark-read" (click)="markAllRead()">Marcar todas como leídas</button>
                }
              </div>
              <div class="notif-list">
                @if (notifList().length === 0) {
                  <div class="notif-empty">Sin notificaciones recientes</div>
                }
                @for (n of notifList(); track n.id) {
                  <div class="notif-item" [class.unread]="!n.viewed">
                    <span class="notif-item-icon">{{ notifSvc.typeIcon(n.type) }}</span>
                    <div class="notif-item-body">
                      <div class="notif-item-title">{{ n.title }}</div>
                      <div class="notif-item-msg">{{ n.message }}</div>
                      <div class="notif-item-time">{{ n.sentAt | date:'d MMM · HH:mm' }}</div>
                    </div>
                  </div>
                }
              </div>
            </div>
          }
        </div>

        <!-- Sentinel admin alert bell -->
        @if (alertCount() > 0) {
          <button class="alert-bell" [class.critical]="hasCritical()" (click)="goToAlerts()" title="Ver alertas Sentinel">
            🚨
            <span class="alert-badge">{{ alertCount() > 9 ? '9+' : alertCount() }}</span>
          </button>
        }

        <span class="chip">👤 {{ userName }}</span>
        @if (showLogoutConfirm()) {
          <div class="logout-inline">
            <span class="confirm-label">¿Cerrar sesión?</span>
            <button class="confirm-yes" (click)="confirmLogout()">Sí, salir</button>
            <button class="confirm-no" (click)="cancelLogout()">No</button>
          </div>
        } @else {
          <button class="btn-exit" (click)="logout()">Salir</button>
        }
      </div>
    </div>`
})
export class NavbarComponent {
  private familyState   = inject(FamilyStateService);
  protected auth        = inject(AuthService);
  private router        = inject(Router);
  private sentinel      = inject(SentinelCoreService);
  protected notifSvc    = inject(UserNotificationService);

  readonly showLogoutConfirm  = signal(false);
  readonly showNotifDropdown  = signal(false);

  // Sentinel admin alerts
  readonly alertCount  = computed(() => this.sentinel.alerts().filter((a: any) => !a.viewed).length);
  readonly hasCritical = computed(() => this.sentinel.hasCriticalAlert());

  // Family notifications
  readonly notifUnread = computed(() => this.notifSvc.unreadCount());
  readonly notifList   = computed(() => this.notifSvc.notifications().slice(0, 8));

  /**
   * Recupera el nombre de la familia seleccionada reactivamente desde el signal.
   */
  get familyName(): string | null { 
    return this.familyState.currentFamilyName() || null; 
  }

  get userName(): string {
    return this.auth.user()?.fullName || 'Usuario';
  }

  logout(): void {
    this.showLogoutConfirm.set(true);
  }

  confirmLogout(): void {
    this.auth.logout();
  }

  cancelLogout(): void {
    this.showLogoutConfirm.set(false);
  }

  goToAlerts(): void {
    this.router.navigate(['/dashboard']);
  }

  toggleNotifs(): void {
    const opening = !this.showNotifDropdown();
    this.showNotifDropdown.set(opening);
    if (opening && this.notifSvc.unreadCount() > 0) {
      this.notifSvc.markAllRead();
    }
  }

  closeNotifs(): void {
    this.showNotifDropdown.set(false);
  }

  markAllRead(): void {
    this.notifSvc.markAllRead();
  }
}