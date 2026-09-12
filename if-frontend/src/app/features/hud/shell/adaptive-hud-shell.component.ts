import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';
import { HudContextService } from '../core/hud-context.service';
import { FamilyStateService } from '../../../core/services/family-state.service';
import { AuthService } from '../../../core/services/auth.service';

@Component({
  selector: 'app-adaptive-hud-shell',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './adaptive-hud-shell.component.html',
  styleUrl: './adaptive-hud-shell.component.css'
})
export class AdaptiveHudShellComponent implements OnInit {
  readonly hudService = inject(HudContextService);
  private readonly familyState = inject(FamilyStateService);
  private readonly router = inject(Router);
  private readonly authService = inject(AuthService);

  familyName = '';
  viewData: any = null;

  ngOnInit(): void {
    // El panel de estado familiar (con datos reales) ahora vive dentro de
    // /family-home (ver ADR-002) — esta pantalla independiente solo sigue
    // siendo el destino real para el rol profesional.
    const user = this.authService.currentUserValue;
    const isProfessionalOrAdmin = !!user?.isProfessional || user?.role === 'ADMIN';
    if (!isProfessionalOrAdmin) {
      this.router.navigate(['/family-home']);
      return;
    }

    this.familyName = this.familyState.currentFamilyName() || 'Familia';

    this.hudService.hudView$.subscribe(view => {
      this.viewData = view;
    });

    this.hudService.loadHud();
  }

  // Los ítems del menú del HUD llegan del backend como texto plano (sin target),
  // reutilizan las páginas completas que ya existen en la app en vez de duplicar
  // contenido dentro del shell — "Resumen"/"Hoy" se quedan aquí porque son este
  // mismo HUD.
  private static readonly SECTION_ROUTES: Record<string, string> = {
    // Profesional (ProfessionalHudProjectionResolver)
    'Evaluación': '/evaluations/history',
    'Intervención': '/plans',
    'Trayectoria': '/trajectory',
    'Registro': '/logbook',
    // Familiar (FamilyHudProjectionResolver)
    'Crecemos': '/transformation/route',
    'Recordamos': '/family-timeline',
    'Somos': '/family-dna',
    'Conversamos': '/chat',
  };

  onNavigate(route: string): void {
    const target = AdaptiveHudShellComponent.SECTION_ROUTES[route];
    if (target) {
      this.router.navigate([target]);
    }
  }
}
