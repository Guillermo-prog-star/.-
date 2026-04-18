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
  templateUrl: './register-page.component.html',
  styleUrls: ['./register-page.component.css']
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