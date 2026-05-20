import { Component, inject } from '@angular/core';
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
        <button class="btn-exit" (click)="logout()">Salir</button>
      </div>
    </div>`
})
export class NavbarComponent {
  private familyState = inject(FamilyStateService);

  constructor(
    protected auth: AuthService,
    private router: Router
  ) {}

  /**
   * Recupera el nombre de la familia seleccionada reactivamente desde el signal.
   */
  get familyName(): string | null { 
    return this.familyState.currentFamilyName() || null; 
  }

  /**
   * Retorna el nombre del usuario logueado.
   * Resuelve la inconsistencia de propiedad 'fullName' en el AuthService.
   */
  get userName(): string { 
    return this.auth.fullName || localStorage.getItem('fullName') || 'Usuario'; 
  }

  /**
   * Cierre de sesión seguro con redirección inmediata.
   */
  logout(): void {
    if (confirm('¿Deseas cerrar tu sesión de forma segura?')) {
      this.auth.logout(); 
      // La redirección se maneja aquí para asegurar el flujo de la UI
      this.router.navigate(['/login']);
    }
  }
}