import {
  Component, OnInit, OnDestroy, inject, signal, computed
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { HttpClient } from '@angular/common/http';
import { ActivatedRoute, Router } from '@angular/router';
import { EvidenceService, SubmitEvidenceRequest } from '../../core/services/evidence.service';
import { FamilyStateService } from '../../core/services/family-state.service';
import { ApiService } from '../../core/services/api.service';
import { catchError, of } from 'rxjs';

// ── Tipos internos ──────────────────────────────────────────────────────────

type Step = 'mission' | 'capture' | 'reflections' | 'preview' | 'submitting' | 'documentary';

type MediaType = 'photo' | 'audio' | 'video' | 'note' | 'location';

interface CapsuleItem {
  localId: string;
  type: MediaType;
  label: string;
  emoji: string;
  base64?: string;
  mime?: string;
  text?: string;
  lat?: number;
  lng?: number;
  blobUrl?: string;  // URL.createObjectURL para previsualización
  emotion?: string;
}

interface Mission {
  id: number;
  title: string;
  description?: string;
  status?: string;
}

interface Reflections {
  whatWeDid: string;
  whoParticipated: string;
  howWeFeeled: string;
  whatWeLearned: string;
  whatWeWouldImprove: string;
}

const EMOTIONS = [
  { key: 'alegria',    label: 'Alegría',    emoji: '😊' },
  { key: 'gratitud',  label: 'Gratitud',   emoji: '🙏' },
  { key: 'amor',      label: 'Amor',       emoji: '❤️' },
  { key: 'orgullo',   label: 'Orgullo',    emoji: '🌟' },
  { key: 'calma',     label: 'Calma',      emoji: '😌' },
  { key: 'esperanza', label: 'Esperanza',  emoji: '🌱' },
];

// ── Componente ──────────────────────────────────────────────────────────────

@Component({
  selector: 'app-mision-documentary',
  standalone: true,
  imports: [CommonModule, FormsModule],
  template: `
<div class="md-page">

  <!-- ══ ENCABEZADO ══════════════════════════════════════════════════════ -->
  <div class="md-header">
    <div class="md-header-icon">🎬</div>
    <div>
      <h1 class="md-title">Mini Documental Familiar</h1>
      <p class="md-sub">Convierte una misión cumplida en una historia con evidencia real</p>
    </div>
  </div>

  <!-- ══ BARRA DE PROGRESO ═══════════════════════════════════════════════ -->
  @if (step() !== 'documentary') {
    <div class="md-progress">
      @for (s of progressSteps; track s.key) {
        <div class="ps-item" [class.done]="isStepDone(s.key)" [class.active]="step() === s.key">
          <div class="ps-dot">{{ isStepDone(s.key) ? '✓' : s.num }}</div>
          <span class="ps-label">{{ s.label }}</span>
        </div>
      }
    </div>
  }

  <!-- ══ PASO 1: SELECCIÓN DE MISIÓN ════════════════════════════════════ -->
  @if (step() === 'mission') {
    <div class="md-section">
      <div class="section-title">📋 ¿Para qué misión es este documental?</div>
      <p class="section-hint">El documental quedará vinculado a esta misión como evidencia oficial.</p>

      @if (loadingMissions()) {
        <div class="loading-row">
          <div class="spinner-sm"></div> Cargando misiones...
        </div>
      }

      @if (!loadingMissions() && missions().length === 0) {
        <div class="empty-hint">No hay misiones activas. Ve al Plan de Transformación y activa una misión primero.</div>
      }

      <div class="mission-list">
        @for (m of missions(); track m.id) {
          <button
            class="mission-card"
            [class.selected]="selectedMission()?.id === m.id"
            (click)="selectMission(m)"
          >
            <span class="mc-icon">🎯</span>
            <div class="mc-info">
              <div class="mc-title">{{ m.title }}</div>
              @if (m.description) {
                <div class="mc-desc">{{ m.description }}</div>
              }
            </div>
            @if (selectedMission()?.id === m.id) {
              <span class="mc-check">✓</span>
            }
          </button>
        }
      </div>

      <!-- Título del documental -->
      @if (selectedMission()) {
        <div class="field-group" style="margin-top: 20px;">
          <label class="field-label">Título del documental</label>
          <input
            class="field-input"
            [(ngModel)]="documentaryTitle"
            [placeholder]="'Ej: ' + (selectedMission()?.title ?? 'Nuestra misión cumplida')"
            maxlength="140"
          />
        </div>
      }

      <div class="step-nav">
        <span></span>
        <button
          class="btn-next"
          [disabled]="!selectedMission() || !documentaryTitle.trim()"
          (click)="step.set('capture')"
        >Continuar — Agregar evidencias →</button>
      </div>
    </div>
  }

  <!-- ══ PASO 2: CAPTURA MÚLTIPLE ════════════════════════════════════════ -->
  @if (step() === 'capture') {
    <div class="md-section">
      <div class="section-title">📦 Evidencias del documental</div>
      <p class="section-hint">
        Agrega fotos, audios, videos, notas y la ubicación donde ocurrió la misión.
        <strong>Mínimo 1 evidencia.</strong>
      </p>

      <!-- Ítems capturados hasta ahora -->
      @if (capturedItems().length > 0) {
        <div class="items-grid">
          @for (item of capturedItems(); track item.localId) {
            <div class="item-card">
              <button class="item-remove" (click)="removeItem(item.localId)" title="Eliminar">✕</button>

              @if (item.type === 'photo' && item.blobUrl) {
                <img [src]="item.blobUrl" class="item-thumb" alt="Foto" />
              }
              @if (item.type === 'video' && item.blobUrl) {
                <video [src]="item.blobUrl" class="item-thumb" muted></video>
              }
              @if (item.type === 'audio') {
                <div class="item-audio-icon">🎙️</div>
              }
              @if (item.type === 'note') {
                <div class="item-note-preview">"{{ item.text?.slice(0, 80) }}{{ (item.text?.length ?? 0) > 80 ? '…' : '' }}"</div>
              }
              @if (item.type === 'location') {
                <div class="item-loc-icon">📍</div>
              }

              <div class="item-label">{{ item.emoji }} {{ item.label }}</div>
            </div>
          }
        </div>
      }

      <!-- Botones de agregar -->
      @if (!addingType()) {
        <div class="add-buttons">
          <button class="add-btn" (click)="startAdd('photo')">📷 Foto</button>
          <button class="add-btn" (click)="startAdd('video')">🎥 Video</button>
          <button class="add-btn" (click)="startAdd('audio')">🎤 Audio</button>
          <button class="add-btn" (click)="startAdd('note')">✍️ Nota</button>
          <button class="add-btn" (click)="addLocation()">📍 Ubicación</button>
        </div>
      }

      <!-- Panel de captura inline -->
      @if (addingType() === 'photo' || addingType() === 'video') {
        <div class="inline-capture">
          <div class="ic-title">{{ addingType() === 'photo' ? '📷 Agregar foto' : '🎥 Agregar video' }}</div>
          <label class="file-drop">
            @if (!pendingBlobUrl()) {
              <span class="fd-icon">{{ addingType() === 'photo' ? '📷' : '🎥' }}</span>
              <span class="fd-text">Toca para {{ addingType() === 'photo' ? 'tomar o elegir una foto' : 'elegir un video' }}</span>
            } @else {
              @if (addingType() === 'photo') {
                <img [src]="pendingBlobUrl()!" class="inline-preview-img" alt="Vista previa" />
              } @else {
                <video [src]="pendingBlobUrl()!" class="inline-preview-img" muted controls></video>
              }
            }
            <input type="file"
              [accept]="addingType() === 'photo' ? 'image/*' : 'video/*'"
              [attr.capture]="addingType() === 'photo' ? 'environment' : null"
              class="file-input"
              (change)="onFileSelected($event)"
            />
          </label>
          <div class="field-group">
            <label class="field-label">Descripción breve (opcional)</label>
            <input class="field-input" [(ngModel)]="pendingLabel" placeholder="Ej: Momento del desayuno familiar" maxlength="100" />
          </div>
          <div class="ic-nav">
            <button class="btn-back" (click)="cancelAdd()">Cancelar</button>
            <button class="btn-next" [disabled]="!pendingBase64()" (click)="confirmAddMedia()">+ Agregar</button>
          </div>
        </div>
      }

      @if (addingType() === 'audio') {
        <div class="inline-capture">
          <div class="ic-title">🎤 Grabar audio</div>
          <div class="audio-zone">
            @if (!recording() && !pendingAudioBlob()) {
              <button class="btn-record" (click)="startRecording()">
                <span class="rec-dot"></span> Iniciar grabación
              </button>
            }
            @if (recording()) {
              <div class="rec-live">
                <span class="rec-pulse"></span>
                <span class="rec-time">{{ formatSeconds(recordSeconds()) }}</span>
                <button class="btn-stop" (click)="stopRecording()">⏹ Detener</button>
              </div>
            }
            @if (pendingAudioBlob() && !recording()) {
              <audio [src]="pendingBlobUrl()!" controls class="audio-player"></audio>
              <button class="btn-back" style="margin-top:8px;" (click)="clearPendingAudio()">🔄 Volver a grabar</button>
            }
          </div>
          @if (audioError()) {
            <div class="cap-error">{{ audioError() }}</div>
          }
          <div class="field-group">
            <label class="field-label">Descripción breve (opcional)</label>
            <input class="field-input" [(ngModel)]="pendingLabel" placeholder="Ej: Testimonio de mamá" maxlength="100" />
          </div>
          <div class="ic-nav">
            <button class="btn-back" (click)="cancelAdd()">Cancelar</button>
            <button class="btn-next" [disabled]="!pendingAudioBlob()" (click)="confirmAddAudio()">+ Agregar</button>
          </div>
        </div>
      }

      @if (addingType() === 'note') {
        <div class="inline-capture">
          <div class="ic-title">✍️ Nota o reflexión</div>
          <textarea
            class="text-area"
            [(ngModel)]="pendingText"
            placeholder="Describe lo que vivieron, un logro, una conversación importante..."
            rows="5"
            maxlength="1200"
          ></textarea>
          <div class="char-count">{{ pendingText.length }} / 1200</div>
          <div class="emotion-row">
            <span class="field-label">Emoción:</span>
            @for (em of EMOTIONS; track em.key) {
              <button class="em-mini" [class.sel]="pendingEmotion() === em.key" (click)="pendingEmotion.set(em.key)">
                {{ em.emoji }}
              </button>
            }
          </div>
          <div class="ic-nav">
            <button class="btn-back" (click)="cancelAdd()">Cancelar</button>
            <button class="btn-next" [disabled]="!pendingText.trim()" (click)="confirmAddNote()">+ Agregar</button>
          </div>
        </div>
      }

      <div class="step-nav" style="margin-top: 24px;">
        <button class="btn-back" (click)="step.set('mission')">← Misión</button>
        <button class="btn-next" [disabled]="capturedItems().length === 0" (click)="step.set('reflections')">
          Continuar — Reflexiones ({{ capturedItems().length }} elementos) →
        </button>
      </div>
    </div>
  }

  <!-- ══ PASO 3: REFLEXIONES ═════════════════════════════════════════════ -->
  @if (step() === 'reflections') {
    <div class="md-section">
      <div class="section-title">📖 Bitácora de la misión</div>
      <p class="section-hint">Estas respuestas formarán la narrativa del mini documental.</p>

      <div class="field-group">
        <label class="field-label">¿Qué hicimos?</label>
        <textarea class="text-area" [(ngModel)]="reflections.whatWeDid"
          placeholder="Describe las acciones concretas que realizaron como familia..."
          rows="3" maxlength="600"></textarea>
      </div>

      <div class="field-group">
        <label class="field-label">¿Quién participó?</label>
        <input class="field-input" [(ngModel)]="reflections.whoParticipated"
          placeholder="Ej: Papá, Mamá, Sebastián (12) y Valentina (8)"
          maxlength="200" />
      </div>

      <div class="field-group">
        <label class="field-label">¿Cómo nos sentimos?</label>
        <textarea class="text-area" [(ngModel)]="reflections.howWeFeeled"
          placeholder="¿Qué emociones vivieron? ¿Hubo tensiones? ¿Cómo terminó?"
          rows="3" maxlength="600"></textarea>
      </div>

      <div class="field-group">
        <label class="field-label">¿Qué aprendimos?</label>
        <textarea class="text-area" [(ngModel)]="reflections.whatWeLearned"
          placeholder="El aprendizaje más valioso que se llevan de esta misión..."
          rows="3" maxlength="600"></textarea>
      </div>

      <div class="field-group">
        <label class="field-label">¿Qué mejoraríamos? (opcional)</label>
        <textarea class="text-area" [(ngModel)]="reflections.whatWeWouldImprove"
          placeholder="Si lo repitieran, ¿qué harían diferente?"
          rows="2" maxlength="400"></textarea>
      </div>

      <!-- Protagonista -->
      <div class="field-group">
        <label class="field-label">Registrado por</label>
        <input class="field-input" [(ngModel)]="submitterName"
          placeholder="Tu nombre" maxlength="80" />
      </div>

      <div class="step-nav">
        <button class="btn-back" (click)="step.set('capture')">← Evidencias</button>
        <button class="btn-next"
          [disabled]="!reflections.whatWeDid.trim() || !reflections.whoParticipated.trim() || !reflections.whatWeLearned.trim()"
          (click)="step.set('preview')">
          Vista previa →
        </button>
      </div>
    </div>
  }

  <!-- ══ PASO 4: VISTA PREVIA ════════════════════════════════════════════ -->
  @if (step() === 'preview') {
    <div class="md-section">
      <div class="section-title">🎞️ Vista previa del documental</div>

      <!-- Encabezado documental -->
      <div class="doc-header-preview">
        <div class="dhp-mission-tag">🎯 {{ selectedMission()?.title }}</div>
        <div class="dhp-title">{{ documentaryTitle }}</div>
        <div class="dhp-meta">{{ capturedItems().length }} elementos · {{ submitterName || 'Familia' }}</div>
      </div>

      <!-- Grid de evidencias -->
      <div class="preview-media-grid">
        @for (item of capturedItems(); track item.localId) {
          <div class="pmg-card">
            @if (item.type === 'photo' && item.blobUrl) {
              <img [src]="item.blobUrl" class="pmg-img" alt="Foto" />
            }
            @if (item.type === 'video' && item.blobUrl) {
              <video [src]="item.blobUrl" class="pmg-img" muted controls></video>
            }
            @if (item.type === 'audio') {
              <div class="pmg-icon-card">🎙️<br><small>Audio</small></div>
            }
            @if (item.type === 'note') {
              <div class="pmg-note">"{{ item.text?.slice(0, 100) }}…"</div>
            }
            @if (item.type === 'location') {
              <div class="pmg-icon-card">📍<br><small>{{ item.lat?.toFixed(3) }}, {{ item.lng?.toFixed(3) }}</small></div>
            }
            <div class="pmg-label">{{ item.emoji }} {{ item.label }}</div>
          </div>
        }
      </div>

      <!-- Reflexiones resumen -->
      <div class="reflections-preview">
        <div class="rp-row"><span class="rp-q">¿Qué hicimos?</span><span class="rp-a">{{ reflections.whatWeDid }}</span></div>
        <div class="rp-row"><span class="rp-q">¿Quién participó?</span><span class="rp-a">{{ reflections.whoParticipated }}</span></div>
        <div class="rp-row"><span class="rp-q">¿Cómo nos sentimos?</span><span class="rp-a">{{ reflections.howWeFeeled }}</span></div>
        <div class="rp-row"><span class="rp-q">¿Qué aprendimos?</span><span class="rp-a">{{ reflections.whatWeLearned }}</span></div>
        @if (reflections.whatWeWouldImprove) {
          <div class="rp-row"><span class="rp-q">¿Qué mejoraríamos?</span><span class="rp-a">{{ reflections.whatWeWouldImprove }}</span></div>
        }
      </div>

      @if (submitError()) {
        <div class="cap-error">{{ submitError() }}</div>
      }

      <div class="step-nav">
        <button class="btn-back" (click)="step.set('reflections')">← Editar</button>
        <button class="btn-submit" (click)="submit()">
          🎬 Crear Mini Documental
        </button>
      </div>
    </div>
  }

  <!-- ══ ENVIANDO ════════════════════════════════════════════════════════ -->
  @if (step() === 'submitting') {
    <div class="md-submitting">
      <div class="sub-projector">📽️</div>
      <h2 class="sub-title">Construyendo el documental...</h2>
      <p class="sub-hint">La IA está tejiendo las evidencias en una historia coherente</p>
      <div class="sub-steps">
        <div class="ss-step" [class.active]="submitStep() >= 1">📤 Enviando evidencias ({{ submitStep() >= 1 ? submitProgress() + '/' + capturedItems().length : '...' }})</div>
        <div class="ss-step" [class.active]="submitStep() >= 2">🧠 Generando narrativa con IA</div>
        <div class="ss-step" [class.active]="submitStep() >= 3">🎬 Ensamblando el documental</div>
      </div>
      <div class="spinner-lg"></div>
    </div>
  }

  <!-- ══ PASO 6: MINI DOCUMENTAL FINAL ══════════════════════════════════ -->
  @if (step() === 'documentary') {
    <div class="documentary-view">

      <!-- Sello de misión cumplida -->
      <div class="doc-seal">
        <span class="seal-icon">🏆</span>
        <div>
          <div class="seal-label">MISIÓN CUMPLIDA</div>
          <div class="seal-date">{{ today() }}</div>
        </div>
      </div>

      <!-- Portada cinematográfica -->
      <div class="doc-cover">
        <div class="cover-gradient"></div>
        <!-- Imagen principal (primera foto del documental) -->
        @if (firstPhoto()) {
          <img [src]="firstPhoto()!" class="cover-img" alt="Portada" />
        }
        <div class="cover-overlay">
          <div class="cover-mission-tag">🎯 {{ selectedMission()?.title }}</div>
          <h1 class="cover-title">{{ documentaryTitle }}</h1>
          <div class="cover-meta">{{ submitterName || 'La familia' }} · {{ capturedItems().length }} evidencias</div>
        </div>
      </div>

      <!-- Narrativa generada por IA -->
      @if (aiNarrative()) {
        <div class="doc-narrative">
          <div class="nar-header">
            <span class="nar-badge">🧠 Narrativa generada por Sentinel AI</span>
          </div>
          <p class="nar-text">{{ aiNarrative() }}</p>
        </div>
      }

      <!-- Galería de evidencias -->
      <div class="doc-section-title">📸 Evidencias de la misión</div>
      <div class="evidence-gallery">
        @for (item of capturedItems(); track item.localId) {
          <div class="eg-card">
            @if (item.type === 'photo' && item.blobUrl) {
              <div class="eg-polaroid">
                <img [src]="item.blobUrl" class="eg-img" alt="Foto" />
              </div>
            }
            @if (item.type === 'video' && item.blobUrl) {
              <div class="eg-polaroid">
                <video [src]="item.blobUrl" class="eg-img" controls muted></video>
              </div>
            }
            @if (item.type === 'audio') {
              <div class="eg-audio-card">
                <div class="eg-audio-icon">🎙️</div>
                @if (item.blobUrl) {
                  <audio [src]="item.blobUrl" controls class="eg-audio-player"></audio>
                }
              </div>
            }
            @if (item.type === 'note') {
              <div class="eg-note-card">
                <div class="eg-note-mark">"</div>
                <p class="eg-note-text">{{ item.text }}</p>
                @if (item.emotion) {
                  <span class="eg-note-emotion">{{ getEmotionEmoji(item.emotion) }}</span>
                }
              </div>
            }
            @if (item.type === 'location') {
              <div class="eg-location-card">
                <div class="eg-loc-icon">📍</div>
                <div class="eg-loc-coords">{{ item.lat?.toFixed(4) }}, {{ item.lng?.toFixed(4) }}</div>
                <a [href]="'https://maps.google.com/?q=' + item.lat + ',' + item.lng" target="_blank" class="eg-maps-link">
                  Abrir en Google Maps →
                </a>
              </div>
            }
            <div class="eg-label">{{ item.emoji }} {{ item.label }}</div>
          </div>
        }
      </div>

      <!-- Bitácora -->
      <div class="doc-section-title">📖 Bitácora de la misión</div>
      <div class="doc-logbook">
        <div class="lb-entry">
          <div class="lb-q">¿Qué hicimos?</div>
          <div class="lb-a">{{ reflections.whatWeDid }}</div>
        </div>
        <div class="lb-entry">
          <div class="lb-q">¿Quién participó?</div>
          <div class="lb-a">{{ reflections.whoParticipated }}</div>
        </div>
        <div class="lb-entry">
          <div class="lb-q">¿Cómo nos sentimos?</div>
          <div class="lb-a">{{ reflections.howWeFeeled }}</div>
        </div>
        <div class="lb-entry">
          <div class="lb-q">¿Qué aprendimos?</div>
          <div class="lb-a lb-highlight">{{ reflections.whatWeLearned }}</div>
        </div>
        @if (reflections.whatWeWouldImprove) {
          <div class="lb-entry">
            <div class="lb-q">¿Qué mejoraríamos?</div>
            <div class="lb-a">{{ reflections.whatWeWouldImprove }}</div>
          </div>
        }
      </div>

      <!-- Conexión con la Película Familiar -->
      <div class="movie-feed-banner">
        <div class="mfb-icon">🎬</div>
        <div>
          <div class="mfb-title">Este documental alimenta la Película Familiar</div>
          <div class="mfb-hint">Cada trimestre, la IA reunirá todos los mini documentales para crear la Película de la Familia.</div>
        </div>
      </div>

      <!-- Acciones finales -->
      <div class="doc-actions">
        <button class="btn-new-doc" (click)="resetAll()">+ Nuevo documental</button>
        <button class="btn-go-movie" (click)="goToMovie()">🎬 Ver Película Familiar</button>
      </div>

    </div>
  }

</div>
  `,
  styles: [`
    .md-page {
      max-width: 720px;
      margin: 0 auto;
      padding: 24px 20px 80px;
      font-family: inherit;
      color: var(--if-text-primary, #e0e0e0);
    }

    /* Encabezado */
    .md-header { display: flex; align-items: center; gap: 14px; margin-bottom: 24px; }
    .md-header-icon { font-size: 40px; }
    .md-title { font-size: 24px; font-weight: 800; margin: 0 0 4px; }
    .md-sub   { font-size: 13px; color: var(--if-text-secondary, #888); margin: 0; }

    /* Barra de progreso */
    .md-progress {
      display: flex; align-items: center; gap: 4px;
      margin-bottom: 28px; overflow-x: auto;
    }
    .ps-item {
      display: flex; align-items: center; gap: 6px;
      font-size: 11px; color: var(--if-text-secondary, #777);
      white-space: nowrap; flex-shrink: 0;
    }
    .ps-item:not(:last-child)::after {
      content: '→';
      margin-left: 4px;
      color: rgba(255,255,255,0.15);
    }
    .ps-dot {
      width: 22px; height: 22px; border-radius: 50%;
      background: rgba(255,255,255,0.06);
      border: 1px solid rgba(255,255,255,0.12);
      display: flex; align-items: center; justify-content: center;
      font-size: 10px; font-weight: 700;
      transition: all 0.3s;
    }
    .ps-item.active .ps-dot { background: rgba(124,58,237,0.3); border-color: #7c3aed; color: #c4b5fd; }
    .ps-item.done  .ps-dot { background: rgba(16,185,129,0.2); border-color: #10b981; color: #6ee7b7; }
    .ps-label { font-weight: 600; }
    .ps-item.active .ps-label { color: #c4b5fd; }
    .ps-item.done  .ps-label { color: #6ee7b7; }

    /* Sección común */
    .md-section { display: flex; flex-direction: column; gap: 16px; }
    .section-title { font-size: 18px; font-weight: 800; margin-bottom: 2px; }
    .section-hint  { font-size: 13px; color: var(--if-text-secondary, #999); margin: 0; line-height: 1.5; }

    /* Misiones */
    .loading-row { display: flex; align-items: center; gap: 8px; color: var(--if-text-secondary, #999); font-size: 13px; padding: 20px 0; }
    .empty-hint  { font-size: 13px; color: var(--if-text-secondary, #999); padding: 20px; text-align: center; background: rgba(255,255,255,0.03); border-radius: 10px; }
    .mission-list { display: flex; flex-direction: column; gap: 10px; }
    .mission-card {
      display: flex; align-items: center; gap: 14px;
      padding: 14px 16px;
      background: rgba(255,255,255,0.04);
      border: 1px solid rgba(255,255,255,0.09);
      border-radius: 14px; cursor: pointer; transition: all 0.2s;
      color: var(--if-text-primary, #ddd); text-align: left;
    }
    .mission-card:hover    { border-color: rgba(124,58,237,0.35); background: rgba(124,58,237,0.07); }
    .mission-card.selected { border-color: #7c3aed; background: rgba(124,58,237,0.12); }
    .mc-icon  { font-size: 22px; flex-shrink: 0; }
    .mc-info  { flex: 1; }
    .mc-title { font-size: 14px; font-weight: 700; }
    .mc-desc  { font-size: 12px; color: var(--if-text-secondary, #999); margin-top: 3px; }
    .mc-check { font-size: 18px; color: #10b981; font-weight: 800; }

    /* Items capturados */
    .items-grid {
      display: grid;
      grid-template-columns: repeat(auto-fill, minmax(120px, 1fr));
      gap: 10px;
    }
    .item-card {
      position: relative;
      background: rgba(255,255,255,0.04);
      border: 1px solid rgba(255,255,255,0.09);
      border-radius: 12px; padding: 10px;
      display: flex; flex-direction: column; align-items: center; gap: 6px;
      min-height: 100px;
    }
    .item-remove {
      position: absolute; top: 4px; right: 4px;
      width: 20px; height: 20px; border-radius: 50%;
      background: rgba(239,68,68,0.15); border: 1px solid rgba(239,68,68,0.3);
      color: #fca5a5; font-size: 10px; cursor: pointer;
      display: flex; align-items: center; justify-content: center;
    }
    .item-thumb { width: 100%; height: 70px; object-fit: cover; border-radius: 8px; }
    .item-audio-icon, .item-loc-icon { font-size: 36px; }
    .item-note-preview { font-size: 11px; color: var(--if-text-secondary, #aaa); font-style: italic; text-align: center; line-height: 1.4; }
    .item-label { font-size: 10px; color: var(--if-text-secondary, #888); text-align: center; font-weight: 600; }

    /* Botones agregar */
    .add-buttons { display: flex; flex-wrap: wrap; gap: 8px; }
    .add-btn {
      padding: 9px 16px; border-radius: 10px;
      background: rgba(255,255,255,0.04);
      border: 1px solid rgba(255,255,255,0.1);
      color: var(--if-text-primary, #ddd);
      font-size: 13px; font-weight: 600; cursor: pointer;
      transition: all 0.18s;
    }
    .add-btn:hover { background: rgba(124,58,237,0.12); border-color: rgba(124,58,237,0.35); }

    /* Panel inline de captura */
    .inline-capture {
      background: rgba(255,255,255,0.03);
      border: 1px solid rgba(255,255,255,0.09);
      border-radius: 14px; padding: 18px;
      display: flex; flex-direction: column; gap: 14px;
    }
    .ic-title { font-size: 15px; font-weight: 700; }
    .ic-nav   { display: flex; justify-content: space-between; }

    .file-drop {
      display: flex; flex-direction: column; align-items: center; justify-content: center;
      gap: 10px; padding: 28px;
      border: 2px dashed rgba(255,255,255,0.12); border-radius: 12px;
      cursor: pointer; position: relative; min-height: 140px;
    }
    .file-drop:hover { border-color: rgba(124,58,237,0.4); }
    .fd-icon { font-size: 30px; }
    .fd-text { font-size: 12px; color: var(--if-text-secondary, #888); text-align: center; }
    .file-input { position: absolute; inset: 0; opacity: 0; cursor: pointer; }
    .inline-preview-img { max-width: 100%; max-height: 200px; border-radius: 10px; object-fit: cover; }

    /* Audio zone */
    .audio-zone { display: flex; flex-direction: column; align-items: center; gap: 12px; padding: 16px; }
    .btn-record {
      display: flex; align-items: center; gap: 10px;
      background: rgba(239,68,68,0.1); border: 1px solid rgba(239,68,68,0.3);
      color: #fca5a5; padding: 12px 24px; border-radius: 99px;
      font-size: 14px; font-weight: 700; cursor: pointer;
    }
    .rec-dot  { width: 10px; height: 10px; border-radius: 50%; background: #ef4444; animation: pulse 1.2s infinite; }
    .rec-live { display: flex; align-items: center; gap: 14px; }
    .rec-pulse { width: 12px; height: 12px; border-radius: 50%; background: #ef4444; animation: pulse 0.8s infinite; }
    .rec-time  { font-size: 22px; font-weight: 800; font-variant-numeric: tabular-nums; color: #fca5a5; }
    .btn-stop  { background: rgba(239,68,68,0.15); border: 1px solid rgba(239,68,68,0.3); color: #fca5a5; padding: 7px 16px; border-radius: 8px; font-size: 12px; cursor: pointer; }
    .audio-player { width: 100%; border-radius: 8px; }
    @keyframes pulse { 0%,100%{opacity:1;transform:scale(1)} 50%{opacity:0.5;transform:scale(0.8)} }

    /* Texto */
    .text-area {
      width: 100%; padding: 12px; border-radius: 10px;
      background: rgba(255,255,255,0.05);
      border: 1px solid rgba(255,255,255,0.1);
      color: var(--if-text-primary, #e0e0e0);
      font-size: 14px; line-height: 1.6; resize: vertical; font-family: inherit;
      box-sizing: border-box;
    }
    .text-area:focus { outline: none; border-color: rgba(124,58,237,0.4); }
    .char-count { font-size: 11px; color: var(--if-text-secondary, #777); text-align: right; }
    .emotion-row { display: flex; align-items: center; gap: 8px; flex-wrap: wrap; }
    .em-mini {
      width: 32px; height: 32px; border-radius: 8px;
      background: rgba(255,255,255,0.04); border: 1px solid rgba(255,255,255,0.1);
      font-size: 18px; cursor: pointer; display: flex; align-items: center; justify-content: center;
      transition: all 0.18s;
    }
    .em-mini.sel { background: rgba(124,58,237,0.2); border-color: rgba(124,58,237,0.5); }

    /* Campos */
    .field-group { display: flex; flex-direction: column; gap: 6px; }
    .field-label { font-size: 11px; font-weight: 700; color: var(--if-text-secondary, #aaa); text-transform: uppercase; letter-spacing: 0.06em; }
    .field-input {
      padding: 10px 14px; border-radius: 10px;
      background: rgba(255,255,255,0.05); border: 1px solid rgba(255,255,255,0.1);
      color: var(--if-text-primary, #e0e0e0); font-size: 14px; font-family: inherit;
    }
    .field-input:focus { outline: none; border-color: rgba(124,58,237,0.4); }

    /* Preview documental */
    .doc-header-preview {
      background: linear-gradient(135deg, rgba(124,58,237,0.15), rgba(79,70,229,0.1));
      border: 1px solid rgba(124,58,237,0.2);
      border-radius: 14px; padding: 20px; text-align: center;
    }
    .dhp-mission-tag { font-size: 11px; font-weight: 700; color: #a78bfa; text-transform: uppercase; letter-spacing: 0.1em; margin-bottom: 8px; }
    .dhp-title { font-size: 22px; font-weight: 900; color: #fff; margin-bottom: 6px; }
    .dhp-meta  { font-size: 12px; color: var(--if-text-secondary, #999); }

    .preview-media-grid {
      display: grid; grid-template-columns: repeat(auto-fill, minmax(130px, 1fr)); gap: 10px;
    }
    .pmg-card {
      background: rgba(255,255,255,0.04); border: 1px solid rgba(255,255,255,0.08);
      border-radius: 12px; padding: 8px; display: flex; flex-direction: column; gap: 6px; align-items: center;
    }
    .pmg-img  { width: 100%; height: 80px; object-fit: cover; border-radius: 8px; }
    .pmg-icon-card { height: 80px; display: flex; flex-direction: column; align-items: center; justify-content: center; font-size: 28px; color: var(--if-text-secondary, #aaa); }
    .pmg-note { font-size: 11px; font-style: italic; color: var(--if-text-secondary, #bbb); padding: 8px; line-height: 1.4; min-height: 60px; }
    .pmg-label { font-size: 10px; color: var(--if-text-secondary, #888); font-weight: 600; text-align: center; }

    .reflections-preview {
      background: rgba(255,255,255,0.03);
      border: 1px solid rgba(255,255,255,0.07);
      border-radius: 12px; padding: 16px;
      display: flex; flex-direction: column; gap: 10px;
    }
    .rp-row { display: flex; flex-direction: column; gap: 4px; }
    .rp-q   { font-size: 10px; font-weight: 700; color: var(--if-text-secondary, #888); text-transform: uppercase; letter-spacing: 0.06em; }
    .rp-a   { font-size: 13px; color: var(--if-text-primary, #ddd); line-height: 1.5; }

    /* Enviando */
    .md-submitting { text-align: center; padding: 60px 20px; }
    .sub-projector { font-size: 56px; animation: proj 1.5s ease-in-out infinite; margin-bottom: 16px; }
    @keyframes proj { 0%,100%{transform:scale(1)} 50%{transform:scale(1.06)} }
    .sub-title { font-size: 20px; font-weight: 800; color: #fff; margin: 0 0 8px; }
    .sub-hint  { font-size: 13px; color: var(--if-text-secondary, #999); margin: 0 0 24px; }
    .sub-steps { display: flex; flex-direction: column; gap: 8px; max-width: 340px; margin: 0 auto 28px; }
    .ss-step {
      font-size: 13px; color: var(--if-text-secondary, #777); padding: 8px 14px; border-radius: 8px;
      background: rgba(255,255,255,0.03); transition: all 0.4s;
    }
    .ss-step.active { color: #c4b5fd; background: rgba(124,58,237,0.12); border: 1px solid rgba(124,58,237,0.25); }
    .spinner-lg {
      width: 40px; height: 40px; border-radius: 50%;
      border: 3px solid rgba(255,255,255,0.05);
      border-top-color: #8b5cf6;
      animation: spin 1s linear infinite;
      margin: 0 auto;
    }
    @keyframes spin { to { transform: rotate(360deg); } }
    .spinner-sm {
      width: 16px; height: 16px; border-radius: 50%;
      border: 2px solid rgba(255,255,255,0.1); border-top-color: #8b5cf6;
      animation: spin 0.9s linear infinite;
    }

    /* ══ VISTA DOCUMENTAL FINAL ══════════════════════════════════════════ */
    .documentary-view { display: flex; flex-direction: column; gap: 28px; }

    /* Sello */
    .doc-seal {
      display: flex; align-items: center; gap: 14px;
      background: linear-gradient(135deg, rgba(16,185,129,0.12), rgba(5,150,105,0.08));
      border: 1px solid rgba(16,185,129,0.3);
      border-radius: 14px; padding: 16px 20px;
    }
    .seal-icon  { font-size: 36px; }
    .seal-label { font-size: 13px; font-weight: 800; color: #6ee7b7; text-transform: uppercase; letter-spacing: 0.1em; }
    .seal-date  { font-size: 12px; color: var(--if-text-secondary, #999); margin-top: 3px; }

    /* Portada */
    .doc-cover {
      position: relative; border-radius: 20px; overflow: hidden;
      min-height: 240px;
      background: linear-gradient(135deg, #1e1b4b 0%, #312e81 50%, #4c1d95 100%);
    }
    .cover-img { position: absolute; inset: 0; width: 100%; height: 100%; object-fit: cover; opacity: 0.35; }
    .cover-gradient {
      position: absolute; inset: 0;
      background: linear-gradient(to top, rgba(0,0,0,0.85) 0%, transparent 60%);
      z-index: 1;
    }
    .cover-overlay {
      position: relative; z-index: 2;
      display: flex; flex-direction: column; align-items: center; justify-content: flex-end;
      padding: 28px 24px; min-height: 240px; text-align: center;
    }
    .cover-mission-tag { font-size: 11px; font-weight: 700; color: #a78bfa; text-transform: uppercase; letter-spacing: 0.15em; margin-bottom: 10px; }
    .cover-title { font-size: 28px; font-weight: 900; color: #fff; margin: 0 0 8px; line-height: 1.2; }
    .cover-meta  { font-size: 13px; color: rgba(255,255,255,0.6); }

    /* Narrativa IA */
    .doc-narrative {
      background: linear-gradient(135deg, rgba(124,58,237,0.08), rgba(79,70,229,0.05));
      border: 1px solid rgba(124,58,237,0.2);
      border-radius: 16px; padding: 22px;
    }
    .nar-header { margin-bottom: 14px; }
    .nar-badge {
      font-size: 11px; font-weight: 700; color: #a78bfa;
      background: rgba(124,58,237,0.12); border: 1px solid rgba(124,58,237,0.25);
      padding: 4px 12px; border-radius: 20px; text-transform: uppercase; letter-spacing: 0.06em;
    }
    .nar-text { font-size: 15px; line-height: 1.8; color: var(--if-text-primary, #ddd); margin: 0; font-style: italic; }

    /* Título de sección */
    .doc-section-title {
      font-size: 15px; font-weight: 800;
      border-bottom: 1px solid rgba(255,255,255,0.07);
      padding-bottom: 10px;
    }

    /* Galería */
    .evidence-gallery {
      display: grid;
      grid-template-columns: repeat(auto-fill, minmax(160px, 1fr));
      gap: 14px;
    }
    .eg-card {
      display: flex; flex-direction: column; gap: 6px; align-items: center;
    }
    .eg-polaroid {
      background: #fff; padding: 8px 8px 22px;
      border-radius: 3px; box-shadow: 0 6px 16px rgba(0,0,0,0.4);
      transform: rotate(-1.5deg); transition: transform 0.3s;
      width: 100%;
    }
    .eg-polaroid:hover { transform: rotate(1deg) scale(1.03); }
    .eg-img { width: 100%; height: 110px; object-fit: cover; border-radius: 2px; display: block; }
    .eg-audio-card {
      background: rgba(255,255,255,0.04); border: 1px solid rgba(255,255,255,0.08);
      border-radius: 12px; padding: 14px; width: 100%;
      display: flex; flex-direction: column; align-items: center; gap: 10px;
    }
    .eg-audio-icon { font-size: 36px; }
    .eg-audio-player { width: 100%; }
    .eg-note-card {
      background: #fefcf3; border-radius: 10px; padding: 14px;
      width: 100%; min-height: 110px; box-shadow: 0 4px 10px rgba(0,0,0,0.15);
    }
    .eg-note-mark { font-size: 48px; line-height: 0.5; color: rgba(124,58,237,0.3); }
    .eg-note-text  { font-size: 13px; line-height: 1.5; color: #3b2a1a; font-style: italic; margin: 8px 0 0; }
    .eg-note-emotion { font-size: 24px; display: block; margin-top: 8px; }
    .eg-location-card {
      background: rgba(16,185,129,0.07); border: 1px solid rgba(16,185,129,0.2);
      border-radius: 12px; padding: 14px; width: 100%;
      display: flex; flex-direction: column; align-items: center; gap: 8px;
    }
    .eg-loc-icon   { font-size: 32px; }
    .eg-loc-coords { font-size: 11px; font-variant-numeric: tabular-nums; color: #6ee7b7; font-weight: 700; }
    .eg-maps-link  { font-size: 11px; color: #6ee7b7; text-decoration: none; border-bottom: 1px dotted #6ee7b7; }
    .eg-label      { font-size: 11px; color: var(--if-text-secondary, #888); font-weight: 600; text-align: center; }

    /* Bitácora */
    .doc-logbook { display: flex; flex-direction: column; gap: 14px; }
    .lb-entry    { border-left: 3px solid rgba(124,58,237,0.4); padding-left: 14px; }
    .lb-q        { font-size: 11px; font-weight: 700; color: #a78bfa; text-transform: uppercase; letter-spacing: 0.08em; margin-bottom: 5px; }
    .lb-a        { font-size: 14px; line-height: 1.7; color: var(--if-text-primary, #ddd); }
    .lb-highlight { color: #c4b5fd; font-style: italic; font-weight: 600; }

    /* Banner película */
    .movie-feed-banner {
      display: flex; align-items: center; gap: 16px;
      background: linear-gradient(135deg, rgba(245,158,11,0.1), rgba(217,119,6,0.07));
      border: 1px solid rgba(245,158,11,0.25);
      border-radius: 14px; padding: 18px 20px;
    }
    .mfb-icon  { font-size: 36px; flex-shrink: 0; }
    .mfb-title { font-size: 14px; font-weight: 700; color: #fbbf24; margin-bottom: 4px; }
    .mfb-hint  { font-size: 12px; color: var(--if-text-secondary, #999); line-height: 1.5; }

    /* Acciones finales */
    .doc-actions { display: flex; gap: 12px; flex-wrap: wrap; }
    .btn-new-doc {
      flex: 1; padding: 12px 20px; border-radius: 10px;
      background: rgba(255,255,255,0.05); border: 1px solid rgba(255,255,255,0.12);
      color: var(--if-text-secondary, #aaa); font-size: 14px; font-weight: 700; cursor: pointer;
      transition: all 0.2s;
    }
    .btn-new-doc:hover { background: rgba(255,255,255,0.09); }
    .btn-go-movie {
      flex: 1; padding: 12px 20px; border-radius: 10px;
      background: linear-gradient(135deg, #7c3aed, #4f46e5);
      border: none; color: #fff; font-size: 14px; font-weight: 700; cursor: pointer;
      box-shadow: 0 4px 16px rgba(124,58,237,0.3); transition: opacity 0.2s;
    }
    .btn-go-movie:hover { opacity: 0.88; }

    /* Navegación compartida */
    .step-nav { display: flex; justify-content: space-between; align-items: center; margin-top: 8px; }
    .btn-back {
      background: transparent; border: 1px solid rgba(255,255,255,0.1);
      color: var(--if-text-secondary, #aaa); padding: 10px 18px;
      border-radius: 9px; font-size: 13px; cursor: pointer;
    }
    .btn-next {
      background: rgba(124,58,237,0.18); border: 1px solid rgba(124,58,237,0.35);
      color: #c4b5fd; padding: 10px 22px;
      border-radius: 9px; font-size: 13px; font-weight: 700; cursor: pointer; transition: all 0.2s;
    }
    .btn-next:hover   { background: rgba(124,58,237,0.28); }
    .btn-next:disabled { opacity: 0.4; cursor: not-allowed; }
    .btn-submit {
      background: linear-gradient(135deg, #7c3aed, #4f46e5);
      border: none; color: #fff; padding: 12px 28px;
      border-radius: 10px; font-size: 15px; font-weight: 800; cursor: pointer;
      box-shadow: 0 4px 16px rgba(124,58,237,0.35); transition: opacity 0.2s;
    }
    .btn-submit:hover { opacity: 0.9; }

    /* Error */
    .cap-error {
      background: rgba(239,68,68,0.08); border: 1px solid rgba(239,68,68,0.25);
      border-radius: 10px; padding: 12px 16px; font-size: 13px; color: #fca5a5;
    }
  `]
})
export class MisionDocumentaryComponent implements OnInit, OnDestroy {

  private readonly evidenceSvc  = inject(EvidenceService);
  private readonly familyState  = inject(FamilyStateService);
  private readonly http         = inject(HttpClient);
  private readonly api          = inject(ApiService);
  private readonly route        = inject(ActivatedRoute);
  private readonly router       = inject(Router);

  // ── Estado de pasos ────────────────────────────────────────────────────
  readonly step           = signal<Step>('mission');
  readonly submitStep     = signal(0);
  readonly submitProgress = signal(0);

  readonly progressSteps = [
    { key: 'mission',     num: '1', label: 'Misión'     },
    { key: 'capture',     num: '2', label: 'Evidencias' },
    { key: 'reflections', num: '3', label: 'Bitácora'   },
    { key: 'preview',     num: '4', label: 'Previsualizar' },
  ] as const;

  isStepDone(key: string): boolean {
    const order = ['mission', 'capture', 'reflections', 'preview', 'submitting', 'documentary'];
    return order.indexOf(this.step()) > order.indexOf(key);
  }

  // ── Misiones ───────────────────────────────────────────────────────────
  readonly missions         = signal<Mission[]>([]);
  readonly loadingMissions  = signal(false);
  readonly selectedMission  = signal<Mission | null>(null);
  documentaryTitle = '';

  // ── Ítems capturados ───────────────────────────────────────────────────
  readonly capturedItems  = signal<CapsuleItem[]>([]);
  readonly addingType     = signal<MediaType | null>(null);

  // Estado del ítem pendiente
  readonly pendingBase64  = signal<string | null>(null);
  readonly pendingMime    = signal<string>('image/jpeg');
  readonly pendingBlobUrl = signal<string | null>(null);
  readonly pendingAudioBlob = signal<Blob | null>(null);
  readonly pendingEmotion = signal<string | null>(null);
  pendingLabel = '';
  pendingText  = '';

  // ── Audio ──────────────────────────────────────────────────────────────
  readonly recording     = signal(false);
  readonly recordSeconds = signal(0);
  readonly audioError    = signal<string | null>(null);
  private mediaRecorder: MediaRecorder | null = null;
  private audioChunks: Blob[] = [];
  private recordTimer: any;

  // ── Reflexiones ────────────────────────────────────────────────────────
  reflections: Reflections = {
    whatWeDid: '', whoParticipated: '', howWeFeeled: '',
    whatWeLearned: '', whatWeWouldImprove: ''
  };
  submitterName = '';

  // ── Resultado ──────────────────────────────────────────────────────────
  readonly aiNarrative  = signal<string | null>(null);
  readonly submitError  = signal<string | null>(null);

  readonly EMOTIONS = EMOTIONS;

  readonly firstPhoto = computed(() =>
    this.capturedItems().find(i => i.type === 'photo')?.blobUrl ?? null
  );

  today(): string {
    return new Date().toLocaleDateString('es-CO', { day: '2-digit', month: 'long', year: 'numeric' });
  }

  // ── Lifecycle ──────────────────────────────────────────────────────────

  ngOnInit(): void {
    this.loadMissions();
    // Preseleccionar misión si viene por query param
    this.route.queryParams.subscribe(p => {
      if (p['taskId']) {
        const id = +p['taskId'];
        const found = this.missions().find(m => m.id === id);
        if (found) this.selectMission(found);
      }
    });
  }

  ngOnDestroy(): void {
    this.stopRecording();
    clearInterval(this.recordTimer);
    this.revokePendingUrl();
    this.capturedItems().forEach(i => { if (i.blobUrl) URL.revokeObjectURL(i.blobUrl); });
  }

  // ── Cargar misiones ────────────────────────────────────────────────────

  private loadMissions(): void {
    const fid = this.familyState.getSelectedFamilyId();
    if (!fid) return;
    this.loadingMissions.set(true);
    this.http.get<any>(`${this.api.base}/plans/family/${fid}`).pipe(
      catchError(() => of(null))
    ).subscribe(res => {
      this.loadingMissions.set(false);
      const plans = res?.data ?? res;
      if (!plans?.length) return;
      const latestPlan = plans[plans.length - 1];
      const tasks: Mission[] = (latestPlan.tasks || []).map((t: any) => ({
        id: t.id, title: t.title, description: t.description, status: t.status
      }));
      this.missions.set(tasks);
    });
  }

  selectMission(m: Mission): void {
    this.selectedMission.set(m);
    if (!this.documentaryTitle) {
      this.documentaryTitle = m.title;
    }
  }

  // ── Captura de ítems ──────────────────────────────────────────────────

  startAdd(type: MediaType): void {
    this.cancelAdd();
    this.addingType.set(type);
  }

  cancelAdd(): void {
    this.revokePendingUrl();
    this.addingType.set(null);
    this.pendingBase64.set(null);
    this.pendingMime.set('image/jpeg');
    this.pendingBlobUrl.set(null);
    this.pendingAudioBlob.set(null);
    this.pendingEmotion.set(null);
    this.pendingLabel = '';
    this.pendingText = '';
    this.clearPendingAudio();
  }

  private revokePendingUrl(): void {
    if (this.pendingBlobUrl()) {
      URL.revokeObjectURL(this.pendingBlobUrl()!);
      this.pendingBlobUrl.set(null);
    }
  }

  onFileSelected(event: Event): void {
    const file = (event.target as HTMLInputElement).files?.[0];
    if (!file) return;
    const mime = file.type || (this.addingType() === 'photo' ? 'image/jpeg' : 'video/mp4');
    this.pendingMime.set(mime);
    this.revokePendingUrl();
    this.pendingBlobUrl.set(URL.createObjectURL(file));
    const reader = new FileReader();
    reader.onload = e => this.pendingBase64.set((e.target?.result as string).split(',')[1]);
    reader.readAsDataURL(file);
  }

  confirmAddMedia(): void {
    const type = this.addingType() as 'photo' | 'video';
    const id = crypto.randomUUID ? crypto.randomUUID() : Math.random().toString(36).slice(2);
    const emoji = type === 'photo' ? '📷' : '🎥';
    const item: CapsuleItem = {
      localId: id,
      type,
      label: this.pendingLabel || (type === 'photo' ? 'Foto' : 'Video'),
      emoji,
      base64: this.pendingBase64() ?? undefined,
      mime: this.pendingMime(),
      blobUrl: this.pendingBlobUrl() ?? undefined,
    };
    this.capturedItems.update(list => [...list, item]);
    // No revocar blobUrl — se necesita para previsualización
    this.pendingBase64.set(null);
    this.pendingBlobUrl.set(null);
    this.pendingLabel = '';
    this.addingType.set(null);
  }

  confirmAddNote(): void {
    const id = crypto.randomUUID ? crypto.randomUUID() : Math.random().toString(36).slice(2);
    const item: CapsuleItem = {
      localId: id,
      type: 'note',
      label: this.pendingLabel || 'Nota',
      emoji: '✍️',
      text: this.pendingText,
      emotion: this.pendingEmotion() ?? undefined,
    };
    this.capturedItems.update(list => [...list, item]);
    this.pendingText = '';
    this.pendingLabel = '';
    this.pendingEmotion.set(null);
    this.addingType.set(null);
  }

  confirmAddAudio(): void {
    const blob = this.pendingAudioBlob();
    if (!blob) return;
    const id = crypto.randomUUID ? crypto.randomUUID() : Math.random().toString(36).slice(2);
    const item: CapsuleItem = {
      localId: id,
      type: 'audio',
      label: this.pendingLabel || 'Audio',
      emoji: '🎤',
      blobUrl: this.pendingBlobUrl() ?? undefined,
      // base64 se calculará en submit
    };
    // Guardar blob referencia para conversión posterior
    (item as any)._blob = blob;
    this.capturedItems.update(list => [...list, item]);
    this.pendingAudioBlob.set(null);
    this.pendingBlobUrl.set(null);
    this.pendingLabel = '';
    this.addingType.set(null);
  }

  addLocation(): void {
    if (!navigator.geolocation) return;
    navigator.geolocation.getCurrentPosition(pos => {
      const id = crypto.randomUUID ? crypto.randomUUID() : Math.random().toString(36).slice(2);
      const item: CapsuleItem = {
        localId: id,
        type: 'location',
        label: 'Ubicación',
        emoji: '📍',
        lat: pos.coords.latitude,
        lng: pos.coords.longitude,
      };
      this.capturedItems.update(list => [...list, item]);
    });
  }

  removeItem(localId: string): void {
    const item = this.capturedItems().find(i => i.localId === localId);
    if (item?.blobUrl) URL.revokeObjectURL(item.blobUrl);
    this.capturedItems.update(list => list.filter(i => i.localId !== localId));
  }

  // ── Audio recording ───────────────────────────────────────────────────

  async startRecording(): Promise<void> {
    this.audioError.set(null);
    try {
      const stream = await navigator.mediaDevices.getUserMedia({ audio: true });
      this.audioChunks = [];
      const mimeType = ['audio/webm', 'audio/ogg', 'audio/mp4', ''].find(m => !m || MediaRecorder.isTypeSupported(m)) ?? '';
      this.mediaRecorder = new MediaRecorder(stream, mimeType ? { mimeType } : {});
      this.mediaRecorder.ondataavailable = e => { if (e.data?.size > 0) this.audioChunks.push(e.data); };
      this.mediaRecorder.onstop = () => {
        const mime = this.mediaRecorder?.mimeType || 'audio/webm';
        const blob = new Blob(this.audioChunks, { type: mime });
        this.pendingAudioBlob.set(blob);
        this.pendingBlobUrl.set(URL.createObjectURL(blob));
        stream.getTracks().forEach(t => t.stop());
      };
      this.mediaRecorder.start();
      this.recording.set(true);
      this.recordSeconds.set(0);
      this.recordTimer = setInterval(() => {
        this.recordSeconds.update(s => s + 1);
        if (this.recordSeconds() >= 90) this.stopRecording();
      }, 1000);
    } catch {
      this.audioError.set('No se pudo acceder al micrófono. Verifica los permisos.');
    }
  }

  stopRecording(): void {
    clearInterval(this.recordTimer);
    if (this.mediaRecorder && this.recording()) {
      this.mediaRecorder.stop();
      this.recording.set(false);
    }
  }

  clearPendingAudio(): void {
    if (this.pendingBlobUrl()) { URL.revokeObjectURL(this.pendingBlobUrl()!); this.pendingBlobUrl.set(null); }
    this.pendingAudioBlob.set(null);
    this.recordSeconds.set(0);
  }

  formatSeconds(s: number): string {
    return `${Math.floor(s / 60)}:${(s % 60).toString().padStart(2, '0')}`;
  }

  // ── Envío ─────────────────────────────────────────────────────────────

  async submit(): Promise<void> {
    const familyId = this.familyState.getSelectedFamilyId();
    const mission  = this.selectedMission();
    if (!familyId || !mission) return;

    this.step.set('submitting');
    this.submitStep.set(1);
    this.submitProgress.set(0);
    this.submitError.set(null);

    const items = this.capturedItems();
    let lastEvidenceId: number | null = null;

    // 1. Enviar cada ítem como evidencia individual
    for (let i = 0; i < items.length; i++) {
      const item = items[i];
      let base64 = item.base64;
      let mime   = item.mime;

      // Convertir audio blob a base64 si aplica
      if (item.type === 'audio' && (item as any)._blob) {
        base64 = await this.blobToBase64((item as any)._blob);
        mime = (item as any)._blob.type || 'audio/webm';
      }

      const typeMap: Record<MediaType, string> = {
        photo: 'PHOTO', video: 'VIDEO', audio: 'AUDIO',
        note: 'SELF_REFLECTION', location: 'DOCUMENT'
      };

      const req: SubmitEvidenceRequest = {
        taskId: mission.id,
        familyId,
        evidenceType: typeMap[item.type] as any,
        title: `[Documental] ${this.documentaryTitle} — ${item.label}`,
        description: item.text,
        textContent: item.type === 'note' ? item.text : undefined,
        submittedBy: this.submitterName || 'Familia',
        emotion: item.emotion,
        latitude: item.lat,
        longitude: item.lng,
        mediaData: base64,
        mediaMime: mime,
      };

      const result = await this.evidenceSvc.submit(req).pipe(catchError(() => of(null))).toPromise();
      if (result?.data?.id) lastEvidenceId = result.data.id;
      this.submitProgress.set(i + 1);
    }

    this.submitStep.set(2);

    // 2. Enviar evidencia maestra tipo BITACORA con todas las reflexiones
    const masterText = [
      `DOCUMENTAL: ${this.documentaryTitle}`,
      `MISIÓN: ${mission.title}`,
      ``,
      `¿Qué hicimos?\n${this.reflections.whatWeDid}`,
      ``,
      `¿Quién participó?\n${this.reflections.whoParticipated}`,
      ``,
      `¿Cómo nos sentimos?\n${this.reflections.howWeFeeled}`,
      ``,
      `¿Qué aprendimos?\n${this.reflections.whatWeLearned}`,
      this.reflections.whatWeWouldImprove ? `\n¿Qué mejoraríamos?\n${this.reflections.whatWeWouldImprove}` : '',
    ].join('\n');

    const masterReq: SubmitEvidenceRequest = {
      taskId: mission.id,
      familyId,
      evidenceType: 'BITACORA',
      title: `🎬 Mini Documental: ${this.documentaryTitle}`,
      description: masterText,
      textContent: masterText,
      submittedBy: this.submitterName || 'Familia',
    };

    const masterResult = await this.evidenceSvc.submit(masterReq).pipe(catchError(() => of(null))).toPromise();

    this.submitStep.set(3);

    // 3. Polling para obtener narrativa IA de la evidencia maestra
    if (masterResult?.data?.id) {
      await this.pollForNarrative(familyId, masterResult.data.id);
    } else {
      // Si falló el master, igual mostramos el documental con narrativa vacía
      setTimeout(() => this.step.set('documentary'), 800);
    }
  }

  private async pollForNarrative(familyId: number, evidenceId: number): Promise<void> {
    return new Promise(resolve => {
      let attempts = 0;
      const poll = setInterval(() => {
        attempts++;
        this.http.get<any>(`${this.api.base}/evidences/family/${familyId}`).subscribe({
          next: res => {
            const ev = (res?.data ?? []).find((e: any) => e.id === evidenceId);
            if (ev?.status === 'VALIDATED' || attempts >= 15) {
              clearInterval(poll);
              if (ev?.description && ev.description !== masterText(this.documentaryTitle, this.selectedMission()?.title ?? '')) {
                this.aiNarrative.set(ev.description);
              }
              this.step.set('documentary');
              resolve();
            }
          },
          error: () => {
            if (attempts >= 15) { clearInterval(poll); this.step.set('documentary'); resolve(); }
          }
        });
      }, 3000);
    });
  }

  getEmotionEmoji(key: string): string {
    return EMOTIONS.find(e => e.key === key)?.emoji ?? '💭';
  }

  goToMovie(): void {
    this.router.navigate(['/family-movie']);
  }

  resetAll(): void {
    this.capturedItems().forEach(i => { if (i.blobUrl) URL.revokeObjectURL(i.blobUrl); });
    this.capturedItems.set([]);
    this.selectedMission.set(null);
    this.documentaryTitle = '';
    this.reflections = { whatWeDid: '', whoParticipated: '', howWeFeeled: '', whatWeLearned: '', whatWeWouldImprove: '' };
    this.submitterName = '';
    this.aiNarrative.set(null);
    this.submitError.set(null);
    this.step.set('mission');
  }

  private blobToBase64(blob: Blob): Promise<string> {
    return new Promise((resolve, reject) => {
      const reader = new FileReader();
      reader.onload  = () => resolve((reader.result as string).split(',')[1]);
      reader.onerror = reject;
      reader.readAsDataURL(blob);
    });
  }
}

// helper puro para comparar texto en el polling
function masterText(title: string, missionTitle: string): string {
  return `DOCUMENTAL: ${title}\nMISIÓN: ${missionTitle}`;
}
