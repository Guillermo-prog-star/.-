import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { FormsModule } from '@angular/forms';
import {
  FamilyTreeService, FamilyTreeNode, HeritageDto,
  AncestorHeritage, GenerationalMessage, MessageRequest
} from '../../core/services/family-tree.service';
import { FamilyStateService } from '../../core/services/family-state.service';
import { catchError, of } from 'rxjs';

type ActiveTab = 'tree' | 'heritage' | 'messages' | 'write' | 'link';

const MSG_ICONS: Record<string, string> = {
  LETTER: '✉️', WISDOM: '🧠', WARNING: '⚠️', BLESSING: '🌟'
};

@Component({
  selector: 'app-family-tree',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink],
  template: `
    <div class="ft-page">

      <!-- Header -->
      <div class="ft-header">
        <div class="ft-icon">🌳</div>
        <div>
          <h1 class="ft-title">Árbol Generacional</h1>
          <p class="ft-sub">La historia de tu familia a través del tiempo — lo que heredaste y lo que dejarás</p>
        </div>
      </div>

      <!-- Tabs -->
      <div class="ft-tabs">
        <button class="ft-tab" [class.active]="tab() === 'tree'"     (click)="tab.set('tree')">     🌳 Árbol</button>
        <button class="ft-tab" [class.active]="tab() === 'heritage'" (click)="loadHeritage(); tab.set('heritage')">🏛️ Herencia</button>
        <button class="ft-tab" [class.active]="tab() === 'messages'" (click)="loadMessages(); tab.set('messages')">✉️ Mensajes</button>
        <button class="ft-tab" [class.active]="tab() === 'write'"    (click)="tab.set('write')">    ✍️ Escribir</button>
        <button class="ft-tab" [class.active]="tab() === 'link'"     (click)="tab.set('link')">     🔗 Vincular</button>
      </div>

      <!-- Cross-link to lineage -->
      <div class="ft-lineage-banner">
        <span class="flb-icon">🌿</span>
        <div class="flb-text">
          <strong>¿Buscas el Árbol de Evolución y Legado?</strong>
          Miembros individuales, valores, aprendizajes y legado generacional están en
          <a routerLink="/lineage" class="flb-link">Linaje Generacional →</a>
        </div>
      </div>

      <!-- ── ÁRBOL ─────────────────────────────────────────────── -->
      @if (tab() === 'tree') {
        <div class="ft-section">
          @if (loadingTree()) {
            <div class="ft-loading"><div class="ft-spinner"></div><p>Construyendo el árbol...</p></div>
          }
          @if (!loadingTree() && treeRoot()) {
            <div class="tree-container">
              <ng-container *ngTemplateOutlet="treeNode; context: { $implicit: treeRoot() }"></ng-container>
            </div>
          }
          @if (!loadingTree() && !treeRoot()) {
            <div class="ft-empty">
              <div class="ei">🌱</div>
              <h2>Tu árbol aún no tiene ramas</h2>
              <p>Vincula tu familia con la familia que le dio origen para construir el árbol generacional.</p>
              <button class="btn-primary" (click)="tab.set('link')">🔗 Vincular familia origen</button>
            </div>
          }
        </div>
      }

      <!-- Template recursivo del árbol -->
      <ng-template #treeNode let-node>
        @if (node) {
          <div class="tree-node" [class.root]="node.generation === 0">
            <div class="node-card" [class.current]="node.familyId === familyId()">
              <div class="nc-top">
                <span class="nc-gen">Gen {{ node.generation + 1 }}</span>
                @if (node.familyId === familyId()) {
                  <span class="nc-you">← Tú</span>
                }
              </div>
              <div class="nc-name">{{ node.familyName }}</div>
              @if (node.familyCode) {
                <div class="nc-code">{{ node.familyCode }}</div>
              }
              @if (node.createdAt) {
                <div class="nc-date">Fundada {{ formatYear(node.createdAt) }}</div>
              }
              <div class="nc-stats">
                <span>👤 {{ node.memberCount }}</span>
                <span>📸 {{ node.evidenceCount }}</span>
                <span>💖 {{ node.gratitudeCount }}</span>
              </div>
              @if (node.dnaValores) {
                <div class="nc-values">{{ node.dnaValores }}</div>
              }
            </div>
            @if (node.children?.length) {
              <div class="tree-children">
                @for (child of node.children; track child.familyId) {
                  <ng-container *ngTemplateOutlet="treeNode; context: { $implicit: child }"></ng-container>
                }
              </div>
            }
          </div>
        }
      </ng-template>

      <!-- ── HERENCIA ──────────────────────────────────────────── -->
      @if (tab() === 'heritage') {
        <div class="ft-section">
          @if (loadingHeritage()) {
            <div class="ft-loading"><div class="ft-spinner"></div><p>Recuperando herencia ancestral...</p></div>
          }
          @if (!loadingHeritage() && heritage()) {
            @if (!heritage()!.ancestors.length) {
              <div class="ft-empty">
                <div class="ei">🏛️</div>
                <h2>Sin herencia registrada</h2>
                <p>Vincula tu familia con su familia origen para acceder a la herencia de tus ancestros.</p>
              </div>
            } @else {
              <div class="heritage-list">
                @for (anc of heritage()!.ancestors; track anc.familyId) {
                  <div class="ancestor-card">
                    <div class="anc-header">
                      <div class="anc-badge">Generación {{ anc.generation }}</div>
                      <div class="anc-name">{{ anc.familyName }}</div>
                      @if (anc.familyCode) { <div class="anc-code">{{ anc.familyCode }}</div> }
                    </div>

                    <div class="anc-stats">
                      <span>📸 {{ anc.evidenceCount }} evidencias</span>
                      <span>💖 {{ anc.gratitudeCount }} gratitudes</span>
                    </div>

                    @if (anc.dnaNarrativeIa) {
                      <div class="heritage-block">
                        <div class="hb-label">🧬 Esencia de la familia</div>
                        <p class="hb-text">{{ anc.dnaNarrativeIa }}</p>
                      </div>
                    }
                    @if (anc.foundingPrinciple) {
                      <div class="heritage-block">
                        <div class="hb-label">📜 Principio fundador</div>
                        <p class="hb-text">{{ anc.foundingPrinciple }}</p>
                      </div>
                    }
                    @if (anc.familyMission) {
                      <div class="heritage-block">
                        <div class="hb-label">🎯 Misión familiar</div>
                        <p class="hb-text">{{ anc.familyMission }}</p>
                      </div>
                    }
                    @if (anc.historyLessons) {
                      <div class="heritage-block">
                        <div class="hb-label">📖 Lecciones de la historia</div>
                        <p class="hb-text">{{ anc.historyLessons }}</p>
                      </div>
                    }
                    @if (anc.historyRecognition) {
                      <div class="heritage-block">
                        <div class="hb-label">🙏 Reconocimiento ancestral</div>
                        <p class="hb-text">{{ anc.historyRecognition }}</p>
                      </div>
                    }
                    @if (anc.dnaValues) {
                      <div class="heritage-block">
                        <div class="hb-label">💎 Valores heredados</div>
                        <p class="hb-text">{{ anc.dnaValues }}</p>
                      </div>
                    }

                    @if (anc.readableMessages.length) {
                      <div class="heritage-block">
                        <div class="hb-label">✉️ Mensajes para ti ({{ anc.readableMessages.length }})</div>
                        @for (msg of anc.readableMessages; track msg.id) {
                          <div class="msg-card">
                            <div class="msg-top">
                              <span class="msg-icon">{{ msgIcon(msg.messageType) }}</span>
                              <span class="msg-subject">{{ msg.subject || 'Mensaje de ' + msg.authorName }}</span>
                              <span class="msg-year">{{ msg.fromYear }}</span>
                            </div>
                            <p class="msg-body">{{ msg.content }}</p>
                            <div class="msg-author">— {{ msg.authorName }}</div>
                          </div>
                        }
                      </div>
                    }
                  </div>
                }
              </div>
            }
          }
        </div>
      }

      <!-- ── MENSAJES ───────────────────────────────────────────── -->
      @if (tab() === 'messages') {
        <div class="ft-section">
          @if (loadingMessages()) {
            <div class="ft-loading"><div class="ft-spinner"></div><p>Cargando mensajes...</p></div>
          }
          @if (!loadingMessages()) {
            @if (ancestorMessages().length) {
              <div class="msgs-section">
                <div class="msgs-label">📬 Mensajes de tus ancestros</div>
                @for (msg of ancestorMessages(); track msg.id) {
                  <div class="msg-card">
                    <div class="msg-top">
                      <span class="msg-icon">{{ msgIcon(msg.messageType) }}</span>
                      <span class="msg-subject">{{ msg.subject || 'Sin asunto' }}</span>
                      <span class="msg-year">{{ formatYear(msg.createdAt) }}</span>
                    </div>
                    <p class="msg-body">{{ msg.content }}</p>
                    <div class="msg-author">— {{ msg.authorName }}</div>
                  </div>
                }
              </div>
            }
            @if (ownMessages().length) {
              <div class="msgs-section" style="margin-top: 28px">
                <div class="msgs-label">✍️ Mensajes que tú has escrito</div>
                @for (msg of ownMessages(); track msg.id) {
                  <div class="msg-card msg-card--own">
                    <div class="msg-top">
                      <span class="msg-icon">{{ msgIcon(msg.messageType) }}</span>
                      <span class="msg-subject">{{ msg.subject || 'Sin asunto' }}</span>
                      @if (msg.openInYear) {
                        <span class="msg-seal">🔒 Se abre en {{ msg.openInYear }}</span>
                      }
                    </div>
                    <p class="msg-body">{{ msg.content }}</p>
                    <div class="msg-author">— {{ msg.authorName }}</div>
                  </div>
                }
              </div>
            }
            @if (!ancestorMessages().length && !ownMessages().length) {
              <div class="ft-empty">
                <div class="ei">✉️</div>
                <h2>Sin mensajes aún</h2>
                <p>Escribe el primer mensaje para las generaciones futuras de tu familia.</p>
                <button class="btn-primary" (click)="tab.set('write')">✍️ Escribir mensaje</button>
              </div>
            }
          }
        </div>
      }

      <!-- ── ESCRIBIR MENSAJE ──────────────────────────────────── -->
      @if (tab() === 'write') {
        <div class="ft-section">
          <div class="write-form">
            <div class="wf-title">✍️ Mensaje para las generaciones futuras</div>

            <div class="field-group">
              <label class="field-label">Tipo de mensaje</label>
              <div class="type-grid">
                @for (t of msgTypes; track t.key) {
                  <button class="type-btn" [class.sel]="newMsg.messageType === t.key" (click)="newMsg.messageType = t.key">
                    <span>{{ t.icon }}</span> <span>{{ t.label }}</span>
                  </button>
                }
              </div>
            </div>

            <div class="field-group">
              <label class="field-label">Tu nombre</label>
              <input class="field-input" [(ngModel)]="newMsg.authorName" placeholder="Quien escribe este mensaje" maxlength="100" />
            </div>

            <div class="field-group">
              <label class="field-label">Asunto (opcional)</label>
              <input class="field-input" [(ngModel)]="newMsg.subject" placeholder="¿De qué trata este mensaje?" maxlength="150" />
            </div>

            <div class="field-group">
              <label class="field-label">Mensaje</label>
              <textarea class="field-textarea" [(ngModel)]="newMsg.content"
                placeholder="Escribe lo que quieres que las próximas generaciones sepan, sientan o recuerden..."
                rows="8" maxlength="3000"></textarea>
              <div class="char-count">{{ newMsg.content.length || 0 }} / 3000</div>
            </div>

            <div class="field-group">
              <label class="field-label">¿Cuándo se puede abrir? (opcional)</label>
              <input class="field-input" type="number" [(ngModel)]="newMsg.openInYear"
                placeholder="Ej: 2050 — dejar vacío para abrir inmediatamente"
                min="2025" max="2200" />
            </div>

            @if (writeError()) { <div class="ft-error">{{ writeError() }}</div> }

            <button class="btn-submit" (click)="sendMessage()" [disabled]="sending()">
              {{ sending() ? '⏳ Enviando...' : '🌳 Enviar al árbol generacional' }}
            </button>

            @if (writeDone()) {
              <div class="write-success">
                ✅ Tu mensaje quedó guardado en el árbol generacional.
                @if (newMsg.openInYear) {
                  Se abrirá en {{ newMsg.openInYear }}.
                }
              </div>
            }
          </div>
        </div>
      }

      <!-- ── VINCULAR ───────────────────────────────────────────── -->
      @if (tab() === 'link') {
        <div class="ft-section">
          <div class="link-form">
            <div class="wf-title">🔗 Vincular con familia origen</div>
            <p class="link-desc">
              Si tu familia desciende de otra familia que también usa Integrity Family,
              ingresen el código de esa familia para conectar las generaciones.
            </p>

            <div class="field-group">
              <label class="field-label">Código de la familia origen</label>
              <input class="field-input" [(ngModel)]="linkCode"
                placeholder="Ej: IF-CO-2026-0001" maxlength="30" />
            </div>
            <div class="field-group">
              <label class="field-label">Tu nombre</label>
              <input class="field-input" [(ngModel)]="linkMember"
                placeholder="Quien crea este vínculo" maxlength="100" />
            </div>
            <div class="field-group">
              <label class="field-label">Nota (opcional)</label>
              <input class="field-input" [(ngModel)]="linkNote"
                placeholder="Ej: Hijo mayor de la familia fundadora" maxlength="200" />
            </div>

            @if (linkError()) { <div class="ft-error">{{ linkError() }}</div> }
            @if (linkDone()) { <div class="write-success">✅ Vinculación exitosa. El árbol ha sido actualizado.</div> }

            <button class="btn-submit" (click)="doLink()" [disabled]="linking() || !linkCode.trim()">
              {{ linking() ? '⏳ Vinculando...' : '🔗 Crear vínculo generacional' }}
            </button>
          </div>
        </div>
      }

      @if (globalError()) {
        <div class="ft-error" style="margin-top: 20px">⚠️ {{ globalError() }}</div>
      }

    </div>
  `,
  styles: [`
    .ft-page {
      max-width: 860px; margin: 0 auto;
      padding: 24px 20px 60px;
      font-family: inherit;
      color: var(--if-text-primary, #e0e0e0);
    }

    /* Header */
    .ft-header { display: flex; align-items: center; gap: 14px; margin-bottom: 24px; }
    .ft-icon   { font-size: 42px; }
    .ft-title  { font-size: 24px; font-weight: 800; margin: 0 0 4px; }
    .ft-sub    { font-size: 13px; color: var(--if-text-secondary, #888); margin: 0; }

    /* Lineage cross-link banner */
    .ft-lineage-banner {
      display: flex; align-items: center; gap: 12px;
      background: linear-gradient(135deg, #1c1005 0%, #0d0d0d 100%);
      border: 1px solid #78350f40; border-radius: 12px;
      padding: 12px 16px; margin-bottom: 24px; font-size: 13px;
    }
    .flb-icon { font-size: 20px; flex-shrink: 0; }
    .flb-text { color: #9ca3af; }
    .flb-text strong { color: #d97706; font-weight: 600; }
    .flb-link { color: #fbbf24; text-decoration: none; font-weight: 600; }
    .flb-link:hover { text-decoration: underline; }

    /* Tabs */
    .ft-tabs {
      display: flex; gap: 6px; flex-wrap: wrap;
      margin-bottom: 28px;
      border-bottom: 1px solid rgba(255,255,255,0.07);
      padding-bottom: 12px;
    }
    .ft-tab {
      padding: 7px 16px; border-radius: 8px;
      background: transparent;
      border: 1px solid rgba(255,255,255,0.08);
      color: var(--if-text-secondary, #aaa);
      font-size: 13px; font-weight: 600; cursor: pointer;
      transition: all 0.18s;
    }
    .ft-tab:hover { background: rgba(255,255,255,0.06); }
    .ft-tab.active {
      background: rgba(34,197,94,0.12);
      border-color: rgba(34,197,94,0.3);
      color: #86efac;
    }

    /* Loading / vacío */
    .ft-loading { display: flex; flex-direction: column; align-items: center; gap: 14px; padding: 48px 20px; color: var(--if-text-secondary, #888); }
    .ft-spinner { width: 36px; height: 36px; border: 3px solid rgba(255,255,255,0.08); border-top-color: #22c55e; border-radius: 50%; animation: spin 0.8s linear infinite; }
    @keyframes spin { to { transform: rotate(360deg); } }
    .ft-empty  { text-align: center; padding: 52px 20px; color: var(--if-text-secondary, #888); }
    .ei { font-size: 48px; margin-bottom: 14px; }
    .ft-empty h2 { font-size: 18px; font-weight: 700; color: var(--if-text-primary, #ccc); margin: 0 0 10px; }

    /* Árbol visual */
    .tree-container { padding: 8px 0; }
    .tree-node { display: flex; flex-direction: column; align-items: flex-start; }
    .tree-children {
      padding-left: 40px;
      border-left: 2px solid rgba(34,197,94,0.2);
      margin-left: 20px;
      margin-top: 6px;
    }
    .node-card {
      background: var(--if-surface, rgba(255,255,255,0.04));
      border: 1px solid rgba(255,255,255,0.08);
      border-radius: 14px; padding: 16px 18px;
      margin-bottom: 10px; min-width: 220px;
      transition: transform 0.18s;
    }
    .node-card:hover { transform: translateX(3px); }
    .node-card.current {
      border-color: rgba(34,197,94,0.4);
      background: rgba(34,197,94,0.07);
    }
    .nc-top { display: flex; gap: 8px; align-items: center; margin-bottom: 6px; }
    .nc-gen { font-size: 10px; font-weight: 700; color: #4ade80; text-transform: uppercase; letter-spacing: 0.06em; }
    .nc-you { font-size: 10px; color: #86efac; font-weight: 600; }
    .nc-name { font-size: 15px; font-weight: 700; margin-bottom: 2px; }
    .nc-code { font-size: 10px; color: var(--if-text-secondary, #777); font-family: monospace; margin-bottom: 4px; }
    .nc-date { font-size: 11px; color: var(--if-text-secondary, #888); margin-bottom: 8px; }
    .nc-stats { display: flex; gap: 12px; font-size: 12px; color: var(--if-text-secondary, #999); margin-bottom: 6px; }
    .nc-values { font-size: 11px; color: #fbbf24; font-style: italic; }

    /* Herencia */
    .heritage-list { display: flex; flex-direction: column; gap: 20px; }
    .ancestor-card {
      background: rgba(255,255,255,0.03);
      border: 1px solid rgba(255,255,255,0.07);
      border-left: 4px solid #22c55e;
      border-radius: 16px; padding: 20px;
    }
    .anc-header { margin-bottom: 12px; }
    .anc-badge { font-size: 10px; font-weight: 700; color: #4ade80; text-transform: uppercase; letter-spacing: 0.08em; margin-bottom: 4px; }
    .anc-name  { font-size: 18px; font-weight: 800; margin-bottom: 2px; }
    .anc-code  { font-size: 11px; color: var(--if-text-secondary, #777); font-family: monospace; }
    .anc-stats { display: flex; gap: 16px; font-size: 12px; color: var(--if-text-secondary, #999); margin-bottom: 14px; }

    .heritage-block { margin-bottom: 14px; }
    .hb-label { font-size: 11px; font-weight: 700; color: var(--if-text-secondary, #888); text-transform: uppercase; letter-spacing: 0.07em; margin-bottom: 6px; }
    .hb-text  { font-size: 14px; line-height: 1.65; color: var(--if-text-primary, #ccc); margin: 0; }

    /* Mensajes */
    .msgs-section { }
    .msgs-label { font-size: 11px; font-weight: 800; letter-spacing: 0.1em; text-transform: uppercase; color: var(--if-text-secondary, #666); margin-bottom: 14px; }
    .msg-card {
      background: rgba(255,255,255,0.04);
      border: 1px solid rgba(255,255,255,0.08);
      border-radius: 14px; padding: 16px 18px;
      margin-bottom: 12px;
    }
    .msg-card--own { border-color: rgba(251,191,36,0.2); background: rgba(251,191,36,0.04); }
    .msg-top { display: flex; align-items: center; gap: 10px; margin-bottom: 10px; }
    .msg-icon    { font-size: 18px; flex-shrink: 0; }
    .msg-subject { font-size: 14px; font-weight: 700; flex: 1; }
    .msg-year    { font-size: 11px; color: var(--if-text-secondary, #777); white-space: nowrap; }
    .msg-seal    { font-size: 11px; color: #fbbf24; white-space: nowrap; }
    .msg-body    { font-size: 13px; line-height: 1.65; color: var(--if-text-primary, #ccc); margin: 0 0 10px; }
    .msg-author  { font-size: 11px; color: var(--if-text-secondary, #777); font-style: italic; }

    /* Formularios */
    .write-form, .link-form { max-width: 600px; display: flex; flex-direction: column; gap: 18px; }
    .wf-title { font-size: 18px; font-weight: 700; margin-bottom: 4px; }
    .link-desc { font-size: 13px; color: var(--if-text-secondary, #aaa); line-height: 1.6; margin: 0; }

    .type-grid { display: grid; grid-template-columns: repeat(4, 1fr); gap: 8px; }
    .type-btn {
      display: flex; flex-direction: column; align-items: center; gap: 4px;
      padding: 10px 6px; border-radius: 10px;
      background: rgba(255,255,255,0.04);
      border: 1px solid rgba(255,255,255,0.08);
      color: var(--if-text-primary, #ddd);
      font-size: 12px; cursor: pointer; transition: all 0.18s;
    }
    .type-btn.sel { background: rgba(34,197,94,0.12); border-color: rgba(34,197,94,0.35); color: #86efac; }

    .field-group { display: flex; flex-direction: column; gap: 6px; }
    .field-label { font-size: 11px; font-weight: 700; text-transform: uppercase; letter-spacing: 0.07em; color: var(--if-text-secondary, #aaa); }
    .field-input, .field-textarea {
      padding: 10px 14px; border-radius: 10px;
      background: rgba(255,255,255,0.05);
      border: 1px solid rgba(255,255,255,0.1);
      color: var(--if-text-primary, #e0e0e0);
      font-size: 14px; font-family: inherit;
    }
    .field-input:focus, .field-textarea:focus { outline: none; border-color: rgba(34,197,94,0.4); }
    .field-textarea { resize: vertical; line-height: 1.6; }
    .char-count { font-size: 11px; color: var(--if-text-secondary, #777); text-align: right; }

    .btn-primary, .btn-submit {
      background: linear-gradient(135deg, #16a34a, #15803d);
      border: none; color: white;
      padding: 12px 28px; border-radius: 10px;
      font-size: 14px; font-weight: 700; cursor: pointer;
      transition: opacity 0.2s; align-self: flex-start;
    }
    .btn-primary { margin-top: 12px; }
    .btn-submit:disabled { opacity: 0.5; cursor: not-allowed; }
    .btn-submit:hover:not(:disabled) { opacity: 0.88; }

    .write-success {
      background: rgba(34,197,94,0.1); border: 1px solid rgba(34,197,94,0.3);
      border-radius: 10px; padding: 12px 16px;
      font-size: 13px; color: #86efac;
    }
    .ft-error {
      background: rgba(239,68,68,0.1); border: 1px solid rgba(239,68,68,0.3);
      border-radius: 10px; padding: 12px 16px;
      font-size: 13px; color: #fca5a5;
    }
  `]
})
export class FamilyTreeComponent implements OnInit {
  private readonly treeSvc     = inject(FamilyTreeService);
  private readonly familyState = inject(FamilyStateService);

  readonly familyId = this.familyState.currentFamilyId;
  readonly tab      = signal<ActiveTab>('tree');

  // Estado del árbol
  readonly treeRoot       = signal<FamilyTreeNode | null>(null);
  readonly loadingTree    = signal(false);

  // Herencia
  readonly heritage        = signal<HeritageDto | null>(null);
  readonly loadingHeritage = signal(false);

  // Mensajes
  readonly ancestorMessages  = signal<GenerationalMessage[]>([]);
  readonly ownMessages       = signal<GenerationalMessage[]>([]);
  readonly loadingMessages   = signal(false);

  // Escribir mensaje
  newMsg: MessageRequest = { authorName: '', content: '', messageType: 'LETTER' };
  readonly sending   = signal(false);
  readonly writeDone = signal(false);
  readonly writeError = signal<string | null>(null);

  // Vincular
  linkCode   = '';
  linkMember = '';
  linkNote   = '';
  readonly linking   = signal(false);
  readonly linkDone  = signal(false);
  readonly linkError = signal<string | null>(null);

  readonly globalError = signal<string | null>(null);

  readonly msgTypes = [
    { key: 'LETTER',  icon: '✉️',  label: 'Carta'     },
    { key: 'WISDOM',  icon: '🧠',  label: 'Sabiduría' },
    { key: 'WARNING', icon: '⚠️',  label: 'Advertencia'},
    { key: 'BLESSING',icon: '🌟',  label: 'Bendición' },
  ];

  ngOnInit(): void {
    const id = this.familyId();
    if (!id) return;
    this.loadTree(id);
  }

  private loadTree(id: number): void {
    this.loadingTree.set(true);
    this.treeSvc.getFullTree(id).pipe(
      catchError(() => { this.globalError.set(null); return of(null); })
    ).subscribe(data => {
      this.treeRoot.set(data);
      this.loadingTree.set(false);
    });
  }

  loadHeritage(): void {
    if (this.heritage()) return;
    const id = this.familyId();
    if (!id) return;
    this.loadingHeritage.set(true);
    this.treeSvc.getHeritage(id).pipe(
      catchError(() => of(null))
    ).subscribe(data => {
      this.heritage.set(data);
      this.loadingHeritage.set(false);
    });
  }

  loadMessages(): void {
    if (this.ancestorMessages().length || this.ownMessages().length) return;
    const id = this.familyId();
    if (!id) return;
    this.loadingMessages.set(true);
    this.treeSvc.getAncestorMessages(id).pipe(catchError(() => of([]))).subscribe(d => this.ancestorMessages.set(d));
    this.treeSvc.getOwnMessages(id).pipe(catchError(() => of([]))).subscribe(d => {
      this.ownMessages.set(d);
      this.loadingMessages.set(false);
    });
  }

  sendMessage(): void {
    const id = this.familyId();
    if (!id) return;
    if (!this.newMsg.content?.trim() || !this.newMsg.authorName?.trim()) {
      this.writeError.set('Escribe tu nombre y el contenido del mensaje.');
      return;
    }
    this.sending.set(true);
    this.writeError.set(null);
    this.treeSvc.createMessage(id, this.newMsg).pipe(
      catchError(() => {
        this.writeError.set('Error al enviar el mensaje. Inténtalo de nuevo.');
        this.sending.set(false);
        return of(null);
      })
    ).subscribe(msg => {
      if (msg) {
        this.writeDone.set(true);
        this.ownMessages.update(list => [msg, ...list]);
        this.newMsg = { authorName: '', content: '', messageType: 'LETTER' };
      }
      this.sending.set(false);
    });
  }

  doLink(): void {
    const id = this.familyId();
    if (!id || !this.linkCode.trim()) return;
    this.linking.set(true);
    this.linkError.set(null);
    this.treeSvc.link(id, {
      parentFamilyCode: this.linkCode.trim(),
      linkedByMember: this.linkMember || undefined,
      note: this.linkNote || undefined
    }).pipe(
      catchError(err => {
        this.linkError.set(err?.error?.message || 'No se pudo vincular. Verifica el código e inténtalo de nuevo.');
        this.linking.set(false);
        return of(null);
      })
    ).subscribe(res => {
      if (res !== null) {
        this.linkDone.set(true);
        this.linking.set(false);
        this.loadTree(id);
      }
    });
  }

  msgIcon(type: string): string { return MSG_ICONS[type] ?? '✉️'; }

  formatYear(iso: string | null): string {
    if (!iso) return '—';
    try { return new Date(iso).getFullYear().toString(); } catch { return iso; }
  }
}
