import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import {
  FamilyCouncilService,
  CouncilRequest,
  CouncilResponse
} from '../../core/services/family-council.service';
import { FamilyStateService } from '../../core/services/family-state.service';
import { catchError, of } from 'rxjs';

type Topic = 'DECISION' | 'CRISIS' | 'CONFLICTO' | 'REFLEXION' | 'LEGADO';

const TOPICS: { key: Topic; icon: string; label: string; hint: string }[] = [
  { key: 'DECISION',  icon: '⚖️',  label: 'Decisión',   hint: 'Algo importante que decidir juntos' },
  { key: 'CRISIS',    icon: '🆘',  label: 'Crisis',      hint: 'Situación urgente que necesita guía' },
  { key: 'CONFLICTO', icon: '🌊',  label: 'Conflicto',   hint: 'Una tensión que necesita resolverse' },
  { key: 'REFLEXION', icon: '🧘',  label: 'Reflexión',   hint: 'Pausar y pensar juntos' },
  { key: 'LEGADO',    icon: '🌳',  label: 'Legado',      hint: 'Lo que queremos transmitir' },
];

@Component({
  selector: 'app-family-council',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink],
  template: `
    <div class="fc-page">

      <!-- Header ceremonial -->
      <div class="fc-header">
        <div class="fc-emblem">⚜️</div>
        <div>
          <h1 class="fc-title">Consejo Familiar</h1>
          <p class="fc-sub">
            La voz de tu familia — responde desde tu constitución, tus valores y tu historia
          </p>
        </div>
      </div>

      <!-- Formulario de consulta -->
      @if (!response() && !consulting()) {
        <div class="fc-form">

          <!-- Selector de tema -->
          <div class="field-group">
            <label class="field-label">¿Sobre qué tema desean consultar al Consejo?</label>
            <div class="topic-grid">
              @for (t of topics; track t.key) {
                <button
                  class="topic-btn"
                  [class.sel]="selectedTopic === t.key"
                  (click)="selectedTopic = t.key"
                >
                  <span class="tb-icon">{{ t.icon }}</span>
                  <span class="tb-label">{{ t.label }}</span>
                  <span class="tb-hint">{{ t.hint }}</span>
                </button>
              }
            </div>
          </div>

          <!-- Pregunta principal -->
          <div class="field-group">
            <label class="field-label">¿Cuál es la pregunta para el Consejo?</label>
            <textarea
              class="fc-textarea"
              [(ngModel)]="question"
              placeholder="Ej: ¿Cómo resolvemos esta situación sin que alguno quede herido? ¿Qué decisión tomaríamos como familia si la tomáramos desde nuestros valores?"
              rows="4"
              maxlength="800"
            ></textarea>
            <div class="char-count">{{ question.length }} / 800</div>
          </div>

          <!-- Contexto adicional -->
          <div class="field-group">
            <label class="field-label">Contexto adicional (opcional)</label>
            <textarea
              class="fc-textarea fc-textarea--sm"
              [(ngModel)]="context"
              placeholder="Describe brevemente la situación si lo deseas..."
              rows="2"
              maxlength="400"
            ></textarea>
          </div>

          <!-- Advertencia si sin constitución -->
          @if (!hasFamilyIdentity()) {
            <div class="fc-warning">
              ⚠️ Tu familia aún no tiene constitución ni ADN sintetizado.
              El Consejo podrá responder, pero será más poderoso cuando definan
              <a routerLink="/legado" class="w-link">su legado</a>
              y
              <a routerLink="/family-dna" class="w-link">su ADN familiar</a>.
            </div>
          }

          @if (formError()) {
            <div class="fc-error">{{ formError() }}</div>
          }

          <button
            class="btn-consult"
            (click)="consult()"
            [disabled]="!question.trim() || !selectedTopic"
          >
            ⚜️ Consultar al Consejo
          </button>
        </div>
      }

      <!-- Estado: consultando -->
      @if (consulting()) {
        <div class="fc-consulting">
          <div class="consulting-orb">
            <div class="orb-ring"></div>
            <span class="orb-icon">⚜️</span>
          </div>
          <div class="consulting-text">El Consejo está deliberando...</div>
          <div class="consulting-sub">Consultando la constitución, los valores y la historia de tu familia</div>
        </div>
      }

      <!-- Respuesta del Consejo -->
      @if (response()) {
        <div class="fc-response">

          <!-- Cabecera de la respuesta -->
          <div class="resp-header">
            <div class="rh-seal">⚜️</div>
            <div>
              <div class="rh-label">El Consejo de la Familia {{ response()!.familyName }} ha deliberado</div>
              <div class="rh-question">« {{ response()!.question }} »</div>
            </div>
          </div>

          <!-- Fuentes consultadas -->
          @if (response()!.sourcesUsed.length) {
            <div class="sources-bar">
              <span class="sb-label">Consultado desde:</span>
              @for (src of response()!.sourcesUsed; track src) {
                <span class="sb-tag">{{ src }}</span>
              }
            </div>
          }

          <!-- Advertencia si identidad incompleta -->
          @if (!response()!.hasConstitution || !response()!.hasDna) {
            <div class="fc-warning fc-warning--sm">
              El Consejo respondió con la información disponible.
              @if (!response()!.hasConstitution) {
                Define la <a routerLink="/legado" class="w-link">constitución familiar</a> para respuestas más profundas.
              }
              @if (!response()!.hasDna) {
                Sintetiza el <a routerLink="/family-dna" class="w-link">ADN familiar</a> para que el Consejo conozca tus valores.
              }
            </div>
          }

          <!-- El texto del Consejo -->
          <div class="council-text" [innerHTML]="renderMarkdown(response()!.councilResponse)"></div>

          <!-- Acciones -->
          <div class="resp-actions">
            <button class="btn-new" (click)="reset()">
              🔄 Nueva consulta
            </button>
            <button class="btn-copy" (click)="copyResponse()">
              {{ copied() ? '✅ Copiado' : '📋 Copiar respuesta' }}
            </button>
          </div>

          <div class="resp-meta">Consultado el {{ formatDate(response()!.consultedAt) }}</div>
        </div>
      }

    </div>
  `,
  styles: [`
    .fc-page {
      max-width: 700px; margin: 0 auto;
      padding: 28px 20px 60px;
      font-family: inherit;
      color: var(--if-text-primary, #e0e0e0);
    }

    /* Header ceremonial */
    .fc-header {
      display: flex; align-items: flex-start; gap: 16px;
      margin-bottom: 36px;
      padding-bottom: 24px;
      border-bottom: 1px solid rgba(255,255,255,0.06);
    }
    .fc-emblem { font-size: 44px; flex-shrink: 0; filter: drop-shadow(0 0 12px rgba(251,191,36,0.4)); }
    .fc-title  { font-size: 28px; font-weight: 900; margin: 0 0 6px; letter-spacing: -0.02em; }
    .fc-sub    { font-size: 14px; color: var(--if-text-secondary, #888); margin: 0; line-height: 1.5; }

    /* Formulario */
    .fc-form { display: flex; flex-direction: column; gap: 20px; }

    .topic-grid { display: grid; grid-template-columns: repeat(5, 1fr); gap: 8px; }
    @media (max-width: 600px) { .topic-grid { grid-template-columns: repeat(3, 1fr); } }

    .topic-btn {
      display: flex; flex-direction: column; align-items: center; gap: 5px;
      padding: 12px 6px; border-radius: 12px;
      background: rgba(255,255,255,0.04);
      border: 1px solid rgba(255,255,255,0.08);
      cursor: pointer; transition: all 0.2s;
      color: var(--if-text-primary, #ddd);
    }
    .topic-btn:hover { background: rgba(255,255,255,0.08); transform: translateY(-2px); }
    .topic-btn.sel {
      background: rgba(251,191,36,0.12);
      border-color: rgba(251,191,36,0.4);
      box-shadow: 0 0 16px rgba(251,191,36,0.1);
    }
    .tb-icon  { font-size: 24px; }
    .tb-label { font-size: 12px; font-weight: 700; }
    .tb-hint  { font-size: 10px; color: var(--if-text-secondary, #888); text-align: center; line-height: 1.3; }

    .field-group { display: flex; flex-direction: column; gap: 8px; }
    .field-label {
      font-size: 12px; font-weight: 700;
      text-transform: uppercase; letter-spacing: 0.07em;
      color: var(--if-text-secondary, #aaa);
    }
    .fc-textarea {
      padding: 14px; border-radius: 12px;
      background: rgba(255,255,255,0.05);
      border: 1px solid rgba(255,255,255,0.1);
      color: var(--if-text-primary, #e0e0e0);
      font-size: 14px; font-family: inherit; resize: vertical; line-height: 1.6;
    }
    .fc-textarea--sm { font-size: 13px; }
    .fc-textarea:focus { outline: none; border-color: rgba(251,191,36,0.4); }
    .char-count { font-size: 11px; color: var(--if-text-secondary, #777); text-align: right; }

    .btn-consult {
      background: linear-gradient(135deg, #b45309, #92400e);
      border: none; color: #fef3c7;
      padding: 14px 32px; border-radius: 12px;
      font-size: 15px; font-weight: 800; cursor: pointer;
      letter-spacing: 0.02em; transition: opacity 0.2s;
      box-shadow: 0 4px 20px rgba(180,83,9,0.3);
    }
    .btn-consult:disabled { opacity: 0.4; cursor: not-allowed; }
    .btn-consult:hover:not(:disabled) { opacity: 0.88; }

    /* Warnings */
    .fc-warning {
      background: rgba(245,158,11,0.08);
      border: 1px solid rgba(245,158,11,0.25);
      border-radius: 10px; padding: 12px 16px;
      font-size: 13px; color: #fcd34d; line-height: 1.5;
    }
    .fc-warning--sm { font-size: 12px; margin-bottom: 4px; }
    .w-link { color: #fbbf24; text-decoration: underline; cursor: pointer; }

    .fc-error {
      background: rgba(239,68,68,0.1); border: 1px solid rgba(239,68,68,0.3);
      border-radius: 10px; padding: 12px 16px; font-size: 13px; color: #fca5a5;
    }

    /* Consultando */
    .fc-consulting {
      display: flex; flex-direction: column; align-items: center;
      gap: 20px; padding: 64px 20px; text-align: center;
    }
    .consulting-orb {
      position: relative; width: 80px; height: 80px;
      display: flex; align-items: center; justify-content: center;
    }
    .orb-ring {
      position: absolute; inset: 0; border-radius: 50%;
      border: 2px solid rgba(251,191,36,0.4);
      animation: orb-pulse 2s ease-in-out infinite;
    }
    @keyframes orb-pulse {
      0%, 100% { transform: scale(1); opacity: 0.5; }
      50%       { transform: scale(1.15); opacity: 1; }
    }
    .orb-icon { font-size: 36px; position: relative; z-index: 1; }
    .consulting-text { font-size: 18px; font-weight: 700; color: #fbbf24; }
    .consulting-sub  { font-size: 13px; color: var(--if-text-secondary, #888); max-width: 320px; line-height: 1.5; }

    /* Respuesta */
    .fc-response { display: flex; flex-direction: column; gap: 16px; }

    .resp-header {
      display: flex; align-items: flex-start; gap: 16px;
      padding: 20px;
      background: rgba(251,191,36,0.06);
      border: 1px solid rgba(251,191,36,0.2);
      border-radius: 16px;
    }
    .rh-seal    { font-size: 32px; flex-shrink: 0; }
    .rh-label   { font-size: 11px; font-weight: 700; text-transform: uppercase; letter-spacing: 0.08em; color: #fbbf24; margin-bottom: 6px; }
    .rh-question { font-size: 15px; font-weight: 600; color: var(--if-text-primary, #ddd); font-style: italic; line-height: 1.4; }

    .sources-bar { display: flex; flex-wrap: wrap; gap: 6px; align-items: center; }
    .sb-label { font-size: 11px; color: var(--if-text-secondary, #888); font-weight: 600; }
    .sb-tag {
      font-size: 10px; font-weight: 700; padding: 3px 10px; border-radius: 99px;
      background: rgba(251,191,36,0.1); color: #fbbf24;
      border: 1px solid rgba(251,191,36,0.25);
    }

    .council-text {
      background: rgba(255,255,255,0.03);
      border: 1px solid rgba(255,255,255,0.07);
      border-left: 4px solid #b45309;
      border-radius: 14px; padding: 24px;
      font-size: 15px; line-height: 1.8;
      color: var(--if-text-primary, #ddd);
    }
    /* Markdown renderizado */
    .council-text :global(strong) { color: #fbbf24; }
    .council-text :global(em)     { color: #fcd34d; }
    .council-text :global(p)      { margin: 0 0 12px; }
    .council-text :global(ul)     { padding-left: 20px; }
    .council-text :global(li)     { margin-bottom: 6px; }

    .resp-actions { display: flex; gap: 10px; }
    .btn-new, .btn-copy {
      padding: 10px 20px; border-radius: 9px;
      font-size: 13px; font-weight: 600; cursor: pointer; transition: all 0.2s;
    }
    .btn-new {
      background: rgba(255,255,255,0.06);
      border: 1px solid rgba(255,255,255,0.12);
      color: var(--if-text-secondary, #bbb);
    }
    .btn-copy {
      background: rgba(251,191,36,0.1);
      border: 1px solid rgba(251,191,36,0.3);
      color: #fbbf24;
    }
    .btn-new:hover, .btn-copy:hover { opacity: 0.8; }

    .resp-meta { font-size: 11px; color: var(--if-text-secondary, #666); text-align: right; }
  `]
})
export class FamilyCouncilComponent implements OnInit {
  private readonly councilSvc  = inject(FamilyCouncilService);
  private readonly familyState = inject(FamilyStateService);

  readonly familyId = this.familyState.currentFamilyId;

  readonly topics = TOPICS;

  // Form state
  selectedTopic: Topic | null = null;
  question = '';
  context  = '';

  // Response state
  readonly consulting = signal(false);
  readonly response   = signal<CouncilResponse | null>(null);
  readonly formError  = signal<string | null>(null);
  readonly copied     = signal(false);

  // Indica si la familia tiene identidad suficiente
  hasFamilyIdentity = () => true; // siempre permite consultar

  ngOnInit(): void {}

  consult(): void {
    const id = this.familyId();
    if (!id) { this.formError.set('Selecciona una familia para consultar al Consejo.'); return; }
    if (!this.question.trim()) { this.formError.set('Escribe tu pregunta para el Consejo.'); return; }
    if (!this.selectedTopic)   { this.formError.set('Selecciona el tema de la consulta.'); return; }

    this.formError.set(null);
    this.consulting.set(true);

    const req: CouncilRequest = {
      question: this.question.trim(),
      topic: this.selectedTopic,
      context: this.context.trim() || undefined,
    };

    this.councilSvc.consult(id, req).pipe(
      catchError(() => {
        this.formError.set('El Consejo no pudo responder en este momento. Inténtalo de nuevo.');
        this.consulting.set(false);
        return of(null);
      })
    ).subscribe(res => {
      if (res) this.response.set(res);
      this.consulting.set(false);
    });
  }

  reset(): void {
    this.response.set(null);
    this.question = '';
    this.context  = '';
    this.selectedTopic = null;
    this.formError.set(null);
    this.copied.set(false);
  }

  copyResponse(): void {
    const text = this.response()?.councilResponse;
    if (!text) return;
    navigator.clipboard.writeText(text).then(() => {
      this.copied.set(true);
      setTimeout(() => this.copied.set(false), 2500);
    });
  }

  /**
   * Convierte Markdown básico a HTML seguro para mostrar la respuesta del Consejo.
   * Solo procesa: **negrita**, *cursiva*, listas con guión y párrafos.
   */
  renderMarkdown(text: string): string {
    if (!text) return '';
    return text
      .replace(/\*\*(.+?)\*\*/g,    '<strong>$1</strong>')
      .replace(/\*(.+?)\*/g,        '<em>$1</em>')
      .replace(/^- (.+)$/gm,        '<li>$1</li>')
      .replace(/(<li>.*<\/li>)/gs,  '<ul>$1</ul>')
      .replace(/\n\n/g,             '</p><p>')
      .replace(/^(.)/,              '<p>$1')
      .replace(/(.)$/,              '$1</p>');
  }

  formatDate(iso: string): string {
    try {
      return new Date(iso).toLocaleString('es-CO', {
        day: '2-digit', month: 'short', year: 'numeric',
        hour: '2-digit', minute: '2-digit'
      });
    } catch { return iso; }
  }
}
