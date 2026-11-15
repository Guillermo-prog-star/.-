import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, map } from 'rxjs';
import { ApiService } from '../../core/services/api.service';
import { ApiResponse } from '../../core/models/api-response.model';
import { BiographyChapter, BiographyAnswer, UpsertAnswerRequest } from './biography.model';

@Injectable({ providedIn: 'root' })
export class BiographyService {
  private http = inject(HttpClient);
  private api = inject(ApiService);

  getCatalog(): Observable<BiographyChapter[]> {
    return this.http
      .get<ApiResponse<BiographyChapter[]>>(`${this.api.base}/biography/catalog`)
      .pipe(map(r => r.data));
  }

  getAnswers(familyId: number, memberId: number): Observable<BiographyAnswer[]> {
    return this.http
      .get<ApiResponse<BiographyAnswer[]>>(
        `${this.api.base}/families/${familyId}/biography/members/${memberId}/answers`
      )
      .pipe(map(r => r.data));
  }

  upsertAnswer(familyId: number, questionId: number, req: UpsertAnswerRequest): Observable<BiographyAnswer> {
    return this.http
      .put<ApiResponse<BiographyAnswer>>(
        `${this.api.base}/families/${familyId}/biography/answers/${questionId}`,
        req
      )
      .pipe(map(r => r.data));
  }

  deleteAnswer(familyId: number, questionId: number): Observable<void> {
    return this.http
      .delete<ApiResponse<void>>(`${this.api.base}/families/${familyId}/biography/answers/${questionId}`)
      .pipe(map(() => undefined));
  }
}
