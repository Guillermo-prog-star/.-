import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FamilyDnaService, FamilyDnaDto } from '../../core/services/family-dna.service';
import { FamilyStateService } from '../../core/services/family-state.service';
import { catchError, of } from 'rxjs';

@Component({
  selector: 'app-family-dna',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div class="dna-page">

      <!-- Header -->
      <div class="dna-header">
        <div class="dna-helix">🧬</div>
        <div class="dna-title-block">
          <h1 class="dna-title">ADN Familiar</h1>
          <p class="dna-sub">Identidad evolutiva de tu familia — quiénes son, cómo se mueven y qué pueden llegar a ser</p>
        </div>
      </div>

      <!-- Estado: sin familia -->
      @if (!familyId()) {
        <div class="dna-empty">
          <div class="empty-icon">👨‍👩‍👧‍👦</div>
          <p>Selecciona una familia para ver su ADN.</p>
        </div>
      }

      <!-- Estado: cargando -->
      @if (familyId() && loading()) {
        <div class="dna-loading">
          <div class="dna-spinner"></div>
          <p>Leyendo el ADN familiar...</p>
        </div>
      }

      <!-- Estado: sin ADN generado aún -->
      @if (familyId() && !loading() && !dna()) {
        <div class="dna-empty">
          <div class="empty-icon">🔬</div>
          <h2>El ADN de esta familia aún no ha sido sintetizado</h2>
          <p>La síntesis analiza todas las evaluaciones, conversaciones y patrones de tu familia para construir su perfil de identidad único.</p>
          <button class="btn-synthesize" (click)="synthesize()" [disabled]="synthesizing()">
            {{ synthesizing() ? 'Sintetizando...' : '✨ Sintetizar ADN Familiar' }}
          </button>
        </div>
      }

      <!-- ADN disponible -->
      @if (dna()) {
        <div class="dna-content">

          <!-- Narrativa IA -->
          @if (dna()!.narrativaIa) {
            <div class="dna-narrative">
              <div class="narrative-label">🧬 Su esencia</div>
              <p class="narrative-text">{{ dna()!.narrativaIa }}</p>
              <div class="narrative-meta">Versión {{ dna()!.version }} · Actualizado {{ formatDate(dna()!.updatedAt) }}</div>
            </div>
          }

          <!-- Grid principal -->
          <div class="dna-grid">

            <!-- Valores -->
            <div class="dna-card card-valores">
              <div class="card-header">
                <span class="card-icon">💎</span>
                <span class="card-label">Valores</span>
              </div>
              <div class="pill-list">
                @for (v of dna()!.valores; track v) {
                  <span class="pill pill-valor">{{ v }}</span>
                }
                @if (!dna()!.valores.length) {
                  <span class="pill-empty">Aún sin valores detectados</span>
                }
              </div>
            </div>

            <!-- Fortalezas -->
            <div class="dna-card card-fortalezas">
              <div class="card-header">
                <span class="card-icon">💪</span>
                <span class="card-label">Fortalezas</span>
              </div>
              <div class="pill-list">
                @for (f of dna()!.fortalezas; track f) {
                  <span class="pill pill-fortaleza">{{ f }}</span>
                }
                @if (!dna()!.fortalezas.length) {
                  <span class="pill-empty">Aún sin fortalezas detectadas</span>
                }
              </div>
            </div>

            <!-- Sombras -->
            <div class="dna-card card-sombras">
              <div class="card-header">
                <span class="card-icon">🌘</span>
                <span class="card-label">Sombras a transformar</span>
              </div>
              <div class="pill-list">
                @for (s of dna()!.sombras; track s) {
                  <span class="pill pill-sombra">{{ s }}</span>
                }
                @if (!dna()!.sombras.length) {
                  <span class="pill-empty">Sin sombras identificadas</span>
                }
              </div>
            </div>

            <!-- Patrones -->
            <div class="dna-card card-patrones">
              <div class="card-header">
                <span class="card-icon">🔁</span>
                <span class="card-label">Patrones observados</span>
              </div>
              <ul class="patron-list">
                @for (p of dna()!.patrones; track p) {
                  <li class="patron-item">{{ p }}</li>
                }
                @if (!dna()!.patrones.length) {
                  <li class="pill-empty">Sin patrones registrados aún</li>
                }
              </ul>
            </div>

            <!-- Estilo de comunicación -->
            @if (dna()!.estiloComunicacion) {
              <div class="dna-card card-comunicacion">
                <div class="card-header">
                  <span class="card-icon">💬</span>
                  <span class="card-label">Estilo de comunicación</span>
                </div>
                <p class="card-text">{{ dna()!.estiloComunicacion }}</p>
              </div>
            }

            <!-- Ritmo familiar -->
            @if (dna()!.ritmoFamiliar) {
              <div class="dna-card card-ritmo">
                <div class="card-header">
                  <span class="card-icon">🌊</span>
                  <span class="card-label">Ritmo familiar</span>
                </div>
                <p class="card-text">{{ dna()!.ritmoFamiliar }}</p>
              </div>
            }

          </div>

          <!-- Potencial oculto por miembro -->
          @if (dna()!.potencialOculto.length) {
            <div class="dna-section">
              <div class="section-title">⭐ Potencial oculto por miembro</div>
              <div class="potencial-grid">
                @for (p of dna()!.potencialOculto; track p.miembro) {
                  <div class="potencial-card">
                    <div class="poc-avatar">{{ inicial(p.miembro) }}</div>
                    <div class="poc-info">
                      <div class="poc-nombre">{{ p.miembro }}</div>
                      <div class="poc-talento">{{ p.talento }}</div>
                      <div class="poc-desc">{{ p.descripcion }}</div>
                    </div>
                  </div>
                }
              </div>
            </div>
          }

          <!-- Acción: re-sintetizar -->
          <div class="dna-actions">
            <button class="btn-synthesize btn-resyn" (click)="synthesize()" [disabled]="synthesizing()">
              {{ synthesizing() ? '⏳ Sintetizando...' : '🔄 Actualizar ADN' }}
            </button>
            <span class="resyn-hint">La IA analizará de nuevo toda la historia familiar para enriquecer este perfil.</span>
          </div>

        </div>
      }

      <!-- Error -->
      @if (error()) {
        <div class="dna-error">⚠️ {{ error() }}</div>
      }

    </div>
  `,
  styles: [`
    .dna-page {
      max-width: 900px;
      margin: 0 auto;
      padding: 24px 20px 48px;
      font-family: inherit;
      color: var(--if-text-primary, #e8e8e8);
    }

    /* Header */
    .dna-header {
      display: flex;
      align-items: center;
      gap: 16px;
      margin-bottom: 32px;
    }
    .dna-helix { font-size: 48px; }
    .dna-title { font-size: 26px; font-weight: 800; margin: 0 0 4px; }
    .dna-sub { font-size: 13px; color: var(--if-text-secondary, #999); margin: 0; }

    /* Estados vacíos / carga */
    .dna-empty, .dna-loading {
      text-align: center;
      padding: 60px 20px;
      color: var(--if-text-secondary, #888);
    }
    .empty-icon { font-size: 52px; margin-bottom: 16px; }
    .dna-loading { display: flex; flex-direction: column; align-items: center; gap: 16px; }
    .dna-spinner {
      width: 40px; height: 40px;
      border: 3px solid rgba(255,255,255,0.1);
      border-top-color: var(--if-accent, #8b5cf6);
      border-radius: 50%;
      animation: spin 0.8s linear infinite;
    }
    @keyframes spin { to { transform: rotate(360deg); } }

    /* Narrativa */
    .dna-narrative {
      background: linear-gradient(135deg, rgba(139,92,246,0.12), rgba(59,130,246,0.08));
      border: 1px solid rgba(139,92,246,0.25);
      border-radius: 16px;
      padding: 24px;
      margin-bottom: 28px;
    }
    .narrative-label { font-size: 11px; font-weight: 700; letter-spacing: 0.1em; text-transform: uppercase; color: var(--if-accent, #8b5cf6); margin-bottom: 10px; }
    .narrative-text { font-size: 15px; line-height: 1.7; margin: 0 0 12px; color: var(--if-text-primary, #e8e8e8); }
    .narrative-meta { font-size: 11px; color: var(--if-text-secondary, #777); }

    /* Grid de tarjetas */
    .dna-grid {
      display: grid;
      grid-template-columns: 1fr 1fr;
      gap: 16px;
      margin-bottom: 24px;
    }
    @media (max-width: 640px) { .dna-grid { grid-template-columns: 1fr; } }

    .dna-card {
      background: var(--if-surface, rgba(255,255,255,0.04));
      border: 1px solid rgba(255,255,255,0.08);
      border-radius: 14px;
      padding: 18px;
    }
    .card-header { display: flex; align-items: center; gap: 8px; margin-bottom: 14px; }
    .card-icon { font-size: 18px; }
    .card-label { font-size: 12px; font-weight: 700; letter-spacing: 0.08em; text-transform: uppercase; color: var(--if-text-secondary, #aaa); }
    .card-text { font-size: 14px; line-height: 1.6; margin: 0; color: var(--if-text-primary, #ddd); }

    /* Colores por tarjeta */
    .card-valores   { border-color: rgba(251,191,36,0.25); }
    .card-fortalezas{ border-color: rgba(34,197,94,0.25); }
    .card-sombras   { border-color: rgba(148,163,184,0.2); }
    .card-patrones  { border-color: rgba(59,130,246,0.2); }
    .card-comunicacion { grid-column: span 1; border-color: rgba(236,72,153,0.2); }
    .card-ritmo     { grid-column: span 1; border-color: rgba(20,184,166,0.2); }

    /* Pills */
    .pill-list { display: flex; flex-wrap: wrap; gap: 8px; }
    .pill {
      padding: 4px 12px;
      border-radius: 99px;
      font-size: 12px;
      font-weight: 600;
    }
    .pill-valor     { background: rgba(251,191,36,0.15); color: #fbbf24; border: 1px solid rgba(251,191,36,0.3); }
    .pill-fortaleza { background: rgba(34,197,94,0.12); color: #4ade80; border: 1px solid rgba(34,197,94,0.3); }
    .pill-sombra    { background: rgba(148,163,184,0.1); color: #94a3b8; border: 1px solid rgba(148,163,184,0.25); }
    .pill-empty     { font-size: 12px; color: var(--if-text-secondary, #666); font-style: italic; }

    /* Patrones */
    .patron-list { list-style: none; margin: 0; padding: 0; display: flex; flex-direction: column; gap: 8px; }
    .patron-item {
      font-size: 13px; color: var(--if-text-primary, #ccc);
      padding-left: 16px;
      position: relative;
    }
    .patron-item::before { content: '→'; position: absolute; left: 0; color: #3b82f6; }

    /* Sección potencial oculto */
    .dna-section { margin-bottom: 28px; }
    .section-title { font-size: 14px; font-weight: 700; margin-bottom: 16px; color: var(--if-text-primary, #e0e0e0); }
    .potencial-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(240px, 1fr)); gap: 14px; }
    .potencial-card {
      display: flex; align-items: flex-start; gap: 14px;
      background: var(--if-surface, rgba(255,255,255,0.04));
      border: 1px solid rgba(255,255,255,0.07);
      border-radius: 12px;
      padding: 16px;
    }
    .poc-avatar {
      width: 40px; height: 40px; border-radius: 50%;
      background: linear-gradient(135deg, #8b5cf6, #3b82f6);
      display: flex; align-items: center; justify-content: center;
      font-weight: 800; font-size: 16px; color: white;
      flex-shrink: 0;
    }
    .poc-nombre { font-size: 13px; font-weight: 700; margin-bottom: 2px; }
    .poc-talento { font-size: 11px; font-weight: 600; color: #a78bfa; margin-bottom: 4px; }
    .poc-desc   { font-size: 12px; color: var(--if-text-secondary, #888); line-height: 1.4; }

    /* Botón sintetizar */
    .btn-synthesize {
      background: linear-gradient(135deg, #7c3aed, #4f46e5);
      color: white;
      border: none;
      padding: 12px 28px;
      border-radius: 10px;
      font-size: 14px;
      font-weight: 700;
      cursor: pointer;
      transition: opacity 0.2s;
      margin-top: 8px;
    }
    .btn-synthesize:disabled { opacity: 0.5; cursor: not-allowed; }
    .btn-synthesize:hover:not(:disabled) { opacity: 0.88; }

    .dna-actions { display: flex; align-items: center; gap: 16px; margin-top: 12px; }
    .btn-resyn { padding: 9px 20px; font-size: 13px; }
    .resyn-hint { font-size: 12px; color: var(--if-text-secondary, #777); }

    /* Error */
    .dna-error {
      background: rgba(239,68,68,0.1);
      border: 1px solid rgba(239,68,68,0.3);
      border-radius: 10px;
      padding: 14px 18px;
      font-size: 13px;
      color: #fca5a5;
      margin-top: 20px;
    }
  `]
})
export class FamilyDnaComponent implements OnInit {
  private readonly dnaService = inject(FamilyDnaService);
  private readonly familyState = inject(FamilyStateService);

  readonly dna       = signal<FamilyDnaDto | null>(null);
  readonly loading   = signal(false);
  readonly synthesizing = signal(false);
  readonly error     = signal<string | null>(null);
  readonly familyId  = this.familyState.currentFamilyId;

  ngOnInit(): void {
    const id = this.familyId();
    if (!id) return;
    this.load(id);
  }

  private load(id: number): void {
    this.loading.set(true);
    this.error.set(null);
    this.dnaService.get(id).pipe(
      catchError(err => {
        if (err.status === 404) return of(null);
        this.error.set('No se pudo cargar el ADN familiar.');
        return of(null);
      })
    ).subscribe(data => {
      this.dna.set(data);
      this.loading.set(false);
    });
  }

  synthesize(): void {
    const id = this.familyId();
    if (!id) return;
    this.synthesizing.set(true);
    this.error.set(null);
    this.dnaService.synthesize(id).pipe(
      catchError(() => {
        this.error.set('Error al sintetizar el ADN. Verifica que la familia tenga al menos una evaluación.');
        this.synthesizing.set(false);
        return of(null);
      })
    ).subscribe(data => {
      if (data) this.dna.set(data);
      this.synthesizing.set(false);
    });
  }

  inicial(nombre: string): string {
    return nombre ? nombre.charAt(0).toUpperCase() : '?';
  }

  formatDate(dateStr: string | null): string {
    if (!dateStr) return '—';
    try {
      return new Date(dateStr).toLocaleDateString('es-CO', { day: '2-digit', month: 'short', year: 'numeric' });
    } catch { return dateStr; }
  }
}
