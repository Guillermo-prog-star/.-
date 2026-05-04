import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { HttpClient } from '@angular/common/http';
import { ApiService } from '../../core/services/api.service';
import { Member } from '../../core/models/models';
import { FamilyStateService } from '../../core/services/family-state.service';

@Component({
  selector: 'app-member-list-page',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './member-list-page.component.html',
  styleUrls: ['./member-list-page.component.css']
})
export class MemberListPageComponent implements OnInit {
  private http = inject(HttpClient);
  private api = inject(ApiService);
  private familyState = inject(FamilyStateService);
  private router = inject(Router);

  members: Member[] = [];
  fullName = ''; role = 'PADRE'; age = 30; aut = 70; resp = 70;
  error = ''; saving = false;

  get familyId(): number | null {
    const fromSignal = this.familyState.currentFamilyId();
    if (fromSignal) return fromSignal;
    const fromStorage = Number(localStorage.getItem('selectedFamilyId') ?? '0');
    return fromStorage > 0 ? fromStorage : null;
  }

  ngOnInit() {
    this.load();
  }

  load() {
    this.http.get<any>(`${this.api.base}/members/mine`)
      .subscribe({
        next: ({ data }) => {
          const list: Member[] = data ?? [];
          const seen = new Set<number>();
          this.members = list.filter(m => {
            if (seen.has(m.id)) return false;
            seen.add(m.id);
            return true;
          });
        },
        error: (e) => {
          this.error = 'No se pudieron cargar los miembros de tu familia.';
        }
      });
  }

  create() {
    console.log('[SDD-MEMBER] Iniciando creación:', this.fullName);
    
    if (!this.fullName || !this.fullName.trim()) {
      this.error = 'El nombre es obligatorio.';
      return;
    }

    this.saving = true;
    this.error = '';
    
    const payload = { 
      fullName: this.fullName, 
      roleType: this.role, 
      age: this.age,
      autonomyLevel: this.aut, 
      responsibilityLevel: this.resp 
    };

    console.log('[SDD-MEMBER] Payload:', payload);

    this.http.post<any>(`${this.api.base}/members/mine`, payload).subscribe({
      next: () => {
        this.fullName = ''; 
        this.saving = false;
        this.load();
      },
      error: (e) => {
        console.error('[SDD-MEMBER] Server Error:', e);
        this.saving = false;
        this.error = e?.error?.message ?? 'Error al registrar miembro.';
      }
    });
  }

  invite(id: number) {
    this.http.post<any>(`${this.api.base}/members/${id}/invite`, {})
      .subscribe({
        next: () => alert('¡Invitación enviada con éxito!'),
        error: (e) => alert(e?.error?.message ?? 'Error al enviar invitación.')
      });
  }

  remove(id: number) {
    if (!confirm('¿Eliminar este miembro?')) return;
    this.http.delete<any>(`${this.api.base}/members/${id}`)
      .subscribe({ next: () => this.load() });
  }

  goToEvaluation() {
    this.router.navigate(['/evaluations/start']);
  }
}
