import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import {
  CreateFamilyLogbookEntryRequest,
  FamilyLogbookEntry,
  LogbookStatus,
  ResolveFamilyLogbookEntryRequest
} from './family-logbook.model';
import { Observable } from 'rxjs';

@Injectable({
  providedIn: 'root'
})
export class FamilyLogbookService {

  private readonly baseUrl = '/api/family-logbook';

  constructor(private readonly http: HttpClient) {}

  create(request: CreateFamilyLogbookEntryRequest): Observable<FamilyLogbookEntry> {
    return this.http.post<FamilyLogbookEntry>(this.baseUrl, request);
  }

  findByFamily(familyId: number): Observable<FamilyLogbookEntry[]> {
    return this.http.get<FamilyLogbookEntry[]>(`${this.baseUrl}/family/${familyId}`);
  }

  findByFamilyAndStatus(
    familyId: number,
    status: LogbookStatus
  ): Observable<FamilyLogbookEntry[]> {
    return this.http.get<FamilyLogbookEntry[]>(
      `${this.baseUrl}/family/${familyId}/status/${status}`
    );
  }

  resolve(
    id: number,
    request: ResolveFamilyLogbookEntryRequest
  ): Observable<FamilyLogbookEntry> {
    return this.http.put<FamilyLogbookEntry>(
      `${this.baseUrl}/${id}/resolve`,
      request
    );
  }
}
