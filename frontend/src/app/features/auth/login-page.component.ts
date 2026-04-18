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
