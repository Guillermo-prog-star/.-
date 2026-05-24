import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, RouterLink } from '@angular/router';
import { HttpClient } from '@angular/common/http';
import { ApiService } from '../../core/services/api.service';
import { ApiResponse } from '../../core/models/api-response.model';
import { Family } from '../../core/models/models';
import { FamilyStateService } from '../../core/services/family-state.service';
import { NarrativeCompanionComponent } from '../../shared/components/narrative-companion.component';

@Component({
  selector: 'app-family-list-page', 
  standalone: true, 
  imports: [CommonModule, RouterLink, NarrativeCompanionComponent],
  templateUrl: './family-list-page.component.html',
  styleUrls: ['./family-list-page.component.css']
})
export class FamilyListPageComponent implements OnInit {
  private http = inject(HttpClient); 
  private api = inject(ApiService); 
  private router = inject(Router);
  private familyState = inject(FamilyStateService);
  
  families: Family[] = []; 
  loading = false;
  
  ngOnInit() {
    this.loading = true;
    this.http.get<ApiResponse<Family[]>>(`${this.api.base}/families`).subscribe({
      next: ({ data }) => { this.families = data; this.loading = false; },
      error: () => this.loading = false
    });
  }
  
  select(f: Family) {
    // Usamos el estado centralizado
    this.familyState.setFamilyId(f.id, f.name);
    // Campos extra específicos de este módulo pueden ir a localstorage o ampliarse en el Service (mantendremos localstorage por ahora para extra params)
    localStorage.setItem('selectedFamilyCode', f.familyCode ?? '');
    localStorage.setItem('currentMilestone', f.currentMilestone ?? 'inicio');
    this.router.navigate(['/members']);
  }
  
  isSelected(f: Family) { return this.familyState.currentFamilyId() === f.id; }
}
