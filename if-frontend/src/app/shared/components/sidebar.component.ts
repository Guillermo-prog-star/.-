import { Component, inject, computed } from '@angular/core';
import { RouterLink, RouterLinkActive, Router } from '@angular/router';
import { CommonModule } from '@angular/common';
import { AuthService } from '../../core/services/auth.service';

/**
 * SDD: Sidebar Sentinel Component (v4.2 Sincronizada)
 * Postura Técnica: Eliminación de getters estáticos de localStorage en favor de Signals.
 */
@Component({
  selector: 'app-sidebar',
  standalone: true,
  imports: [CommonModule, RouterLink, RouterLinkActive],
  template: `
    <div class="sidebar">
      <div class="brand">
        <div class="logo-circle">IF</div>
        <div class="brand-title">INTEGRITY FAMILY</div>
        <div class="version-tag">NODO ARMENIA v4.2</div>
      </div>
      
      <nav>
        <a routerLink="/dashboard" class="nav-item" routerLinkActive="active">
          <span class="icon">📊</span> Panóptico Clínico
        </a>
        <a routerLink="/portal" class="nav-item" routerLinkActive="active">
          <span class="icon">📱</span> Portal Familiar Móvil
        </a>
        <div class="divider"></div>
        
        <a routerLink="/families" class="nav-item" routerLinkActive="active"><span class="icon">👨‍👩‍👧‍👦</span> 1. Familias</a>
        <a routerLink="/members"  class="nav-item" routerLinkActive="active"><span class="icon">👥</span> 2. Miembros</a>
        <a routerLink="/evaluations/start" class="nav-item" routerLinkActive="active"><span class="icon">◈</span> 3. Diagnóstico</a>
        <a routerLink="/plans" class="nav-item" routerLinkActive="active"><span class="icon">📝</span> 4. Planes</a>
        <a routerLink="/checklist" class="nav-item" routerLinkActive="active"><span class="icon">📸</span> 5. Evidencias</a>
        <a routerLink="/logbook"   class="nav-item" routerLinkActive="active"><span class="icon">📔</span> 6. Bitácora</a>
        <a routerLink="/gratitude" class="nav-item" routerLinkActive="active"><span class="icon">💖</span> 7. Gratitud</a>
        
        <div class="divider"></div>
        <a routerLink="/chat"   class="nav-item" routerLinkActive="active"><span class="icon">✨</span> Consultor IA</a>
        <a routerLink="/crisis" class="nav-item crisis-btn" routerLinkActive="active"><span class="icon">🆘</span> Crisis</a>
      </nav>

      <div class="family-box">
        <div class="f-name">{{ user()?.fullName }}</div>
        <div class="f-milestone">● {{ user()?.role }}</div>
        <button (click)="handleLogout()" class="logout-link">Cerrar Sesión</button>
      </div>
    </div>
  `,
  styles: [`
    .sidebar { width: 280px; background: #0a0a0c; height: 100vh; padding: 32px 0; display: flex; flex-direction: column; position: fixed; top: 0; left: 0; border-right: 1px solid rgba(255,255,255,0.05); z-index: 1000; }
    .brand { display: flex; flex-direction: column; align-items: center; padding: 0 20px 24px; border-bottom: 1px solid rgba(255,255,255,0.1); margin-bottom: 24px; }
    .logo-circle { width: 50px; height: 50px; background: linear-gradient(135deg, #6366f1, #a855f7); border-radius: 12px; display: flex; align-items: center; justify-content: center; color: white; font-weight: 900; font-size: 20px; box-shadow: 0 10px 20px rgba(0,0,0,0.4); }
    .brand-title { color: #fff; font-size: 12px; font-weight: 800; letter-spacing: 2px; margin-top: 14px; }
    .version-tag { font-size: 8px; color: #6366f1; font-weight: bold; background: rgba(99, 102, 241, 0.1); padding: 2px 10px; border-radius: 20px; margin-top: 6px; }
    nav { flex: 1; padding: 0 16px; overflow-y: auto; }
    .nav-item { display: flex; align-items: center; gap: 12px; padding: 12px 16px; border-radius: 10px; color: rgba(255,255,255,.5); font-size: 14px; transition: all .3s; text-decoration: none; margin-bottom: 4px; }
    .nav-item:hover { color: #fff; background: rgba(255,255,255,0.05); transform: translateX(4px); }
    .active { background: rgba(99, 102, 241, 0.1) !important; color: #818cf8 !important; border: 1px solid rgba(99, 102, 241, 0.2); }
    .divider { height: 1px; background: rgba(255,255,255,0.05); margin: 16px 20px; }
    .family-box { margin: 20px; padding: 16px; background: rgba(255,255,255,0.03); border-radius: 12px; border: 1px solid rgba(255,255,255,0.05); }
    .f-name { color: #fff; font-size: 13px; font-weight: 700; }
    .f-milestone { color: #6366f1; font-size: 10px; text-transform: uppercase; font-weight: bold; margin-bottom: 8px; }
    .logout-link { background: none; border: none; color: #ff4444; font-size: 10px; font-weight: bold; cursor: pointer; padding: 0; text-transform: uppercase; }
    .admin-item { border: 1px dashed rgba(99, 102, 241, 0.3); }
  `]
})
export class SidebarComponent {
  constructor(private authService: AuthService) {}

  // Estado reactivo sincronizado
  user = this.authService.user;


  handleLogout() {
    if (confirm('¿Finalizar sesión en el Nodo Armenia?')) {
      this.authService.logout();
    }
  }
}