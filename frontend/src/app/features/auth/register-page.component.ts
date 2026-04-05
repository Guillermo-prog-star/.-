import { Component, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { AuthService } from '../../core/services/auth.service';
import { RegisterRequest } from '../../core/models/auth.model';

/**
 * RegisterPageComponent: Punto de entrada para nuevos usuarios del Nodo Armenia.
 * Optimizado con Standalone Components y validación reactiva.
 * Resuelve errores TS2339 (auth.register) y TS7006 (parámetro de error).
 */
@Component({
  selector: 'app-register-page',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink],
  styles: [`
    .wrap { min-height: 100vh; background: linear-gradient(135deg, #1A3A2A, #2D6A4F); display: grid; place-items: center; font-family: 'Inter', sans-serif; }
    .box { background: #fff; border-radius: 20px; padding: 40px; width: 400px; box-shadow: 0 20px 60px rgba(0,0,0,.2); }
    .logo { width: 56px; height: 56px; background: #1A3A2A; border-radius: 14px; display: grid; place-items: center; color: #fff; font-weight: 700; font-size: 18px; margin: 0 auto 16px; }
    .err { background: #FEE2E2; color: #991B1B; border-radius: 8px; padding: 10px 14px; font-size: 13px; margin-bottom: 16px; border: 1px solid #FECACA; }
    .form-group { margin-bottom: 16px; }
    label { display: block; font-size: 13px; margin-bottom: 6px; color: #4B5563; font-weight: 500; }
    input { width: 100%; padding: 10px; border: 1px solid #D1D5DB; border-radius: 8px; outline: none; box-sizing: border-box; transition: all 0.2s; }
    input:focus { border-color: #2D6A4F; box-shadow: 0 0 0 3px rgba(45, 106, 79, 0.2); }
    .btn-submit { background: #1A3A2A; color: white; border: none; border-radius: 8px; cursor: pointer; font-weight: 600; transition: opacity 0.2s; }
    .btn-submit:disabled { opacity: 0.7; cursor: not-allowed; }
  `],
  template: `
    <div class="wrap">
      <div class="box">
        <div class="logo">IF</div>
        <h1 style="text-align:center; margin-bottom:24px; font-size: 24px; color: #111827;">Crear cuenta</h1>
        
        <form (ngSubmit)="submit()">
          <div class="form-group">
            <label>Nombre completo</label>
            <input [(ngModel)]="fullName" name="fn" required placeholder="Ej: William López"/>
          </div>
          
          <div class="form-group">
            <label>Email</label>
            <input [(ngModel)]="email" name="em" type="email" required placeholder="william@ejemplo.com"/>
          </div>
          
          <div class="form-group">
            <label>Contraseña (mín. 8 caracteres)</label>
            <input [(ngModel)]="password" name="pw" type="password" required minlength="8" placeholder="********"/>
          </div>

          <div *ngIf="error" class="err">{{ error }}</div>

          <button type="submit" class="btn-submit" style="width:100%; padding:12px;" [disabled]="loading">
            {{ loading ? 'Procesando...' : 'Registrar Nodo' }}
          </button>
        </form>

        <p style="text-align:center; margin-top:16px; font-size:13px; color: #6B7280;">
          ¿Ya tienes cuenta? <a routerLink="/login" style="color:#2D6A4F; font-weight:600; text-decoration:none;">Ingresar</a>
        </p>
      </div>
    </div>
  `
})
export class RegisterPageComponent {
  private auth = inject(AuthService);
  private router = inject(Router);

  fullName = '';
  email = '';
  password = '';
  loading = false;
  error = '';

  /**
   * Envía los datos al Backend y gestiona la respuesta única.
   */
  submit(): void {
    if (!this.fullName || !this.email || !this.password) {
      this.error = 'Todos los campos son obligatorios.';
      return;
    }

    this.loading = true;
    this.error = '';

    const payload: RegisterRequest = {
      fullName: this.fullName,
      email: this.email.trim().toLowerCase(),
      password: this.password
    };

    this.auth.register(payload).subscribe({
      next: () => {
        this.loading = false;
        this.router.navigate(['/dashboard']); 
      },
      error: (e: any) => { // Tipado explícito para evitar error TS7006 en la build
        this.loading = false;
        this.error = e?.error?.message ?? 'Error en el servidor. Verifica el puerto 8080.';
        console.error('Error de registro:', e);
      }
    });
  }
}