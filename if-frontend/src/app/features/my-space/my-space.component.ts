import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterModule } from '@angular/router';
import { MySpaceService } from './services/my-space.service';
import { NarrativeCompanionComponent } from '../../shared/components/narrative-companion.component';

@Component({
  selector: 'app-my-space',
  standalone: true,
  imports: [CommonModule, FormsModule, NarrativeCompanionComponent, RouterModule],
  templateUrl: './my-space.component.html',
  styleUrls: ['./my-space.component.css']
})
export class MySpaceComponent implements OnInit {
  private mySpaceService = inject(MySpaceService);

  entries:  any[] = [];
  loading       = false;
  saving        = false;
  errorMessage  = '';
  successMessage = '';

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
    if (!confirm('¿Eliminar esta reflexión? No se puede deshacer.')) return;
    this.mySpaceService.deleteEntry(id).subscribe({
      next: () => { this.entries = this.entries.filter((e: any) => e.id !== id); },
      error: () => { this.errorMessage = 'No se pudo eliminar la entrada.'; }
    });
  }

  getEmotionIcon(state: string): string {
    return this.emotionalStates.find(e => e.value === state)?.icon ?? '📝';
  }

  getCategoryLabel(cat: string): string {
    return this.categories.find(c => c.value === cat)?.label ?? cat;
  }
}
