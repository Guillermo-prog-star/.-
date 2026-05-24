import { Component, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterOutlet, RouterLink, RouterLinkActive } from '@angular/router';
import { AuthService } from '../../core/services/auth.service';
// [SDD] CORRECCIÓN CRÍTICA: Importación del servicio faltante
import { SentinelCoreService } from '../../core/services/sentinel-core.service';

@Component({
  selector: 'app-shell',
  standalone: true,
  imports: [CommonModule, RouterOutlet, RouterLink, RouterLinkActive],
  template: `
    `,
  styles: [`
    /* ... tus estilos actuales se mantienen igual ... */
  `]
})
export class ShellComponent {
  // [SDD] Tipado explícito para garantizar éxito en compilación AOT
  public auth: AuthService = inject(AuthService);
  public sentinel: SentinelCoreService = inject(SentinelCoreService);

  onLogout(): void {
    // Referencia al contexto local "Nodo Central" según tus principios
    if (confirm('¿Deseas cerrar la sesión en el Nodo Central?')) {
      this.auth.logout();
    }
  }
}