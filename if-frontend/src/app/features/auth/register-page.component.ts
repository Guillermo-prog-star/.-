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
  confirmPassword = '';
  voucher = '';
  familyName = '';
  // FIX Bug #13: Backend RegisterFamilyRequest requires these @NotBlank fields
  municipio = '';
  countryCode = 'CO';
  departmentCode = '';
  
  termsAccepted = false;
  loading = false;
  error = '';

  setMode(m: 'VOUCHER' | 'NEW_FAMILY'): void {
    this.mode = m;
    this.error = '';
  }

  submit(): void {
    if (!this.termsAccepted) {
      this.error = 'Debes aceptar la Política de Privacidad y los Términos de Uso para continuar.';
      return;
    }

    if (!this.fullName || !this.email || !this.password) {
      this.error = 'Campos obligatorios: Nombre, Email y Password.';
      return;
    }

    if (this.mode === 'NEW_FAMILY' && !this.familyName) {
      this.error = 'Debes asignar un nombre a tu familia.';
      return;
    }

    if (this.mode === 'NEW_FAMILY' && (!this.municipio || !this.departmentCode)) {
      this.error = 'Debes completar el municipio y el departamento.';
      return;
    }

    if (this.mode === 'NEW_FAMILY' && this.password !== this.confirmPassword) {
      this.error = 'Las contraseñas no coinciden.';
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
        next: (res: any) => {
          this.loading = false;
          // Si obtuvo una familia, ir al dashboard; si no, a crear familia
          const familyId = res?.user?.familyId || res?.familyId;
          this.router.navigate(familyId ? ['/dashboard'] : ['/families/create']);
        },
        error: (e: any) => {
          this.loading = false;
          const status = e?.status;
          if (status === 0 || status === 502 || status === 503 || status === 504) {
            this.error = `El servidor de Integrity Family no responde o se encuentra en mantenimiento (HTTP ${status}). Por favor, inténtelo de nuevo en unos minutos.`;
          } else {
            this.error = e?.error?.message ?? 'Error en el servidor. Verifica el Voucher.';
          }
        }
      });
    } else {
      const payload: RegisterFamilyRequest = {
        familyName: this.familyName,
        fullName: this.fullName,
        email: this.email.trim().toLowerCase(),
        password: this.password,
        confirmPassword: this.confirmPassword,
        // FIX Bug #13: Include @NotBlank location fields required by backend
        municipio: this.municipio.trim(),
        countryCode: this.countryCode.trim() || 'CO',
        departmentCode: this.departmentCode.trim()
      };



      this.auth.registerFamily(payload).subscribe({
        next: (res: any) => {
          this.loading = false;
          // El nuevo admin de familia siempre tiene familyId asignado
          this.router.navigate(['/dashboard']);
        },
        error: (e: any) => {
          this.loading = false;
          const status = e?.status;
          if (status === 0 || status === 502 || status === 503 || status === 504) {
            this.error = `El servidor de Integrity Family no responde o se encuentra en mantenimiento (HTTP ${status}). Por favor, inténtelo de nuevo en unos minutos.`;
          } else {
            this.error = e?.error?.message ?? 'Error al crear familia. Verifica los datos e intenta de nuevo.';
          }
        }
      });


    }
  }
}