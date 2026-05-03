import { Component, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { AuthService } from '../../core/services/auth.service';
import { RegisterRequest, RegisterFamilyRequest } from '../../core/models/auth.model';


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

  mode: 'VOUCHER' | 'NEW_FAMILY' = 'VOUCHER';
  fullName = '';
  email = '';
  password = '';
  voucher = ''; 
  familyName = '';
  
  loading = false;
  error = '';

  setMode(m: 'VOUCHER' | 'NEW_FAMILY'): void {
    this.mode = m;
    this.error = '';
  }

  submit(): void {
    if (!this.fullName || !this.email || !this.password) {
      this.error = 'Campos obligatorios: Nombre, Email y Password.';
      return;
    }

    if (this.mode === 'NEW_FAMILY' && !this.familyName) {
      this.error = 'Debes asignar un nombre a tu familia.';
      return;
    }

    this.loading = true;
    this.error = '';

    if (this.mode === 'VOUCHER') {
      const payload: RegisterRequest = {
        fullName: this.fullName,
        email: this.email.trim().toLowerCase(),
        password: this.password,
        voucher: this.voucher.trim().toUpperCase()
      };

      this.auth.register(payload).subscribe({
        next: () => {
          this.loading = false;
          this.router.navigate(['/members']); 
        },
        error: (e: any) => {
          this.loading = false;
          this.error = e?.error?.message ?? 'Error en el servidor. Verifica el Voucher.';
        }
      });
    } else {
      const payload: RegisterFamilyRequest = {
        familyName: this.familyName,
        fullName: this.fullName,
        email: this.email.trim().toLowerCase(),
        password: this.password
      };


      this.auth.registerFamily(payload).subscribe({
        next: () => {
          this.loading = false;
          this.router.navigate(['/members']);
        },
        error: (e: any) => {
          this.loading = false;
          this.error = e?.error?.message ?? 'Error al crear familia.';
        }
      });


    }
  }
}