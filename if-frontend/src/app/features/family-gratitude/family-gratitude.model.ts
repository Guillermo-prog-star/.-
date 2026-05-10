export interface CreateFamilyGratitudeRequest {
  familyId: number;
  fromMember: string;
  toMember: string;
  description: string;
}

export interface FamilyGratitude {
  id: number;
  familyId: number;
  fromMember: string;
  toMember: string;
  description: string;
  createdAt: string;
}
