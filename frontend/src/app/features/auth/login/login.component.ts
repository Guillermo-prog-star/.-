import { Component, inject } from '@angular/core';
import { NonNullableFormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { AuthService } from '../../../core/services/auth.service';
import { StorageService } from '../../../core/services/storage.service';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  // 👈 CAMBIO: Usamos 'template' (inline) para evitar el error de archivo faltante
  template: `
    <div class="d-flex justify-content-center align-items-center vh-100 bg-light">
      <div class="card shadow-lg" style="width: 400px;">
        <div class="card-body p-5">
          <h3 class="text-center mb-4 text-primary">Integrity Family</h3>
          <form [formGroup]="loginForm" (ngSubmit)="onSubmit()">
            <div class="mb-3">
              <label class="form-label">Email</label>
              <input type="email" formControlName="email" class="form-control">
            </div>
            <div class="mb-3">
              <label class="form-label">Password</label>
              <input type="password" formControlName="password" class="form-control">
            </div>
            <button type="submit" [disabled]="loginForm.invalid || loading" class="btn btn-primary w-100">
              {{ loading ? 'Autenticando...' : 'Ingresar' }}
            </button>
            <div *ngIf="error" class="alert alert-danger mt-3 small">{{ error }}</div>
          </form>
        </div>
      </div>
    </div>
  `
})
export class LoginComponent {
  // 👈 Usamos NonNullableFormBuilder para que los valores nunca sean null
  private fb = inject(NonNullableFormBuilder);
  private authService = inject(AuthService);
  private storage = inject(StorageService);
  private router = inject(Router);

  loading = false;
  error: string | null = null;

  loginForm = this.fb.group({
    email: ['', [Validators.required, Validators.email]],
    password: ['', [Validators.required, Validators.minLength(6)]]
  });

  onSubmit() {
    if (this.loginForm.invalid) return;

    this.loading = true;
    this.error = null;

    // 👈 LA SOLUCIÓN DEFINITIVA: getRawValue() con NonNullable garantiza strings
    const credentials = this.loginForm.getRawValue();

    this.authService.login(credentials).subscribe({
      next: (response) => {
        this.storage.set('accessToken', response.token);
        this.router.navigate(['/admin/stats']);
      },
      error: (err) => {
        this.error = 'Error de autenticación. Verifica tus credenciales.';
        this.loading = false;
      }
    });
  }
}