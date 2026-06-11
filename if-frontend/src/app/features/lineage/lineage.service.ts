import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, map } from 'rxjs';
import { ApiService } from '../../core/services/api.service';
import {
  Lineage, LineageMember, LineageMemberRequest,
  LineageRelationship, GenerationInfo, GenerationInfoRequest
} from './lineage.model';

interface ApiResponse<T> { data: T; message?: string; success: boolean; }

@Injectable({ providedIn: 'root' })
export class LineageService {
  private http = inject(HttpClient);
  private api  = inject(ApiService);

  private url(familyId: number, suffix = '') {
    return `${this.api.base}/api/families/${familyId}/lineage${suffix}`;
  }

  getLineage(familyId: number): Observable<Lineage> {
    return this.http.get<ApiResponse<Lineage>>(this.url(familyId)).pipe(map(r => r.data));
  }

  createLineage(familyId: number, req: Partial<{ title: string; description: string;
    anchorGeneration: number; maxPastGen: number; maxFutureGen: number;
    visionStatement: string; foundingYear: string }>): Observable<Lineage> {
    return this.http.post<ApiResponse<Lineage>>(this.url(familyId), req).pipe(map(r => r.data));
  }

  updateLineage(familyId: number, req: Partial<{ title: string; description: string;
    anchorGeneration: number; maxPastGen: number; maxFutureGen: number;
    visionStatement: string; foundingYear: string }>): Observable<Lineage> {
    return this.http.put<ApiResponse<Lineage>>(this.url(familyId), req).pipe(map(r => r.data));
  }

  addMember(familyId: number, req: LineageMemberRequest): Observable<LineageMember> {
    return this.http.post<ApiResponse<LineageMember>>(this.url(familyId, '/members'), req)
      .pipe(map(r => r.data));
  }

  updateMember(familyId: number, memberId: number, req: LineageMemberRequest): Observable<LineageMember> {
    return this.http.put<ApiResponse<LineageMember>>(this.url(familyId, `/members/${memberId}`), req)
      .pipe(map(r => r.data));
  }

  deleteMember(familyId: number, memberId: number): Observable<void> {
    return this.http.delete<ApiResponse<void>>(this.url(familyId, `/members/${memberId}`))
      .pipe(map(() => undefined));
  }

  addRelationship(familyId: number, fromMemberId: number, toMemberId: number,
    relationshipType: string, isCouple: boolean): Observable<LineageRelationship> {
    return this.http.post<ApiResponse<LineageRelationship>>(this.url(familyId, '/relationships'),
      { fromMemberId, toMemberId, relationshipType, isCouple }).pipe(map(r => r.data));
  }

  deleteRelationship(familyId: number, relId: number): Observable<void> {
    return this.http.delete<ApiResponse<void>>(this.url(familyId, `/relationships/${relId}`))
      .pipe(map(() => undefined));
  }

  upsertGenerationInfo(familyId: number, req: GenerationInfoRequest): Observable<GenerationInfo> {
    return this.http.put<ApiResponse<GenerationInfo>>(this.url(familyId, '/generation-info'), req)
      .pipe(map(r => r.data));
  }
}
