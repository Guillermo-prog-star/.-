import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { HttpClient } from '@angular/common/http';

type PageState = 'FORM' | 'SENT' | 'ERROR';

@Component({
  selector: 'app-forgot-password',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink],
  template: `
<div class="fp-page">
  <div class="fp-card">

    <img src="assets/logo.svg" alt="Integrity Family" class="fp-logo" />
    <h1 class="fp-title">Recuperar contraseña</h1>

    <!-- ── Estado: Formulario ── -->
    <ng-container *ngIf="state === 'FORM'">
      <p class="fp-desc">
        Ingresa tu correo registrado y te enviaremos un enlace para restablecer tu contraseña.
      </p>

      <form (ngSubmit)="submit()" #f="ngForm">
        <div class="form-group">
          <label for="email">Correo electrónico</label>
          <input
            id="email"
            name="email"
            type="email"
            [(ngModel)]="email"
            placeholder="tu@correo.com"
            required
            email
            autocomplete="email"
          />
        </div>

        <div class="err-block" *ngIf="error">{{ error }}</div>

        <button
          type="submit"
          class="btn-primary"
          [disabled]="loading || !email">
          {{ loading ? 'Enviando...' : 'Enviar enlace de recuperación' }}
        </button>
      </form>
    </ng-container>

    <!-- ── Estado: Enviado con éxito ── -->
    <ng-container *ngIf="state === 'SENT'">
      <div class="sent-block">
        <div class="sent-icon">✅</div>
        <h2 class="sent-title">Revisa tu correo</h2>
        <p class="sent-desc">
          Si <strong>{{ email }}</strong> está registrado en Integrity Family,
          recibirás un enlace en los próximos minutos.
        </p>
        <p class="sent-note">
          El enlace es válido por <strong>30 minutos</strong>.
          Revisa también la carpeta de spam o correo no deseado.
        </p>
      </div>
    </ng-container>

    <!-- ── Estado: Error de red ── -->
    <ng-container *ngIf="state === 'ERROR'">
      <div class="error-block">
        <div class="error-icon">⚠️</div>
        <p>No pudimos procesar tu solicitud. Por favor intenta de nuevo.</p>
        <button class="btn-secondary" (click)="reset()">Volver a intentar</button>
      </div>
    </ng-container>

    <!-- Volver al login (siempre visible) -->
    <div class="back-link">
      <a routerLink="/auth/login">← Volver al inicio de sesión</a>
    </div>

  </div>
</div>
  `,
  styles: [`
    .fp-page {
      min-height: 100vh;
      background: radial-gradient(ellipse at 30% 20%, rgba(99,102,241,0.12) 0%, transparent 60%),
                  radial-gradient(ellipse at 80% 80%, rgba(139,92,246,0.08) 0%, transparent 60%),
                  #0a0a0c;
      display: flex; align-items: center; justify-content: center; padding: 2rem;
    }

    .fp-card {
      background: rgba(15,23,42,0.9);
      border: 1px solid rgba(99,102,241,0.2);
      border-radius: 20px;
      padding: 2.5rem 2rem;
      width: 100%; max-width: 420px;
      display: flex; flex-direction: column; align-items: center; gap: 1.25rem;
    }

    .fp-logo { width: 64px; height: 64px; object-fit: contain; border-radius: 12px; }
    .fp-title { color: #e2e8f0; font-size: 1.4rem; font-weight: 700; margin: 0; text-align: center; }
    .fp-desc { color: #94a3b8; font-size: 0.9rem; text-align: center; line-height: 1.5; margin: 0; }

    form { width: 100%; display: flex; flex-direction: column; gap: 1rem; }

    .form-group { display: flex; flex-direction: column; gap: 0.4rem; }
    .form-group label { color: #94a3b8; font-size: 0.85rem; }
    .form-group input {
      background: rgba(255,255,255,0.04);
      border: 1px solid rgba(255,255,255,0.1);
      border-radius: 10px; padding: 0.75rem 1rem;
      color: #e2e8f0; font-size: 0.95rem; outline: none; width: 100%;
      box-sizing: border-box; transition: border-color 0.2s;
    }
    .form-group input:focus { border-color: #6366f1; }

    .btn-primary {
      width: 100%; padding: 0.8rem;
      background: linear-gradient(135deg, #6366f1, #8b5cf6);
      color: white; border: none; border-radius: 10px;
      font-size: 0.95rem; font-weight: 600; cursor: pointer;
      transition: opacity 0.2s;
    }
    .btn-primary:disabled { opacity: 0.5; cursor: not-allowed; }

    .btn-secondary {
      padding: 0.6rem 1.5rem;
      background: rgba(99,102,241,0.15);
      border: 1px solid rgba(99,102,241,0.3);
      color: #a5b4fc; border-radius: 8px; cursor: pointer; font-size: 0.9rem;
    }

    .err-block {
      background: rgba(239,68,68,0.1);
      border: 1px solid rgba(239,68,68,0.3);
      color: #fca5a5; border-radius: 8px;
      padding: 0.6rem 0.9rem; font-size: 0.85rem;
    }

    /* Sent state */
    .sent-block { text-align: center; display: flex; flex-direction: column; align-items: center; gap: 0.75rem; }
    .sent-icon  { font-size: 2.5rem; }
    .sent-title { color: #34d399; font-size: 1.2rem; font-weight: 700; margin: 0; }
    .sent-desc  { color: #94a3b8; font-size: 0.9rem; line-height: 1.5; margin: 0; }
    .sent-desc strong { color: #c7d2fe; }
    .sent-note  { color: #64748b; font-size: 0.82rem; line-height: 1.4; margin: 0; }
    .sent-note strong { color: #94a3b8; }

    /* Error state */
    .error-block { text-align: center; display: flex; flex-direction: column; align-items: center; gap: 1rem; }
    .error-icon  { font-size: 2rem; }
    .error-block p { color: #94a3b8; font-size: 0.9rem; margin: 0; }

    .back-link { margin-top: 0.5rem; }
    .back-link a { color: #6366f1; font-size: 0.85rem; text-decoration: none; }
    .back-link a:hover { color: #a5b4fc; }
  `]
})
export class ForgotPasswordComponent {
  email    = '';
  loading  = false;
  error    = '';
  state: PageState = 'FORM';

  constructor(private http: HttpClient) {}

  submit(): void {
    if (!this.email.trim()) return;
    this.loading = true;
    this.error   = '';

    this.http.post('/api/auth/forgot-password', { email: this.email.trim() }).subscribe({
      next: () => {
        this.loading = false;
        this.state   = 'SENT';
      },
      error: (err) => {
        this.loading = false;
        // 202 Accepted is the success response — treat unexpected 4xx/5xx as error
        // but 400 with specific message should show inline
        const msg = err?.error?.message;
        if (msg) {
          this.error = msg;
        } else {
          this.state = 'ERROR';
        }
      }
    });
  }

  reset(): void {
    this.state = 'FORM';
    this.error = '';
  }
}
