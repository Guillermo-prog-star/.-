import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

export interface DetectedPattern {
  pattern: string;
  frequency: number;
  confidence: number;
  description: string;
}

export interface Correlation {
  trigger: string;
  effect: string;
  lagDays: number;
  confidence: number;
}

export interface PredictionDto {
  id: number;
  predictionType: string;
  title: string;
  description: string | null;
  confidence: number;
  timeHorizon: string | null;
  recommendedAction: string | null;
  status: string;
}

export interface DigitalTwinDto {
  familyId: number;
  familyName: string;
  behavioralSignature: string | null;
  communicationPattern: string | null;
  resilienceIndex: number;
  bondingRhythm: string | null;
  dominantStrength: string | null;
  dominantVulnerability: string | null;
  dataRichness: string;
  avgDaysBetweenCrises: number | null;
  avgRecoveryDays: number | null;
  peakActivityDay: string | null;
  detectedPatterns: DetectedPattern[];
  correlations: Correlation[];
  activePredictions: PredictionDto[];
  computedAt: string;
}

@Injectable({ providedIn: 'root' })
export class DigitalTwinService {
  private readonly http = inject(HttpClient);

  get(familyId: number): Observable<DigitalTwinDto> {
    return this.http.get<DigitalTwinDto>(`/api/families/${familyId}/twin`);
  }

  compute(familyId: number): Observable<DigitalTwinDto> {
    return this.http.post<DigitalTwinDto>(`/api/families/${familyId}/twin/compute`, {});
  }
}
