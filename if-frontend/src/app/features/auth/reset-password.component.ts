import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterLink, ActivatedRoute, Router } from '@angular/router';
import { HttpClient } from '@angular/common/http';

type PageState = 'LOADING' | 'FORM' | 'SUCCESS' | 'INVALID_TOKEN' | 'ERROR';

@Component({
  selector: 'app-reset-password',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink],
  template: `
<div class="rp-page">
  <div class="rp-card">

    <img src="assets/logo.svg" alt="Integrity Family" class="rp-logo" />
    <h1 class="rp-title">Nueva contraseña</h1>

    <!-- Cargando token -->
    @if (state === 'LOADING') {
      <div class="loading-state">
        <div class="spinner"></div>
        <p>Verificando enlace...</p>
      </div>
    }

    <!-- Token inválido / no presente -->
    @if (state === 'INVALID_TOKEN') {
      <div class="invalid-state">
        <div class="state-icon">⛔</div>
        <h2>Enlace inválido o expirado</h2>
        <p>
          Este enlace de recuperación no es válido o ya fue utilizado.
          Los enlaces expiran en <strong>30 minutos</strong>.
        </p>
        <a routerLink="/auth/forgot-password" class="btn-primary-link">
          Solicitar nuevo enlace
        </a>
      </div>
    }

    <!-- Formulario de nueva contraseña -->
    @if (state === 'FORM') {
      <p class="rp-desc">Elige una contraseña segura para tu cuenta.</p>

      <form (ngSubmit)="submit()" #f="ngForm">

        <div class="form-group">
          <label for="newPassword">Nueva contraseña</label>
          <div class="input-wrapper">
            <input
              id="newPassword"
              name="newPassword"
              [type]="showPwd ? 'text' : 'password'"
              [(ngModel)]="newPassword"
              placeholder="Mínimo 8 caracteres"
              required
              autocomplete="new-password"
            />
            <button type="button" class="toggle-btn" (click)="showPwd = !showPwd">
              {{ showPwd ? 'Ocultar' : 'Ver' }}
            </button>
          </div>
          <span class="hint" [class.ok]="isPasswordValid()">
            ✓ Al menos 8 caracteres, una mayúscula, una minúscula y un número
          </span>
        </div>

        <div class="form-group">
          <label for="confirmPassword">Confirmar contraseña</label>
          <input
            id="confirmPassword"
            name="confirmPassword"
            type="password"
            [(ngModel)]="confirmPassword"
            placeholder="Repite la contraseña"
            required
            autocomplete="new-password"
          />
          @if (confirmPassword && !passwordsMatch()) {
            <span class="hint error">Las contraseñas no coinciden</span>
          }
        </div>

        @if (error) {
          <div class="err-block">{{ error }}</div>
        }

        <button
          type="submit"
          class="btn-primary"
          [disabled]="loading || !isPasswordValid() || !passwordsMatch()">
          {{ loading ? 'Guardando...' : '🔒 Establecer nueva contraseña' }}
        </button>

      </form>
    }

    <!-- Éxito -->
    @if (state === 'SUCCESS') {
      <div class="success-state">
        <div class="state-icon">🎉</div>
        <h2>¡Contraseña actualizada!</h2>
        <p>Tu contraseña fue cambiada exitosamente. Ya puedes iniciar sesión.</p>
        <a routerLink="/auth/login" class="btn-primary-link">Ir al inicio de sesión</a>
      </div>
    }

    <!-- Error de red -->
    @if (state === 'ERROR') {
      <div class="error-state">
        <div class="state-icon">⚠️</div>
        <p>Ocurrió un error al procesar tu solicitud. Por favor intenta de nuevo.</p>
        <button class="btn-secondary" (click)="state = 'FORM'">Volver a intentar</button>
      </div>
    }

    @if (state !== 'SUCCESS') {
      <div class="back-link">
        <a routerLink="/auth/login">← Volver al inicio de sesión</a>
      </div>
    }

  </div>
</div>
  `,
  styles: [`
    .rp-page {
      min-height: 100vh;
      background: radial-gradient(ellipse at 30% 20%, rgba(99,102,241,0.12) 0%, transparent 60%),
                  radial-gradient(ellipse at 80% 80%, rgba(139,92,246,0.08) 0%, transparent 60%),
                  #0a0a0c;
      display: flex; align-items: center; justify-content: center; padding: 2rem;
    }

    .rp-card {
      background: rgba(15,23,42,0.9);
      border: 1px solid rgba(99,102,241,0.2);
      border-radius: 20px;
      padding: 2.5rem 2rem;
      width: 100%; max-width: 420px;
      display: flex; flex-direction: column; align-items: center; gap: 1.25rem;
    }

    .rp-logo  { width: 64px; height: 64px; object-fit: contain; border-radius: 12px; }
    .rp-title { color: #e2e8f0; font-size: 1.4rem; font-weight: 700; margin: 0; text-align: center; }
    .rp-desc  { color: #94a3b8; font-size: 0.9rem; text-align: center; margin: 0; }

    /* Loading */
    .loading-state { display: flex; flex-direction: column; align-items: center; gap: 1rem; color: #64748b; font-size: 0.9rem; }
    .spinner {
      width: 32px; height: 32px; border: 3px solid rgba(99,102,241,0.2);
      border-top-color: #6366f1; border-radius: 50%;
      animation: spin 0.8s linear infinite;
    }
    @keyframes spin { to { transform: rotate(360deg); } }

    /* States */
    .invalid-state, .success-state, .error-state {
      text-align: center; display: flex; flex-direction: column; align-items: center; gap: 0.75rem;
    }
    .state-icon { font-size: 2.5rem; }
    .invalid-state h2, .success-state h2 { font-size: 1.15rem; font-weight: 700; margin: 0; }
    .invalid-state h2 { color: #f87171; }
    .success-state h2 { color: #34d399; }
    .invalid-state p, .success-state p, .error-state p {
      color: #94a3b8; font-size: 0.9rem; margin: 0; line-height: 1.5;
    }
    .invalid-state p strong { color: #e2e8f0; }

    /* Form */
    form { width: 100%; display: flex; flex-direction: column; gap: 1rem; }
    .form-group { display: flex; flex-direction: column; gap: 0.4rem; }
    .form-group label { color: #94a3b8; font-size: 0.85rem; }

    .input-wrapper { position: relative; display: flex; }
    .input-wrapper input { flex: 1; padding-right: 4.5rem; }
    .toggle-btn {
      position: absolute; right: 0.75rem; top: 50%; transform: translateY(-50%);
      background: none; border: none; color: #6366f1; font-size: 0.8rem; cursor: pointer;
    }

    .form-group input {
      background: rgba(255,255,255,0.04);
      border: 1px solid rgba(255,255,255,0.1);
      border-radius: 10px; padding: 0.75rem 1rem;
      color: #e2e8f0; font-size: 0.95rem; outline: none; width: 100%;
      box-sizing: border-box; transition: border-color 0.2s;
    }
    .form-group input:focus { border-color: #6366f1; }

    .hint { font-size: 0.75rem; color: #475569; transition: color 0.2s; }
    .hint.ok  { color: #34d399; }
    .hint.error { color: #f87171; }

    .btn-primary {
      width: 100%; padding: 0.8rem;
      background: linear-gradient(135deg, #6366f1, #8b5cf6);
      color: white; border: none; border-radius: 10px;
      font-size: 0.95rem; font-weight: 600; cursor: pointer; transition: opacity 0.2s;
    }
    .btn-primary:disabled { opacity: 0.5; cursor: not-allowed; }

    .btn-primary-link {
      display: inline-block; padding: 0.7rem 1.75rem;
      background: linear-gradient(135deg, #6366f1, #8b5cf6);
      color: white; border-radius: 10px;
      font-size: 0.9rem; font-weight: 600; text-decoration: none; margin-top: 0.5rem;
    }

    .btn-secondary {
      padding: 0.6rem 1.5rem;
      background: rgba(99,102,241,0.15);
      border: 1px solid rgba(99,102,241,0.3);
      color: #a5b4fc; border-radius: 8px; cursor: pointer; font-size: 0.9rem;
    }

    .err-block {
      background: rgba(239,68,68,0.1); border: 1px solid rgba(239,68,68,0.3);
      color: #fca5a5; border-radius: 8px; padding: 0.6rem 0.9rem; font-size: 0.85rem;
    }

    .back-link { margin-top: 0.25rem; }
    .back-link a { color: #6366f1; font-size: 0.85rem; text-decoration: none; }
    .back-link a:hover { color: #a5b4fc; }
  `]
})
export class ResetPasswordComponent implements OnInit {
  token           = '';
  newPassword     = '';
  confirmPassword = '';
  showPwd         = false;
  loading         = false;
  error           = '';
  state: PageState = 'LOADING';

  // Regex igual al ResetPasswordRequest del backend
  private readonly PWD_PATTERN = /^(?=.*[a-z])(?=.*[A-Z])(?=.*\d).{8,}$/;

  constructor(
    private http: HttpClient,
    private route: ActivatedRoute,
    private router: Router
  ) {}

  ngOnInit(): void {
    const token = this.route.snapshot.queryParamMap.get('token');
    if (!token || token.trim().length === 0) {
      this.state = 'INVALID_TOKEN';
      return;
    }
    this.token = token.trim();
    this.state = 'FORM';
  }

  isPasswordValid(): boolean {
    return this.PWD_PATTERN.test(this.newPassword);
  }

  passwordsMatch(): boolean {
    return this.newPassword === this.confirmPassword;
  }

  submit(): void {
    if (!this.isPasswordValid() || !this.passwordsMatch()) return;
    this.loading = true;
    this.error   = '';

    this.http.post('/api/auth/reset-password', {
      token: this.token,
      newPassword: this.newPassword
    }).subscribe({
      next: () => {
        this.loading = false;
        this.state   = 'SUCCESS';
        // Redirigir automáticamente al login después de 3 segundos
        setTimeout(() => this.router.navigate(['/auth/login']), 3000);
      },
      error: (err) => {
        this.loading = false;
        const msg = err?.error?.message;
        if (msg) {
          this.error = msg;
          // Token expirado/inválido → cambiar estado
          if (err.status === 400) {
            this.state = 'INVALID_TOKEN';
          }
        } else {
          this.state = 'ERROR';
        }
      }
    });
  }
}
