import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterModule } from '@angular/router';
import { HttpClient } from '@angular/common/http';
import { MySpaceService } from './services/my-space.service';
import { FamilyStateService } from '../../core/services/family-state.service';
import { TransformationFlowService } from '../../core/services/transformation-flow.service';
import { NarrativeCompanionComponent } from '../../shared/components/narrative-companion.component';

@Component({
  selector: 'app-my-space',
  standalone: true,
  imports: [CommonModule, FormsModule, NarrativeCompanionComponent, RouterModule],
  templateUrl: './my-space.component.html',
  styleUrls: ['./my-space.component.css']
})
export class MySpaceComponent implements OnInit {
  private mySpaceService  = inject(MySpaceService);
  private http            = inject(HttpClient);
  private familyState     = inject(FamilyStateService);
  readonly flow           = inject(TransformationFlowService);

  entries:  any[] = [];
  loading        = false;
  saving         = false;
  identityProfile: any = null;
  errorMessage   = '';
  successMessage = '';
  pendingDeleteId: number | null = null;

  newEntry = {
    title: '',
    content: '',
    emotionalState: 'NEUTRAL',
    category: 'REFLEXION'
  };

  emotionalStates = [
    { value: 'CALM',    label: 'Calmado',  icon: '😊' },
    { value: 'HAPPY',   label: 'Feliz',    icon: '😃' },
    { value: 'SAD',     label: 'Triste',   icon: '😢' },
    { value: 'ANGRY',   label: 'Enojado',  icon: '😠' },
    { value: 'ANXIOUS', label: 'Ansioso',  icon: '😰' },
    { value: 'NEUTRAL', label: 'Neutral',  icon: '😐' }
  ];

  categories = [
    { value: 'REFLEXION', label: 'Reflexión' },
    { value: 'EMOCION',   label: 'Desahogo emocional' },
    { value: 'LOGRO',     label: 'Logro personal' },
    { value: 'DUDA',      label: 'Duda o inquietud' }
  ];

  ngOnInit() {
    this.loadEntries();
    this.loadIdentityProfile();
  }

  loadIdentityProfile() {
    const memberId = this.familyState.currentMemberId();
    if (!memberId) return;
    this.http.get<any>(`/api/members/${memberId}/identity-profile`)
      .subscribe({ next: (res) => { this.identityProfile = res?.data ?? null; } });
  }

  reflexivityLabel(level: number): string {
    return ['', 'Muy impulsivo', 'Algo impulsivo', 'Equilibrado', 'Reflexivo', 'Muy reflexivo'][level] ?? '—';
  }

  sensitivityLabel(level: number): string {
    return ['', 'Muy contenido', 'Algo contenido', 'Equilibrado', 'Sensible', 'Muy sensible'][level] ?? '—';
  }

  resistanceLabel(r: string): string {
    return r === 'LOW' ? 'Baja' : r === 'HIGH' ? 'Alta' : 'Media';
  }

  resistanceColor(r: string): string {
    return r === 'LOW' ? 'text-emerald-400' : r === 'HIGH' ? 'text-red-400' : 'text-amber-400';
  }

  parseJsonArray(json: string | null): string[] {
    if (!json) return [];
    try { return JSON.parse(json); } catch { return []; }
  }

  loadEntries() {
    this.loading = true;
    this.mySpaceService.getEntries().subscribe({
      next: (response: any) => {
        this.entries = response.data ?? response ?? [];
        this.loading = false;
      },
      error: () => {
        this.errorMessage = 'No se pudieron cargar tus reflexiones. Intenta de nuevo.';
        this.loading = false;
      }
    });
  }

  saveEntry() {
    this.errorMessage = '';
    if (!this.newEntry.title.trim() || !this.newEntry.content.trim()) {
      this.errorMessage = 'El título y el contenido son obligatorios.';
      return;
    }

    this.saving = true;
    this.mySpaceService.createEntry(this.newEntry).subscribe({
      next: () => {
        this.saving         = false;
        this.successMessage = '✓ Reflexión guardada en tu espacio privado.';
        this.resetForm();
        this.loadEntries();
        setTimeout(() => { this.successMessage = ''; }, 3500);
      },
      error: () => {
        this.saving       = false;
        this.errorMessage = 'No se pudo guardar la reflexión. Intenta de nuevo.';
      }
    });
  }

  resetForm() {
    this.newEntry = { title: '', content: '', emotionalState: 'NEUTRAL', category: 'REFLEXION' };
  }

  deleteEntry(id: number): void {
    this.pendingDeleteId = id;
  }

  confirmDelete(): void {
    if (this.pendingDeleteId === null) return;
    const id = this.pendingDeleteId;
    this.pendingDeleteId = null;
    this.mySpaceService.deleteEntry(id).subscribe({
      next: () => { this.entries = this.entries.filter((e: any) => e.id !== id); },
      error: () => { this.errorMessage = 'No se pudo eliminar la entrada.'; }
    });
  }

  cancelDelete(): void {
    this.pendingDeleteId = null;
  }

  getEmotionIcon(state: string): string {
    return this.emotionalStates.find(e => e.value === state)?.icon ?? '📝';
  }

  getCategoryLabel(cat: string): string {
    return this.categories.find(c => c.value === cat)?.label ?? cat;
  }
}
