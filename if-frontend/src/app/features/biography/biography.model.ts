export type EpistemicSource = 'OBSERVED' | 'TOLD_BY_OTHER' | 'DOCUMENT' | 'UNCERTAIN';
export type EntryVisibility = 'PRIVATE' | 'SHARED_WITH_FAMILY';

export interface BiographyQuestion {
  id: number;
  questionKey: string;
  promptText: string;
  deepenText: string | null;
  sortOrder: number;
}

export interface BiographyChapter {
  chapterNumber: number;
  chapterTitle: string;
  questions: BiographyQuestion[];
}

export interface BiographyAnswer {
  questionId: number;
  questionKey: string;
  answerText: string;
  epistemicSource: EpistemicSource | null;
  occurredPeriod: string | null;
  meaningThen: string | null;
  currentMeaning: string | null;
  relatedSourceNote: string | null;
  perceivedEventKey: string | null;
  confidenceLevel: number | null;
  visibility: EntryVisibility;
  correctionNote: string | null;
  completedAt: string | null;
  updatedAt: string | null;
}

export interface UpsertAnswerRequest {
  answerText: string;
  epistemicSource?: EpistemicSource | null;
  occurredPeriod?: string | null;
  meaningThen?: string | null;
  currentMeaning?: string | null;
  relatedSourceNote?: string | null;
  perceivedEventKey?: string | null;
  confidenceLevel?: number | null;
  visibility?: EntryVisibility | null;
  completed?: boolean | null;
  correctionNote?: string | null;
}

export const EPISTEMIC_SOURCE_LABELS: Record<EpistemicSource, string> = {
  OBSERVED: 'Lo viví / lo vi directamente',
  TOLD_BY_OTHER: 'Me lo contaron',
  DOCUMENT: 'Lo sé por un documento',
  UNCERTAIN: 'No estoy seguro / reconstruido'
};

export const VISIBILITY_LABELS: Record<EntryVisibility, string> = {
  PRIVATE: 'Privado',
  SHARED_WITH_FAMILY: 'Compartido con la familia'
};
