import { Component, signal, computed, inject, ChangeDetectionStrategy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterOutlet, RouterLink, RouterLinkActive } from '@angular/router';
import { AuthService } from '../../core/services/auth.service';
import { SentinelCoreService } from '../../core/services/sentinel-core.service';
import { FamilyStateService } from '../../core/services/family-state.service';

interface NavSection {
  label?: string;
  items: NavItem[];
}

interface NavItem {
  label: string;
  icon: string;
  route: string | null;   // null → computed guardian route
  exact?: boolean;
  adminOnly?: boolean;
}

@Component({
  selector: 'app-shell',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [CommonModule, RouterOutlet, RouterLink, RouterLinkActive],
  template: `
    <div class="shell-root">

      <!-- ── Sidebar ──────────────────────────────────────────────────────── -->
      <!-- Mobile overlay -->
      <div *ngIf="sidebarOpen()"
           class="sidebar-overlay lg:hidden"
           (click)="closeSidebar()"></div>

      <aside class="sidebar" [class.sidebar--open]="sidebarOpen()">

        <!-- Logo -->
        <div class="sidebar-logo">
          <div class="logo-icon">🧩</div>
          <div>
            <p class="logo-title">INTEGRITY</p>
            <p class="logo-sub">FAMILY</p>
          </div>
          <!-- Mobile close -->
          <button class="sidebar-close lg:hidden" (click)="closeSidebar()">✕</button>
        </div>

        <!-- Navigation -->
        <nav class="sidebar-nav">

          <!-- Core -->
          <ul class="nav-list">
            <li>
              <a routerLink="/dashboard" routerLinkActive="nav-link--active"
                 [routerLinkActiveOptions]="{exact:true}"
                 class="nav-link" (click)="closeSidebar()">
                <span class="nav-icon">🏠</span>
                <span class="nav-label">Dashboard</span>
              </a>
            </li>
            <li>
              <a routerLink="/my-space" routerLinkActive="nav-link--active"
                 class="nav-link" (click)="closeSidebar()">
                <span class="nav-icon">🌟</span>
                <span class="nav-label">Mi Espacio</span>
              </a>
            </li>
            <li>
              <a routerLink="/chat" routerLinkActive="nav-link--active"
                 class="nav-link" (click)="closeSidebar()">
                <span class="nav-icon">💬</span>
                <span class="nav-label">Copiloto IA</span>
              </a>
            </li>
          </ul>

          <!-- Familia -->
          <p class="nav-section-label">Núcleo Familiar</p>
          <ul class="nav-list">
            <li>
              <a routerLink="/members" routerLinkActive="nav-link--active"
                 class="nav-link" (click)="closeSidebar()">
                <span class="nav-icon">👥</span>
                <span class="nav-label">Miembros</span>
              </a>
            </li>
            <li>
              <a routerLink="/plans" routerLinkActive="nav-link--active"
                 class="nav-link" (click)="closeSidebar()">
                <span class="nav-icon">🎯</span>
                <span class="nav-label">Plan de Acción</span>
              </a>
            </li>
            <li>
              <a routerLink="/checklist" routerLinkActive="nav-link--active"
                 class="nav-link" (click)="closeSidebar()">
                <span class="nav-icon">✅</span>
                <span class="nav-label">Checklist</span>
              </a>
            </li>
            <li>
              <a routerLink="/logbook" routerLinkActive="nav-link--active"
                 class="nav-link" (click)="closeSidebar()">
                <span class="nav-icon">📖</span>
                <span class="nav-label">Bitácora</span>
              </a>
            </li>
            <li>
              <a routerLink="/gratitude" routerLinkActive="nav-link--active"
                 class="nav-link" (click)="closeSidebar()">
                <span class="nav-icon">🙏</span>
                <span class="nav-label">Gratitud</span>
              </a>
            </li>
            <li>
              <a routerLink="/crisis" routerLinkActive="nav-link--active"
                 class="nav-link" (click)="closeSidebar()">
                <span class="nav-icon">🆘</span>
                <span class="nav-label">Crisis</span>
              </a>
            </li>
            <li *ngIf="familyId > 0">
              <a [routerLink]="['/guardian', familyId, 'election']"
                 routerLinkActive="nav-link--active"
                 class="nav-link" (click)="closeSidebar()">
                <span class="nav-icon">🛡️</span>
                <span class="nav-label">Guardián</span>
              </a>
            </li>
          </ul>

          <!-- Diagnóstico -->
          <p class="nav-section-label">Diagnóstico</p>
          <ul class="nav-list">
            <li>
              <a routerLink="/evaluations/history" routerLinkActive="nav-link--active"
                 class="nav-link" (click)="closeSidebar()">
                <span class="nav-icon">🔬</span>
                <span class="nav-label">Evaluaciones</span>
              </a>
            </li>
            <li>
              <a routerLink="/cognitive" routerLinkActive="nav-link--active"
                 class="nav-link" (click)="closeSidebar()">
                <span class="nav-icon">🧠</span>
                <span class="nav-label">Motor Cognitivo</span>
              </a>
            </li>
            <li>
              <a routerLink="/portal" routerLinkActive="nav-link--active"
                 class="nav-link" (click)="closeSidebar()">
                <span class="nav-icon">📱</span>
                <span class="nav-label">Portal Familiar</span>
              </a>
            </li>
          </ul>

          <!-- Admin (role-gated) -->
          <ng-container *ngIf="isAdmin">
            <p class="nav-section-label nav-section-label--admin">Admin</p>
            <ul class="nav-list">
              <li>
                <a routerLink="/families" routerLinkActive="nav-link--active"
                   class="nav-link" (click)="closeSidebar()">
                  <span class="nav-icon">👨‍👩‍👧‍👦</span>
                  <span class="nav-label">Familias</span>
                </a>
              </li>
              <li>
                <a routerLink="/admin/stats" routerLinkActive="nav-link--active"
                   class="nav-link" (click)="closeSidebar()">
                  <span class="nav-icon">⚙️</span>
                  <span class="nav-label">Panel Admin</span>
                </a>
              </li>
            </ul>
          </ng-container>

        </nav>

        <!-- Sidebar Footer -->
        <div class="sidebar-footer">
          <a routerLink="/profile" routerLinkActive="nav-link--active"
             class="nav-link" (click)="closeSidebar()">
            <span class="nav-icon">👤</span>
            <span class="nav-label footer-name">{{ firstName }}</span>
          </a>
          <div *ngIf="showLogoutConfirm(); else logoutIconBtn">
            <button class="logout-btn logout-btn--confirm" (click)="confirmLogout()">✓</button>
            <button class="logout-btn" (click)="cancelLogout()">✕</button>
          </div>
          <ng-template #logoutIconBtn>
            <button class="logout-btn" (click)="onLogout()">
              <span>⏏</span>
            </button>
          </ng-template>
        </div>

      </aside>

      <!-- ── Main Area ─────────────────────────────────────────────────────── -->
      <div class="main-wrapper">

        <!-- Mobile top bar -->
        <header class="topbar lg:hidden">
          <button class="hamburger" (click)="toggleSidebar()" aria-label="Abrir menú">
            <span></span><span></span><span></span>
          </button>
          <span class="topbar-title">Integrity Family</span>
          <div class="topbar-spacer"></div>
        </header>

        <!-- Content -->
        <main class="main-content">
          <router-outlet></router-outlet>
        </main>

      </div>

    </div>
  `,
  styles: [`
    /* ── Layout ─────────────────────────────────────────────────── */
    .shell-root {
      display: flex;
      height: 100vh;
      overflow: hidden;
      background: #020617;
      font-family: 'Outfit', sans-serif;
    }

    /* ── Sidebar ─────────────────────────────────────────────────── */
    .sidebar {
      width: 240px;
      min-width: 240px;
      height: 100vh;
      display: flex;
      flex-direction: column;
      background: rgba(15, 23, 42, 0.95);
      border-right: 1px solid rgba(255, 255, 255, 0.06);
      overflow-y: auto;
      overflow-x: hidden;
      flex-shrink: 0;
      transition: transform 0.3s cubic-bezier(0.4, 0, 0.2, 1);
      z-index: 50;
    }

    /* Mobile: sidebar off-canvas */
    @media (max-width: 1023px) {
      .sidebar {
        position: fixed;
        top: 0; left: 0;
        transform: translateX(-100%);
      }
      .sidebar--open {
        transform: translateX(0);
        box-shadow: 8px 0 40px rgba(0,0,0,0.5);
      }
      .sidebar-overlay {
        position: fixed;
        inset: 0;
        background: rgba(0,0,0,0.6);
        z-index: 40;
      }
    }

    /* Scrollbar inside sidebar */
    .sidebar::-webkit-scrollbar { width: 4px; }
    .sidebar::-webkit-scrollbar-track { background: transparent; }
    .sidebar::-webkit-scrollbar-thumb {
      background: rgba(99,102,241,0.2);
      border-radius: 4px;
    }

    /* ── Logo ────────────────────────────────────────────────────── */
    .sidebar-logo {
      display: flex;
      align-items: center;
      gap: 10px;
      padding: 20px 18px 16px;
      border-bottom: 1px solid rgba(255,255,255,0.05);
      margin-bottom: 8px;
    }
    .logo-icon {
      font-size: 22px;
      width: 36px; height: 36px;
      display: flex; align-items: center; justify-content: center;
      background: rgba(99,102,241,0.15);
      border-radius: 10px;
      border: 1px solid rgba(99,102,241,0.25);
    }
    .logo-title {
      font-size: 11px; font-weight: 800;
      letter-spacing: 0.15em;
      color: #f8fafc;
      margin: 0; line-height: 1;
    }
    .logo-sub {
      font-size: 9px; font-weight: 500;
      letter-spacing: 0.2em;
      color: rgba(99,102,241,0.8);
      margin: 2px 0 0; line-height: 1;
    }
    .sidebar-close {
      margin-left: auto;
      background: none; border: none;
      color: rgba(255,255,255,0.4);
      cursor: pointer; font-size: 14px;
      padding: 4px;
    }

    /* ── Nav ──────────────────────────────────────────────────────── */
    .sidebar-nav {
      flex: 1;
      padding: 4px 10px;
    }
    .nav-list {
      list-style: none;
      margin: 0 0 4px;
      padding: 0;
    }
    .nav-section-label {
      font-size: 9px;
      font-weight: 700;
      letter-spacing: 0.15em;
      text-transform: uppercase;
      color: rgba(255,255,255,0.25);
      padding: 12px 8px 4px;
      margin: 0;
    }
    .nav-section-label--admin {
      color: rgba(168,85,247,0.5);
    }
    .nav-link {
      display: flex;
      align-items: center;
      gap: 10px;
      padding: 8px 10px;
      border-radius: 10px;
      text-decoration: none;
      color: rgba(255,255,255,0.55);
      font-size: 13px;
      font-weight: 500;
      transition: all 0.2s ease;
      cursor: pointer;
      background: transparent;
      border: none;
      width: 100%;
      text-align: left;
    }
    .nav-link:hover {
      color: rgba(255,255,255,0.9);
      background: rgba(255,255,255,0.05);
    }
    .nav-link--active {
      color: #f8fafc !important;
      background: rgba(99,102,241,0.15) !important;
      border: 1px solid rgba(99,102,241,0.2);
    }
    .nav-icon {
      font-size: 15px;
      width: 20px;
      text-align: center;
      flex-shrink: 0;
    }
    .nav-label {
      white-space: nowrap;
      overflow: hidden;
      text-overflow: ellipsis;
    }

    /* ── Sidebar Footer ───────────────────────────────────────────── */
    .sidebar-footer {
      padding: 12px 10px;
      border-top: 1px solid rgba(255,255,255,0.05);
      display: flex;
      align-items: center;
      gap: 6px;
    }
    .sidebar-footer .nav-link {
      flex: 1;
    }
    .footer-name {
      font-weight: 600;
      color: rgba(255,255,255,0.7);
      overflow: hidden;
      text-overflow: ellipsis;
    }
    .logout-btn {
      width: 32px; height: 32px;
      display: flex; align-items: center; justify-content: center;
      background: rgba(239,68,68,0.08);
      border: 1px solid rgba(239,68,68,0.15);
      border-radius: 8px;
      color: rgba(239,68,68,0.7);
      cursor: pointer;
      font-size: 14px;
      transition: all 0.2s ease;
      flex-shrink: 0;
    }
    .logout-btn:hover {
      background: rgba(239,68,68,0.15);
      color: #f87171;
    }
    .logout-btn--confirm {
      background: rgba(34,197,94,0.12);
      border-color: rgba(34,197,94,0.25);
      color: rgba(34,197,94,0.8);
    }
    .logout-btn--confirm:hover {
      background: rgba(34,197,94,0.25);
      color: #4ade80;
    }

    /* ── Main ────────────────────────────────────────────────────── */
    .main-wrapper {
      flex: 1;
      display: flex;
      flex-direction: column;
      overflow: hidden;
    }
    .main-content {
      flex: 1;
      overflow-y: auto;
      overflow-x: hidden;
    }

    /* ── Mobile topbar ───────────────────────────────────────────── */
    .topbar {
      height: 52px;
      display: flex;
      align-items: center;
      gap: 12px;
      padding: 0 16px;
      background: rgba(15, 23, 42, 0.95);
      border-bottom: 1px solid rgba(255,255,255,0.05);
      flex-shrink: 0;
    }
    .topbar-title {
      font-size: 14px;
      font-weight: 700;
      color: rgba(255,255,255,0.8);
      letter-spacing: 0.05em;
    }
    .topbar-spacer { flex: 1; }
    .hamburger {
      display: flex;
      flex-direction: column;
      gap: 5px;
      background: none;
      border: none;
      cursor: pointer;
      padding: 4px;
    }
    .hamburger span {
      display: block;
      width: 20px; height: 2px;
      background: rgba(255,255,255,0.7);
      border-radius: 2px;
    }
  `]
})
export class ShellComponent {
  public auth: AuthService                = inject(AuthService);
  public sentinel: SentinelCoreService    = inject(SentinelCoreService);
  private familyState: FamilyStateService = inject(FamilyStateService);

  readonly sidebarOpen        = signal(false);
  readonly showLogoutConfirm  = signal(false);

  get isAdmin(): boolean {
    return this.auth.user()?.role === 'ADMIN';
  }

  get familyId(): number {
    return this.familyState.getFamilyId();
  }

  get firstName(): string {
    const full = this.auth.user()?.fullName || 'Usuario';
    return full.split(' ')[0];
  }

  toggleSidebar(): void {
    this.sidebarOpen.update(v => !v);
  }

  closeSidebar(): void {
    this.sidebarOpen.set(false);
  }

  onLogout(): void {
    this.showLogoutConfirm.set(true);
  }

  confirmLogout(): void {
    this.auth.logout();
  }

  cancelLogout(): void {
    this.showLogoutConfirm.set(false);
  }
}
