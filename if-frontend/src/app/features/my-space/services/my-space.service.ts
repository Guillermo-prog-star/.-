import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { ApiService } from '../../../core/services/api.service';

@Injectable({
  providedIn: 'root'
})
export class MySpaceService {
  private http = inject(HttpClient);
  private api = inject(ApiService);

  getEntries(): Observable<any> {
    return this.http.get<any>(`${this.api.base}/private/journals`);
  }

  createEntry(entry: any): Observable<any> {
    return this.http.post<any>(`${this.api.base}/private/journals`, entry);
  }

  deleteEntry(id: number): Observable<any> {
    return this.http.delete<any>(`${this.api.base}/private/journals/${id}`);
  }
}
