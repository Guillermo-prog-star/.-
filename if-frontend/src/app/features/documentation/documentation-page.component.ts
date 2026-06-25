import { Component, OnInit, inject, signal, computed } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import {
  DocumentationService,
  DocumentSummary,
  DocumentDetail,
  DocumentCategory,
  QueryResponse
} from '../../core/services/documentation.service';

type ViewMode = 'list' | 'detail' | 'query';

interface CategoryConfig {
  label: string;
  icon: string;
  value: DocumentCategory | 'ALL';
}

const CATEGORIES: CategoryConfig[] = [
  { label: 'Todos',        icon: '📚', value: 'ALL'         },
  { label: 'Proyecto',     icon: '🏗️',  value: 'PROJECT'     },
  { label: 'IA',           icon: '🤖', value: 'AI'          },
  { label: 'Desarrollo',   icon: '⚙️',  value: 'DEVELOPMENT' },
  { label: 'Familia',      icon: '👨‍👩‍👧‍👦', value: 'FAMILY'      },
  { label: 'Investigación',icon: '🔬', value: 'RESEARCH'    },
];

@Component({
  selector: 'app-documentation-page',
  standalone: true,
  imports: [CommonModule, FormsModule],
  template: `
    <div class="docs-page">

      <!-- HEADER -->
      <div class="docs-header">
        <div class="docs-icon">📚</div>
        <div>
          <h1 class="docs-title">Centro de Documentación</h1>
          <p class="docs-sub">Documentación oficial de Integrity Family — consulta, busca o pregunta libremente</p>
        </div>
      </div>

      <!-- BARRA DE ACCIONES -->
      <div class="docs-toolbar">
        <div class="search-box">
          <span class="search-icon">🔍</span>
          <input
            type="text"
            [(ngModel)]="searchText"
            (input)="onSearch()"
            placeholder="Buscar documento o escribe una pregunta..."
            class="search-input"
          />
          @if (searchText) {
            <button class="clear-btn" (click)="clearSearch()">✕</button>
          }
        </div>
        <button class="btn-query" (click)="switchMode('query')" [class.active]="mode() === 'query'">
          ✨ Consultar con IA
        </button>
      </div>

      <!-- CATEGORÍAS -->
      @if (mode() !== 'query') {
        <div class="categories-row">
          @for (cat of categories; track cat.value) {
            <button
              class="cat-chip"
              [class.active]="selectedCategory() === cat.value"
              (click)="selectCategory(cat.value)"
            >
              {{ cat.icon }} {{ cat.label }}
            </button>
          }
        </div>
      }

      <!-- MODO: LISTA -->
      @if (mode() === 'list') {

        @if (loading()) {
          <div class="docs-loading">
            <div class="spinner"></div>
            <p>Cargando documentación...</p>
          </div>
        }

        @if (!loading() && filteredDocs().length === 0) {
          <div class="docs-empty">
            <div class="empty-icon">📄</div>
            <p>No se encontraron documentos.</p>
          </div>
        }

        @if (!loading() && filteredDocs().length > 0) {
          <div class="docs-count">{{ filteredDocs().length }} documento(s)</div>
          <div class="docs-grid">
            @for (doc of filteredDocs(); track doc.code) {
              <div class="doc-card" (click)="openDoc(doc.code)">
                <div class="doc-card-top">
                  <span class="doc-code">{{ doc.code }}</span>
                  <span class="doc-version">v{{ doc.version }}</span>
                </div>
                <h3 class="doc-title">{{ doc.title }}</h3>
                <p class="doc-summary">{{ doc.summary }}</p>
                <div class="doc-footer">
                  <span class="doc-cat-badge {{ doc.category.toLowerCase() }}">{{ categoryLabel(doc.category) }}</span>
                  <span class="doc-date">{{ formatDate(doc.updatedAt) }}</span>
                </div>
              </div>
            }
          </div>
        }
      }

      <!-- MODO: DETALLE -->
      @if (mode() === 'detail') {
        @if (loadingDetail()) {
          <div class="docs-loading">
            <div class="spinner"></div>
            <p>Cargando documento...</p>
          </div>
        }

        @if (!loadingDetail() && currentDoc()) {
          <div class="detail-view">
            <button class="back-btn" (click)="backToList()">← Volver</button>

            <div class="detail-header">
              <div class="detail-meta-row">
                <span class="doc-code large">{{ currentDoc()!.code }}</span>
                <span class="doc-version large">v{{ currentDoc()!.version }}</span>
                <span class="doc-cat-badge {{ currentDoc()!.category.toLowerCase() }}">
                  {{ categoryLabel(currentDoc()!.category) }}
                </span>
              </div>
              <h2 class="detail-title">{{ currentDoc()!.title }}</h2>
              <p class="detail-updated">Actualizado: {{ formatDate(currentDoc()!.updatedAt) }}</p>
            </div>

            <div class="detail-content markdown-body" [innerHTML]="renderMarkdown(currentDoc()!.content)"></div>

            <div class="detail-actions">
              <button class="btn-action" (click)="askAboutDoc()">✨ Preguntarle a la IA sobre este documento</button>
            </div>
          </div>
        }
      }

      <!-- MODO: CONSULTA IA -->
      @if (mode() === 'query') {
        <div class="query-view">
          <button class="back-btn" (click)="backToList()">← Volver a documentos</button>

          <div class="query-box">
            <div class="query-title">✨ Consultor IA de Documentación</div>
            <p class="query-hint">
              Escribe cualquier pregunta sobre el proyecto y la IA responderá usando únicamente la documentación oficial.
            </p>
            <textarea
              [(ngModel)]="queryText"
              placeholder="Ej: ¿Cómo se calcula el ICF? ¿Qué hace el Scanner Emocional? ¿Cuáles son las convenciones de Flyway?"
              class="query-textarea"
              rows="3"
            ></textarea>
            <button
              class="btn-send"
              (click)="submitQuery()"
              [disabled]="!queryText.trim() || querying()"
            >
              {{ querying() ? 'Consultando...' : 'Enviar pregunta' }}
            </button>
          </div>

          @if (querying()) {
            <div class="docs-loading">
              <div class="spinner"></div>
              <p>La IA está buscando en la documentación...</p>
            </div>
          }

          @if (queryResult()) {
            <div class="query-result">
              <div class="result-label">📋 Respuesta</div>
              <div class="result-answer" [innerHTML]="renderMarkdown(queryResult()!.answer)"></div>

              @if (queryResult()!.sources.length > 0) {
                <div class="result-sources">
                  <div class="sources-label">📎 Fuentes consultadas</div>
                  <div class="sources-list">
                    @for (src of queryResult()!.sources; track src.code) {
                      <button class="source-chip" (click)="openDoc(src.code)">
                        {{ src.code }} — {{ src.title }}
                      </button>
                    }
                  </div>
                </div>
              }
            </div>
          }
        </div>
      }

    </div>
  `,
  styles: [`
    .docs-page {
      max-width: 1100px;
      margin: 0 auto;
      padding: 0 0 60px;
      color: #e2e8f0;
    }

    /* Header */
    .docs-header {
      display: flex;
      align-items: center;
      gap: 16px;
      margin-bottom: 28px;
    }
    .docs-icon { font-size: 2.4rem; }
    .docs-title {
      font-size: 1.6rem;
      font-weight: 700;
      color: #f1f5f9;
      margin: 0 0 4px;
    }
    .docs-sub { color: #94a3b8; font-size: 0.85rem; margin: 0; }

    /* Toolbar */
    .docs-toolbar {
      display: flex;
      gap: 12px;
      margin-bottom: 20px;
      flex-wrap: wrap;
    }
    .search-box {
      flex: 1;
      min-width: 260px;
      display: flex;
      align-items: center;
      gap: 10px;
      background: rgba(255,255,255,0.05);
      border: 1px solid rgba(255,255,255,0.1);
      border-radius: 12px;
      padding: 10px 14px;
    }
    .search-icon { font-size: 1rem; opacity: 0.6; }
    .search-input {
      flex: 1;
      background: none;
      border: none;
      outline: none;
      color: #f1f5f9;
      font-size: 0.9rem;
    }
    .search-input::placeholder { color: #64748b; }
    .clear-btn {
      background: none;
      border: none;
      color: #64748b;
      cursor: pointer;
      font-size: 0.8rem;
      padding: 2px 6px;
      border-radius: 4px;
    }
    .clear-btn:hover { color: #f1f5f9; background: rgba(255,255,255,0.1); }

    .btn-query {
      padding: 10px 20px;
      border-radius: 12px;
      border: 1px solid rgba(139,92,246,0.4);
      background: rgba(139,92,246,0.12);
      color: #c4b5fd;
      font-size: 0.85rem;
      font-weight: 600;
      cursor: pointer;
      transition: all 0.2s;
      white-space: nowrap;
    }
    .btn-query:hover, .btn-query.active {
      background: rgba(139,92,246,0.25);
      border-color: rgba(139,92,246,0.7);
      box-shadow: 0 0 16px rgba(139,92,246,0.2);
    }

    /* Categorías */
    .categories-row {
      display: flex;
      gap: 8px;
      flex-wrap: wrap;
      margin-bottom: 24px;
    }
    .cat-chip {
      padding: 6px 14px;
      border-radius: 20px;
      border: 1px solid rgba(255,255,255,0.1);
      background: rgba(255,255,255,0.04);
      color: #94a3b8;
      font-size: 0.8rem;
      cursor: pointer;
      transition: all 0.2s;
    }
    .cat-chip:hover { border-color: rgba(255,255,255,0.2); color: #e2e8f0; }
    .cat-chip.active {
      background: rgba(99,102,241,0.2);
      border-color: rgba(99,102,241,0.5);
      color: #a5b4fc;
      font-weight: 600;
    }

    /* Loading / Empty */
    .docs-loading {
      text-align: center;
      padding: 60px 20px;
      color: #64748b;
    }
    .spinner {
      width: 36px; height: 36px;
      border: 3px solid rgba(255,255,255,0.1);
      border-top-color: #6366f1;
      border-radius: 50%;
      animation: spin 0.8s linear infinite;
      margin: 0 auto 16px;
    }
    @keyframes spin { to { transform: rotate(360deg); } }
    .docs-empty { text-align: center; padding: 60px 20px; color: #64748b; }
    .empty-icon { font-size: 2.5rem; margin-bottom: 12px; }

    /* Grid de documentos */
    .docs-count { color: #64748b; font-size: 0.8rem; margin-bottom: 16px; }
    .docs-grid {
      display: grid;
      grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
      gap: 16px;
    }
    .doc-card {
      background: rgba(255,255,255,0.04);
      border: 1px solid rgba(255,255,255,0.08);
      border-radius: 14px;
      padding: 18px;
      cursor: pointer;
      transition: all 0.2s;
      display: flex;
      flex-direction: column;
      gap: 8px;
    }
    .doc-card:hover {
      background: rgba(255,255,255,0.07);
      border-color: rgba(99,102,241,0.4);
      transform: translateY(-2px);
      box-shadow: 0 8px 24px rgba(0,0,0,0.3);
    }
    .doc-card-top { display: flex; justify-content: space-between; align-items: center; }
    .doc-code { font-size: 0.7rem; font-weight: 700; color: #6366f1; letter-spacing: 0.05em; }
    .doc-version { font-size: 0.7rem; color: #475569; }
    .doc-title { font-size: 0.95rem; font-weight: 600; color: #f1f5f9; margin: 0; line-height: 1.3; }
    .doc-summary { font-size: 0.8rem; color: #94a3b8; line-height: 1.5; flex: 1; margin: 0; }
    .doc-footer { display: flex; justify-content: space-between; align-items: center; margin-top: 4px; }
    .doc-date { font-size: 0.7rem; color: #475569; }

    /* Badges de categoría */
    .doc-cat-badge {
      font-size: 0.65rem;
      font-weight: 700;
      padding: 3px 8px;
      border-radius: 6px;
      text-transform: uppercase;
      letter-spacing: 0.05em;
    }
    .doc-cat-badge.project     { background: rgba(59,130,246,0.15); color: #60a5fa; }
    .doc-cat-badge.ai          { background: rgba(139,92,246,0.15); color: #c4b5fd; }
    .doc-cat-badge.development { background: rgba(16,185,129,0.15); color: #6ee7b7; }
    .doc-cat-badge.family      { background: rgba(245,158,11,0.15); color: #fcd34d; }
    .doc-cat-badge.research    { background: rgba(236,72,153,0.15); color: #f9a8d4; }

    /* Vista detalle */
    .detail-view { max-width: 820px; }
    .back-btn {
      background: none;
      border: none;
      color: #6366f1;
      cursor: pointer;
      font-size: 0.85rem;
      padding: 0;
      margin-bottom: 20px;
      display: inline-flex;
      align-items: center;
      gap: 6px;
    }
    .back-btn:hover { color: #a5b4fc; }
    .detail-header { margin-bottom: 28px; }
    .detail-meta-row { display: flex; gap: 10px; align-items: center; margin-bottom: 10px; flex-wrap: wrap; }
    .doc-code.large { font-size: 0.85rem; font-weight: 700; color: #6366f1; }
    .doc-version.large { font-size: 0.8rem; color: #475569; }
    .detail-title { font-size: 1.5rem; font-weight: 700; color: #f1f5f9; margin: 0 0 6px; }
    .detail-updated { font-size: 0.75rem; color: #475569; margin: 0; }
    .detail-actions { margin-top: 32px; padding-top: 20px; border-top: 1px solid rgba(255,255,255,0.06); }
    .btn-action {
      padding: 10px 20px;
      border-radius: 10px;
      border: 1px solid rgba(139,92,246,0.4);
      background: rgba(139,92,246,0.1);
      color: #c4b5fd;
      font-size: 0.85rem;
      cursor: pointer;
      transition: all 0.2s;
    }
    .btn-action:hover { background: rgba(139,92,246,0.2); }

    /* Markdown */
    .markdown-body {
      font-size: 0.9rem;
      line-height: 1.8;
      color: #cbd5e1;
    }
    .markdown-body :global(h1) { font-size: 1.4rem; color: #f1f5f9; font-weight: 700; margin: 24px 0 12px; }
    .markdown-body :global(h2) { font-size: 1.15rem; color: #e2e8f0; font-weight: 600; margin: 20px 0 10px; border-bottom: 1px solid rgba(255,255,255,0.06); padding-bottom: 6px; }
    .markdown-body :global(h3) { font-size: 1rem; color: #e2e8f0; font-weight: 600; margin: 16px 0 8px; }
    .markdown-body :global(p)  { margin: 0 0 12px; }
    .markdown-body :global(ul), .markdown-body :global(ol) { padding-left: 20px; margin: 0 0 12px; }
    .markdown-body :global(li) { margin-bottom: 4px; }
    .markdown-body :global(code) { background: rgba(255,255,255,0.08); padding: 2px 6px; border-radius: 4px; font-size: 0.85em; font-family: monospace; color: #a5b4fc; }
    .markdown-body :global(pre) { background: rgba(0,0,0,0.4); border: 1px solid rgba(255,255,255,0.08); border-radius: 10px; padding: 16px; overflow-x: auto; margin: 12px 0; }
    .markdown-body :global(pre code) { background: none; padding: 0; color: #e2e8f0; }
    .markdown-body :global(table) { width: 100%; border-collapse: collapse; margin: 12px 0; font-size: 0.85rem; }
    .markdown-body :global(th) { background: rgba(99,102,241,0.15); color: #a5b4fc; padding: 8px 12px; text-align: left; font-weight: 600; }
    .markdown-body :global(td) { border-top: 1px solid rgba(255,255,255,0.06); padding: 8px 12px; }
    .markdown-body :global(strong) { color: #f1f5f9; font-weight: 600; }
    .markdown-body :global(blockquote) { border-left: 3px solid #6366f1; padding-left: 14px; color: #94a3b8; margin: 12px 0; }

    /* Consulta IA */
    .query-view { max-width: 820px; }
    .query-box {
      background: rgba(139,92,246,0.06);
      border: 1px solid rgba(139,92,246,0.2);
      border-radius: 16px;
      padding: 24px;
      margin-bottom: 24px;
    }
    .query-title { font-size: 1.1rem; font-weight: 700; color: #c4b5fd; margin-bottom: 8px; }
    .query-hint { color: #94a3b8; font-size: 0.85rem; margin-bottom: 16px; }
    .query-textarea {
      width: 100%;
      background: rgba(0,0,0,0.3);
      border: 1px solid rgba(255,255,255,0.1);
      border-radius: 10px;
      color: #f1f5f9;
      font-size: 0.9rem;
      padding: 12px;
      resize: vertical;
      outline: none;
      font-family: inherit;
      box-sizing: border-box;
      margin-bottom: 14px;
    }
    .query-textarea:focus { border-color: rgba(139,92,246,0.5); }
    .btn-send {
      padding: 10px 28px;
      border-radius: 10px;
      border: none;
      background: linear-gradient(135deg, #6366f1, #8b5cf6);
      color: white;
      font-size: 0.9rem;
      font-weight: 600;
      cursor: pointer;
      transition: all 0.2s;
    }
    .btn-send:hover:not(:disabled) { opacity: 0.9; transform: translateY(-1px); }
    .btn-send:disabled { opacity: 0.4; cursor: not-allowed; }

    .query-result {
      background: rgba(255,255,255,0.03);
      border: 1px solid rgba(255,255,255,0.08);
      border-radius: 14px;
      padding: 24px;
    }
    .result-label {
      font-size: 0.75rem;
      font-weight: 700;
      color: #6366f1;
      text-transform: uppercase;
      letter-spacing: 0.08em;
      margin-bottom: 14px;
    }
    .result-answer { font-size: 0.9rem; line-height: 1.8; color: #cbd5e1; margin-bottom: 20px; }
    .result-sources { border-top: 1px solid rgba(255,255,255,0.06); padding-top: 16px; }
    .sources-label { font-size: 0.75rem; color: #64748b; margin-bottom: 10px; font-weight: 600; text-transform: uppercase; letter-spacing: 0.06em; }
    .sources-list { display: flex; flex-wrap: wrap; gap: 8px; }
    .source-chip {
      padding: 5px 12px;
      border-radius: 20px;
      border: 1px solid rgba(99,102,241,0.3);
      background: rgba(99,102,241,0.08);
      color: #a5b4fc;
      font-size: 0.75rem;
      cursor: pointer;
      transition: all 0.2s;
    }
    .source-chip:hover { background: rgba(99,102,241,0.18); border-color: rgba(99,102,241,0.5); }
  `]
})
export class DocumentationPageComponent implements OnInit {
  private svc = inject(DocumentationService);

  categories = CATEGORIES;

  mode            = signal<ViewMode>('list');
  loading         = signal(false);
  loadingDetail   = signal(false);
  querying        = signal(false);
  selectedCategory = signal<DocumentCategory | 'ALL'>('ALL');
  allDocs         = signal<DocumentSummary[]>([]);
  searchResults   = signal<DocumentSummary[] | null>(null);
  currentDoc      = signal<DocumentDetail | null>(null);
  queryResult     = signal<QueryResponse | null>(null);

  searchText = '';
  queryText  = '';
  private searchTimer: any;

  filteredDocs = computed(() => {
    const results = this.searchResults();
    if (results !== null) return results;
    const cat = this.selectedCategory();
    if (cat === 'ALL') return this.allDocs();
    return this.allDocs().filter(d => d.category === cat);
  });

  ngOnInit() {
    this.loadAll();
  }

  loadAll() {
    this.loading.set(true);
    this.svc.listAll().subscribe({
      next: r => { this.allDocs.set(r.documents); this.loading.set(false); },
      error: () => this.loading.set(false)
    });
  }

  selectCategory(cat: DocumentCategory | 'ALL') {
    this.selectedCategory.set(cat);
    this.searchResults.set(null);
    this.searchText = '';
    this.mode.set('list');
  }

  onSearch() {
    clearTimeout(this.searchTimer);
    const q = this.searchText.trim();
    if (!q) { this.searchResults.set(null); return; }
    this.searchTimer = setTimeout(() => {
      this.svc.search(q).subscribe(r => this.searchResults.set(r.documents));
    }, 350);
  }

  clearSearch() {
    this.searchText = '';
    this.searchResults.set(null);
  }

  openDoc(code: string) {
    this.mode.set('detail');
    this.loadingDetail.set(true);
    this.currentDoc.set(null);
    this.svc.getByCode(code).subscribe({
      next: d => { this.currentDoc.set(d); this.loadingDetail.set(false); },
      error: () => this.loadingDetail.set(false)
    });
  }

  backToList() {
    this.mode.set('list');
    this.currentDoc.set(null);
    this.queryResult.set(null);
    this.queryText = '';
  }

  switchMode(m: ViewMode) {
    this.mode.set(m);
    this.queryResult.set(null);
  }

  askAboutDoc() {
    const doc = this.currentDoc();
    if (doc) {
      this.queryText = `Explícame el documento ${doc.code}: ${doc.title}`;
      this.mode.set('query');
    }
  }

  submitQuery() {
    const q = this.queryText.trim();
    if (!q) return;
    this.querying.set(true);
    this.queryResult.set(null);
    this.svc.query(q).subscribe({
      next: r => { this.queryResult.set(r); this.querying.set(false); },
      error: () => this.querying.set(false)
    });
  }

  categoryLabel(cat: DocumentCategory): string {
    return CATEGORIES.find(c => c.value === cat)?.label ?? cat;
  }

  formatDate(dateStr: string): string {
    if (!dateStr) return '';
    const d = new Date(dateStr);
    return d.toLocaleDateString('es-CO', { day: '2-digit', month: 'short', year: 'numeric' });
  }

  renderMarkdown(text: string): string {
    if (!text) return '';
    return text
      .replace(/^### (.+)$/gm,  '<h3>$1</h3>')
      .replace(/^## (.+)$/gm,   '<h2>$1</h2>')
      .replace(/^# (.+)$/gm,    '<h1>$1</h1>')
      .replace(/\*\*(.+?)\*\*/g, '<strong>$1</strong>')
      .replace(/`([^`]+)`/g,     '<code>$1</code>')
      .replace(/```[\w]*\n([\s\S]*?)```/g, '<pre><code>$1</code></pre>')
      .replace(/^\| (.+) \|$/gm, (row) => {
        const cells = row.split('|').filter(c => c.trim() !== '');
        return '<tr>' + cells.map(c => `<td>${c.trim()}</td>`).join('') + '</tr>';
      })
      .replace(/(<tr>.*<\/tr>\n?)+/g, m => `<table>${m}</table>`)
      .replace(/^- (.+)$/gm,    '<li>$1</li>')
      .replace(/(<li>.*<\/li>\n?)+/g, m => `<ul>${m}</ul>`)
      .replace(/^(\d+)\. (.+)$/gm, '<li>$2</li>')
      .replace(/\n\n/g, '</p><p>')
      .replace(/^(?!<[hupolt])/gm, '')
      .replace(/<p><\/p>/g, '');
  }
}
