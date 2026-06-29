import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';

export type NetworkType = 'FAMILIAR' | 'INSTITUTIONAL' | 'COMMUNITY' | 'TERRITORIAL';
export type LinkStatus   = 'INVITED' | 'ACTIVE' | 'SUSPENDED' | 'REVOKED';

export interface EcosystemParticipant {
  id: number;
  name: string;
  networkType: NetworkType;
  description: string;
  contactEmail: string;
  contactPhone: string;
  website: string;
  active: boolean;
}

export interface EcosystemAccessScope {
  canViewIcfScore: boolean;
  canViewRiskLevel: boolean;
  canViewPlanSummary: boolean;
  canViewSprintProgress: boolean;
  canViewCrisisHistory: boolean;
  canReceiveAlerts: boolean;
}

export interface EcosystemLink {
  id: number;
  familyId: number;
  participant: EcosystemParticipant;
  networkType: NetworkType;
  accessLevel: number;
  objective: string;
  responsibilities: string;
  validFrom: string | null;
  validUntil: string | null;
  expired: boolean;
  status: LinkStatus;
  invitedByEmail: string;
  invitedAt: string;
  consentedByEmail: string | null;
  consentedAt: string | null;
  accessScope: EcosystemAccessScope;
}

export interface EcosystemSummary {
  familyId: number;
  totalLinks: number;
  activeLinks: number;
  familiar: EcosystemLink[];
  institutional: EcosystemLink[];
  community: EcosystemLink[];
  territorial: EcosystemLink[];
}

export interface AuditEntry {
  id: number;
  linkId: number;
  actorEmail: string;
  action: string;
  detail: string;
  accessLevel: number;
  createdAt: string;
}

@Injectable({ providedIn: 'root' })
export class EcosystemService {
  private http = inject(HttpClient);
  private base  = environment.apiBaseUrl;

  getParticipants(networkType?: NetworkType): Observable<EcosystemParticipant[]> {
    const params = networkType ? `?networkType=${networkType}` : '';
    return this.http.get<EcosystemParticipant[]>(`${this.base}/ecosystem/participants${params}`);
  }

  getSummary(familyId: number): Observable<EcosystemSummary> {
    return this.http.get<EcosystemSummary>(`${this.base}/families/${familyId}/ecosystem`);
  }

  getActiveLinks(familyId: number): Observable<EcosystemLink[]> {
    return this.http.get<EcosystemLink[]>(`${this.base}/families/${familyId}/ecosystem/active`);
  }

  link(familyId: number, payload: {
    participantId: number;
    objective?: string;
    responsibilities?: string;
    validFrom?: string;
    validUntil?: string;
    accessScope?: Partial<EcosystemAccessScope>;
  }): Observable<EcosystemLink> {
    return this.http.post<EcosystemLink>(`${this.base}/families/${familyId}/ecosystem/links`, payload);
  }

  giveConsent(familyId: number, linkId: number, accessScope?: Partial<EcosystemAccessScope>): Observable<EcosystemLink> {
    return this.http.post<EcosystemLink>(`${this.base}/families/${familyId}/ecosystem/consent`, { linkId, accessScope });
  }

  revoke(familyId: number, linkId: number, reason?: string): Observable<EcosystemLink> {
    return this.http.post<EcosystemLink>(`${this.base}/families/${familyId}/ecosystem/revoke`, { linkId, reason });
  }

  getAuditLog(familyId: number): Observable<AuditEntry[]> {
    return this.http.get<AuditEntry[]>(`${this.base}/families/${familyId}/ecosystem/audit`);
  }
}
