import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { CreateFamilyGratitudeRequest, FamilyGratitude } from './family-gratitude.model';
import { Observable } from 'rxjs';

@Injectable({
  providedIn: 'root'
})
export class FamilyGratitudeService {

  private readonly baseUrl = '/api/family-gratitude';

  constructor(private readonly http: HttpClient) {}

  create(request: CreateFamilyGratitudeRequest): Observable<FamilyGratitude> {
    return this.http.post<FamilyGratitude>(this.baseUrl, request);
  }

  findByFamily(familyId: number): Observable<FamilyGratitude[]> {
    return this.http.get<FamilyGratitude[]>(`${this.baseUrl}/family/${familyId}`);
  }
}
