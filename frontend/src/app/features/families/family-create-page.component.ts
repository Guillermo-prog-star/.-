import { Component, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { HttpClient } from '@angular/common/http';
import { ApiService } from '../../core/services/api.service';
import { ApiResponse } from '../../core/models/api-response.model';
import { Family } from '../../core/models/models';
@Component({
  selector: 'app-family-create-page', standalone: true, imports: [FormsModule],
  template: `
    <div class="page-header"><div><h1>Crear familia</h1><p>Registra un nuevo núcleo familiar</p></div></div>
    <div class="card" style="max-width:540px;">
      <form (ngSubmit)="submit()">
        <div class="form-group"><label>Nombre de la familia *</label><input [(ngModel)]="name" name="name" required/></div>
        <div class="form-group"><label>Descripción</label><textarea [(ngModel)]="desc" name="desc"></textarea></div>
        <div class="form-group"><label>Municipio</label><input [(ngModel)]="municipio" name="mun"/></div>
        <div class="form-group"><label>WhatsApp</label><input [(ngModel)]="whatsapp" name="wa" placeholder="+573001234567"/></div>
        <div class="form-group"><label>PIN de acceso (4 dígitos)</label><input [(ngModel)]="pin" name="pin" maxlength="4" placeholder="1234"/></div>
        @if (error) { <div style="color:var(--red);font-size:13px;margin-bottom:12px;">{{ error }}</div> }
        <div style="display:flex;gap:10px;">
          <button type="submit" class="btn btn-primary" [disabled]="loading">{{ loading ? 'Guardando...' : 'Crear familia' }}</button>
          <button type="button" class="btn" (click)="router.navigate(['/families'])">Cancelar</button>
        </div>
      </form>
    </div>`
})
export class FamilyCreatePageComponent {
  private http = inject(HttpClient); private api = inject(ApiService);
  router = inject(Router);
  name=''; desc=''; municipio=''; whatsapp=''; pin=''; loading=false; error='';
  submit() {
    if (!this.name.trim()) return;
    this.loading = true;
    this.http.post<ApiResponse<Family>>(`${this.api.base}/families`, {
      name: this.name, description: this.desc,
      municipio: this.municipio, whatsapp: this.whatsapp, pin: this.pin
    }).subscribe({
      next: ({ data }) => {
        localStorage.setItem('selectedFamilyId', String(data.id));
        localStorage.setItem('selectedFamilyName', data.name);
        localStorage.setItem('selectedFamilyCode', data.familyCode ?? '');
        this.router.navigate(['/dashboard']);
      },
      error: (e) => { this.loading = false; this.error = e?.error?.message ?? 'Error al crear familia.'; }
    });
  }
}
