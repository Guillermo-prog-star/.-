export type LogbookStatus = 'OPEN' | 'RESOLVED';

export interface FamilyLogbookEntry {
  id: number;
  familyId: number;
  situation: string;
  difficultyDetected: string;
  emotionIdentified: string;
  understanding: string;
  correctionAction: string;
  familyAgreement: string;
  progressEvidence?: string;
  status: LogbookStatus;
  createdBy?: string;
  resolvedBy?: string;
  createdAt: string;
  resolvedAt?: string;
}

export interface CreateFamilyLogbookEntryRequest {
  familyId: number;
  situation: string;
  difficultyDetected: string;
  emotionIdentified: string;
  understanding: string;
  correctionAction: string;
  familyAgreement: string;
  createdBy?: string;
}

export interface ResolveFamilyLogbookEntryRequest {
  progressEvidence: string;
  resolvedBy?: string;
}
