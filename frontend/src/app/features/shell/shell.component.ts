import { Component, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterOutlet, RouterLink, RouterLinkActive } from '@angular/router';
import { AuthService } from '../../core/services/auth.service';

@Component({
  selector: 'app-shell',
  standalone: true,
  imports: [CommonModule, RouterOutlet, RouterLink, RouterLinkActive],
  template: `
    <nav class="navbar navbar-expand-lg navbar-dark bg-primary shadow">
      <div class="container">
        <a class="navbar-brand fw-bold" routerLink="/dashboard">Integrity Family</a>
        
        <div class="collapse navbar-collapse">
          <ul class="navbar-nav me-auto">
            <li class="nav-item">
              <a class="nav-link" routerLink="/admin/stats" routerLinkActive="active">Auditoría</a>
            </li>
          </ul>
          
          <div class="d-flex align-items-center text-white">
            <span class="me-3 small">Bienvenido, <strong>{{ auth.fullName }}</strong></span>
            <button (click)="onLogout()" class="btn btn-outline-light btn-sm">
              <i class="fas fa-sign-out-alt"></i> Salir
            </button>
          </div>
        </div>
      </div>
    </nav>

    <main class="container mt-4">
      <router-outlet></router-outlet>
    </main>
  `,
  styles: [`
    .active { border-bottom: 2px solid white; font-weight: bold; }
    .navbar-brand { letter-spacing: 1px; }
  `]
})
export class ShellComponent {
  public auth = inject(AuthService);

  onLogout(): void {
    if (confirm('¿Deseas cerrar la sesión en el Nodo Armenia?')) {
      this.auth.logout();
    }
  }
}