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
    // Verificar si el usuario ya tiene familia — si es así, auto-seleccionarla y redirigir
    this.http.get<ApiResponse<Family>>(`${this.api.base}/families/mine`).subscribe({
      next: ({ data }) => {
        if (data && data.id) {
          this.familyState.setFamilyId(data.id, data.name);
          localStorage.setItem('selectedFamilyCode', data.familyCode ?? '');
          this.router.navigate(['/families']);
        }
      },
      error: () => { /* Sin familia → el usuario puede crear una */ }
    });
  }
  
  submit() {
    if (!this.name.trim()) return;
    this.loading = true;
    this.error = '';
    this.http.post<any>(`${this.api.base}/families`, {
      name: this.name, description: this.desc,
      municipio: this.municipio, whatsapp: this.whatsapp, pin: this.pin
    }).subscribe({
      next: (res) => {
        // Puede venir como ApiResponse<Family>.data o directamente como Family
        const family: Family = res.data ?? res;
        this.familyState.setFamilyId(family.id, family.name);
        localStorage.setItem('selectedFamilyCode', family.familyCode ?? '');
        // Pequeño delay para que el usuario vea el éxito antes de saltar
        setTimeout(() => this.router.navigate(['/dashboard']), 500);
      },
      error: (e) => { 
        this.loading = false;
        const body = e?.error ?? {};
        const msg: string = body.message ?? 'Error al crear familia.';
        // Si el usuario ya tiene familia (409), auto-seleccionarla y redirigir
        if (e?.status === 409 || msg.toLowerCase().includes('ya posee')) {
          if (body.familyId) {
            this.familyState.setFamilyId(Number(body.familyId), body.familyName ?? '');
            localStorage.setItem('selectedFamilyCode', body.familyCode ?? '');
          }
          this.router.navigate(['/families']);
          return;
        }
        this.error = msg;
      }
    });
  }
}
