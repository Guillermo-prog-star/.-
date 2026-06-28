import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, map, catchError, of } from 'rxjs';
import { environment } from '../../../../environments/environment';
import { IndicatorsSnapshot } from '../models/smff.model';

interface ApiResponse<T> {
  success: boolean;
  data:    T;
  message: string;
}

@Injectable({ providedIn: 'root' })
export class SmffDataService {
  private readonly http = inject(HttpClient);
  private readonly base = `${environment.apiBaseUrl}/indicators`;

  getSnapshot(familyId: number): Observable<IndicatorsSnapshot | null> {
    return this.http
      .get<ApiResponse<IndicatorsSnapshot>>(`${this.base}/family/${familyId}`)
      .pipe(
        map(r => r.success ? r.data : null),
        catchError(() => of(null))
      );
  }
}
