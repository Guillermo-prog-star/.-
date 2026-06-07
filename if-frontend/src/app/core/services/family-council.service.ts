import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

export interface CouncilRequest {
  question: string;
  topic?: string;
  context?: string;
}

export interface CouncilResponse {
  familyId: number;
  familyName: string;
  question: string;
  topic: string | null;
  councilResponse: string;
  hasConstitution: boolean;
  hasDna: boolean;
  sourcesUsed: string[];
  consultedAt: string;
}

@Injectable({ providedIn: 'root' })
export class FamilyCouncilService {
  private readonly http = inject(HttpClient);

  consult(familyId: number, req: CouncilRequest): Observable<CouncilResponse> {
    return this.http.post<CouncilResponse>(
      `/api/families/${familyId}/council/consult`, req
    );
  }
}
