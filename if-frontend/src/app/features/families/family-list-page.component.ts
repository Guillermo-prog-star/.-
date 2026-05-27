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
    this.http.get<ApiResponse<Family>>(`${this.api.base}/families/mine`).subscribe({
      next: ({ data }) => {
        if (data) {
          this.families = [data];
          // Corrige cualquier estado stale en localStorage
          this.familyState.setFamily(data);
          this.familyState.setMilestone((data as any).currentMilestone ?? 'inicio');
        } else {
          this.families = [];
        }
        this.loading = false;
      },
      error: () => this.loading = false
    });
  }
  
  select(f: Family) {
    this.familyState.setFamily(f);
    this.familyState.setMilestone(f.currentMilestone ?? 'inicio');
    this.router.navigate(['/members']);
  }
  
  isSelected(f: Family) { return this.familyState.currentFamilyId() === f.id; }
}
