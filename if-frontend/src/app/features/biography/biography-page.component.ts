import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute } from '@angular/router';
import { HttpClient } from '@angular/common/http';
import { forkJoin } from 'rxjs';
import { BiographyService } from './biography.service';
import { FamilyStateService } from '../../core/services/family-state.service';
import {
  BiographyAnswer,
  BiographyQuestion,
  EPISTEMIC_SOURCE_LABELS,
  EntryVisibility,
  EpistemicSource,
  UpsertAnswerRequest,
  VISIBILITY_LABELS
} from './biography.model';

interface QuestionVM {
  question: BiographyQuestion;
  answer: BiographyAnswer | null;
}

interface ChapterVM {
  chapterNumber: number;
  chapterTitle: string;
  questions: QuestionVM[];
  answeredCount: number;
}

@Component({
  selector: 'app-biography-page',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './biography-page.component.html',
  styleUrl: './biography-page.component.css'
})
export class BiographyPageComponent implements OnInit {
  private readonly route = inject(ActivatedRoute);
  private readonly http = inject(HttpClient);
  private readonly biographyService = inject(BiographyService);
  private readonly familyState = inject(FamilyStateService);

  readonly epistemicSourceLabels = EPISTEMIC_SOURCE_LABELS;
  readonly visibilityLabels = VISIBILITY_LABELS;
  readonly epistemicSourceOptions: EpistemicSource[] = ['OBSERVED', 'TOLD_BY_OTHER', 'DOCUMENT', 'UNCERTAIN'];
  readonly visibilityOptions: EntryVisibility[] = ['PRIVATE', 'SHARED_WITH_FAMILY'];

  chapters = signal<ChapterVM[]>([]);
  loading = signal(true);
  saving = signal(false);
  errorMessage = signal('');
  expandedChapter = signal<number | null>(1);
  editingQuestionId = signal<number | null>(null);

  draft: UpsertAnswerRequest = this.blankDraft();

  familyId = 0;
  viewingMemberId: number | null = null;
  isOwnDiary = false;

  ngOnInit(): void {
    this.familyId = this.familyState.getSelectedFamilyId();
    const paramMemberId = this.route.snapshot.paramMap.get('memberId');
    const ownMemberId = this.familyState.currentMemberId();

    if (this.familyId && (paramMemberId || ownMemberId)) {
      this.startWithMember(paramMemberId ? Number(paramMemberId) : ownMemberId!);
      return;
    }

    // Familia y/o miembro propio todavía no resueltos en esta sesión (p. ej. se entró
    // directo a /biography sin pasar antes por Dashboard/Portal Familiar). Mismo patrón
    // exacto que DashboardPage/PortalFamiliar: GET /api/families/mine + match por email.
    this.http.get<any>('/api/families/mine').subscribe({
      next: res => {
        const family = res?.data ?? res;
        if (!family?.id) {
          this.errorMessage.set('No se encontró una familia activa. Inicia sesión nuevamente.');
          this.loading.set(false);
          return;
        }
        if (!this.familyId) {
          this.familyState.setFamily(family);
          this.familyId = family.id;
        }

        let resolvedMemberId = paramMemberId ? Number(paramMemberId) : ownMemberId;
        if (!resolvedMemberId) {
          const authUser = JSON.parse(localStorage.getItem('auth_user') || '{}');
          const matched = family.members?.find((m: any) => m.email === authUser.email);
          if (matched?.id) {
            this.familyState.setMemberId(matched.id);
            resolvedMemberId = matched.id;
          }
        }

        if (!resolvedMemberId) {
          this.errorMessage.set('No se encontró tu perfil de miembro en esta familia.');
          this.loading.set(false);
          return;
        }
        this.startWithMember(resolvedMemberId);
      },
      error: () => {
        this.errorMessage.set('No fue posible cargar tu familia. Inicia sesión nuevamente.');
        this.loading.set(false);
      }
    });
  }

  private startWithMember(memberId: number): void {
    this.viewingMemberId = memberId;
    this.isOwnDiary = memberId === this.familyState.currentMemberId();

    forkJoin([
      this.biographyService.getCatalog(),
      this.biographyService.getAnswers(this.familyId, memberId)
    ]).subscribe({
      next: ([chapters, answers]) => {
        const answersByQuestionId = new Map(answers.map(a => [a.questionId, a]));
        this.chapters.set(
          chapters.map(c => {
            const questions: QuestionVM[] = c.questions.map(q => ({
              question: q,
              answer: answersByQuestionId.get(q.id) ?? null
            }));
            return {
              chapterNumber: c.chapterNumber,
              chapterTitle: c.chapterTitle,
              questions,
              answeredCount: questions.filter(q => q.answer).length
            };
          })
        );
        this.loading.set(false);
      },
      error: () => {
        this.errorMessage.set('No fue posible cargar el diario guiado.');
        this.loading.set(false);
      }
    });
  }

  toggleChapter(chapterNumber: number): void {
    this.expandedChapter.set(this.expandedChapter() === chapterNumber ? null : chapterNumber);
  }

  startEdit(qvm: QuestionVM): void {
    this.errorMessage.set('');
    this.editingQuestionId.set(qvm.question.id);
    this.draft = qvm.answer
      ? {
          answerText: qvm.answer.answerText ?? '',
          epistemicSource: qvm.answer.epistemicSource,
          occurredPeriod: qvm.answer.occurredPeriod,
          meaningThen: qvm.answer.meaningThen,
          currentMeaning: qvm.answer.currentMeaning,
          relatedSourceNote: qvm.answer.relatedSourceNote,
          confidenceLevel: qvm.answer.confidenceLevel,
          visibility: qvm.answer.visibility,
          completed: !!qvm.answer.completedAt,
          correctionNote: qvm.answer.correctionNote
        }
      : this.blankDraft();
  }

  cancelEdit(): void {
    this.editingQuestionId.set(null);
    this.draft = this.blankDraft();
  }

  saveAnswer(chapter: ChapterVM, qvm: QuestionVM): void {
    if (!this.draft.answerText?.trim()) {
      this.errorMessage.set('La respuesta no puede estar vacía.');
      return;
    }
    this.saving.set(true);
    this.errorMessage.set('');

    this.biographyService.upsertAnswer(this.familyId, qvm.question.id, this.draft).subscribe({
      next: answer => {
        const wasAnswered = !!qvm.answer;
        qvm.answer = answer;
        if (!wasAnswered) chapter.answeredCount++;
        this.saving.set(false);
        this.editingQuestionId.set(null);
        this.draft = this.blankDraft();
      },
      error: () => {
        this.errorMessage.set('No fue posible guardar la respuesta.');
        this.saving.set(false);
      }
    });
  }

  deleteAnswer(chapter: ChapterVM, qvm: QuestionVM): void {
    if (!qvm.answer) return;
    if (!confirm('¿Borrar esta respuesta? Esta acción no se puede deshacer.')) return;

    this.biographyService.deleteAnswer(this.familyId, qvm.question.id).subscribe({
      next: () => {
        qvm.answer = null;
        chapter.answeredCount--;
      },
      error: () => this.errorMessage.set('No fue posible borrar la respuesta.')
    });
  }

  private blankDraft(): UpsertAnswerRequest {
    return {
      answerText: '',
      epistemicSource: null,
      occurredPeriod: null,
      meaningThen: null,
      currentMeaning: null,
      relatedSourceNote: null,
      confidenceLevel: null,
      visibility: 'PRIVATE',
      completed: null,
      correctionNote: null
    };
  }
}
