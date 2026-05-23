import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { CrisisService } from '../../core/services/crisis.service';
import { FamilyStateService } from '../../core/services/family-state.service';
import { NarrativeCompanionComponent } from '../../shared/components/narrative-companion.component';

@Component({
  selector: 'app-crisis-page',
  standalone: true,
  imports: [CommonModule, FormsModule, NarrativeCompanionComponent],
  template: `
    <div class="crisis-container" style="animation: fadeIn 0.6s cubic-bezier(0.16, 1, 0.3, 1);">
      
      <!-- CABECERA -->
      <div class="header-section mb-32">
        <div class="sentinel-pulse-badge">
          <span class="pulse-dot"></span>
          🛡️ Guardián Sentinel Activo
        </div>
        <h1 class="gradient-text text-4xl font-extrabold tracking-tight">Protocolo Sentinel: Día Crítico</h1>
        <p class="text-muted mt-8 text-lg" style="color: var(--text-dim);">
          Un espacio de contención clínica inmediata diseñado para momentos de alta tensión familiar.
        </p>
      </div>

      <!-- Narrative Guidance Engine -->
      <app-narrative-companion module="crisis"></app-narrative-companion>

      <div class="grid-2">
        <!-- FORMULARIO DE REPORTE -->
        <div class="glass-card p-32 form-card-glow" style="animation: slideUp 0.5s cubic-bezier(0.16, 1, 0.3, 1);">
          <h3 class="mb-24 text-xl font-bold flex items-center gap-12 text-white">
            <span style="font-size: 1.4rem;">📝</span> Registrar Incidente
          </h3>
          
          <!-- CATEGORÍA -->
          <div class="glass-input-group mb-24">
            <label class="glass-input-label">Categoría de la Crisis</label>
            <select [(ngModel)]="crisis.category" class="glass-input">
              <option value="Ruptura de Diálogo">🗣️ Ruptura de Diálogo</option>
              <option value="Tensión Financiera">💵 Tensión Financiera</option>
              <option value="Crisis de Autoridad">👑 Crisis de Autoridad</option>
              <option value="Conflicto de Convivencia">🤝 Conflicto de Convivencia</option>
              <option value="Emergencia Emocional">🚨 Emergencia Emocional</option>
            </select>
          </div>

          <!-- EMOCIÓN PREDOMINANTE -->
          <div class="glass-input-group mb-24">
            <label class="glass-input-label">Emoción Predominante</label>
            <input 
              type="text" 
              [(ngModel)]="crisis.emotion" 
              placeholder="Escribe o selecciona abajo..." 
              class="glass-input"
            >
            <div class="emotion-tags-container">
              @for (tag of emotionTags; track tag) {
                <button 
                  (click)="selectEmotion(tag)" 
                  [class.active]="crisis.emotion === tag" 
                  class="emotion-pill"
                >
                  {{ tag }}
                </button>
              }
            </div>
          </div>

          <!-- DESCRIPCIÓN -->
          <div class="glass-input-group mb-32">
            <label class="glass-input-label">Descripción Breve</label>
            <textarea 
              [(ngModel)]="crisis.description" 
              rows="4" 
              placeholder="Describe lo ocurrido brevemente. Tu descripción será procesada clínicamente para darte pasos específicos de calma..." 
              class="glass-input"
            ></textarea>
            <div class="text-right text-xs mt-4" style="color: rgba(255,255,255,0.3);">
              {{ crisis.description.length }} caracteres
            </div>
          </div>

          <button 
            (click)="submitCrisis()" 
            [disabled]="loading || !crisis.description" 
            class="btn-sentinel w-full"
          >
            @if (loading) {
              <div class="flex items-center justify-center gap-8">
                <span class="loading-spinner"></span>
                <span>Invocando Inteligencia Sentinel...</span>
              </div>
            } @else {
              🔥 Activar Contención Inmediata
            }
          </button>
        </div>

        <!-- RESPUESTA DEL MENTOR IA -->
        <div class="response-section">
          @if (lastResponse) {
            <div class="glass-card p-32 ai-guide-card" style="animation: slideUp 0.6s cubic-bezier(0.16, 1, 0.3, 1);">
              <div class="flex-between mb-24 pb-16" style="border-bottom: 1px solid rgba(255,255,255,0.08);">
                <div class="flex items-center gap-12">
                  <span class="badge-sentinel-active">🚨 GUÍA DE CONTENCIÓN</span>
                  <span class="category-badge-pill" [style.--card-accent-color]="getCategoryColor(lastResponse.category)">
                    {{ lastResponse.category }}
                  </span>
                </div>
                <span class="text-muted small" style="color: var(--text-dim);">{{ lastResponse.createdAt | date:'shortTime' }}</span>
              </div>
              
              <div class="mentor-content markdown-body mb-24" [innerHTML]="formatAiResponse(lastResponse.aiContainmentGuide)"></div>
              
              <!-- CITA DE REFLEXIÓN -->
              <div class="quote-container-glass mb-24">
                "Recuerden: La crisis es la grieta por donde entra la luz para la transformación familiar."
              </div>

              <!-- CHECKLIST DE CONTENCIÓN -->
              <div class="checklist-container">
                <h4 class="checklist-title">
                  <span>🎯</span> Práctica del Protocolo Familiar
                </h4>
                <div class="checklist-item" (click)="step1Ticked = !step1Ticked">
                  <div class="checklist-checkbox" [class.checked]="step1Ticked">
                    <div class="checklist-checkbox-inner"></div>
                  </div>
                  <span class="checklist-text" [class.checked]="step1Ticked">
                    <strong>Paso 1:</strong> Detención de pensamiento (pausa de 2 minutos de silencio consciente).
                  </span>
                </div>
                <div class="checklist-item" (click)="step2Ticked = !step2Ticked">
                  <div class="checklist-checkbox" [class.checked]="step2Ticked">
                    <div class="checklist-checkbox-inner"></div>
                  </div>
                  <span class="checklist-text" [class.checked]="step2Ticked">
                    <strong>Paso 2:</strong> Validación mutua (cada miembro comparte su emoción en 1 frase corta sin interrupciones).
                  </span>
                </div>
                <div class="checklist-item" (click)="step3Ticked = !step3Ticked">
                  <div class="checklist-checkbox" [class.checked]="step3Ticked">
                    <div class="checklist-checkbox-inner"></div>
                  </div>
                  <span class="checklist-text" [class.checked]="step3Ticked">
                    <strong>Paso 3:</strong> Sellar un acuerdo mínimo para buscar la calma del nodo familiar.
                  </span>
                </div>

                @if (step1Ticked && step2Ticked && step3Ticked) {
                  <div class="success-celebration-banner">
                    <p class="text-white font-bold" style="font-size: 0.95rem;">
                      💚 ¡Excelente trabajo de co-regulación! Han completado el protocolo de contención. La calma está retornando al hogar.
                    </p>
                  </div>
                }
              </div>
            </div>
          } @else {
            <div class="placeholder-container">
              <div class="shield-icon-floating">🛡️</div>
              <h4 class="placeholder-title">Mentor Clínico Disponible</h4>
              <p class="placeholder-desc">
                Registra cualquier incidente de tensión o desentendimiento familiar. El Guardián Sentinel analizará el contexto para brindarte de inmediato pautas terapéuticas de contención adaptadas.
              </p>
            </div>
          }
        </div>
      </div>

      <!-- HISTORIAL RECIENTE -->
      <div class="mt-48" *ngIf="history.length > 0">
        <h3 class="history-section-title">
          <span>🕒</span> Historial de Contención Reciente
        </h3>
        <div class="history-grid">
          @for (item of history; track item.id) {
            <div 
              class="history-card-glass" 
              [style.--card-accent-color]="getCategoryColor(item.category)"
              (click)="selectHistoryItem(item)"
            >
              <div class="flex-between mb-12">
                <span class="category-badge-pill" [style.--card-accent-color]="getCategoryColor(item.category)">
                  {{ item.category }}
                </span>
                <span class="text-muted small" style="color: var(--text-dim); font-size: 0.8rem;">
                  {{ item.createdAt | date:'mediumDate' }}
                </span>
              </div>
              <p class="font-bold text-white mb-8" style="font-size: 0.95rem;">
                Emoción: {{ item.emotion || 'No registrada' }}
              </p>
              <p class="text-truncate text-muted small" style="color: var(--text-dim); line-height: 1.4;">
                {{ item.description }}
              </p>
              <div class="mt-16 text-right" style="font-size: 0.8rem; color: var(--accent); font-weight: 600;">
                Ver guía completa →
              </div>
            </div>
          }
        </div>
      </div>
    </div>
  `,
  styles: [`
    .crisis-container {
      max-width: 1200px;
      margin: 0 auto;
      padding: 24px;
    }
    .header-section {
      text-align: left;
    }
    .sentinel-pulse-badge {
      display: inline-flex;
      align-items: center;
      gap: 10px;
      background: rgba(239, 68, 68, 0.12);
      border: 1px solid rgba(239, 68, 68, 0.3);
      color: #f87171;
      padding: 6px 16px;
      border-radius: 99px;
      font-size: 0.85rem;
      font-weight: 700;
      letter-spacing: 0.5px;
      text-transform: uppercase;
      margin-bottom: 16px;
      animation: soft-pulse 2s infinite;
    }
    .pulse-dot {
      width: 8px;
      height: 8px;
      background-color: #ef4444;
      border-radius: 50%;
      display: inline-block;
    }
    @keyframes soft-pulse {
      0% { box-shadow: 0 0 0 0 rgba(239, 68, 68, 0.4); }
      70% { box-shadow: 0 0 0 10px rgba(239, 68, 68, 0); }
      100% { box-shadow: 0 0 0 0 rgba(239, 68, 68, 0); }
    }
    .grid-2 { display: grid; grid-template-columns: 1fr 1fr; gap: 32px; }
    
    @media (max-width: 768px) {
      .grid-2 { grid-template-columns: 1fr; gap: 24px; }
    }

    /* INPUTS & FORM STYLES */
    .form-card-glow:hover {
      box-shadow: 0 0 35px rgba(239, 68, 68, 0.12);
      border-color: rgba(239, 68, 68, 0.25);
    }
    .glass-input-group {
      display: flex;
      flex-direction: column;
      gap: 8px;
    }
    .glass-input-label {
      font-size: 0.85rem;
      font-weight: 600;
      color: var(--text-dim);
      letter-spacing: 0.5px;
      text-transform: uppercase;
    }
    .glass-input {
      background: rgba(15, 23, 42, 0.5) !important;
      border: 1px solid rgba(255, 255, 255, 0.1) !important;
      border-radius: 12px !important;
      padding: 12px 16px !important;
      color: var(--text-bright) !important;
      font-family: inherit !important;
      font-size: 0.95rem !important;
      transition: all 0.3s ease !important;
      backdrop-filter: blur(8px) !important;
      width: 100%;
    }
    .glass-input::placeholder {
      color: rgba(255, 255, 255, 0.25);
    }
    .glass-input:focus {
      outline: none !important;
      border-color: rgba(239, 68, 68, 0.5) !important;
      box-shadow: 0 0 15px rgba(239, 68, 68, 0.25) !important;
      background: rgba(15, 23, 42, 0.75) !important;
    }
    select.glass-input option {
      background: #0f172a;
      color: white;
    }

    /* EMOTION TAGS */
    .emotion-tags-container {
      display: flex;
      flex-wrap: wrap;
      gap: 8px;
      margin-top: 10px;
    }
    .emotion-pill {
      background: rgba(255, 255, 255, 0.04);
      border: 1px solid rgba(255, 255, 255, 0.08);
      color: var(--text-dim);
      padding: 6px 14px;
      border-radius: 20px;
      font-size: 0.8rem;
      font-weight: 600;
      cursor: pointer;
      transition: all 0.2s cubic-bezier(0.16, 1, 0.3, 1);
    }
    .emotion-pill:hover, .emotion-pill.active {
      background: rgba(239, 68, 68, 0.12);
      border-color: rgba(239, 68, 68, 0.4);
      color: #f87171;
      transform: translateY(-2px);
    }

    /* BTN SENTINEL */
    .btn-sentinel {
      background: linear-gradient(135deg, #ef4444, #ec4899, #f43f5e);
      color: white;
      font-weight: 700;
      border: none;
      border-radius: 12px;
      padding: 16px;
      cursor: pointer;
      font-size: 1rem;
      letter-spacing: 0.5px;
      transition: all 0.3s cubic-bezier(0.175, 0.885, 0.32, 1.275);
      box-shadow: 0 4px 20px rgba(239, 68, 68, 0.3);
      text-transform: uppercase;
      font-size: 0.9rem;
    }
    .btn-sentinel:hover:not(:disabled) {
      transform: translateY(-3px) scale(1.01);
      box-shadow: 0 10px 25px rgba(239, 68, 68, 0.45);
      filter: brightness(1.05);
    }
    .btn-sentinel:active:not(:disabled) {
      transform: translateY(1px);
    }
    .btn-sentinel:disabled {
      opacity: 0.4;
      cursor: not-allowed;
      box-shadow: none;
    }

    /* LOADING SPINNER */
    .loading-spinner {
      width: 18px;
      height: 18px;
      border: 2px solid rgba(255,255,255,0.3);
      border-top-color: white;
      border-radius: 50%;
      display: inline-block;
      animation: spin 0.8s linear infinite;
    }
    @keyframes spin {
      to { transform: rotate(360deg); }
    }

    /* PLACEHOLDER CARD */
    .placeholder-container {
      background: rgba(15, 23, 42, 0.45);
      border: 1px dashed rgba(255, 255, 255, 0.12);
      border-radius: var(--radius);
      padding: 60px 40px;
      text-align: center;
      display: flex;
      flex-direction: column;
      align-items: center;
      justify-content: center;
      height: 100%;
      min-height: 420px;
      position: relative;
      overflow: hidden;
      box-shadow: var(--glass-shadow);
      backdrop-filter: var(--glass-blur);
    }
    .placeholder-container::before {
      content: '';
      position: absolute;
      top: -50%;
      left: -50%;
      width: 200%;
      height: 200%;
      background: radial-gradient(circle, rgba(168, 85, 247, 0.05) 0%, transparent 60%);
      animation: slow-rotation 25s linear infinite;
    }
    @keyframes slow-rotation {
      from { transform: rotate(0deg); }
      to { transform: rotate(360deg); }
    }
    .shield-icon-floating {
      font-size: 3.5rem;
      margin-bottom: 24px;
      animation: float 4s ease-in-out infinite;
      z-index: 1;
      filter: drop-shadow(0 0 15px rgba(99, 102, 241, 0.3));
    }
    @keyframes float {
      0% { transform: translateY(0px) rotate(0deg); }
      50% { transform: translateY(-10px) rotate(2deg); }
      100% { transform: translateY(0px) rotate(0deg); }
    }
    .placeholder-title {
      font-size: 1.35rem;
      font-weight: 700;
      color: var(--text-bright);
      margin-bottom: 12px;
      z-index: 1;
      letter-spacing: 0.5px;
    }
    .placeholder-desc {
      font-size: 0.95rem;
      color: var(--text-dim);
      max-width: 380px;
      line-height: 1.6;
      z-index: 1;
    }

    /* ACTIVE AI RESPONSE CARD */
    .ai-guide-card {
      background: linear-gradient(135deg, rgba(30, 27, 75, 0.3), rgba(15, 23, 42, 0.55)) !important;
      border: 1px solid rgba(168, 85, 247, 0.25) !important;
      box-shadow: 0 12px 40px rgba(168, 85, 247, 0.12) !important;
      position: relative;
    }
    .ai-guide-card::after {
      content: '';
      position: absolute;
      top: 0;
      left: 0;
      width: 100%;
      height: 4px;
      background: linear-gradient(90deg, var(--accent), var(--danger));
      border-radius: var(--radius) var(--radius) 0 0;
    }
    .badge-sentinel-active {
      background: linear-gradient(135deg, var(--accent), #db2777);
      color: white;
      font-size: 0.7rem;
      font-weight: 800;
      padding: 6px 14px;
      border-radius: 99px;
      letter-spacing: 1px;
      box-shadow: 0 4px 10px rgba(168, 85, 247, 0.3);
    }

    /* MARKDOWN RENDERING */
    .markdown-body { line-height: 1.6; }
    .markdown-body ::ng-deep h3 { 
      margin-top: 24px; 
      color: var(--text-bright); 
      border-bottom: 1px solid rgba(255,255,255,0.08); 
      padding-bottom: 8px; 
      font-size: 1.15rem; 
      font-weight: 700;
      display: flex;
      align-items: center;
      gap: 8px;
    }
    .markdown-body ::ng-deep h4 { 
      margin-top: 18px; 
      color: var(--secondary); 
      font-size: 1.05rem; 
      font-weight: 600; 
    }
    .markdown-body ::ng-deep strong { 
      color: #f43f5e; 
      font-weight: 600; 
    }
    .markdown-body ::ng-deep ul, .markdown-body ::ng-deep ol { 
      margin: 14px 0; 
      padding-left: 20px; 
    }
    .markdown-body ::ng-deep li { 
      margin-bottom: 10px; 
      line-height: 1.5; 
      color: var(--text-bright);
    }
    .markdown-body ::ng-deep p { 
      margin-bottom: 14px; 
      color: rgba(255,255,255,0.85);
    }

    /* GLASS QUOTE CARD */
    .quote-container-glass {
      background: rgba(255, 255, 255, 0.03);
      border-left: 4px solid var(--accent);
      padding: 14px 18px;
      border-radius: 0 12px 12px 0;
      font-style: italic;
      color: var(--text-dim);
      font-size: 0.9rem;
      line-height: 1.5;
    }

    /* INTERACTIVE CHECKLIST */
    .checklist-container {
      background: rgba(15, 23, 42, 0.3);
      border: 1px solid rgba(255, 255, 255, 0.05);
      border-radius: 14px;
      padding: 20px 24px;
      margin-top: 28px;
    }
    .checklist-title {
      font-size: 0.95rem;
      font-weight: 700;
      color: var(--text-bright);
      margin-bottom: 16px;
      display: flex;
      align-items: center;
      gap: 10px;
      text-transform: uppercase;
      letter-spacing: 0.5px;
    }
    .checklist-item {
      display: flex;
      align-items: flex-start;
      gap: 12px;
      margin-bottom: 14px;
      cursor: pointer;
      user-select: none;
    }
    .checklist-item:last-child {
      margin-bottom: 0;
    }
    .checklist-checkbox {
      width: 20px;
      height: 20px;
      border: 2px solid rgba(255, 255, 255, 0.2);
      border-radius: 6px;
      display: flex;
      align-items: center;
      justify-content: center;
      transition: all 0.3s cubic-bezier(0.16, 1, 0.3, 1);
      margin-top: 3px;
      background: rgba(15, 23, 42, 0.5);
      flex-shrink: 0;
    }
    .checklist-checkbox.checked {
      background: var(--success);
      border-color: var(--success);
      box-shadow: 0 0 12px rgba(16, 185, 129, 0.45);
    }
    .checklist-checkbox-inner {
      width: 8px;
      height: 8px;
      background: white;
      border-radius: 1.5px;
      transform: scale(0);
      transition: all 0.2s cubic-bezier(0.175, 0.885, 0.32, 1.275);
    }
    .checklist-checkbox.checked .checklist-checkbox-inner {
      transform: scale(1);
    }
    .checklist-text {
      font-size: 0.9rem;
      color: rgba(255,255,255,0.75);
      line-height: 1.5;
      transition: all 0.3s ease;
    }
    .checklist-text strong {
      color: var(--text-bright);
    }
    .checklist-text.checked {
      color: rgba(255, 255, 255, 0.35);
      text-decoration: line-through;
    }
    .success-celebration-banner {
      background: rgba(16, 185, 129, 0.12);
      border: 1px dashed var(--success);
      border-radius: 10px;
      padding: 14px;
      margin-top: 18px;
      text-align: center;
      animation: popIn 0.4s cubic-bezier(0.175, 0.885, 0.32, 1.275);
    }

    /* HISTORIAL SECTION */
    .history-section-title {
      font-size: 1.3rem;
      font-weight: 700;
      color: var(--text-bright);
      margin-bottom: 24px;
      display: flex;
      align-items: center;
      gap: 10px;
    }
    .history-grid { 
      display: grid; 
      grid-template-columns: repeat(auto-fill, minmax(280px, 1fr)); 
      gap: 20px; 
    }
    .history-card-glass {
      background: rgba(255, 255, 255, 0.03);
      border: 1px solid rgba(255, 255, 255, 0.05);
      border-radius: 16px;
      padding: 24px;
      transition: all 0.3s cubic-bezier(0.16, 1, 0.3, 1);
      cursor: pointer;
      position: relative;
      overflow: hidden;
      box-shadow: 0 4px 15px rgba(0,0,0,0.1);
    }
    .history-card-glass:hover {
      transform: translateY(-6px);
      background: rgba(255, 255, 255, 0.06);
      border-color: rgba(255, 255, 255, 0.12);
      box-shadow: 0 12px 30px rgba(0, 0, 0, 0.25);
    }
    .history-card-glass::before {
      content: '';
      position: absolute;
      top: 0;
      left: 0;
      width: 4px;
      height: 100%;
      background: var(--card-accent-color, var(--primary));
    }
    .category-badge-pill {
      font-size: 0.65rem;
      font-weight: 800;
      text-transform: uppercase;
      letter-spacing: 0.8px;
      padding: 4px 12px;
      border-radius: 99px;
      color: white;
      background: var(--card-accent-color, var(--primary));
    }

    /* KEYFRAMES ANIMATIONS */
    @keyframes fadeIn {
      from { opacity: 0; transform: translateY(10px); }
      to { opacity: 1; transform: translateY(0); }
    }
    @keyframes slideUp {
      from { opacity: 0; transform: translateY(20px); }
      to { opacity: 1; transform: translateY(0); }
    }
  `]
})
export class CrisisPageComponent implements OnInit {
  private crisisService = inject(CrisisService);
  private familyState = inject(FamilyStateService);

  crisis = { category: 'Conflicto de Convivencia', emotion: '', description: '' };
  loading = false;
  lastResponse: any = null;
  history: any[] = [];

  // EMOTION PRE-SETS
  emotionTags = ['Ira ⚡', 'Tristeza 💧', 'Frustración 🌀', 'Ansiedad 🌪️', 'Distanciamiento 🔇'];

  // PROTOCOL CHECKLIST STATE
  step1Ticked = false;
  step2Ticked = false;
  step3Ticked = false;

  ngOnInit() {
    this.loadHistory();
  }

  loadHistory() {
    const familyId = this.familyState.getSelectedFamilyId();
    if (familyId) {
      this.crisisService.getHistory(familyId).subscribe(res => this.history = res);
    }
  }

  selectEmotion(emotion: string) {
    this.crisis.emotion = emotion;
  }

  resetChecklist() {
    this.step1Ticked = false;
    this.step2Ticked = false;
    this.step3Ticked = false;
  }

  selectHistoryItem(item: any) {
    this.lastResponse = item;
    this.resetChecklist();
    // Scroll window smoothly to response area
    window.scrollTo({ top: 120, behavior: 'smooth' });
  }

  getCategoryColor(category: string): string {
    switch (category) {
      case 'Emergencia Emocional':
        return '#ef4444'; // Red
      case 'Crisis de Autoridad':
        return '#a855f7'; // Purple
      case 'Tensión Financiera':
        return '#f59e0b'; // Amber
      case 'Ruptura de Diálogo':
        return '#3b82f6'; // Blue
      case 'Conflicto de Convivencia':
        return '#0ea5e9'; // Sky Blue
      default:
        return '#6366f1'; // Primary Indigo
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
        this.resetChecklist();
        this.loadHistory();
      },
      error: () => this.loading = false
    });
  }

  formatAiResponse(text: string) {
    if (!text) return '';
    
    // Convert headings (supports ###, ##, #)
    let html = text
      .replace(/^### (.*)/gm, '<h4>$1</h4>')
      .replace(/^## (.*)/gm, '<h3>$1</h3>')
      .replace(/^# (.*)/gm, '<h2>$1</h2>');

    // Convert strong markdown (**text**)
    html = html.replace(/\*\*(.*?)\*\*/g, '<strong>$1</strong>');

    // Convert italics markdown (*text*)
    html = html.replace(/\*(.*?)\*/g, '<em>$1</em>');

    // Convert bullet lists and ordered lists (supports lines starting with * or numbers)
    html = html.replace(/^\s*\*\s*(.*)/gm, '<li style="list-style-type: disc; margin-left: 20px;">$1</li>');
    html = html.replace(/^\s*-\s*(.*)/gm, '<li style="list-style-type: disc; margin-left: 20px;">$1</li>');
    html = html.replace(/^\s*\d+\.\s*(.*)/gm, '<li style="list-style-type: decimal; margin-left: 20px;">$1</li>');

    // Convert newlines to breaks
    html = html.replace(/\n/g, '<br>');

    return html;
  }
}
