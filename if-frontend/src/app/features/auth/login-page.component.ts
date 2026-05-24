import { Component, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { AuthService } from '../../core/services/auth.service';
import { NarrativeCompanionComponent } from '../../shared/components/narrative-companion.component';
@Component({
  selector: 'app-login-page', 
  standalone: true, 
  imports: [FormsModule, RouterLink, NarrativeCompanionComponent],
  templateUrl: './login-page.component.html',
  styleUrls: ['./login-page.component.css']
})
export class LoginPageComponent {
  private auth   = inject(AuthService);
  private router = inject(Router);
  email = ''; password = '';
  showPassword = false; rememberMe = true;
  loading = false; error = '';
  
  togglePassword() {
    this.showPassword = !this.showPassword;
  }
  submit() {
    this.loading = true; this.error = '';
    this.auth.login({ email: this.email.trim().toLowerCase(), password: this.password }).subscribe({
      next: (res) => { 
        this.loading = false; 
        const user = this.auth.user();
        if (user && user.familyId) {
          this.router.navigate(['/dashboard']); 
        } else {
          this.router.navigate(['/families/create']);
        }
      },
      error: (err) => { 
        this.loading = false; 
        const status = err?.status;
        const errMsg = err?.error?.message ?? '';
        
        if (status === 0 || status === 502 || status === 503 || status === 504) {
          this.error = `El servidor de Integrity Family no responde o se encuentra en mantenimiento (HTTP ${status}). Por favor, inténtelo de nuevo en unos minutos.`;
        } else if (status === 423 || errMsg.includes('locked')) {
          this.error = 'Tu cuenta ha sido bloqueada temporalmente por seguridad.';
        } else if (status === 401 || status === 400) {
          this.error = 'Credenciales incorrectas.';
        } else {
          this.error = `Error temporal en el servicio (Código: ${status}). Por favor, intente de nuevo.`;
        }
      }
    });
  }
}
