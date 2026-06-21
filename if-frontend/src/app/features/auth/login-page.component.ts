import { Component, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink, ActivatedRoute } from '@angular/router';
import { AuthService } from '../../core/services/auth.service';
import { TransformationFlowService } from '../../core/services/transformation-flow.service';
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
  private route  = inject(ActivatedRoute);
  private flow   = inject(TransformationFlowService);
  email = ''; password = '';
  showPassword = false; rememberMe = true;
  loading = false; error = '';

  // ── Bifurcación psicológica ──────────────────────
  /**
   * true = no hay sesión activa en este dispositivo ahora mismo.
   * Se basa en si existe un token JWT guardado, no en un flag de visita,
   * para que cualquier persona nueva que llegue al dispositivo vea la opción
   * "Crear mi familia" aunque alguien más haya usado la app antes.
   */
  readonly isNewVisitor = (() => {
    try {
      const saved = localStorage.getItem('auth_user');
      if (!saved) return true;
      const user = JSON.parse(saved);
      return !user?.token; // hay sesión activa → usuario recurrente
    } catch {
      return true;
    }
  })();
  /** 'new' | 'returning' — fuerza una zona específica */
  mode: 'new' | 'returning' | null = null;
  
  togglePassword() {
    this.showPassword = !this.showPassword;
  }
  submit() {
    this.loading = true; this.error = '';
    this.auth.login({ email: this.email.trim().toLowerCase(), password: this.password }).subscribe({
      next: (res) => {
        this.loading = false;
        // El token JWT ya queda guardado por AuthService — no se necesita flag adicional
        const user = this.auth.user();
        const params = new URLSearchParams(window.location.search);
        const returnUrl = params.get('returnUrl') || this.route.snapshot.queryParams['returnUrl'];
        if (returnUrl && !returnUrl.includes('/auth/login')) {
          this.router.navigateByUrl(returnUrl);
        } else if (user && user.familyId) {
          this.flow.getRouteForNextStep(user.familyId).subscribe(route => {
            this.router.navigateByUrl(route);
          });
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
