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

  items: any[] = []; 
  resolvedEvidences: any[] = []; // Archivo permanente de evidencias (Victorias de todos los hitos)
  loading = false; 
  
  get familyId() { return this.familyState.getSelectedFamilyId(); }

  get done() { return this.items.filter(i => i.completed).length; }
  get pct()  { return this.items.length ? Math.round(this.done / this.items.length * 100) : 0; }

  // Agrupación por dimensiones para estética premium
  get itemsByDimension() {
    const groups: { [key: string]: any[] } = { 'emociones': [], 'comunicacion': [], 'habitos': [], 'tiempos': [], 'general': [] };
    this.items.forEach(i => {
      const dim = i.dimension || 'general';
      if (groups[dim]) groups[dim].push(i);
      else groups['general'].push(i);
    });
    return Object.entries(groups).filter(([_, items]) => items.length > 0);
  }
  
  ngOnInit() { 
    if (this.familyId) {
      this.load(); 
    }
  }
  
  load() {
    this.loading = true;
    
    // Cargar hábitos/checklist
    this.http.get<any>(`${this.api.base}/checklist/family/${this.familyId}`)
      .subscribe({ 
        next: ({ data }) => { 
          this.items = data || []; 
          this.loading = false; 
        }, 
        error: () => {
          this.loading = false;
        } 
      });

    // Cargar evidencias históricas acumuladas de la bitácora (estado RESOLVED)
    this.http.get<any[]>(`${this.api.base}/family-logbook/family/${this.familyId}/status/RESOLVED`)
      .subscribe({
        next: (data) => {
          this.resolvedEvidences = data || [];
        },
        error: () => {}
      });
  }
  
  toggle(id: number, current: boolean) {
    if (current) return; // Por ahora solo marcamos como completado para rigor pedagógico
    
    // Simular quién completa (en versión pro vendría del perfil)
    const who = 'Núcleo Familiar';
    
    this.http.put<any>(`${this.api.base}/checklist/${id}/complete`, { completedBy: who })
      .subscribe({ next: () => this.load() });
  }
}
