import { Component, OnInit, inject, signal, computed } from '@angular/core';
import { CommonModule } from '@angular/common';
import { HttpClient } from '@angular/common/http';
import { ApiService } from '../../../core/services/api.service';
import { ApiResponse } from '../../../core/models/api-response.model';
import { Family } from '../../../core/models/models';

@Component({
  selector: 'app-admin-families-list',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './families-list.component.html',
  styleUrls: ['./families-list.component.css']
})
export class AdminFamiliesListComponent implements OnInit {
  private http = inject(HttpClient);
  private api = inject(ApiService);

  families = signal<Family[]>([]);
  loading = signal(false);
  error = signal<string | null>(null);
  search = signal('');

  filteredFamilies = computed(() => {
    const term = this.search().trim().toLowerCase();
    if (!term) return this.families();
    return this.families().filter(f =>
      f.name?.toLowerCase().includes(term) ||
      f.familyCode?.toLowerCase().includes(term) ||
      f.municipio?.toLowerCase().includes(term)
    );
  });

  ngOnInit() {
    this.loading.set(true);
    this.error.set(null);
    this.http.get<ApiResponse<Family[]>>(`${this.api.base}/families`).subscribe({
      next: ({ data }) => {
        this.families.set(data ?? []);
        this.loading.set(false);
      },
      error: () => {
        this.error.set('No se pudo cargar el listado de familias.');
        this.loading.set(false);
      }
    });
  }

  memberCount(f: Family): number {
    return f.members?.length ?? 0;
  }
}
