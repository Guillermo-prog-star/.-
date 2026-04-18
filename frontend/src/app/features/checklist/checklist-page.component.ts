import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { HttpClient } from '@angular/common/http';
import { ApiService } from '../../core/services/api.service';
import { ChecklistItem, Plan } from '../../core/models/models';
import { FamilyStateService } from '../../core/services/family-state.service';

@Component({
  selector: 'app-checklist-page', 
  standalone: true, 
  imports: [CommonModule, FormsModule],
  templateUrl: './checklist-page.component.html',
  styleUrls: ['./checklist-page.component.css']
})
export class ChecklistPageComponent implements OnInit {
  private http = inject(HttpClient); 
  private api = inject(ApiService);
  private familyState = inject(FamilyStateService);

  items: ChecklistItem[] = []; 
  plans: Plan[] = [];
  selectedPlan: number | null = null; 
  newTitle = '';
  loading = false; 
  genLoading = false;
  
  get familyId() { return this.familyState.currentFamilyId(); }

  get done() { return this.items.filter(i => i.completed).length; }
  get pct()  { return this.items.length ? Math.round(this.done / this.items.length * 100) : 0; }
  
  ngOnInit() { 
    if (this.familyId) {
      this.load(); 
      this.loadPlans(); 
    }
  }
  
  load() {
    this.loading = true;
    this.http.get<any>(`${this.api.base}/checklist/family/${this.familyId}`)
      .subscribe({ next: ({ data }) => { this.items = data; this.loading = false; }, error: () => this.loading = false });
  }
  
  loadPlans() {
    this.http.get<any>(`${this.api.base}/plans/family/${this.familyId}`)
      .subscribe({ next: ({ data }) => { this.plans = data; if (data.length && !this.selectedPlan) this.selectedPlan = data[0].id; } });
  }
  
  fromPlan() {
    if (!this.selectedPlan) return;
    this.genLoading = true;
    this.http.post<any>(`${this.api.base}/checklist/generate-from-plan`, { planId: this.selectedPlan })
      .subscribe({ next: () => { this.genLoading = false; this.load(); }, error: () => this.genLoading = false });
  }
  
  addItem() {
    if (!this.newTitle.trim()) return;
    this.http.post<any>(`${this.api.base}/checklist/items`, { familyId: this.familyId, title: this.newTitle.trim() })
      .subscribe({ next: () => { this.newTitle = ''; this.load(); } });
  }
  
  toggle(id: number, completed: boolean) {
    this.http.put<any>(`${this.api.base}/checklist/items/${id}/complete`, { completed })
      .subscribe({ next: () => this.load() });
  }
}
