import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';
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
  // Inyección de dependencias moderna
  private assessmentService = inject(AssessmentService);
  private router = inject(Router);

  // Estado del Test
  questions: Question[] = [];
  currentIndex: number = 0;
  answers: Map<number, number> = new Map();
  isFinished: boolean = false;

  // Contexto de la Familia
  familyId: number = 0;
  familyName: string = '';

  ngOnInit(): void {
    // 1. Recuperamos el contexto del LocalStorage
    this.familyId = Number(localStorage.getItem('selectedFamilyId') || 0);
    this.familyName = localStorage.getItem('selectedFamilyName') || 'la familia';

    // 2. Validación de seguridad: si no hay familia, volvemos a la lista
    if (this.familyId === 0) {
      console.warn('⚠️ Intento de acceso sin familia seleccionada.');
      this.router.navigate(['/families']);
      return;
    }

    this.loadQuestions();
  }

  loadQuestions(): void {
    this.assessmentService.getRandomQuestions().subscribe({
      next: (data) => {
        this.questions = data;
        console.log(`✅ ${this.questions.length} preguntas listas para: ${this.familyName}`);
      },
      error: (err) => console.error('❌ Error al conectar con el Backend:', err)
    });
  }

  selectAnswer(score: number): void {
    if (!this.questions[this.currentIndex]) return;

    const currentQuestion = this.questions[this.currentIndex];
    this.answers.set(currentQuestion.id, score);
    
    if (this.currentIndex < this.questions.length - 1) {
      this.currentIndex++;
    } else {
      this.isFinished = true;
      this.sendResults();
    }
  }

  sendResults(): void {
    const payload = {
      familyId: this.familyId,
      answers: Object.fromEntries(this.answers),
      completedAt: new Date().toISOString()
    };
    
    console.log('📦 Payload final para el Backend:', payload);
    
    // TODO: Implementar en el servicio: 
    // this.assessmentService.submit(payload).subscribe(...)
  }

  restartTest(): void {
    this.currentIndex = 0;
    this.answers.clear();
    this.isFinished = false;
    this.loadQuestions();
  }
}