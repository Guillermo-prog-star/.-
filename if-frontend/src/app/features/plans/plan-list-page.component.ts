import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { HttpClient } from '@angular/common/http';
import { ApiService } from '../../core/services/api.service';
import { Plan } from '../../core/models/models';
import { FamilyStateService } from '../../core/services/family-state.service';

@Component({
  selector: 'app-plan-list-page', 
  standalone: true, 
  imports: [CommonModule, RouterLink],
  templateUrl: './plan-list-page.component.html',
  styleUrls: ['./plan-list-page.component.css']
})
export class PlanListPageComponent implements OnInit {
  private http = inject(HttpClient); 
  private api = inject(ApiService);
  private familyState = inject(FamilyStateService);

  plans: Plan[] = []; 
  loading = false;
  isWaitingForPlan = false;
  
  get familyId() { return this.familyState.currentFamilyId(); }

  ngOnInit() { 
    if (this.familyId) {
      this.load(false); // Carga inicial
      
      this.isWaitingForPlan = true;
      let attempts = 0;
      
      const interval = setInterval(() => {
        if (this.plans.length > 0 || attempts > 20) {
          this.isWaitingForPlan = false;
          clearInterval(interval);
        } else {
          // Si llegamos al intento 6 (unos 9 segundos) y sigue vacío, activamos médico
          if (attempts === 6 && this.plans.length === 0) {
            console.warn("🛠️ Activando diagnóstico de emergencia...");
            this.http.get(`${this.api.base}/diagnostic/fix-plans/${this.familyId}`)
              .subscribe(() => this.load(true));
          }
          this.load(true);
          attempts++;
        }
      }, 1500);
    }
  }

  load(silent: boolean = false) {
    if (!silent) this.loading = true;
    this.http.get<any>(`${this.api.base}/plans/family/${this.familyId}`)
      .subscribe({ 
        next: ({ data }) => { 
          this.plans = data; 
          this.loading = false;
        }, 
        error: () => this.loading = false 
      });
  }

  toggle(taskId: number, completed: boolean) {
    this.http.put<any>(`${this.api.base}/plans/tasks/${taskId}/complete`, { completed })
      .subscribe({ next: () => this.load() });
  }

  completedCount(p: Plan) { return p.tasks.filter(t => t.completed).length; }
  planPct(p: Plan) { return p.tasks.length ? Math.round(this.completedCount(p) / p.tasks.length * 100) : 0; }
  
  // Cálculo de circunferencia para el progreso visual circular
  getDashOffset(p: Plan) {
    const pct = this.planPct(p);
    const circumference = 2 * Math.PI * 25; // r=25
    return circumference - (pct / 100) * circumference;
  }
}
