import { inject, Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

export type DocumentaryScope = 'MISSION' | 'SPRINT' | 'PILLAR' | 'MONTH' | 'TRIMESTER' | 'ROUTE_36';
export type ProductionStatus = 'DRAFT' | 'CURATED' | 'GENERATED' | 'REVIEWED' | 'APPROVED' | 'PUBLISHED' | 'ARCHIVED';

export interface CreateDraftRequest {
  familyId: number;
  title: string;
  scope: DocumentaryScope;
  referenceId?: number | null;
}

export interface DocumentaryProductionDTO {
  id: number;
  familyId: number;
  title: string;
  scope: DocumentaryScope;
  referenceId: number;
  status: ProductionStatus;
  curatedEvidences: any[];
  scriptData: string | null;
  exportUrl: string | null;
  createdAt: string;
  updatedAt: string;
}

@Injectable({ providedIn: 'root' })
export class DocumentaryProductionService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = '/api/documentary-productions';

  createDraft(req: CreateDraftRequest): Observable<{ data: DocumentaryProductionDTO }> {
    return this.http.post<{ data: DocumentaryProductionDTO }>(`${this.baseUrl}/draft`, req);
  }

  updateCuration(id: number, evidenceIds: number[]): Observable<{ data: DocumentaryProductionDTO }> {
    return this.http.put<{ data: DocumentaryProductionDTO }>(`${this.baseUrl}/${id}/curation`, { evidenceIds });
  }

  generateScript(id: number): Observable<{ data: DocumentaryProductionDTO }> {
    return this.http.post<{ data: DocumentaryProductionDTO }>(`${this.baseUrl}/${id}/generate-script`, {});
  }

  approveProduction(id: number): Observable<{ data: DocumentaryProductionDTO }> {
    return this.http.post<{ data: DocumentaryProductionDTO }>(`${this.baseUrl}/${id}/approve`, {});
  }

  getProductions(familyId: number): Observable<{ data: DocumentaryProductionDTO[] }> {
    return this.http.get<{ data: DocumentaryProductionDTO[] }>(`${this.baseUrl}/family/${familyId}`);
  }

  getProduction(id: number): Observable<{ data: DocumentaryProductionDTO }> {
    return this.http.get<{ data: DocumentaryProductionDTO }>(`${this.baseUrl}/${id}`);
  }
}
