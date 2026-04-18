import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router } from '@angular/router';
import { AssessmentService } from '../../core/services/assessment.service';
import { Question } from '../../core/models/question.model';

@Component({
  selector: 'app-evaluation',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './evaluation.component.html',
  styleUrls: ['./evaluation.component.css']
})
export class EvaluationComponent implements OnInit {
  private assessmentService = inject(AssessmentService);
  private router = inject(Router);
  private route = inject(ActivatedRoute);

  evaluationId: number = 0;
  questions: Question[] = [];
  currentIndex: number = 0;
  answers: Map<number, number> = new Map();
  isFinished: boolean = false;
  familyId: number = 0;
  familyName: string = '';

  ngOnInit(): void {
    this.familyId = Number(localStorage.getItem('selectedFamilyId') || 0);
    this.familyName = localStorage.getItem('selectedFamilyName') || 'la familia';

    this.route.params.subscribe((params: any) => {
        this.evaluationId = Number(params['id']);
        if (!this.evaluationId) {
            this.router.navigate(['/evaluations/start']);
        }
    });

    if (this.familyId === 0) {
      this.router.navigate(['/families']);
      return;
    }
    this.loadQuestions();
  }

  loadQuestions(): void {
    if (this.familyId === 0) return;
    this.assessmentService.getRandomQuestions(this.familyId).subscribe({
      next: (data: Question[]) => {
        this.questions = data;
      },
      error: (err: any) => console.error('Error:', err)
    });
  }

  selectAnswer(score: number): void {
    if (!this.questions[this.currentIndex]) return;
    const currentQuestion = this.questions[this.currentIndex];
    this.answers.set(currentQuestion.id, score);
    
    setTimeout(() => {
      if (this.currentIndex < this.questions.length - 1) {
        this.currentIndex++;
      } else {
        this.sendResults();
      }
    }, 400);
  }

  sendResults(): void {
    this.isFinished = true;
    const answerList = Array.from(this.answers.keys()).map(qId => ({
      questionId: Number(qId),
      answerValue: Number(this.answers.get(qId))
    }));

    this.assessmentService.finalizeEvaluation(this.evaluationId, { answers: answerList }).subscribe({
      next: () => this.router.navigate(['/plans']),
      error: (err: any) => console.error('Error al finalizar:', err)
    });
  }

  restartTest(): void {
    this.currentIndex = 0;
    this.answers.clear();
    this.isFinished = false;
    this.loadQuestions();
  }

  getScoreLabel(score: number): string {
    const labels: { [key: number]: string } = {
      1: 'Inconsciente', // Automático / No me doy cuenta
      2: 'Reactivo',    // Reacciono por impulso
      3: 'Consciente',  // Me doy cuenta pero cuesta
      4: 'Intencional', // Elijo el bienestar activamente
      5: 'Pleno'        // Fluye con naturalidad y paz
    };
    return labels[score] || '';
  }
}