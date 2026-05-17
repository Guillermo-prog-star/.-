import { Component, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { AuthService } from '../../core/services/auth.service';
@Component({
  selector: 'app-login-page', 
  standalone: true, 
  imports: [FormsModule, RouterLink],
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
        if (err.status === 423 || (err.error && err.error.message && err.error.message.includes('locked'))) {
          this.error = 'Tu cuenta ha sido bloqueada temporalmente por seguridad.';
        } else {
          this.error = 'Credenciales incorrectas.'; 
        }
      }
    });
  }
}
