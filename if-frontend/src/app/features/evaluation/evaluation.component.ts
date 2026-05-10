import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { AssessmentService } from '../../core/services/assessment.service';
import { Question } from '../../core/models/question.model';

@Component({
  selector: 'app-evaluation',
  standalone: true,
  imports: [CommonModule, RouterLink],
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
  isTransitioning: boolean = false;
  isLoadingResults: boolean = false;
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
    if (!this.questions[this.currentIndex] || this.isTransitioning) return;
    
    const currentQuestion = this.questions[this.currentIndex];
    this.answers.set(currentQuestion.id, score);
    
    console.log(`Paso ${this.currentIndex + 1}: Respuesta ${score} guardada.`);

    this.isTransitioning = true;
    setTimeout(() => {
      if (this.currentIndex < this.questions.length - 1) {
        this.currentIndex++;
        this.isTransitioning = false;
      } else {
        this.isTransitioning = false;
        this.sendResults();
      }
    }, 350);
  }

  prevQuestion(): void {
    if (this.currentIndex > 0 && !this.isTransitioning) {
      this.isTransitioning = true;
      setTimeout(() => {
        this.currentIndex--;
        this.isTransitioning = false;
      }, 200);
    }
  }

  sendResults(): void {
    this.isFinished = true;
    this.isLoadingResults = true;
    
    const answerList = Array.from(this.answers.keys()).map(qId => ({
      questionId: Number(qId),
      answerValue: Number(this.answers.get(qId))
    }));

    this.assessmentService.finalizeEvaluation(this.evaluationId, { answers: answerList }).subscribe({
      next: () => {
        console.log('✅ Evaluación finalizada. Redirigiendo...');
        this.isLoadingResults = false;
        this.router.navigate(['/plans']);
      },
      error: (err: any) => {
        console.error('❌ Error al finalizar:', err);
        this.isLoadingResults = false;
        this.isFinished = false; // Permitir reintentar si falla
        alert('Error al finalizar el diagnóstico: ' + (err.error?.message || err.message));
      }
    });
  }

  restartTest(): void {
    this.currentIndex = 0;
    this.answers.clear();
    this.isFinished = false;
    this.isTransitioning = false;
    this.isLoadingResults = false;
    this.loadQuestions();
  }

  // --- Helpers de Taxonomía y Diseño Premium ---

  getDimensionColor(dimension: string): string {
    const normalized = (dimension || '').toLowerCase();
    switch(normalized) {
      case 'comunicacion': return 'var(--dim-communication)';
      case 'emociones': return 'var(--dim-emotions)';
      case 'habitos': return 'var(--dim-habits)';
      case 'tiempos': return 'var(--dim-times)';
      default: return 'var(--accent)';
    }
  }

  getDimensionColorGlow(dimension: string): string {
    const normalized = (dimension || '').toLowerCase();
    switch(normalized) {
      case 'comunicacion': return 'rgba(56, 189, 248, 0.2)';
      case 'emociones': return 'rgba(251, 113, 133, 0.2)';
      case 'habitos': return 'rgba(251, 191, 36, 0.2)';
      case 'tiempos': return 'rgba(167, 139, 250, 0.2)';
      default: return 'rgba(168, 85, 247, 0.2)';
    }
  }

  getDimensionName(dimension: string): string {
    const normalized = (dimension || '').toLowerCase();
    switch(normalized) {
      case 'comunicacion': return 'Comunicación Asertiva';
      case 'emociones': return 'Regulación & Clima Emocional';
      case 'habitos': return 'Hábitos & Convivencia Colectiva';
      case 'tiempos': return 'Tiempos de Conexión Activa';
      default: return dimension || 'Consciencia Familiar';
    }
  }

  getQuestionTypeLabel(type: string | undefined): string {
    const normalized = (type || '').toUpperCase();
    switch(normalized) {
      case 'CORE': return 'Medición Longitudinal Base';
      case 'ADAPTIVE': return 'Profundización por Vulnerabilidad';
      case 'FASE_PILLAR': return 'Evaluación de Hito Temporal';
      case 'MIRROR': return 'Control de Consistencia Interna';
      case 'EXPLORATORY': return 'Exploración de Entorno IA';
      default: return 'Reactivo de Consciencia';
    }
  }

  getQuestionTypeDesc(type: string | undefined): string {
    const normalized = (type || '').toUpperCase();
    switch(normalized) {
      case 'CORE': return 'Mide la evolución histórica constante de los pilares del hogar.';
      case 'ADAPTIVE': return 'Adaptada para indagar a fondo la dimensión con mayor riesgo detectado.';
      case 'FASE_PILLAR': return 'Alineada al hito temporal de la ruta de transformación familiar.';
      case 'MIRROR': return 'Validador psicométrico para asegurar la sinceridad y consistencia clínica.';
      case 'EXPLORATORY': return 'Análisis predictivo de patrones emergentes asistido por Inteligencia Artificial.';
      default: return 'Reactivo de diagnóstico familiar.';
    }
  }

  getSeverityLevel(weight: number | undefined): string {
    const val = weight || 0.5;
    if (val >= 0.8) return 'Impacto Crítico';
    if (val >= 0.6) return 'Impacto Alto';
    return 'Impacto Estructural';
  }

  getSeverityStars(weight: number | undefined): number[] {
    const val = weight || 0.5;
    const count = val >= 0.8 ? 3 : (val >= 0.6 ? 2 : 1);
    return Array(count).fill(0);
  }

  getScoreLabel(score: number): string {
    const labels: { [key: number]: string } = {
      1: 'Inconsciente', 
      2: 'Reactivo',    
      3: 'Consciente',  
      4: 'Intencional', 
      5: 'Pleno'        
    };
    return labels[score] || '';
  }

  getScoreDetail(score: number): string {
    const details: { [key: number]: string } = {
      1: 'No nos damos cuenta o lo vemos normal en el día a día.',
      2: 'Reaccionamos por impulso cuando ocurre, sin control previo.',
      3: 'Nos damos cuenta del patrón pero nos cuesta mucho gestionarlo.',
      4: 'Elegimos activamente actuar por el bienestar del hogar.',
      5: 'Fluye de manera natural con paz profunda y amor voluntario.'
    };
    return details[score] || '';
  }
}