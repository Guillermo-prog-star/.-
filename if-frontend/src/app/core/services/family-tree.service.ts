import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

export interface FamilyTreeNode {
  familyId: number;
  familyName: string;
  familyCode: string | null;
  createdAt: string | null;
  generation: number;
  memberCount: number;
  evidenceCount: number;
  gratitudeCount: number;
  dnaValores: string | null;
  linkedByMember: string | null;
  linkedAt: string | null;
  children: FamilyTreeNode[];
}

export interface AncestorHeritage {
  familyId: number;
  familyName: string;
  familyCode: string | null;
  generation: number;
  foundingPrinciple: string | null;
  familyMission: string | null;
  familyVision: string | null;
  historyLessons: string | null;
  historyRecognition: string | null;
  dnaValues: string | null;
  dnaNarrativeIa: string | null;
  readableMessages: MessageSummary[];
  evidenceCount: number;
  gratitudeCount: number;
}

export interface MessageSummary {
  id: number;
  subject: string | null;
  content: string;
  authorName: string;
  messageType: string;
  fromYear: number;
}

export interface HeritageDto {
  familyId: number;
  familyName: string;
  ancestors: AncestorHeritage[];
}

export interface GenerationalMessage {
  id: number;
  fromFamilyId: number;
  toFamilyId: number | null;
  authorName: string;
  subject: string | null;
  content: string;
  messageType: string;
  sealed: boolean;
  openInYear: number | null;
  createdAt: string;
}

export interface LinkRequest {
  parentFamilyCode: string;
  linkedByMember?: string;
  note?: string;
}

export interface MessageRequest {
  authorName: string;
  subject?: string;
  content: string;
  messageType?: string;
  toFamilyId?: number | null;
  openInYear?: number | null;
}

@Injectable({ providedIn: 'root' })
export class FamilyTreeService {
  private readonly http = inject(HttpClient);

  getFullTree(familyId: number): Observable<FamilyTreeNode> {
    return this.http.get<FamilyTreeNode>(`/api/families/${familyId}/tree`);
  }

  getAncestors(familyId: number): Observable<FamilyTreeNode[]> {
    return this.http.get<FamilyTreeNode[]>(`/api/families/${familyId}/tree/ancestors`);
  }

  getHeritage(familyId: number): Observable<HeritageDto> {
    return this.http.get<HeritageDto>(`/api/families/${familyId}/tree/heritage`);
  }

  link(familyId: number, req: LinkRequest): Observable<void> {
    return this.http.post<void>(`/api/families/${familyId}/tree/link`, req);
  }

  unlink(familyId: number): Observable<void> {
    return this.http.delete<void>(`/api/families/${familyId}/tree/link`);
  }

  createMessage(familyId: number, req: MessageRequest): Observable<GenerationalMessage> {
    return this.http.post<GenerationalMessage>(`/api/families/${familyId}/tree/messages`, req);
  }

  getAncestorMessages(familyId: number): Observable<GenerationalMessage[]> {
    return this.http.get<GenerationalMessage[]>(`/api/families/${familyId}/tree/messages/ancestors`);
  }

  getOwnMessages(familyId: number): Observable<GenerationalMessage[]> {
    return this.http.get<GenerationalMessage[]>(`/api/families/${familyId}/tree/messages/own`);
  }
}
