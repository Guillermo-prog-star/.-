import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { CrisisService } from '../../core/services/crisis.service';
import { FamilyStateService } from '../../core/services/family-state.service';

@Component({
  selector: 'app-crisis-page',
  standalone: true,
  imports: [CommonModule, FormsModule],
  template: `
    <div class="page-container" style="animation: fadeIn 0.6s ease-out;">
      <div class="header mb-32">
        <h1 class="gradient-text">🚨 Protocolo Sentinel: Día Crítico</h1>
        <p class="text-muted">Espacio de contención inmediata para momentos de alta tensión familiar.</p>
      </div>

      <div class="grid-2">
        <!-- FORMULARIO DE REPORTE -->
        <div class="glass-card p-32">
          <h3 class="mb-24">¿Qué está sucediendo?</h3>
          
          <div class="form-group mb-20">
            <label>Categoría de la Crisis</label>
            <select [(ngModel)]="crisis.category" class="form-control">
              <option value="Ruptura de Diálogo">Ruptura de Diálogo</option>
              <option value="Tensión Financiera">Tensión Financiera</option>
              <option value="Crisis de Autoridad">Crisis de Autoridad</option>
              <option value="Conflicto de Convivencia">Conflicto de Convivencia</option>
              <option value="Emergencia Emocional">Emergencia Emocional</option>
            </select>
          </div>

          <div class="form-group mb-20">
            <label>Emoción Predominante</label>
            <input type="text" [(ngModel)]="crisis.emotion" placeholder="Ej: Ira, Tristeza, Miedo, Frustración..." class="form-control">
          </div>

          <div class="form-group mb-24">
            <label>Descripción Breve</label>
            <textarea [(ngModel)]="crisis.description" rows="4" placeholder="Describe brevemente lo ocurrido para que el mentor pueda guiarte..." class="form-control"></textarea>
          </div>

          <button (click)="submitCrisis()" [disabled]="loading || !crisis.description" class="btn btn-danger w-full py-16">
            {{ loading ? 'Solicitando Contención...' : 'Activar Protocolo de Ayuda' }}
          </button>
        </div>

        <!-- RESPUESTA DEL MENTOR IA -->
        <div class="response-section">
          @if (lastResponse) {
            <div class="glass-card p-32 ai-response accent-border" style="animation: slideUp 0.5s ease-out;">
              <div class="flex-between mb-20">
                <span class="badge-ai">GUÍA DE CONTENCIÓN ACTIVA</span>
                <span class="text-muted small">{{ lastResponse.createdAt | date:'shortTime' }}</span>
              </div>
              <div class="mentor-content markdown-body" [innerHTML]="formatAiResponse(lastResponse.aiContainmentGuide)"></div>
              <div class="mt-24 p-16 glass-dark rounded text-center italic" style="font-size: 0.9rem;">
                "Recuerden: La crisis es la grieta por donde entra la luz para la transformación."
              </div>
            </div>
          } @else {
            <div class="glass-dark p-60 text-center opacity-50 h-full flex-center flex-col">
              <div style="font-size: 48px;" class="mb-16">🛡️</div>
              <p>El Mentor de Integridad está listo para apoyarlos.<br>Registren el evento para recibir guía.</p>
            </div>
          }
        </div>
      </div>

      <!-- HISTORIAL RECIENTE -->
      <div class="mt-40" *ngIf="history.length > 0">
        <h3 class="mb-20">Historial de Contención</h3>
        <div class="history-grid">
          @for (item of history; track item.id) {
            <div class="glass-card p-20 clickable" (click)="lastResponse = item">
              <div class="flex-between mb-8">
                <span class="f-bold">{{ item.category }}</span>
                <span class="text-muted small">{{ item.createdAt | date:'mediumDate' }}</span>
              </div>
              <p class="text-truncate text-muted small">{{ item.description }}</p>
            </div>
          }
        </div>
      </div>
    </div>
  `,
  styles: [`
    .grid-2 { display: grid; grid-template-columns: 1fr 1fr; gap: 32px; }
    .accent-border { border: 1px solid var(--accent); box-shadow: 0 0 30px rgba(var(--accent-rgb), 0.2); }
    .ai-response { background: rgba(var(--accent-rgb), 0.05) !important; }
    .history-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(280px, 1fr)); gap: 16px; }
    .markdown-body { line-height: 1.6; }
    .markdown-body ::ng-deep h3 { margin-top: 20px; color: var(--accent); border-bottom: 1px solid rgba(255,255,255,0.1); padding-bottom: 8px; font-size: 1.1rem; }
  `]
})
export class CrisisPageComponent implements OnInit {
  private crisisService = inject(CrisisService);
  private familyState = inject(FamilyStateService);

  crisis = { category: 'Conflicto de Convivencia', emotion: '', description: '' };
  loading = false;
  lastResponse: any = null;
  history: any[] = [];

  ngOnInit() {
    this.loadHistory();
  }

  loadHistory() {
    const familyId = this.familyState.getSelectedFamilyId();
    if (familyId) {
      this.crisisService.getHistory(familyId).subscribe(res => this.history = res);
    }
  }

  submitCrisis() {
    const familyId = this.familyState.getSelectedFamilyId();
    if (!familyId) return;

    this.loading = true;
    const payload = { ...this.crisis, familyId };

    this.crisisService.reportCrisis(payload).subscribe({
      next: (res) => {
        this.lastResponse = res;
        this.loading = false;
        this.crisis = { category: 'Conflicto de Convivencia', emotion: '', description: '' };
        this.loadHistory();
      },
      error: () => this.loading = false
    });
  }

  formatAiResponse(text: string) {
    if (!text) return '';
    return text.replace(/\n/g, '<br>').replace(/## (.*)/g, '<h3>$1</h3>');
  }
}
