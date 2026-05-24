import { Component, inject, computed, OnInit } from '@angular/core';
import { RouterLink, RouterLinkActive, Router, NavigationEnd } from '@angular/router';
import { CommonModule } from '@angular/common';
import { AuthService } from '../../core/services/auth.service';
import { filter } from 'rxjs/operators';

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
        <div class="sidebar-logo-container">
          <img src="assets/logo.svg" alt="Integrity Family" class="sidebar-logo" />
        </div>
        <div class="version-tag">NODO CENTRAL v4.2</div>
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
        
        <div class="nav-group" [class.expanded]="isDiagnosticExpanded" [class.group-active]="isDiagnosticActive()">
          <button type="button" class="nav-item group-header" (click)="toggleDiagnostic($event)">
            <span class="icon">◈</span> 3. Diagnóstico <span class="chevron" [class.rotated]="isDiagnosticExpanded">▶</span>
          </button>
          <div class="sub-menu" *ngIf="isDiagnosticExpanded">
            <a routerLink="/evaluations/start" class="nav-item nav-sub" routerLinkActive="active">
              <span class="bullet">{{ isRouteActive('/evaluations/start') ? '●' : '○' }}</span> Nueva evaluación
            </a>
            <a routerLink="/evaluations/history" class="nav-item nav-sub" routerLinkActive="active">
              <span class="bullet">{{ isRouteActive('/evaluations/history') ? '●' : '○' }}</span> Historial
            </a>
            <a routerLink="/evaluations/evolution" class="nav-item nav-sub" routerLinkActive="active">
              <span class="bullet">{{ isRouteActive('/evaluations/evolution') ? '●' : '○' }}</span> Evolución
            </a>
          </div>
        </div>

        <a routerLink="/plans" class="nav-item" routerLinkActive="active"><span class="icon">📝</span> 4. Planes</a>
        <a routerLink="/checklist" class="nav-item" routerLinkActive="active"><span class="icon">📸</span> 5. Evidencias</a>
        <a routerLink="/logbook"   class="nav-item" routerLinkActive="active"><span class="icon">📔</span> 6. Bitácora</a>
        <a routerLink="/gratitude" class="nav-item" routerLinkActive="active"><span class="icon">💖</span> 7. Gratitud</a>
        <a routerLink="/my-space"  class="nav-item" routerLinkActive="active"><span class="icon">🔒</span> 8. Mi Espacio</a>
        
        <div class="divider"></div>
        <a routerLink="/cognitive" class="nav-item" routerLinkActive="active"><span class="icon">🧠</span> Sistema Cognitivo</a>
        <a routerLink="/chat"   class="nav-item" routerLinkActive="active"><span class="icon">✨</span> Consultor IA</a>
        <a routerLink="/crisis" class="nav-item crisis-btn" routerLinkActive="active"><span class="icon">🆘</span> Crisis</a>

        <div class="divider"></div>
        <a routerLink="/profile" class="nav-item" routerLinkActive="active"><span class="icon">👤</span> Mi Perfil</a>
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
    .brand { display: flex; flex-direction: column; align-items: center; padding: 0 20px 20px; border-bottom: 1px solid rgba(255,255,255,0.06); margin-bottom: 24px; }
    .sidebar-logo-container { width: 100px; height: 120px; border-radius: 20px; overflow: hidden; box-shadow: 0 8px 24px rgba(0,0,0,0.5); border: 1px solid rgba(255,255,255,0.05); transition: transform 0.3s; }
    .sidebar-logo-container:hover { transform: scale(1.05); }
    .sidebar-logo { width: 100%; height: 100%; object-fit: cover; }
    .version-tag { font-size: 8px; color: #818cf8; font-weight: bold; background: rgba(99, 102, 241, 0.12); padding: 3px 12px; border-radius: 20px; margin-top: 12px; border: 1px solid rgba(99, 102, 241, 0.15); }
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
    .group-header { background: none; border: none; width: 100%; text-align: left; cursor: pointer; font-family: inherit; }
    .group-header .chevron { margin-left: auto; font-size: 10px; transition: transform 0.3s ease; color: rgba(255,255,255,0.3); }
    .group-header .chevron.rotated { transform: rotate(90deg); color: #818cf8; }
    .group-active .group-header { color: #818cf8 !important; font-weight: 600; }
    .sub-menu { margin-left: 28px; padding-left: 12px; border-left: 1px dashed rgba(255,255,255,0.08); margin-top: 4px; margin-bottom: 8px; display: flex; flex-direction: column; gap: 2px; }
    .nav-sub { padding: 8px 12px; font-size: 13px; color: rgba(255,255,255,0.5); border-radius: 6px; display: flex; align-items: center; gap: 8px; text-decoration: none; transition: all 0.2s; }
    .nav-sub:hover { color: #fff; background: rgba(255,255,255,0.03); }
    .nav-sub.active { background: rgba(99, 102, 241, 0.08) !important; color: #818cf8 !important; border: 1px solid rgba(99, 102, 241, 0.15); opacity: 1; }
    .bullet { font-size: 11px; transition: color 0.3s; width: 12px; text-align: center; color: rgba(255,255,255,0.3); }
    .nav-sub.active .bullet { color: #818cf8; }
  `]
})
export class SidebarComponent implements OnInit {
  private router = inject(Router);

  constructor(private authService: AuthService) {}

  // Estado reactivo sincronizado
  user = this.authService.user;

  isDiagnosticExpanded = false;

  ngOnInit() {
    this.checkActiveRoute();
    this.router.events.pipe(
      filter((event): event is NavigationEnd => event instanceof NavigationEnd)
    ).subscribe(() => {
      this.checkActiveRoute();
    });
  }

  private checkActiveRoute() {
    if (this.router.url.includes('/evaluations')) {
      this.isDiagnosticExpanded = true;
    }
  }

  isRouteActive(route: string): boolean {
    return this.router.url === route;
  }

  isDiagnosticActive(): boolean {
    return this.router.url.includes('/evaluations');
  }

  toggleDiagnostic(event: MouseEvent) {
    event.preventDefault();
    this.isDiagnosticExpanded = !this.isDiagnosticExpanded;
  }

  handleLogout() {
    if (confirm('¿Finalizar sesión en el Nodo Central?')) {
      this.authService.logout();
    }
  }
}