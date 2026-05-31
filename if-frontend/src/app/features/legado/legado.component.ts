import { Component, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';

type LegadoTab = 'history' | 'constitution' | 'mission' | 'values' | 'letter';

interface FamilyValue { id: string; icon: string; name: string; description: string; }

@Component({
  selector: 'app-legado',
  standalone: true,
  imports: [CommonModule, FormsModule],
  template: `
    <div class="leg-page">

      <!-- Header -->
      <div class="leg-header">
        <div class="lh-icon">🏛️</div>
        <div>
          <div class="lh-title">Legado Familiar</div>
          <div class="lh-sub">Homenaje a la generación anterior · Misión para las generaciones futuras</div>
        </div>
      </div>

      <!-- Tabs -->
      <div class="tabs">
        <button class="tab" [class.active]="activeTab() === 'history'"      (click)="activeTab.set('history')">      📖 Historia</button>
        <button class="tab" [class.active]="activeTab() === 'constitution'" (click)="activeTab.set('constitution')"> 📜 Constitución</button>
        <button class="tab" [class.active]="activeTab() === 'mission'"      (click)="activeTab.set('mission')">      🎯 Misión &amp; Visión</button>
        <button class="tab" [class.active]="activeTab() === 'values'"       (click)="activeTab.set('values')">       💎 Valores</button>
        <button class="tab" [class.active]="activeTab() === 'letter'"       (click)="activeTab.set('letter')">       ✉️ Carta al Futuro</button>
      </div>

      <!-- ── HISTORIA ─────────────────────────────────────────── -->
      @if (activeTab() === 'history') {
        <div class="section-card">
          <div class="sc-title">Historia Familiar</div>
          <div class="reflection-questions">
            <div class="rq-item">
              <label class="rq-label">¿Qué nos enseñaron nuestros padres y abuelos?</label>
              <textarea class="rq-input" rows="3" [(ngModel)]="history.lessons" placeholder="Escribe aquí…"></textarea>
            </div>
            <div class="rq-item">
              <label class="rq-label">¿Qué debemos conservar de ellos?</label>
              <textarea class="rq-input" rows="3" [(ngModel)]="history.conserve" placeholder="Tradiciones, valores, formas de vida…"></textarea>
            </div>
            <div class="rq-item">
              <label class="rq-label">¿Qué errores no repetiremos?</label>
              <textarea class="rq-input" rows="3" [(ngModel)]="history.avoidErrors" placeholder="Con amor y sin juicio…"></textarea>
            </div>
            <div class="rq-item">
              <label class="rq-label">¿Qué queremos dejar a nuestros hijos?</label>
              <textarea class="rq-input" rows="3" [(ngModel)]="history.toLeave" placeholder="¿Qué legado material y espiritual?"></textarea>
            </div>
          </div>
          <div class="recognition-box">
            <div class="rb-title">🙏 Reconocimiento Explícito a la Generación Anterior</div>
            <textarea class="rq-input" rows="4" [(ngModel)]="history.recognition"
              placeholder="Escriban un homenaje a sus padres/abuelos reconociendo su misión, sus sacrificios y el amor que dejaron como herencia…"></textarea>
          </div>
          <button class="btn-save">💾 Guardar Historia</button>
        </div>
      }

      <!-- ── CONSTITUCIÓN ─────────────────────────────────────── -->
      @if (activeTab() === 'constitution') {
        <div class="section-card">
          <div class="sc-title">Constitución Familiar</div>
          <div class="sc-subtitle">El documento fundacional que guía a nuestra familia.</div>

          <div class="constitution-doc">
            <div class="cd-header">
              <div class="cd-family-name">
                <label>Nombre de la familia</label>
                <input [(ngModel)]="constitution.familyName" placeholder="Familia ___________" class="cd-input" />
              </div>
              <div class="cd-date">Establecida en {{ constitution.year || currentYear }}</div>
            </div>

            <div class="cd-section">
              <div class="cd-sec-title">§ 1. Principio Fundador</div>
              <textarea [(ngModel)]="constitution.foundingPrinciple" rows="2" class="rq-input" placeholder="El propósito central que nos une como familia…"></textarea>
            </div>
            <div class="cd-section">
              <div class="cd-sec-title">§ 2. Compromisos Sagrados</div>
              <textarea [(ngModel)]="constitution.commitments" rows="3" class="rq-input" placeholder="Nos comprometemos a…"></textarea>
            </div>
            <div class="cd-section">
              <div class="cd-sec-title">§ 3. Lo que Nunca Haremos</div>
              <textarea [(ngModel)]="constitution.neverDo" rows="2" class="rq-input" placeholder="Jamás en esta familia…"></textarea>
            </div>
            <div class="cd-section">
              <div class="cd-sec-title">§ 4. Cómo Resolvemos Conflictos</div>
              <textarea [(ngModel)]="constitution.conflictResolution" rows="2" class="rq-input" placeholder="Cuando haya desacuerdo…"></textarea>
            </div>
          </div>
          <button class="btn-save">📜 Guardar Constitución</button>
        </div>
      }

      <!-- ── MISIÓN & VISIÓN ──────────────────────────────────── -->
      @if (activeTab() === 'mission') {
        <div class="section-card">
          <div class="sc-title">Misión &amp; Visión Familiar</div>
          <div class="mv-grid">
            <div class="mv-card">
              <div class="mv-icon">🎯</div>
              <div class="mv-label">Misión Familiar</div>
              <div class="mv-hint">¿Para qué existe nuestra familia hoy?</div>
              <textarea rows="4" class="rq-input" [(ngModel)]="missionVision.mission" placeholder="Nuestra familia existe para…"></textarea>
            </div>
            <div class="mv-card">
              <div class="mv-icon">🌅</div>
              <div class="mv-label">Visión Familiar</div>
              <div class="mv-hint">¿Cómo queremos ser recordados en 30 años?</div>
              <textarea rows="4" class="rq-input" [(ngModel)]="missionVision.vision" placeholder="En 30 años, nuestra familia será…"></textarea>
            </div>
          </div>
          <div class="tag-line-section">
            <label class="rq-label">Lema familiar (tagline)</label>
            <input class="cd-input" [(ngModel)]="missionVision.tagline" placeholder="Ej: Unidos, crecemos. Separados, fallamos." />
          </div>
          <button class="btn-save">✅ Guardar Misión &amp; Visión</button>
        </div>
      }

      <!-- ── VALORES ──────────────────────────────────────────── -->
      @if (activeTab() === 'values') {
        <div class="section-card">
          <div class="sc-title">Valores Familiares</div>
          <div class="sc-subtitle">Los 3–7 valores que definen quiénes somos.</div>
          <div class="values-grid">
            @for (v of familyValues(); track v.id) {
              <div class="value-card">
                <input class="value-icon-inp" [(ngModel)]="v.icon" placeholder="🌟" maxlength="2" />
                <input class="value-name-inp" [(ngModel)]="v.name" placeholder="Nombre del valor" />
                <textarea rows="2" class="rq-input" [(ngModel)]="v.description" placeholder="¿Qué significa para nuestra familia?"></textarea>
                <button class="btn-rm-val" (click)="removeValue(v.id)">✕</button>
              </div>
            }
            @if (familyValues().length < 7) {
              <button class="add-value-btn" (click)="addValue()">+ Agregar valor</button>
            }
          </div>
          <button class="btn-save">💎 Guardar Valores</button>
        </div>
      }

      <!-- ── CARTA AL FUTURO ──────────────────────────────────── -->
      @if (activeTab() === 'letter') {
        <div class="section-card">
          <div class="sc-title">Carta a las Generaciones Futuras</div>
          <div class="sc-subtitle">Una carta de amor y esperanza para tus descendientes.</div>
          <div class="letter-meta">
            <input class="cd-input" [(ngModel)]="letter.from" placeholder="De parte de…" />
            <input class="cd-input" [(ngModel)]="letter.to" placeholder="Para…" />
            <input class="cd-input" [(ngModel)]="letter.openIn" placeholder="Abrir en el año…" />
          </div>
          <div class="letter-box">
            <textarea rows="14" class="letter-textarea" [(ngModel)]="letter.content"
              placeholder="Querida generación futura,&#10;&#10;Cuando lean esto ya no estaremos aquí, pero lo que fuimos, lo que amamos y lo que construimos juntos…"></textarea>
          </div>
          <button class="btn-save letter-btn">✉️ Sellar Carta del Legado</button>
        </div>
      }

    </div>
  `,
  styles: [`
    .leg-page { max-width: 860px; margin: 0 auto; }
    .leg-header { display: flex; align-items: center; gap: 16px; margin-bottom: 24px; }
    .lh-icon  { font-size: 44px; }
    .lh-title { font-size: 24px; font-weight: 800; color: #fff; margin-bottom: 4px; }
    .lh-sub   { font-size: 13px; color: rgba(255,255,255,0.4); }

    .tabs { display: flex; gap: 8px; margin-bottom: 24px; flex-wrap: wrap; }
    .tab  { padding: 9px 16px; border-radius: 10px; background: rgba(255,255,255,0.03); border: 1px solid rgba(255,255,255,0.07); color: rgba(255,255,255,0.5); font-size: 12px; font-weight: 600; cursor: pointer; transition: all 0.2s; }
    .tab:hover { background: rgba(255,255,255,0.06); border-color: rgba(255,255,255,0.12); }
    .tab.active { background: rgba(251,191,36,0.1); border-color: rgba(251,191,36,0.3); color: #fbbf24; }

    .section-card { background: rgba(255,255,255,0.02); border: 1px solid rgba(255,255,255,0.07); border-radius: 16px; padding: 24px; }
    .sc-title    { font-size: 18px; font-weight: 800; color: #fff; margin-bottom: 6px; }
    .sc-subtitle { font-size: 13px; color: rgba(255,255,255,0.4); margin-bottom: 20px; }

    .reflection-questions { display: flex; flex-direction: column; gap: 16px; margin-bottom: 20px; }
    .rq-item  { display: flex; flex-direction: column; gap: 6px; }
    .rq-label { font-size: 13px; font-weight: 600; color: rgba(255,255,255,0.6); }
    .rq-input { background: rgba(255,255,255,0.04); border: 1px solid rgba(255,255,255,0.08); border-radius: 10px; color: rgba(255,255,255,0.75); font-size: 13px; padding: 10px 14px; resize: vertical; outline: none; font-family: inherit; line-height: 1.5; width: 100%; box-sizing: border-box; }
    .rq-input:focus { border-color: rgba(251,191,36,0.3); }

    .recognition-box { background: rgba(251,191,36,0.04); border: 1px solid rgba(251,191,36,0.15); border-radius: 12px; padding: 16px; margin-bottom: 20px; }
    .rb-title { font-size: 13px; font-weight: 700; color: #fbbf24; margin-bottom: 10px; }

    .btn-save { font-size: 13px; font-weight: 700; padding: 11px 24px; background: rgba(251,191,36,0.12); border: 1px solid rgba(251,191,36,0.25); color: #fbbf24; border-radius: 10px; cursor: pointer; transition: all 0.2s; }
    .btn-save:hover { background: rgba(251,191,36,0.22); }

    /* Constitution */
    .constitution-doc { background: rgba(255,255,255,0.02); border: 1px solid rgba(255,255,255,0.06); border-radius: 12px; padding: 20px; margin-bottom: 20px; }
    .cd-header { display: flex; justify-content: space-between; align-items: flex-end; margin-bottom: 20px; flex-wrap: wrap; gap: 12px; }
    .cd-family-name label { font-size: 11px; color: rgba(255,255,255,0.4); display: block; margin-bottom: 4px; }
    .cd-input { background: rgba(255,255,255,0.04); border: none; border-bottom: 1px solid rgba(255,255,255,0.1); color: #fff; font-size: 16px; font-weight: 700; padding: 4px 0; outline: none; width: 100%; }
    .cd-date  { font-size: 11px; color: rgba(255,255,255,0.3); }
    .cd-section { margin-bottom: 16px; }
    .cd-sec-title { font-size: 12px; font-weight: 700; color: #fbbf24; margin-bottom: 8px; letter-spacing: 0.04em; }

    /* Mission */
    .mv-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 16px; margin-bottom: 20px; }
    @media(max-width:640px) { .mv-grid { grid-template-columns: 1fr; } }
    .mv-card  { background: rgba(255,255,255,0.02); border: 1px solid rgba(255,255,255,0.07); border-radius: 12px; padding: 16px; }
    .mv-icon  { font-size: 24px; margin-bottom: 8px; }
    .mv-label { font-size: 14px; font-weight: 700; color: #fff; margin-bottom: 4px; }
    .mv-hint  { font-size: 11px; color: rgba(255,255,255,0.4); margin-bottom: 10px; }
    .tag-line-section { margin-bottom: 20px; }

    /* Values */
    .values-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(220px,1fr)); gap: 12px; margin-bottom: 20px; }
    .value-card  { background: rgba(255,255,255,0.02); border: 1px solid rgba(255,255,255,0.07); border-radius: 12px; padding: 14px; position: relative; }
    .value-icon-inp { background: none; border: none; font-size: 24px; color: #fff; outline: none; width: 36px; text-align: center; margin-bottom: 6px; }
    .value-name-inp { background: none; border: none; border-bottom: 1px solid rgba(255,255,255,0.1); color: #fff; font-size: 14px; font-weight: 700; outline: none; width: 100%; margin-bottom: 8px; padding-bottom: 4px; }
    .btn-rm-val { position: absolute; top: 8px; right: 8px; background: none; border: none; color: rgba(255,255,255,0.2); cursor: pointer; font-size: 12px; }
    .btn-rm-val:hover { color: #ef4444; }
    .add-value-btn { background: rgba(255,255,255,0.03); border: 2px dashed rgba(255,255,255,0.1); border-radius: 12px; color: rgba(255,255,255,0.3); font-size: 13px; font-weight: 700; cursor: pointer; padding: 20px; transition: all 0.2s; }
    .add-value-btn:hover { border-color: rgba(251,191,36,0.3); color: #fbbf24; }

    /* Letter */
    .letter-meta { display: flex; gap: 12px; flex-wrap: wrap; margin-bottom: 16px; }
    .letter-meta .cd-input { flex: 1; min-width: 140px; }
    .letter-box { margin-bottom: 20px; }
    .letter-textarea { width: 100%; background: rgba(255,248,220,0.03); border: 1px solid rgba(251,191,36,0.12); border-radius: 12px; color: rgba(255,255,255,0.8); font-size: 14px; padding: 16px; resize: none; outline: none; font-family: 'Georgia', serif; line-height: 1.8; box-sizing: border-box; }
    .letter-textarea:focus { border-color: rgba(251,191,36,0.3); }
    .letter-btn { background: rgba(251,191,36,0.15) !important; border-color: rgba(251,191,36,0.35) !important; }
  `]
})
export class LegadoComponent {
  readonly activeTab = signal<LegadoTab>('history');
  readonly currentYear = new Date().getFullYear();

  history = {
    lessons: '', conserve: '', avoidErrors: '', toLeave: '', recognition: ''
  };
  constitution = {
    familyName: '', year: '', foundingPrinciple: '', commitments: '', neverDo: '', conflictResolution: ''
  };
  missionVision = { mission: '', vision: '', tagline: '' };
  letter = { from: '', to: '', openIn: '', content: '' };

  readonly familyValues = signal<FamilyValue[]>([
    { id: '1', icon: '❤️', name: 'Amor',       description: '' },
    { id: '2', icon: '🤝', name: 'Respeto',     description: '' },
    { id: '3', icon: '✨', name: 'Integridad',  description: '' },
  ]);

  addValue() {
    const id = Date.now().toString();
    this.familyValues.update(v => [...v, { id, icon: '⭐', name: '', description: '' }]);
  }

  removeValue(id: string) {
    this.familyValues.update(v => v.filter(x => x.id !== id));
  }
}
