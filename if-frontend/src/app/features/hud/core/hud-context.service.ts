import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { ApiService } from '../../../core/services/api.service';
import { FamilyStateService } from '../../../core/services/family-state.service';
import { AuthService } from '../../../core/services/auth.service';
import { BehaviorSubject, of } from 'rxjs';
import { switchMap, catchError, tap } from 'rxjs/operators';

@Injectable({
  providedIn: 'root'
})
export class HudContextService {
  private readonly http = inject(HttpClient);
  private readonly api = inject(ApiService);
  private readonly familyState = inject(FamilyStateService);
  private readonly authService = inject(AuthService);

  // El tipo de HUD lo decide el rol con el que se inició sesión, no una preferencia
  // manual del usuario en pantalla: un familiar no debe poder auto-asignarse la
  // vista clínica, y un profesional no debería tener que elegirla cada vez.
  private readonly activeHudType = new BehaviorSubject<'FAMILY' | 'PROFESSIONAL'>(this.resolveHudTypeFromLogin());
  readonly activeHudType$ = this.activeHudType.asObservable();

  private readonly hudView = new BehaviorSubject<any>(null);
  readonly hudView$ = this.hudView.asObservable();

  constructor() {
    // Automatically trigger safe switchMap reload whenever context type changes
    this.activeHudType$.pipe(
      tap(() => this.hudView.next(null)), // Immediate cache clear on switch
      switchMap(type => {
        // El contrato IFRM-D Family Home (AdaptiveHudController) espera el UUID público
        // de la familia (homeId), no el id numérico interno usado por el resto del app.
        const homeId = this.familyState.getSelectedHomeId();
        if (!homeId) return of(null);

        const endpoint = type === 'PROFESSIONAL' ? 'professional' : 'family';
        return this.http.get<any>(`${this.api.base}/v1/families/${homeId}/hud/${endpoint}`).pipe(
          catchError(err => {
            console.error('Error fetching HUD:', err);
            return of(null);
          })
        );
      })
    ).subscribe(data => {
      this.hudView.next(data);
    });
  }

  getHudType(): 'FAMILY' | 'PROFESSIONAL' {
    return this.activeHudType.value;
  }

  loadHud(): void {
    // Re-resuelve el tipo por si el login cambió (p.ej. tras un logout/login sin recargar la página)
    this.activeHudType.next(this.resolveHudTypeFromLogin());
  }

  private resolveHudTypeFromLogin(): 'FAMILY' | 'PROFESSIONAL' {
    const user = this.authService.currentUserValue;
    const isProfessionalOrAdmin = !!user?.isProfessional || user?.role === 'ADMIN';
    return isProfessionalOrAdmin ? 'PROFESSIONAL' : 'FAMILY';
  }
}
