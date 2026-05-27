import { Component, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common'; 
import { Router } from '@angular/router';
import { AuthService } from '../../core/services/auth.service';
import { FamilyStateService } from '../../core/services/family-state.service';

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
  private familyState = inject(FamilyStateService);
  protected auth      = inject(AuthService);
  private router      = inject(Router);

  readonly showLogoutConfirm = signal(false);

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
}