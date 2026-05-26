import { Component, OnInit, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { HttpClient } from '@angular/common/http';
import { ApiService } from '../../core/services/api.service';
import { ApiResponse } from '../../core/models/api-response.model';
import { Family } from '../../core/models/models';
import { FamilyStateService } from '../../core/services/family-state.service';

@Component({
  selector: 'app-family-create-page', 
  standalone: true, 
  imports: [FormsModule],
  templateUrl: './family-create-page.component.html',
  styleUrls: ['./family-create-page.component.css']
})
export class FamilyCreatePageComponent implements OnInit {
  private http = inject(HttpClient); 
  private api = inject(ApiService);
  private familyState = inject(FamilyStateService);
  router = inject(Router);
  
  name=''; desc=''; municipio=''; whatsapp=''; pin=''; loading=false; error='';
  
  // Validaciones básicas
  isWaValid() { return this.whatsapp.length >= 10; }
  isPinValid() { return this.pin.length === 4; }

  ngOnInit() {
    // [SDD Spec] Protocolo de Recuperación Automática:
    // Si el usuario ya tiene familia, restauramos contexto y saltamos el formulario.
    this.http.get<ApiResponse<Family>>(`${this.api.base}/families/mine`).subscribe({
      next: ({ data }) => {
        if (data && data.id) {
          this.familyState.setFamily(data);
          this.router.navigate(['/members']);
        }
      },
      error: () => { /* Sin familia → Flujo normal de creación */ }
    });
  }
  
  submit() {
    // 1. Blindaje de Validación Proactiva
    if (!this.name.trim() || !this.isWaValid() || !this.isPinValid()) {
      this.error = 'Datos incompletos. Revisa WhatsApp (10+) y PIN (4).';
      return;
    }

    this.loading = true;
    this.error = '';

    this.http.post<any>(`${this.api.base}/families`, {
      name: this.name, description: this.desc,
      municipio: this.municipio, whatsapp: this.whatsapp, pin: this.pin
    }).subscribe({
      next: (res) => {
        const family: Family = res.data ?? res;
        // Sincronización atómica del estado
        this.familyState.setFamily(family);
        console.log('✅ Familia creada y seleccionada:', family.name);
        
        // Redirigir a la elección del Guardián Familiar (onboarding emocional)
        this.router.navigate(['/guardian', family.id, 'election']).then(() => {
          console.log('🌱 Redirigiendo a elección del Guardián Familiar');
        });
      },
      error: (e) => { 
        this.loading = false;
        const status = e?.status;
        if (status === 0 || status === 502 || status === 503 || status === 504) {
          this.error = `El servidor de Integrity Family no responde o se encuentra en mantenimiento (HTTP ${status}). Por favor, inténtelo de nuevo en unos minutos.`;
          return;
        }
        const body = e?.error ?? {};

        // [SDD Spec] Protocolo de Colisión (409 Conflict):
        // Si el usuario intenta crear pero ya posee una, el sistema recupera la identidad del error y redirige.
        if (e?.status === 409 || (body.message && body.message.toLowerCase().includes('ya posee'))) {
          if (body.familyId) {
            this.familyState.setFamily({
              id: Number(body.familyId),
              name: body.familyName || 'Familia',
              familyCode: body.familyCode
            } as any);
            this.router.navigate(['/members']);
            return;
          }
          this.router.navigate(['/families']);
          return;
        }
        this.error = body.message || 'Falla de comunicación con el Nodo Central.';
      }
    });
  }
}
