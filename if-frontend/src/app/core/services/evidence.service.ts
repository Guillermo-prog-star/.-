import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

export type EvidenceType = 'PHOTO' | 'VIDEO' | 'AUDIO' | 'BITACORA' | 'CHECKLIST' | 'SELF_REFLECTION' | 'DOCUMENT' | 'FAMILY_SIGNATURE';

export interface SubmitEvidenceRequest {
  taskId: number;
  familyId: number;
  evidenceType: EvidenceType;
  title: string;
  description?: string;
  fileUrl?: string;
  textContent?: string;
  submittedBy: string;
  // Multimodal
  emotion?: string;
  latitude?: number;
  longitude?: number;
  memberName?: string;
  mediaData?: string;   // base64
  mediaMime?: string;   // image/jpeg | audio/webm
}

export interface EvidenceResponse {
  id: number;
  evidenceType: EvidenceType;
  status: string;
  title: string;
  description: string | null;
  textContent: string | null;
  submittedBy: string;
  emotion: string | null;
  latitude: number | null;
  longitude: number | null;
  memberName: string | null;
  mediaData: string | null;
  mediaMime: string | null;
  createdAt: string;
}

@Injectable({ providedIn: 'root' })
export class EvidenceService {
  private readonly http = inject(HttpClient);

  submit(req: SubmitEvidenceRequest): Observable<{ data: EvidenceResponse }> {
    return this.http.post<{ data: EvidenceResponse }>('/api/evidences/submit', req);
  }

  getByFamily(familyId: number): Observable<{ data: EvidenceResponse[] }> {
    return this.http.get<{ data: EvidenceResponse[] }>(`/api/evidences/family/${familyId}`);
  }
}
