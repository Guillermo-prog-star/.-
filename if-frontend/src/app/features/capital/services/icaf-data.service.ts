import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, of } from 'rxjs';
import { map, catchError } from 'rxjs/operators';
import { environment } from '../../../../environments/environment';
import { IcafDashboardResponse, IcafDomainScoreResult, IcafQuestion, ObservatorySnapshot } from '../../../core/models/icaf.model';

interface ApiResponse<T> {
  success: boolean;
  data: T;
  message: string;
}

@Injectable({ providedIn: 'root' })
export class IcafDataService {

  private readonly http = inject(HttpClient);
  private readonly base        = `${environment.apiBaseUrl}/capital`;
  private readonly observatory = `${environment.apiBaseUrl}/observatory`;

  getDashboard(familyId: number): Observable<IcafDashboardResponse | null> {
    return this.http
      .get<ApiResponse<IcafDashboardResponse>>(`${this.base}/family/${familyId}/dashboard`)
      .pipe(
        map(r => r.data),
        catchError(() => of(null))
      );
  }

  recalculate(familyId: number, trigger = 'MANUAL'): Observable<IcafDashboardResponse | null> {
    return this.http
      .post<ApiResponse<IcafDashboardResponse>>(
        `${this.base}/family/${familyId}/recalculate?trigger=${trigger}`, {}
      )
      .pipe(
        map(r => r.data),
        catchError(() => of(null))
      );
  }

  getQuestions(domain: string): Observable<IcafQuestion[]> {
    return this.http
      .get<ApiResponse<IcafQuestion[]>>(`${this.base}/questionnaire/${domain}/questions`)
      .pipe(
        map(r => r.data ?? []),
        catchError(() => of([]))
      );
  }

  getObservatoryLatest(): Observable<ObservatorySnapshot | null> {
    return this.http
      .get<ApiResponse<ObservatorySnapshot>>(`${this.observatory}/snapshots/latest`)
      .pipe(map(r => r.data), catchError(() => of(null)));
  }

  getObservatoryHistory(months = 6): Observable<ObservatorySnapshot[]> {
    return this.http
      .get<ApiResponse<ObservatorySnapshot[]>>(`${this.observatory}/snapshots?months=${months}`)
      .pipe(map(r => r.data ?? []), catchError(() => of([])));
  }

  generateObservatoryMonth(yearMonth: string): Observable<ObservatorySnapshot | null> {
    return this.http
      .post<ApiResponse<ObservatorySnapshot>>(`${this.observatory}/snapshots/generate/${yearMonth}`, {})
      .pipe(map(r => r.data), catchError(() => of(null)));
  }

  saveAnswers(
    familyId: number,
    domain: string,
    answers: Record<string, number>,
    answeredBy?: string
  ): Observable<IcafDomainScoreResult | null> {
    const params = answeredBy ? `?answeredBy=${encodeURIComponent(answeredBy)}` : '';
    return this.http
      .post<ApiResponse<IcafDomainScoreResult>>(
        `${this.base}/family/${familyId}/questionnaire/${domain}/answers${params}`,
        answers
      )
      .pipe(
        map(r => r.data),
        catchError(() => of(null))
      );
  }
}
