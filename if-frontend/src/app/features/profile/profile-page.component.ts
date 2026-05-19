import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { AuthService } from '../../core/services/auth.service';
import { StorageService } from '../../core/services/storage.service';

@Component({
  selector: 'app-profile-page',
  standalone: true,
  imports: [CommonModule, FormsModule],
  template: `
    <div class="page-header">
      <div>
        <h1>Mi Perfil de Consultor</h1>
        <p>Gestiona tu identidad y seguridad en la plataforma</p>
      </div>
    </div>

    <div class="grid-profile">
      <div class="card profile-card">
        <div class="avatar-large">{{ initials }}</div>
        <h2 style="margin-top: 16px;">{{ fullName }}</h2>
        <span class="badge badge-blue">Consultor Principal</span>
        <hr style="margin: 20px 0; border: 0; border-top: 1px solid #eee;">
        <div class="info-row">
          <span class="label">Email:</span>
          <span class="value">{{ email }}</span>
        </div>
        <div class="info-row">
          <span class="label">ID de Usuario:</span>
          <span class="value">#{{ userId }}</span>
        </div>
      </div>

      <div class="card">
        <h3>Seguridad de la Sesión</h3>
        <p style="font-size: 13px; color: #64748b; margin-bottom: 20px;">
          Tu sesión está protegida mediante tokens JWT cifrados y comunicación segura con el servidor central.
        </p>
        
        <div class="security-status" [class.secure]="isSecure">
          <div class="status-icon">🛡️</div>
          <div>
            <strong>Estado: Conexión Segura</strong>
            <p style="font-size: 12px; margin: 0;">Token activo e interceptores de red vinculados.</p>
          </div>
        </div>

        <div style="margin-top: 24px;">
          <button class="btn btn-outline" (click)="logout()" style="width: 100%; border-color: #fee2e2; color: #dc2626;">
            Cerrar Sesión Global
          </button>
        </div>
      </div>
    </div>
  `,
  styles: [`
    .grid-profile { display: grid; grid-template-columns: 1fr 1.5fr; gap: 24px; }
    .profile-card { text-align: center; padding: 32px; }
    .avatar-large { 
      width: 80px; height: 80px; background: #1A3A2A; color: white; 
      border-radius: 50%; display: grid; place-items: center; 
      font-size: 28px; font-weight: 700; margin: 0 auto; 
    }
    .info-row { display: flex; justify-content: space-between; margin-bottom: 12px; font-size: 14px; }
    .label { color: #64748b; }
    .value { font-weight: 600; color: #1e293b; }
    .security-status { 
      display: flex; gap: 12px; align-items: center; padding: 16px; 
      background: #f8fafc; border-radius: 12px; border: 1px solid #e2e8f0; 
    }
    .security-status.secure { border-left: 4px solid #22c55e; background: #f0fdf4; }
    @media (max-width: 768px) { .grid-profile { grid-template-columns: 1fr; } }
  `]
})
export class ProfilePageComponent implements OnInit {
  private auth = inject(AuthService);
  private st = inject(StorageService);

  fullName = '';
  email = '';
  userId = '';
  initials = '';
  isSecure = false;

  ngOnInit() {
    this.loadUserData();
  }

  loadUserData() {
    this.fullName = this.st.get('userFullName') || 'William Lopez Blanco';
    this.email = this.st.get('userEmail') || 'consultor@integrityfamily.com';
    this.userId = this.st.get('userId') || '---';
    this.isSecure = !!this.st.get('accessToken');
    
    this.initials = this.fullName.split(' ').map(n => n[0]).join('').toUpperCase().substring(0, 2);
  }

  logout() {
    if (confirm('¿Cerrar sesión y limpiar datos de navegación?')) {
      this.auth.logout();
      window.location.reload(); // Recarga para limpiar cualquier estado de memoria
    }
  }
}