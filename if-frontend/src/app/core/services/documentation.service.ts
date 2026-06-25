import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { ApiService } from './api.service';

export type DocumentCategory = 'PROJECT' | 'RESEARCH' | 'FAMILY' | 'AI' | 'DEVELOPMENT';

export interface DocumentSummary {
  id: number;
  code: string;
  title: string;
  category: DocumentCategory;
  summary: string;
  version: string;
  tags: string;
  updatedAt: string;
}

export interface DocumentDetail extends DocumentSummary {
  content: string;
  status: string;
  createdAt: string;
}

export interface DocumentListResponse {
  documents: DocumentSummary[];
  total: number;
}

export interface QueryResponse {
  answer: string;
  sources: DocumentSummary[];
}

@Injectable({ providedIn: 'root' })
export class DocumentationService {
  private http = inject(HttpClient);
  private api  = inject(ApiService);
  private base = `${this.api.base}/documentation`;

  listAll(): Observable<DocumentListResponse> {
    return this.http.get<DocumentListResponse>(this.base);
  }

  listByCategory(category: DocumentCategory): Observable<DocumentListResponse> {
    return this.http.get<DocumentListResponse>(`${this.base}/category/${category}`);
  }

  getByCode(code: string): Observable<DocumentDetail> {
    return this.http.get<DocumentDetail>(`${this.base}/${code}`);
  }

  search(q: string): Observable<DocumentListResponse> {
    return this.http.get<DocumentListResponse>(`${this.base}/search`, { params: { q } });
  }

  query(question: string): Observable<QueryResponse> {
    return this.http.post<QueryResponse>(`${this.base}/query`, { question });
  }
}
