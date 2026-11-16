import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, finalize, map, of, shareReplay } from 'rxjs';
import { ApiService } from '../../core/services/api.service';
import { FamilyStateService } from '../../core/services/family-state.service';
import { ApiResponse } from '../../core/models/api-response.model';
import { CatalogResponse, FourWeekCheckResponse, ReviewResponse } from './sistemas.model';

@Injectable({ providedIn: 'root' })
export class SistemasService {
  private readonly http = inject(HttpClient);
  private readonly api = inject(ApiService);
  private readonly familyState = inject(FamilyStateService);
  private familyInFlight: Observable<number> | null = null;

  getCatalog(): Observable<CatalogResponse> {
    return this.http
      .get<ApiResponse<CatalogResponse>>(`${this.api.base}/sistemas/catalog`)
      .pipe(map(response => response.data));
  }

  history(familyId: number): Observable<ReviewResponse[]> {
    return this.http
      .get<ApiResponse<ReviewResponse[]>>(`${this.api.base}/families/${familyId}/sistemas/reviews`)
      .pipe(map(response => response.data ?? []));
  }

  saveReview(familyId: number, body: unknown): Observable<ReviewResponse> {
    return this.http
      .post<ApiResponse<ReviewResponse>>(`${this.api.base}/families/${familyId}/sistemas/reviews`, body)
      .pipe(map(response => response.data));
  }

  fourWeekCheck(familyId: number): Observable<FourWeekCheckResponse> {
    return this.http
      .get<ApiResponse<FourWeekCheckResponse>>(`${this.api.base}/families/${familyId}/sistemas/check`)
      .pipe(map(response => response.data));
  }

  saveCheck(familyId: number, unregisteredCommitment: boolean): Observable<FourWeekCheckResponse> {
    return this.http
      .post<ApiResponse<FourWeekCheckResponse>>(`${this.api.base}/families/${familyId}/sistemas/check`, {
        unregisteredCommitment
      })
      .pipe(map(response => response.data));
  }

  ensureFamily(): Observable<number> {
    const current = this.familyState.getSelectedFamilyId();
    if (current) return of(current);
    if (!this.familyInFlight) {
      this.familyInFlight = this.http.get<any>('/api/families/mine').pipe(
        map(res => {
          const family = res?.data ?? res;
          if (!family?.id) {
            throw new Error('No se encontró una familia activa. Inicia sesión nuevamente.');
          }
          this.familyState.setFamily(family);
          const authUser = JSON.parse(localStorage.getItem('auth_user') || '{}');
          const matched = family.members?.find((member: { email?: string; id?: number }) => member.email === authUser.email);
          if (matched?.id) this.familyState.setMemberId(matched.id);
          return family.id as number;
        }),
        finalize(() => {
          this.familyInFlight = null;
        }),
        shareReplay(1)
      );
    }
    return this.familyInFlight;
  }
}
