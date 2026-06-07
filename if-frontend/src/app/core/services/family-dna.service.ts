import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';

export interface PotencialMiembro {
  miembro: string;
  talento: string;
  descripcion: string;
}

export interface FamilyDnaDto {
  familyId: number;
  valores: string[];
  fortalezas: string[];
  sombras: string[];
  patrones: string[];
  estiloComunicacion: string | null;
  ritmoFamiliar: string | null;
  potencialOculto: PotencialMiembro[];
  narrativaIa: string | null;
  version: number;
  updatedAt: string | null;
}

@Injectable({ providedIn: 'root' })
export class FamilyDnaService {
  private readonly http = inject(HttpClient);
  private readonly base = environment.apiBaseUrl;

  get(familyId: number): Observable<FamilyDnaDto> {
    return this.http.get<FamilyDnaDto>(`${this.base}/families/${familyId}/dna`);
  }

  synthesize(familyId: number): Observable<FamilyDnaDto> {
    return this.http.post<FamilyDnaDto>(`${this.base}/families/${familyId}/dna/synthesize`, {});
  }
}
