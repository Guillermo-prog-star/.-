export interface CreateSprintRequest {
  objective: string;
  riskDimension: string;
  durationDays: number;
  missions: string[];
}

export interface CreateDailyCheckinRequest {
  yesterdayText: string;
  todayText: string;
  blockagesText: string;
  resolutionText: string;
  emotionalIndicator: string;
  memberName: string;
}

export interface CloseSprintRequest {
  whatWentWell: string;
  whatWasDifficult: string;
  whatLearned: string;
  whatToAdjust: string;
  tensionLevel: number;
  mindfulCompliance: number;
  sharedTime: number;
  positiveInteractions: number;
  emotionalPersistence: number;
}

export interface SprintMissionResponse {
  id: number;
  description: string;
  status: 'PENDING' | 'COMPLETED';
  completedAt?: string;
}

export interface SprintDailyResponse {
  id: number;
  memberName: string;
  checkinDate: string;
  yesterdayText: string;
  todayText: string;
  blockagesText: string;
  resolutionText: string;
  emotionalIndicator?: string;
  createdAt: string;
}

export interface SprintRetrospectiveResponse {
  id: number;
  whatWentWell: string;
  whatWasDifficult: string;
  whatLearned: string;
  whatToAdjust: string;
  tensionLevel: number;
  mindfulCompliance: number;
  sharedTime: number;
  positiveInteractions: number;
  emotionalPersistence: number;
  consistencyScore: number;
  aiFeedback: string;
  createdAt: string;
}

export interface SprintResponse {
  id: number;
  familyId: number;
  objective: string;
  riskDimension: string;
  durationDays: number;
  startDate: string;
  endDate: string;
  status: 'ACTIVE' | 'COMPLETED' | 'CANCELLED';
  missions: SprintMissionResponse[];
  dailies: SprintDailyResponse[];
  retrospective?: SprintRetrospectiveResponse;
  createdAt: string;
}
