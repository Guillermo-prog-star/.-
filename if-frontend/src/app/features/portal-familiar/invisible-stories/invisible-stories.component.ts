import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { HttpClient } from '@angular/common/http';
import { EmotionalEngineService, EmotionalStimulus, EmotionalInferenceDto } from '../../../core/services/emotional-engine.service';
import { FamilyStateService } from '../../../core/services/family-state.service';
import { ApiService } from '../../../core/services/api.service';
import { NarrativeCompanionComponent } from '../../../shared/components/narrative-companion.component';

@Component({
  selector: 'app-invisible-stories',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink, NarrativeCompanionComponent],
  templateUrl: './invisible-stories.component.html',
  styleUrls: ['./invisible-stories.component.css']
})
export class InvisibleStoriesComponent implements OnInit {
  private emotionalService = inject(EmotionalEngineService);
  private familyState = inject(FamilyStateService);
  private http = inject(HttpClient);
  private api = inject(ApiService);
  private router = inject(Router);

  // Estados
  familyId = 0;
  familyName = '';
  members: any[] = [];
  selectedMemberId: number | null = null;

  activeStimulus: EmotionalStimulus | null = null;
  reflectionText = '';
  emotionalScore = 3; // Nivel de presencia autodeclarada (1-5)

  loadingStimulus = false;
  submitting = false;
  success = false;
  errorMessage = '';

  // Resultado de la inferencia de Claude
  inferenceResult: EmotionalInferenceDto | null = null;

  // Quantum loader steps
  loaderStep = 0;
  loaderSteps = [
    'Sincronizando con el Nodo Central...',
    'Analizando métricas de Empatía...',
    'Evaluando Desconexión y Presencia Activa...',
    'Orquestando micro-misión familiar con Claude IA...'
  ];

  get completionPercentage(): number {
    return Math.min(100, Math.round((this.reflectionText.length / 20) * 100));
  }

  ngOnInit(): void {
    this.familyId   = this.familyState.getSelectedFamilyId();
    this.familyName = this.familyState.currentFamilyName() || 'Familia';

    if (this.familyId > 0) {
      this.loadStimulus();
      this.loadMembers();
    } else {
      this.errorMessage = 'No se ha detectado ningún nodo familiar activo. Por favor, selecciona una familia primero.';
    }
  }

  loadStimulus(): void {
    this.loadingStimulus = true;
    this.emotionalService.getActiveStimulus().subscribe({
      next: (stimulus) => {
        this.activeStimulus = stimulus;
        this.loadingStimulus = false;
      },
      error: () => {
        this.errorMessage = 'No se pudo cargar el estímulo reflexivo activo.';
        this.loadingStimulus = false;
      }
    });
  }

  loadMembers(): void {
    this.http.get<any>(`${this.api.base}/members/family/${this.familyId}`).subscribe({
      next: (res) => {
        this.members = res?.data || [];
        if (this.members.length > 0) {
          this.selectedMemberId = this.members[0].id;
        }
      },
      error: () => {
        console.error('Error cargando miembros de la familia.');
      }
    });
  }

  setScore(score: number): void {
    if (this.submitting) return;
    this.emotionalScore = score;
  }

  submitReflection(): void {
    if (!this.activeStimulus || !this.selectedMemberId || this.reflectionText.length < 20) {
      return;
    }

    this.submitting = true;
    this.errorMessage = '';
    this.inferenceResult = null;
    this.loaderStep = 0;

    // Animación de pasos del Quantum Loader
    const stepInterval = setInterval(() => {
      if (this.loaderStep < this.loaderSteps.length - 1) {
        this.loaderStep++;
      }
    }, 1200);

    const payload = {
      familyId: this.familyId,
      memberId: this.selectedMemberId,
      reflection: this.reflectionText,
      emotionalScore: this.emotionalScore
    };

    this.emotionalService.submitReflection(this.activeStimulus.id, payload).subscribe({
      next: (res) => {
        clearInterval(stepInterval);
        this.loaderStep = this.loaderSteps.length; // Fin del cargado
        setTimeout(() => {
          this.inferenceResult = res;
          this.submitting = false;
          this.success = true;
        }, 600);
      },
      error: (err) => {
        clearInterval(stepInterval);
        this.submitting = false;
        this.errorMessage = 'Ocurrió un error al procesar tu reflexión en el motor cognitivo.';
      }
    });
  }

  resetForm(): void {
    this.reflectionText = '';
    this.emotionalScore = 3;
    this.success = false;
    this.inferenceResult = null;
  }

  goToPortal(): void {
    this.router.navigate(['/portal']);
  }
}
