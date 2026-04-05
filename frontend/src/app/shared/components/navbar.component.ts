import { Component, inject } from '@angular/core';
import { CommonModule } from '@angular/common'; 
import { Router } from '@angular/router';
import { AuthService } from '../../core/services/auth.service';

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
      padding: 14px 28px;
      background: #fff;
      border-bottom: 1px solid #e2e8f0;
      height: 64px;
    }
    .brand-context {
      display: flex;
      flex-direction: column;
    }
    .user-area {
      display: flex;
      align-items: center;
      gap: 12px;
    }
    .chip {
      background: #F5F4F0;
      border: 1px solid #e2e8f0;
      border-radius: 999px;
      padding: 6px 16px;
      font-size: 13px;
      font-weight: 600;
      color: #1A3A2A; 
    }
    .btn-exit {
      font-size: 13px;
      padding: 6px 14px;
      border: 1px solid #fee2e2;
      background: #fef2f2;
      color: #dc2626;
      border-radius: 8px;
      cursor: pointer;
      font-weight: 600;
      transition: all 0.2s;
    }
    .btn-exit:hover {
      background: #dc2626;
      color: #fff;
    }
  `],
  template: `
    <div class="topbar">
      <div class="brand-context">
        <strong style="font-size: 14px; color: #1e293b;">Bienestar, autonomía y progreso familiar</strong>
        
        @if (familyName) {
          <div style="font-size: 11px; color: #64748b; font-weight: 500;">
            📍 Contexto: {{ familyName }}
          </div>
        } @else {
          <div style="font-size: 11px; color: #94a3b8; font-style: italic;">
            Selecciona una familia para comenzar
          </div>
        }
      </div>

      <div class="user-area">
        <span class="chip">👤 {{ userName }}</span>
        <button class="btn-exit" (click)="logout()">Salir</button>
      </div>
    </div>`
})
export class NavbarComponent {
  protected auth = inject(AuthService);
  private router = inject(Router);

  /**
   * Recupera el nombre de la familia seleccionada desde el almacenamiento.
   */
  get familyName(): string | null { 
    return localStorage.getItem('selectedFamilyName'); 
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