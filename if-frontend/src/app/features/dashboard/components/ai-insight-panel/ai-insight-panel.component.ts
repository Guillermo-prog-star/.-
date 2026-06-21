import { Component, Input, inject, SecurityContext } from '@angular/core';
import { CommonModule } from '@angular/common';
import { DomSanitizer, SafeHtml } from '@angular/platform-browser';

@Component({
  selector: 'app-ai-insight-panel',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div class="aip-card">
      <!-- Cabecera -->
      <div class="aip-header">
        <div class="aip-avatar">🤖</div>
        <div>
          <h3 class="aip-name">Mentor de Integridad</h3>
          <span class="aip-label">Análisis Cognitivo en Tiempo Real</span>
        </div>
      </div>

      <!-- Bloques parseados -->
      <div class="aip-body">
        @for (block of parsedBlocks; track $index) {

          @if (block.type === 'alert') {
            <div class="aip-alert">
              <span class="aip-alert-icon">🚨</span>
              <span [innerHTML]="block.html"></span>
            </div>
          }

          @if (block.type === 'section') {
            <div class="aip-section-title" [innerHTML]="block.html"></div>
          }

          @if (block.type === 'meta') {
            <div class="aip-meta" [innerHTML]="block.html"></div>
          }

          @if (block.type === 'divider') {
            <hr class="aip-divider">
          }

          @if (block.type === 'heading') {
            <div class="aip-heading" [innerHTML]="block.html"></div>
          }

          @if (block.type === 'paragraph') {
            <p class="aip-paragraph" [innerHTML]="block.html"></p>
          }

          @if (block.type === 'bullets') {
            <ul class="aip-bullets">
              @for (item of block.items; track $index) {
                <li [innerHTML]="item"></li>
              }
            </ul>
          }

          @if (block.type === 'highlight') {
            <div class="aip-highlight" [innerHTML]="block.html"></div>
          }
        }
      </div>

      <!-- Tendencia -->
      @if (trend) {
        <div class="aip-trend">
          <span class="aip-trend-label">Tendencia de Riesgo:</span>
          <span class="aip-trend-value" [ngClass]="trendClass">
            {{ trend === 'UP' ? '↑ Incrementando' : trend === 'DOWN' ? '↓ Mitigándose' : '↔ Estable' }}
          </span>
        </div>
      }
    </div>
  `,
  styles: [`
    .aip-card {
      background: rgba(255,255,255,0.03);
      border: 1px solid rgba(255,255,255,0.06);
      border-left: 4px solid #6366f1;
      border-radius: 20px;
      padding: 24px;
      transition: box-shadow 0.3s;
    }
    .aip-card:hover { box-shadow: 0 0 40px rgba(99,102,241,0.08); }

    /* Cabecera */
    .aip-header { display: flex; align-items: center; gap: 14px; margin-bottom: 20px; }
    .aip-avatar {
      width: 42px; height: 42px; border-radius: 12px;
      background: rgba(99,102,241,0.15); border: 1px solid rgba(99,102,241,0.25);
      display: flex; align-items: center; justify-content: center; font-size: 20px; flex-shrink: 0;
    }
    .aip-name  { font-size: 15px; font-weight: 700; color: rgba(255,255,255,0.9); margin: 0 0 2px; }
    .aip-label { font-size: 9px; color: rgba(255,255,255,0.3); text-transform: uppercase; letter-spacing: 0.2em; }

    /* Cuerpo */
    .aip-body { display: flex; flex-direction: column; gap: 12px; margin-bottom: 16px; }

    /* Alerta S.O.S */
    .aip-alert {
      display: flex; align-items: flex-start; gap: 10px;
      background: rgba(239,68,68,0.08); border: 1px solid rgba(239,68,68,0.2);
      border-radius: 12px; padding: 12px 14px;
      font-size: 13px; font-weight: 600; color: #fca5a5; line-height: 1.5;
    }
    .aip-alert-icon { flex-shrink: 0; font-size: 16px; }

    /* Título de sección principal */
    .aip-section-title {
      font-size: 11px; font-weight: 800; text-transform: uppercase;
      letter-spacing: 0.12em; color: #818cf8;
      padding-bottom: 6px; border-bottom: 1px solid rgba(99,102,241,0.15);
    }

    /* Meta info (familia, nivel de riesgo) */
    .aip-meta {
      background: rgba(99,102,241,0.06); border: 1px solid rgba(99,102,241,0.12);
      border-radius: 10px; padding: 10px 14px;
      font-size: 12px; color: rgba(255,255,255,0.55); line-height: 1.6;
    }
    .aip-meta :global(strong) { color: rgba(255,255,255,0.8); font-weight: 700; }

    /* Divisor */
    .aip-divider {
      border: none; border-top: 1px solid rgba(255,255,255,0.07); margin: 0;
    }

    /* Subtítulo de sección */
    .aip-heading {
      font-size: 13px; font-weight: 800; color: rgba(255,255,255,0.85);
      letter-spacing: 0.01em;
    }
    .aip-heading :global(strong) { color: #a5b4fc; }

    /* Párrafo normal */
    .aip-paragraph {
      font-size: 13px; line-height: 1.75; color: rgba(255,255,255,0.65);
      margin: 0; padding-left: 12px;
      border-left: 2px solid rgba(255,255,255,0.06);
    }
    .aip-paragraph :global(strong) { color: rgba(255,255,255,0.88); font-weight: 700; }

    /* Lista de acciones */
    .aip-bullets {
      margin: 0; padding: 0 0 0 4px;
      list-style: none; display: flex; flex-direction: column; gap: 8px;
    }
    .aip-bullets li {
      display: flex; align-items: flex-start; gap: 8px;
      font-size: 13px; line-height: 1.65; color: rgba(255,255,255,0.65);
      padding: 8px 12px;
      background: rgba(255,255,255,0.025); border-radius: 10px;
      border: 1px solid rgba(255,255,255,0.05);
    }
    .aip-bullets li::before {
      content: '›'; color: #6366f1; font-size: 16px; font-weight: 800;
      flex-shrink: 0; line-height: 1.4;
    }
    .aip-bullets li :global(strong) { color: rgba(255,255,255,0.88); font-weight: 700; }

    /* Resaltado (próximo hito, etc.) */
    .aip-highlight {
      background: rgba(16,185,129,0.07); border: 1px solid rgba(16,185,129,0.15);
      border-radius: 10px; padding: 10px 14px;
      font-size: 12px; color: #6ee7b7; line-height: 1.6;
    }
    .aip-highlight :global(strong) { color: #a7f3d0; font-weight: 700; }

    /* Tendencia */
    .aip-trend {
      display: flex; align-items: center; gap: 8px;
      padding: 8px 14px; background: rgba(255,255,255,0.04);
      border-radius: 10px; width: fit-content;
    }
    .aip-trend-label { font-size: 10px; color: rgba(255,255,255,0.35); font-weight: 700; text-transform: uppercase; letter-spacing: 0.08em; }
    .aip-trend-value { font-size: 11px; font-weight: 800; text-transform: uppercase; }
  `]
})
export class AiInsightPanelComponent {
  @Input() set recommendation(val: string) { this._raw = val; this._parse(); }
  @Input() trend: 'UP' | 'DOWN' | 'STABLE' | undefined;

  private sanitizer = inject(DomSanitizer);
  private _raw = '';
  parsedBlocks: Array<{type: string; html?: SafeHtml; items?: SafeHtml[]}> = [];

  get trendClass() {
    return {
      'text-red-400':     this.trend === 'UP',
      'text-emerald-400': this.trend === 'DOWN',
      'text-indigo-400':  this.trend === 'STABLE',
      'aip-trend-up':     this.trend === 'UP',
      'aip-trend-down':   this.trend === 'DOWN',
      'aip-trend-stable': this.trend === 'STABLE',
    };
  }

  private _parse(): void {
    const raw = (this._raw || '').trim();
    if (!raw) return;

    // Quitar comillas envolventes si vienen del backend
    let text = raw.replace(/^["']|["']$/g, '').trim();

    // El backend puede enviar todo en una sola línea con marcadores inline.
    // Paso 1: insertar \n antes de cada marcador estructural
    text = text
      .replace(/\s*(---+)\s*/g, '\n---\n')
      .replace(/\s*(#{1,3} )/g, '\n$1')
      .replace(/\s+-\s+\*\*/g, '\n- **')
      .replace(/\.\s+-\s+/g, '.\n- ')
      .replace(/\*\*Próximo hito\*\*/gi, '\n**Próximo hito**');

    // Paso 2: separar "## Heading BodyText" → "## Heading\nBodyText"
    // El heading termina cuando aparece texto en minúscula tras 1–5 palabras capitalizadas
    text = text.replace(
      /(##? [A-ZÁÉÍÓÚÑ\w()áéíóúñ ]{3,50}?)\s+([A-Za-záéíóúñ]+\s+[a-záéíóúñ])/g,
      '$1\n$2'
    );

    // Paso 3: separar meta-info (**...**) que viene pegada al título de sección
    text = text.replace(/(# [^\n*]+)\s+(\*\*[^\n]+\*\*)/g, '$1\n$2');

    const blocks: Array<{type: string; html?: SafeHtml; items?: SafeHtml[]}> = [];

    // Dividir por líneas y agrupar
    const lines = text.split('\n');
    let i = 0;

    while (i < lines.length) {
      const line = lines[i].trim();

      if (!line || line === '---' || line === '—') {
        if (line === '---' || line === '—') blocks.push({ type: 'divider' });
        i++; continue;
      }

      // S.O.S / Alerta
      if (line.startsWith('[S.O.S') || line.startsWith('⚠️') || line.includes('Protocolo de Contencion')) {
        blocks.push({ type: 'alert', html: this._toHtml(line.replace(/^\[S\.O\.S[^\]]*\]\s*/,'')) });
        i++; continue;
      }

      // # Título principal (H1)
      if (/^#\s/.test(line)) {
        blocks.push({ type: 'section-title', html: this._toHtml(line.replace(/^#+\s*/, '')) });
        i++; continue;
      }

      // ## Subtítulo (H2 o más)
      if (/^#{2,}\s/.test(line)) {
        const title = line.replace(/^#+\s*/, '');
        blocks.push({ type: 'heading', html: this._toHtml(title) });
        i++; continue;
      }

      // Línea de meta (negrita sola, tipo: **Familia | Riesgo | Mes**)
      if (/^\*\*[^*]+\*\*$/.test(line) || /^\*\*Familia/.test(line)) {
        blocks.push({ type: 'meta', html: this._toHtml(line) });
        i++; continue;
      }

      // Próximo hito
      if (line.toLowerCase().startsWith('**próximo hito') || line.toLowerCase().startsWith('**próximo')) {
        blocks.push({ type: 'highlight', html: this._toHtml(line) });
        i++; continue;
      }

      // Lista de bullets (- item)
      if (line.startsWith('- ') || line.startsWith('* ')) {
        const items: SafeHtml[] = [];
        while (i < lines.length && (lines[i].trim().startsWith('- ') || lines[i].trim().startsWith('* '))) {
          items.push(this._toHtml(lines[i].trim().replace(/^[-*]\s*/, '')));
          i++;
        }
        blocks.push({ type: 'bullets', items });
        continue;
      }

      // Párrafo normal — acumular líneas consecutivas
      const paraLines: string[] = [];
      while (i < lines.length) {
        const l = lines[i].trim();
        if (!l || /^#+\s/.test(l) || l === '---' || l.startsWith('- ') || l.startsWith('* ')
            || l.startsWith('[S.O.S') || /^\*\*[^*]+\*\*$/.test(l)) break;
        paraLines.push(l);
        i++;
      }
      if (paraLines.length) {
        const paraText = paraLines.join(' ');
        // ¿Es highlight?
        if (paraText.toLowerCase().includes('próximo hito')) {
          blocks.push({ type: 'highlight', html: this._toHtml(paraText) });
        } else {
          blocks.push({ type: 'paragraph', html: this._toHtml(paraText) });
        }
      } else {
        // Fallback de seguridad contra bucles infinitos
        i++;
      }
    }

    // Renombrar 'section-title' a 'section' para el template
    this.parsedBlocks = blocks.map(b => b.type === 'section-title' ? { ...b, type: 'section' } : b);
  }

  private _toHtml(text: string): SafeHtml {
    let h = text
      .replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;')
      .replace(/\*\*([^*]+)\*\*/g, '<strong>$1</strong>')
      .replace(/\*([^*]+)\*/g, '<em>$1</em>')
      .replace(/`([^`]+)`/g, '<code>$1</code>');
    return this.sanitizer.bypassSecurityTrustHtml(h);
  }
}
