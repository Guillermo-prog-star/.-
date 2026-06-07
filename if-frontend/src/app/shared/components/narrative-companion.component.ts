import { Component, Input, inject, signal, computed } from '@angular/core';
import { CommonModule } from '@angular/common';
import { NarrativeGuidanceService, UserRole, EmotionalState, NarrativeContent } from '../../core/services/narrative-guidance.service';

@Component({
  selector: 'app-narrative-companion',
  standalone: true,
  imports: [CommonModule],
  styles: [`
    :host {
      display: block;
      margin-bottom: 24px;
      width: 100%;
    }

    .companion-card {
      background: rgba(15, 23, 42, 0.95); /* Deep slate dark glass for perfect legibility */
      backdrop-filter: blur(20px);
      -webkit-backdrop-filter: blur(20px);
      border: 1px solid rgba(255, 255, 255, 0.1);
      border-radius: 24px;
      padding: 20px;
      box-shadow: 0 12px 40px 0 rgba(0, 0, 0, 0.3);
      position: relative;
      overflow: hidden;
      transition: all 0.4s cubic-bezier(0.175, 0.885, 0.32, 1.275);
    }

    .companion-card:hover {
      transform: translateY(-4px);
      border-color: rgba(255, 255, 255, 0.18);
      box-shadow: 0 20px 48px 0 rgba(99, 102, 241, 0.15);
    }

    .companion-body {
      display: flex;
      align-items: center;
      gap: 20px;
    }

    /* 3D Pulsing Orb Avatar */
    .convi-avatar {
      width: 48px;
      height: 48px;
      border-radius: 50%;
      position: relative;
      flex-shrink: 0;
      transition: all 0.5s ease;
      box-shadow: 0 0 20px var(--avatar-glow);
      animation: pulse-avatar 3s infinite ease-in-out;
    }

    .convi-avatar::after {
      content: '';
      position: absolute;
      top: 4px;
      left: 8px;
      width: 12px;
      height: 6px;
      background: rgba(255, 255, 255, 0.5);
      border-radius: 50%;
      transform: rotate(-15deg);
    }

    /* Ambient colors mapping */
    .avatar-calma {
      background: radial-gradient(circle at 30% 30%, #34d399 0%, #059669 80%);
      --avatar-glow: rgba(52, 211, 153, 0.4);
    }

    .avatar-estres {
      background: radial-gradient(circle at 30% 30%, #fca5a5 0%, #dc2626 80%);
      --avatar-glow: rgba(252, 165, 165, 0.4);
    }

    .avatar-agotamiento {
      background: radial-gradient(circle at 30% 30%, #fbbf24 0%, #d97706 80%);
      --avatar-glow: rgba(251, 191, 36, 0.4);
    }

    .avatar-cercania {
      background: radial-gradient(circle at 30% 30%, #f59e0b 0%, #d97706 80%);
      --avatar-glow: rgba(245, 158, 11, 0.4);
    }

    .message-container {
      flex-grow: 1;
      display: flex;
      flex-direction: column;
      gap: 6px;
    }

    .meta-row {
      display: flex;
      align-items: center;
      gap: 8px;
      flex-wrap: wrap;
    }

    /* Spark 10-char badge */
    .spark-badge {
      font-size: 10px;
      font-weight: 800;
      letter-spacing: 0.1em;
      text-transform: uppercase;
      padding: 3px 10px;
      border-radius: 99px;
      white-space: nowrap;
      transition: all 0.3s ease;
    }

    .badge-calma {
      background: rgba(249, 115, 22, 0.12);
      color: #fb923c;
      border: 1px solid rgba(249, 115, 22, 0.3);
    }

    .badge-estres {
      background: rgba(239, 68, 68, 0.1);
      color: #f87171;
      border: 1px solid rgba(239, 68, 68, 0.2);
    }

    .badge-agotamiento {
      background: rgba(251, 191, 36, 0.1);
      color: #fbbf24;
      border: 1px solid rgba(251, 191, 36, 0.2);
    }

    .badge-cercania {
      background: rgba(245, 158, 11, 0.1);
      color: #fbbf24;
      border: 1px solid rgba(245, 158, 11, 0.2);
    }

    .context-label {
      font-size: 11px;
      color: rgba(255, 255, 255, 0.65); /* Increased for perfect contrast */
      font-weight: 600;
      letter-spacing: 0.05em;
      text-transform: uppercase;
    }

    .warm-paragraph {
      font-size: 14px;
      line-height: 1.5;
      color: rgba(255, 255, 255, 0.98); /* Crystal clear legibility */
      font-weight: 500;
      letter-spacing: -0.01em;
      animation: text-slide-up 0.4s ease-out;
    }

    /* Expandable Settings Trigger */
    .controls-toggle {
      background: rgba(255, 255, 255, 0.04);
      border: 1px solid rgba(255, 255, 255, 0.08);
      color: rgba(255, 255, 255, 0.6);
      width: 28px;
      height: 28px;
      border-radius: 50%;
      cursor: pointer;
      display: flex;
      align-items: center;
      justify-content: center;
      transition: all 0.3s ease;
      font-size: 12px;
    }

    .controls-toggle:hover {
      background: rgba(255, 255, 255, 0.1);
      color: #fff;
      transform: rotate(45deg);
    }

    /* Floating Playground Panel */
    .playground-panel {
      margin-top: 16px;
      padding-top: 16px;
      border-t: 1px dashed rgba(255, 255, 255, 0.08);
      display: flex;
      flex-direction: column;
      gap: 12px;
      animation: pane-fade-in 0.3s ease-out;
    }

    .selector-group {
      display: flex;
      align-items: center;
      gap: 10px;
      flex-wrap: wrap;
    }

    .group-title {
      font-size: 10px;
      font-weight: 700;
      color: rgba(255, 255, 255, 0.55); /* Increased contrast */
      text-transform: uppercase;
      letter-spacing: 0.05em;
      min-width: 80px;
    }

    .pill-btn {
      background: rgba(255, 255, 255, 0.05); /* Premium glass background */
      border: 1px solid rgba(255, 255, 255, 0.1);
      color: rgba(255, 255, 255, 0.85); /* Premium contrast ratio */
      padding: 4px 12px;
      border-radius: 99px;
      font-size: 11px;
      font-weight: 600;
      cursor: pointer;
      transition: all 0.2s ease;
    }

    .pill-btn:hover {
      background: rgba(255, 255, 255, 0.08);
      color: #fff;
    }

    .pill-btn.active {
      background: rgba(99, 102, 241, 0.15);
      border-color: rgba(99, 102, 241, 0.4);
      color: #818cf8;
      box-shadow: 0 4px 12px rgba(99, 102, 241, 0.1);
    }

    .pill-btn.active.color-calma {
      background: rgba(249, 115, 22, 0.15);
      border-color: rgba(249, 115, 22, 0.4);
      color: #fb923c;
    }

    .pill-btn.active.color-estres {
      background: rgba(239, 68, 68, 0.15);
      border-color: rgba(239, 68, 68, 0.4);
      color: #f87171;
    }

    .pill-btn.active.color-agotamiento {
      background: rgba(251, 191, 36, 0.15);
      border-color: rgba(251, 191, 36, 0.4);
      color: #fbbf24;
    }

    .pill-btn.active.color-cercania {
      background: rgba(245, 158, 11, 0.15);
      border-color: rgba(245, 158, 11, 0.4);
      color: #fbbf24;
    }

    @keyframes pulse-avatar {
      0%, 100% {
        transform: scale(1);
        opacity: 0.95;
      }
      50% {
        transform: scale(1.05);
        opacity: 1;
        box-shadow: 0 0 28px var(--avatar-glow);
      }
    }

    @keyframes text-slide-up {
      from {
        opacity: 0;
        transform: translateY(4px);
      }
      to {
        opacity: 1;
        transform: translateY(0);
      }
    }

    @keyframes pane-fade-in {
      from {
        opacity: 0;
        max-height: 0;
        overflow: hidden;
      }
      to {
        opacity: 1;
        max-height: 200px;
      }
    }

    /* Light Theme Adaptive Overrides */
    .companion-card.light-theme {
      background: rgba(255, 255, 255, 0.55);
      border: 1px solid rgba(0, 0, 0, 0.08);
      box-shadow: 0 8px 32px 0 rgba(31, 38, 135, 0.05);
    }

    .companion-card.light-theme:hover {
      border-color: rgba(0, 0, 0, 0.15);
      box-shadow: 0 16px 40px 0 rgba(99, 102, 241, 0.08);
    }

    .companion-card.light-theme .context-label {
      color: #000000 !important; /* Solid Black letters for title label as requested */
      opacity: 0.9;
    }

    .companion-card.light-theme .warm-paragraph {
      color: #000000 !important; /* Solid Black letters for message as requested */
      font-weight: 600;
    }

    .companion-card.light-theme .controls-toggle {
      background: rgba(0, 0, 0, 0.04);
      border: 1px solid rgba(0, 0, 0, 0.1);
      color: #1e293b;
    }

    .companion-card.light-theme .controls-toggle:hover {
      background: rgba(0, 0, 0, 0.08);
      color: #000;
    }

    .companion-card.light-theme .group-title {
      color: #475569;
    }

    .companion-card.light-theme .pill-btn {
      background: rgba(0, 0, 0, 0.03);
      border: 1px solid rgba(0, 0, 0, 0.1);
      color: #1e293b;
    }

    .companion-card.light-theme .pill-btn:hover {
      background: rgba(0, 0, 0, 0.08);
      color: #000;
    }

    .companion-card.light-theme .pill-btn.active {
      background: rgba(99, 102, 241, 0.12);
      border-color: rgba(99, 102, 241, 0.4);
      color: #4f46e5;
    }

    .companion-card.light-theme .pill-btn.active.color-calma {
      background: rgba(16, 185, 129, 0.12);
      border-color: rgba(16, 185, 129, 0.4);
      color: #059669;
    }

    .companion-card.light-theme .pill-btn.active.color-estres {
      background: rgba(239, 68, 68, 0.12);
      border-color: rgba(239, 68, 68, 0.4);
      color: #dc2626;
    }

    .companion-card.light-theme .pill-btn.active.color-agotamiento {
      background: rgba(245, 158, 11, 0.12);
      border-color: rgba(245, 158, 11, 0.4);
      color: #d97706;
    }

    .companion-card.light-theme .pill-btn.active.color-cercania {
      background: rgba(245, 158, 11, 0.12);
      border-color: rgba(245, 158, 11, 0.4);
      color: #d97706;
    }
  `],
  template: `
    <div class="companion-card" [class.light-theme]="isLightTheme">
      <div class="companion-body">
        <!-- Floating Convi Orb -->
        <div class="convi-avatar" [ngClass]="avatarClass"></div>

        <!-- Narrative Content -->
        <div class="message-container">
          <div class="meta-row">
            <span class="spark-badge" [ngClass]="badgeClass">
              {{ narrative.badge }}
            </span>
            <span class="context-label">
              Acompañamiento Emocional para {{ roleLabel }}
            </span>
          </div>

          <p class="warm-paragraph" [attr.data-key]="textKey">
            {{ narrative.message }}
          </p>
        </div>

        <!-- Toggle Controls (Interactive Sandbox) -->
        <button 
          class="controls-toggle" 
          (click)="togglePlayground()" 
          title="Personalizar Contexto Emocional"
          aria-label="Personalizar Contexto Emocional">
          ⚙️
        </button>
      </div>

      <!-- Settings Playground Panel -->
      @if (showPlayground) {
        <div class="playground-panel">
          <!-- Role selector -->
          <div class="selector-group">
            <span class="group-title">Rol Familiar</span>
            <button 
              *ngFor="let r of roles"
              class="pill-btn" 
              [class.active]="currentRole === r.value"
              (click)="changeRole(r.value)">
              {{ r.label }}
            </button>
          </div>

          <!-- Emotion selector -->
          <div class="selector-group">
            <span class="group-title">Emoción</span>
            <button 
              *ngFor="let e of emotions"
              class="pill-btn"
              [ngClass]="getEmotionClass(e.value)"
              [class.active]="currentEmotion === e.value"
              (click)="changeEmotion(e.value)">
              {{ e.label }}
            </button>
          </div>
        </div>
      }
    </div>
  `
})
export class NarrativeCompanionComponent {
  @Input() module: string = 'dashboard';

  private ngeService = inject(NarrativeGuidanceService);

  showPlayground = false;

  // Available options
  roles = [
    { value: 'FAMILIA' as UserRole, label: '🏡 Familia' },
    { value: 'PADRE' as UserRole, label: '👨‍💼 Padre' },
    { value: 'MADRE' as UserRole, label: '👩‍💼 Madre' },
    { value: 'HIJO' as UserRole, label: '👦 Hijo' },
    { value: 'ADOLESCENTE' as UserRole, label: '🎧 Adolescente' }
  ];

  emotions = [
    { value: 'CALMA' as EmotionalState, label: 'Serenidad 🟢' },
    { value: 'ESTRES' as EmotionalState, label: 'Tensión 🔴' },
    { value: 'AGOTAMIENTO' as EmotionalState, label: 'Cansancio 🟡' },
    { value: 'CERCANIA' as EmotionalState, label: 'Alegría 🟣' }
  ];

  // Dynamic getters to bind active state
  get currentRole(): UserRole {
    return this.ngeService.activeRole();
  }

  get currentEmotion(): EmotionalState {
    return this.ngeService.activeEmotion();
  }

  get narrative(): NarrativeContent {
    return this.ngeService.getContent(this.module);
  }

  get isLightTheme(): boolean {
    return this.module === 'my-space';
  }

  // Visual classes computations
  get avatarClass(): string {
    const emo = this.currentEmotion.toLowerCase();
    return `avatar-${emo}`;
  }

  get badgeClass(): string {
    const emo = this.currentEmotion.toLowerCase();
    return `badge-${emo}`;
  }

  getEmotionClass(emo: EmotionalState): string {
    return `color-${emo.toLowerCase()}`;
  }

  get roleLabel(): string {
    switch (this.currentRole) {
      case 'PADRE': return 'Padre';
      case 'MADRE': return 'Madre';
      case 'HIJO': return 'Hijo';
      case 'ADOLESCENTE': return 'Adolescente';
      default: return 'la Familia';
    }
  }

  // Simple key generator to trigger angular re-rendering animation
  get textKey(): string {
    return `${this.currentRole}-${this.currentEmotion}-${this.module}`;
  }

  togglePlayground() {
    this.showPlayground = !this.showPlayground;
  }

  changeRole(role: UserRole) {
    this.ngeService.setRole(role);
  }

  changeEmotion(emotion: EmotionalState) {
    this.ngeService.setEmotion(emotion);
  }
}
