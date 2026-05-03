import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

@Injectable({
  providedIn: 'root'
})
export class FeedbackService {
  private http = inject(HttpClient);

  sendFeedback(data: any): Observable<any> {
    return this.http.post('/api/feedback/send', data);
  }

  getAllFeedback(): Observable<any> {
    return this.http.get('/api/feedback/all');
  }
}
