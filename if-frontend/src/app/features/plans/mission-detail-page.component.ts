import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { HttpClient } from '@angular/common/http';
import { ApiService } from '../../core/services/api.service';
import { FamilyStateService } from '../../core/services/family-state.service';
import { TransformationFlowService } from '../../core/services/transformation-flow.service';
import { forkJoin } from 'rxjs';

@Component({
  selector: 'app-mission-detail-page',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink],
  templateUrl: './mission-detail-page.component.html',
  styleUrls: ['./mission-detail-page.component.css']
})
export class MissionDetailPageComponent implements OnInit {
  private route        = inject(ActivatedRoute);
  private router       = inject(Router);
  private http         = inject(HttpClient);
  private api          = inject(ApiService);
  private familyState  = inject(FamilyStateService);
  private flow         = inject(TransformationFlowService);

  task: any = null;
  evidences: any[] = [];
  loading    = true;
  submitting = false;
  submitted  = false;
  errorMsg   = '';

  familyMembers: any[] = [];

  form = {
    title:        '',
    description:  '',
    textContent:  '',
    fileUrl:      '',
    evidenceType: 'BITACORA',
    submittedBy:  '',
    feelingEmoji: ''
  };

  readonly emojis = ['😊','🙌','💪','❤️','🌱','🎯','✨','🙏'];

  get familyId() { return this.familyState.currentFamilyId(); }

  ngOnInit(): void {
    const taskId = this.route.snapshot.paramMap.get('taskId');
    if (!taskId) { this.router.navigate(['/plans']); return; }

    forkJoin({
      task:    this.http.get<any>(`${this.api.base}/plans/tasks/${taskId}`),
      evs:     this.http.get<any>(`${this.api.base}/evidences/task/${taskId}`),
      members: this.http.get<any>(`${this.api.base}/members/family/${this.familyId}`)
    }).subscribe({
      next: ({ task, evs, members }) => {
        this.task     = task?.data ?? task;
        this.evidences = (evs?.data ?? evs ?? []).sort(
          (a: any, b: any) => new Date(b.createdAt).getTime() - new Date(a.createdAt).getTime()
        );
        this.familyMembers = members?.data ?? [];
        this.form.title       = `Evidencia: ${this.task?.title ?? ''}`;
        this.form.submittedBy = this.familyMembers[0]?.fullName ?? '';
        this.loading = false;
        // Registrar misión activa en el estado de transformación
        if (this.task?.id) this.flow.setActiveMission(String(this.task.id));
      },
      error: () => {
        this.errorMsg = 'No se pudo cargar la misión.';
        this.loading  = false;
      }
    });
  }

  submit(): void {
    if (!this.isFormValid()) return;
    this.submitting = true;
    this.errorMsg   = '';

    let textPayload = this.form.textContent;
    if (this.form.feelingEmoji) {
      textPayload = `[Cohesión Emocional: ${this.form.feelingEmoji}] ${textPayload}`;
    }

    const payload = {
      taskId:       this.task.id,
      familyId:     this.familyId,
      evidenceType: this.form.evidenceType,
      title:        this.form.title,
      description:  this.form.description,
      fileUrl:      this.form.fileUrl,
      textContent:  textPayload,
      submittedBy:  this.form.submittedBy
    };

    this.http.post<any>(`${this.api.base}/evidences/submit`, payload).subscribe({
      next: (res) => {
        this.submitting = false;
        this.submitted  = true;
        const newEv     = res?.data ?? res;
        if (newEv) this.evidences = [newEv, ...this.evidences];
        this.resetForm();
        setTimeout(() => { this.submitted = false; }, 5000);
      },
      error: () => {
        this.submitting = false;
        this.errorMsg   = 'No se pudo enviar la evidencia. Intenta de nuevo.';
      }
    });
  }

  private resetForm(): void {
    this.form = {
      title:        `Evidencia: ${this.task?.title ?? ''}`,
      description:  '',
      textContent:  '',
      fileUrl:      '',
      evidenceType: 'BITACORA',
      submittedBy:  this.familyMembers[0]?.fullName ?? '',
      feelingEmoji: ''
    };
  }

  isFormValid(): boolean {
    if (!this.form.title.trim() || !this.form.submittedBy.trim()) return false;
    if (this.form.evidenceType === 'BITACORA') return !!this.form.textContent.trim();
    if (this.form.evidenceType === 'PHOTO')    return !!this.form.fileUrl.trim();
    return true;
  }

  statusLabel(status: string): string {
    return status === 'VALIDATED' ? 'Aprobada' : status === 'REJECTED' ? 'Rechazada' : 'Pendiente';
  }

  statusClass(status: string): string {
    return status === 'VALIDATED' ? 'status-ok' : status === 'REJECTED' ? 'status-no' : 'status-wait';
  }

  scoreColor(score: number | null): string {
    if (score == null) return '';
    if (score >= 70) return 'score-high';
    if (score >= 40) return 'score-mid';
    return 'score-low';
  }

  dimensionColor(dim: string): string {
    const d = (dim ?? '').toUpperCase();
    if (d.includes('COMUNICACION') || d.includes('COMUNICACIÓN')) return 'dim-blue';
    if (d.includes('EMOCIONES'))    return 'dim-purple';
    if (d.includes('HABITOS') || d.includes('HÁBITOS')) return 'dim-green';
    if (d.includes('TIEMPOS'))      return 'dim-amber';
    return 'dim-default';
  }
}
