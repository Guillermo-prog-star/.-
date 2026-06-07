import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';

export interface FamilyMovieDto {
  id: number;
  familyId: number;
  familyName: string;
  periodLabel: string;
  periodStart: string;
  periodEnd: string;
  evidencesCount: number;
  gratitudesCount: number;
  missionsCompleted: number;
  crisesCount: number;
  ritualsCompleted: number;
  daysActive: number;
  bestStreak: number;
  icfStart: number | null;
  icfEnd: number | null;
  icfDelta: number | null;
  openingLine: string | null;
  chapter1: string | null;
  chapter2: string | null;
  chapter3: string | null;
  mentorLetter: string | null;
  highlightQuote: string | null;
  generatedAt: string;
}

@Injectable({ providedIn: 'root' })
export class FamilyMovieService {
  private readonly http = inject(HttpClient);

  list(familyId: number): Observable<FamilyMovieDto[]> {
    return this.http.get<FamilyMovieDto[]>(`/api/families/${familyId}/movies`);
  }

  getLatest(familyId: number): Observable<FamilyMovieDto> {
    return this.http.get<FamilyMovieDto>(`/api/families/${familyId}/movies/latest`);
  }

  generateQuarter(familyId: number): Observable<FamilyMovieDto> {
    return this.http.post<FamilyMovieDto>(`/api/families/${familyId}/movies/generate/quarter`, {});
  }

  generate(familyId: number, from: string, to: string): Observable<FamilyMovieDto> {
    const params = new HttpParams().set('from', from).set('to', to);
    return this.http.post<FamilyMovieDto>(`/api/families/${familyId}/movies/generate`, {}, { params });
  }
}
