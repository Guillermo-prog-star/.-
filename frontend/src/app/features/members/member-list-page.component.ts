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
  fn = ''; role = 'PADRE'; age = 30; aut = 70; resp = 70;
  error = ''; saving = false;

  get familyId(): number | null {
    const fromSignal = this.familyState.currentFamilyId();
    if (fromSignal) return fromSignal;
    const fromStorage = Number(localStorage.getItem('selectedFamilyId') ?? '0');
    return fromStorage > 0 ? fromStorage : null;
  }

  ngOnInit() {
    if (this.familyId) this.load();
  }

  load() {
    this.http.get<any>(`${this.api.base}/members/family/${this.familyId}`)
      .subscribe({
        next: ({ data }) => {
          const list: Member[] = data ?? [];
          const seen = new Set<number>();
          this.members = list.filter(m => {
            if (seen.has(m.id)) return false;
            seen.add(m.id);
            return true;
          });
        }
      });
  }

  create() {
    if (!this.fn.trim() || !this.familyId) return;
    this.saving = true;
    this.error = '';
    this.http.post<any>(
      `${this.api.base}/members/family/${this.familyId}`,
      { fullName: this.fn, roleType: this.role, age: this.age,
        autonomyLevel: this.aut, responsibilityLevel: this.resp }
    ).subscribe({
      next: () => {
        this.fn = ''; this.age = 30; this.aut = 70; this.resp = 70;
        this.saving = false;
        this.load();
      },
      error: (e) => {
        this.saving = false;
        this.error = e?.error?.message ?? 'Error al agregar miembro.';
      }
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
