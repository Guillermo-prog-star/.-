import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, of } from 'rxjs';
import { catchError, map, timeout } from 'rxjs/operators';
import { environment } from '../../../environments/environment';

// ─── DTOs (espejo de los records Java) ───────────────────────────────────────

export interface DimensionTrend {
  dimension: string;
  currentScore: number | null;
  previousScore: number | null;
  delta: number | null;
  direction: 'IMPROVING' | 'STRONG_IMPROVING' | 'STABLE' | 'DECLINING' | 'CRITICAL_DECLINE' | 'NO_DATA';
  signal: string;
}

export interface IcfTrend {
  current: number;
  delta30d: number | null;
  delta90d: number | null;
  direction: string;
  evolutionPhase: string | null;
}

export interface MicroSignal {
  dimension: string;
  signalCode: string;
  description: string;
  severity: 'LOW' | 'MEDIUM' | 'HIGH';
  weight: number;
}

export interface InvisibleStrength {
  dimension: string;
  description: string;
  evidence: string;
}

export interface TrajectoryMatch {
  trajectoryCode: string;
  trajectoryName: string;
  confidenceScore: number;
  reason: string;
}

export interface RadarResponse {
  familyId: number;
  evaluationsAnalyzed: number;
  emociones: DimensionTrend | null;
  comunicacion: DimensionTrend | null;
  habitos: DimensionTrend | null;
  tiempos: DimensionTrend | null;
  icfOverall: IcfTrend | null;
  microSignals: MicroSignal[];
  strengths: InvisibleStrength[];
  trajectoryMatches: TrajectoryMatch[];
  confidenceScore: number;
  narrativeSummary: string;
  generatedAt: string;
}

// ─── Escenarios ───────────────────────────────────────────────────────────────

export interface ProjectionPoint {
  weekNumber: number;
  icfProjected: number;
  icfMin: number;
  icfMax: number;
  riskLevel: string;
}

export interface DimensionProjection {
  dimension: string;
  currentScore: number;
  projectedScore: number;
  delta: number;
  direction: string;
}

export interface Scenario {
  label: string;
  code: 'A' | 'B' | 'C';
  probabilityPercent: number;
  direction: string;
  week4: ProjectionPoint;
  week8: ProjectionPoint;
  week12: ProjectionPoint;
  emociones: DimensionProjection;
  comunicacion: DimensionProjection;
  habitos: DimensionProjection;
  tiempos: DimensionProjection;
  estimatedRiskLevel: string;
  narrative: string;
  keyActions: string[];
}

export interface ScenarioResponse {
  familyId: number;
  icfBaseline: number;
  scenarioA: Scenario;
  scenarioB: Scenario;
  scenarioC: Scenario;
  pivotMessage: string;
  opportunityWindow: string;
  generatedAt: string;
}

// ─── Narrativa ────────────────────────────────────────────────────────────────

export interface NarrativeResponse {
  familyId: number;
  familyName: string;
  narrative: string;
  radarConfidence: number;
  generatedAt: string;
}

// ─── Servicio ─────────────────────────────────────────────────────────────────

@Injectable({ providedIn: 'root' })
export class SubtleSignalRadarService {

  private readonly base = environment.apiBaseUrl;

  constructor(private readonly http: HttpClient) {}

  getRadar(familyId: number): Observable<RadarResponse | null> {
    return this.http
      .get<{ data: RadarResponse }>(`${this.base}/families/${familyId}/radar`)
      .pipe(
        timeout(12000),
        map(res => res?.data ?? (res as any)),
        catchError(() => of(null))
      );
  }

  getScenarios(familyId: number): Observable<ScenarioResponse | null> {
    return this.http
      .get<{ data: ScenarioResponse }>(`${this.base}/families/${familyId}/radar/scenarios`)
      .pipe(
        timeout(12000),
        map(res => res?.data ?? (res as any)),
        catchError(() => of(null))
      );
  }

  getNarrative(familyId: number): Observable<NarrativeResponse | null> {
    return this.http
      .get<{ data: NarrativeResponse }>(`${this.base}/families/${familyId}/radar/narrative`)
      .pipe(
        timeout(30000),
        map(res => res?.data ?? (res as any)),
        catchError(() => of(null))
      );
  }
}
