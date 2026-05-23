import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MySpaceService } from './services/my-space.service';
import { NarrativeCompanionComponent } from '../../shared/components/narrative-companion.component';

@Component({
  selector: 'app-my-space',
  standalone: true,
  imports: [CommonModule, FormsModule, NarrativeCompanionComponent],
  templateUrl: './my-space.component.html',
  styleUrls: ['./my-space.component.css']
})
export class MySpaceComponent implements OnInit {
  private mySpaceService = inject(MySpaceService);

  entries: any[] = [];
  loading = false;

  newEntry = {
    title: '',
    content: '',
    emotionalState: 'NEUTRAL',
    category: 'REFLEXION'
  };

  emotionalStates = [
    { value: 'CALM', label: 'Calmado', icon: '😊' },
    { value: 'HAPPY', label: 'Feliz', icon: '😃' },
    { value: 'SAD', label: 'Triste', icon: '😢' },
    { value: 'ANGRY', label: 'Enojado', icon: '😠' },
    { value: 'ANXIOUS', label: 'Ansioso', icon: '😰' },
    { value: 'NEUTRAL', label: 'Neutral', icon: '😐' }
  ];

  categories = [
    { value: 'REFLEXION', label: 'Reflexión' },
    { value: 'EMOCION', label: 'Desahogo Emocional' },
    { value: 'LOGRO', label: 'Logro Personal' },
    { value: 'DUDA', label: 'Duda o Inquietud' }
  ];

  ngOnInit() {
    this.loadEntries();
  }

  loadEntries() {
    this.loading = true;
    this.mySpaceService.getEntries().subscribe({
      next: (response: any) => {
        this.entries = response.data || response;
        this.loading = false;
      },
      error: (err: any) => {
        console.error('Error cargando entradas:', err);
        this.loading = false;
      }
    });
  }

  saveEntry() {
    if (!this.newEntry.title || !this.newEntry.content) {
      alert('Por favor completa el título y el contenido.');
      return;
    }

    this.loading = true;
    this.mySpaceService.createEntry(this.newEntry).subscribe({
      next: (response: any) => {
        this.loadEntries();
        this.resetForm();
        this.loading = false;
      },
      error: (err: any) => {
        console.error('Error guardando entrada:', err);
        this.loading = false;
        alert('No se pudo guardar la entrada.');
      }
    });
  }

  resetForm() {
    this.newEntry = {
      title: '',
      content: '',
      emotionalState: 'NEUTRAL',
      category: 'REFLEXION'
    };
  }

  getEmotionIcon(state: string): string {
    const found = this.emotionalStates.find(e => e.value === state);
    return found ? found.icon : '📝';
  }
}
