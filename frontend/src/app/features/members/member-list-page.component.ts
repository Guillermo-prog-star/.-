import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { HttpClient } from '@angular/common/http';
import { ApiService } from '../../core/services/api.service';
import { Member } from '../../core/models/models';
@Component({
  selector: 'app-member-list-page', standalone: true, imports: [CommonModule, FormsModule],
  template: `
    <div class="page-header"><div><h1>Miembros</h1><p>Autonomía individual · Responsabilidad compartida</p></div></div>
    <div class="card" style="margin-bottom:20px;">
      <h3 style="margin-bottom:16px;">Agregar miembro</h3>
      <form (ngSubmit)="create()" style="display:grid;grid-template-columns:repeat(3,1fr);gap:12px;">
        <div class="form-group" style="margin:0"><label>Nombre</label><input [(ngModel)]="fn" name="fn" required/></div>
        <div class="form-group" style="margin:0"><label>Rol</label>
          <select [(ngModel)]="role" name="role">
            <option>PADRE</option><option>MADRE</option><option>HIJO</option><option>HIJA</option><option>ABUELO</option><option>ABUELA</option><option>OTRO</option>
          </select>
        </div>
        <div class="form-group" style="margin:0"><label>Edad</label><input [(ngModel)]="age" name="age" type="number" min="0" max="120"/></div>
        <div class="form-group" style="margin:0"><label>Autonomía (0-100)</label><input [(ngModel)]="aut" name="aut" type="number" min="0" max="100"/></div>
        <div class="form-group" style="margin:0"><label>Responsabilidad (0-100)</label><input [(ngModel)]="resp" name="resp" type="number" min="0" max="100"/></div>
        <div style="display:flex;align-items:flex-end;"><button type="submit" class="btn btn-primary" style="width:100%;justify-content:center;">+ Agregar</button></div>
      </form>
    </div>
    <div class="grid-3">
      @for (m of members; track m.id) {
        <div class="card">
          <div style="display:flex;align-items:center;gap:12px;margin-bottom:16px;">
            <div style="width:48px;height:48px;border-radius:50%;background:var(--greenLt);display:grid;place-items:center;font-weight:700;color:var(--green);font-size:20px;">{{ m.fullName[0] }}</div>
            <div><div style="font-weight:600;">{{ m.fullName }}</div><span class="badge badge-gray">{{ m.roleType }}</span></div>
          </div>
          <div style="font-size:13px;color:var(--muted);margin-bottom:12px;">Edad: {{ m.age }}</div>
          <div style="margin-bottom:8px;">
            <div style="display:flex;justify-content:space-between;font-size:12px;margin-bottom:4px;"><span>Autonomía</span><strong>{{ m.autonomyLevel }}</strong></div>
            <div class="progress-track"><div class="progress-fill" [style.width]="m.autonomyLevel+'%'"></div></div>
          </div>
          <div>
            <div style="display:flex;justify-content:space-between;font-size:12px;margin-bottom:4px;"><span>Responsabilidad</span><strong>{{ m.responsibilityLevel }}</strong></div>
            <div class="progress-track"><div class="progress-fill" [style.width]="m.responsibilityLevel+'%'" style="background:var(--amber);"></div></div>
          </div>
        </div>
      }
    </div>`
})
export class MemberListPageComponent implements OnInit {
  private http = inject(HttpClient); private api = inject(ApiService);
  members: Member[] = [];
  fn=''; role='PADRE'; age=30; aut=70; resp=70;
  familyId = Number(localStorage.getItem('selectedFamilyId') ?? 1);
  ngOnInit() { this.load(); }
  load() {
    this.http.get<any>(`${this.api.base}/members/family/${this.familyId}`)
      .subscribe({ next: ({ data }) => this.members = data });
  }
  create() {
    this.http.post<any>(`${this.api.base}/members`,
      { familyId: this.familyId, fullName: this.fn, roleType: this.role,
        age: this.age, autonomyLevel: this.aut, responsibilityLevel: this.resp })
      .subscribe({ next: () => { this.fn=''; this.load(); } });
  }
}
