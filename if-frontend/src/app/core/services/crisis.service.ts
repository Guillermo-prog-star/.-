import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, map } from 'rxjs';
import { environment } from '../../../environments/environment';

@Injectable({
  providedIn: 'root'
})
export class CrisisService {
  private http = inject(HttpClient);
  private apiUrl = `${environment.apiBaseUrl}/crisis`;

  reportCrisis(data: any): Observable<any> {
    return this.http.post<any>(`${this.apiUrl}/report`, data).pipe(
      map(res => res.data)
    );
  }

  getCrisisStatus(familyId: number): Observable<boolean> {
    return this.http.get<any>(`${this.apiUrl}/status/${familyId}`).pipe(
      map(res => !!(res?.data ?? res))
    );
  }

  getHistory(familyId: number): Observable<any[]> {
    return this.http.get<any>(`${this.apiUrl}/family/${familyId}`).pipe(
      map(res => res.data)
    );
  }
}
