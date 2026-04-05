import { Component, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { AuthService } from '../../core/services/auth.service';
@Component({
  selector: 'app-login-page', standalone: true, imports: [FormsModule, RouterLink],
  styles: [`
    .wrap{min-height:100vh;background:linear-gradient(135deg,#1A3A2A 0%,#2D6A4F 100%);display:grid;place-items:center;}
    .box{background:#fff;border-radius:20px;padding:40px;width:380px;box-shadow:0 20px 60px rgba(0,0,0,.2);}
    .logo{width:56px;height:56px;background:#1A3A2A;border-radius:14px;display:grid;place-items:center;color:#fff;font-weight:700;font-size:18px;margin:0 auto 16px;}
    h1{text-align:center;margin-bottom:6px;}
    .sub{text-align:center;color:var(--muted);font-size:14px;margin-bottom:28px;}
    .err{background:#FEE2E2;color:#991B1B;border-radius:8px;padding:10px 14px;font-size:13px;margin-bottom:16px;}
    .footer{text-align:center;margin-top:20px;font-size:13px;color:var(--muted);}
  `],
  template: `
    <div class="wrap">
      <div class="box">
        <div class="logo">IF</div>
        <h1>Integrity Family</h1>
        <p class="sub">Sistema de bienestar familiar con IA</p>
        <form (ngSubmit)="submit()">
          <div class="form-group"><label>Email</label><input [(ngModel)]="email" name="email" type="email" required/></div>
          <div class="form-group"><label>Contraseña</label><input [(ngModel)]="password" name="password" type="password" required/></div>
          @if (error) { <div class="err">{{ error }}</div> }
          <button type="submit" class="btn btn-primary" style="width:100%;justify-content:center;padding:12px;" [disabled]="loading">
            {{ loading ? 'Ingresando...' : 'Ingresar al sistema' }}
          </button>
        </form>
        <div class="footer">¿No tienes cuenta? <a routerLink="/register" style="color:var(--green);font-weight:600;">Registrarse</a></div>
        <div class="footer" style="margin-top:8px;font-size:11px;">Demo: admin&#64;integrityfamily.com / Admin123*</div>
      </div>
    </div>`
})
export class LoginPageComponent {
  private auth   = inject(AuthService);
  private router = inject(Router);
  email = 'william@integrity.family'; password = 'Admin123*';
  loading = false; error = '';
  submit() {
    this.loading = true; this.error = '';
    this.auth.login({ email: this.email.trim().toLowerCase(), password: this.password }).subscribe({
      next: () => { this.loading = false; this.router.navigate(['/families']); },
      error: () => { this.loading = false; this.error = 'Credenciales incorrectas.'; }
    });
  }
}
