import { Component, OnInit, inject, signal, computed, ChangeDetectionStrategy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { HttpClient } from '@angular/common/http';
import { AuthService } from '../../core/services/auth.service';
import { catchError, of } from 'rxjs';

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
  imports: [CommonModule],
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

        <!-- Actions -->
        <div class="glass-card">
          <p class="section-title">Acciones de Cuenta</p>
          <button class="btn-logout" (click)="logout()">
            🚪 Cerrar Sesión de Forma Segura
          </button>
        </div>

      </div>
    </div>
  `
})
export class ProfilePageComponent implements OnInit {
  private auth = inject(AuthService);
  private http = inject(HttpClient);

  readonly profile = signal<UserProfile | null>(null);
  readonly loading = signal(true);
  readonly hasBackendError = signal(false);

  // Data immediately available from AuthService signal (no HTTP needed)
  private readonly localUser = this.auth.user;

  ngOnInit(): void {
    this.loadProfile();
  }

  loadProfile(): void {
    this.loading.set(true);
    this.hasBackendError.set(false);

    this.http.get<UserProfile>('/api/auth/me').pipe(
      catchError(() => of(null))
    ).subscribe(data => {
      if (data) {
        this.profile.set(data);
      } else {
        // Backend unreachable: fallback to cached AuthService data
        const u = this.localUser();
        if (u) {
          this.profile.set({
            id: 0,
            email: u.email,
            fullName: u.fullName,
            role: u.role,
            familyId: u.familyId ?? null,
            familyName: u.familyName ?? null
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
    return 'Consultor Familiar';
  });

  readonly roleColorClass = computed(() => {
    const role = this.profile()?.role || this.localUser()?.role || '';
    return (role.includes('ADMIN') || role.includes('SENTINEL')) ? 'badge-admin' : 'badge-user';
  });

  logout(): void {
    if (confirm('¿Deseas cerrar tu sesión de forma segura?')) {
      this.auth.logout();
    }
  }
}
