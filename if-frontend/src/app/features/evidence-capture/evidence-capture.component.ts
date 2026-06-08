import { Component, OnInit, OnDestroy, inject, signal, Input } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { HttpClient } from '@angular/common/http';
import { EvidenceService, SubmitEvidenceRequest } from '../../core/services/evidence.service';
import { FamilyStateService } from '../../core/services/family-state.service';
import { ApiService } from '../../core/services/api.service';
import { catchError, of } from 'rxjs';

type CaptureMode = 'menu' | 'photo' | 'audio' | 'text' | 'location' | 'preview';
type MainTab    = 'capture' | 'gallery';

// Documental agrupado para la galería
interface SavedDocumental {
  id: string;            // id de la evidencia BITACORA maestra
  title: string;
  missionTitle: string;
  date: string;
  coverMime: string | null;
  coverData: string | null;   // base64 de la primera foto
  aiNarrative: string | null;
  items: SavedEvidenceItem[];
  reflections: { q: string; a: string }[];
}
interface SavedEvidenceItem {
  id: number;
  type: string;         // PHOTO | VIDEO | AUDIO | SELF_REFLECTION | DOCUMENT
  label: string;
  mediaData: string | null;
  mediaMime: string | null;
  textContent: string | null;
  latitude: number | null;
  longitude: number | null;
  emotion: string | null;
  createdAt: string;
}
const EMOTIONS = [
  { key: 'alegria',     label: 'Alegría',     emoji: '😊' },
  { key: 'gratitud',   label: 'Gratitud',    emoji: '🙏' },
  { key: 'amor',       label: 'Amor',        emoji: '❤️' },
  { key: 'orgullo',    label: 'Orgullo',     emoji: '🌟' },
  { key: 'calma',      label: 'Calma',       emoji: '😌' },
  { key: 'esperanza',  label: 'Esperanza',   emoji: '🌱' },
  { key: 'tristeza',   label: 'Tristeza',    emoji: '😢' },
  { key: 'tension',    label: 'Tensión',     emoji: '😤' },
];

@Component({
  selector: 'app-evidence-capture',
  standalone: true,
  imports: [CommonModule, FormsModule],
  template: `
    <div class="ec-page">

      <!-- Header -->
      <div class="ec-header">
        <div class="ec-icon">📸</div>
        <div>
          <h1 class="ec-title">Cápsula Familiar</h1>
          <p class="ec-sub">Captura este momento — la IA lo leerá y responderá</p>
        </div>
      </div>

      <!-- Tabs principales -->
      <div class="main-tabs">
        <button class="mtab" [class.active]="mainTab() === 'capture'" (click)="switchTab('capture')">
          📸 Capturar
        </button>
        <button class="mtab" [class.active]="mainTab() === 'gallery'" (click)="switchTab('gallery')">
          🎬 Mis Documentales
          @if (documentals().length > 0) {
            <span class="mtab-badge">{{ documentals().length }}</span>
          }
        </button>
      </div>

      <!-- ══ GALERÍA DE DOCUMENTALES ══════════════════════════════════════ -->
      @if (mainTab() === 'gallery') {

        @if (loadingDocs()) {
          <div class="docs-loading">
            <div class="dl-spinner"></div>
            <span>Cargando tus documentales...</span>
          </div>
        }

        @if (!loadingDocs() && documentals().length === 0) {
          <div class="docs-empty">
            <div class="de-icon">🎞️</div>
            <h3>Aún no hay documentales</h3>
            <p>Cuando completes una misión y crees un Mini Documental, aparecerá aquí como una historia viva de tu familia.</p>
            <button class="btn-go-doc" (click)="goToDocumentary()">🎬 Crear primer documental</button>
          </div>
        }

        @if (!loadingDocs() && documentals().length > 0 && !viewingDoc()) {
          <div class="docs-grid">
            @for (doc of documentals(); track doc.id) {
              <div class="doc-card" (click)="openDoc(doc)">

                <!-- Portada -->
                <div class="dc-cover">
                  @if (doc.coverData && doc.coverMime) {
                    <img [src]="'data:' + doc.coverMime + ';base64,' + doc.coverData"
                         class="dc-cover-img" alt="Portada" />
                  } @else {
                    <div class="dc-cover-placeholder">🎬</div>
                  }
                  <div class="dc-cover-gradient"></div>
                  <div class="dc-cover-label">
                    <div class="dc-mission">🎯 {{ doc.missionTitle }}</div>
                    <div class="dc-title">{{ doc.title }}</div>
                  </div>
                </div>

                <!-- Info -->
                <div class="dc-info">
                  <div class="dc-meta">
                    <span class="dc-items">{{ doc.items.length }} evidencias</span>
                    <span class="dc-date">{{ formatDocDate(doc.date) }}</span>
                  </div>
                  @if (doc.aiNarrative) {
                    <p class="dc-narrative-preview">"{{ doc.aiNarrative.slice(0, 100) }}…"</p>
                  }
                  <button class="btn-ver-doc">Ver documental completo →</button>
                </div>

              </div>
            }
          </div>
        }

        <!-- ── VISOR DE DOCUMENTAL COMPLETO ───────────────────────────── -->
        @if (viewingDoc(); as doc) {
          <div class="doc-viewer">

            <!-- Botón volver -->
            <button class="btn-back-gallery" (click)="viewingDoc.set(null)">← Volver a documentales</button>

            <!-- Sello misión cumplida -->
            <div class="dv-seal">
              <span>🏆</span>
              <div>
                <div class="dv-seal-label">MISIÓN CUMPLIDA</div>
                <div class="dv-seal-date">{{ formatDocDate(doc.date) }}</div>
              </div>
            </div>

            <!-- Portada cinematográfica -->
            <div class="dv-cover">
              @if (doc.coverData && doc.coverMime) {
                <img [src]="'data:' + doc.coverMime + ';base64,' + doc.coverData"
                     class="dv-cover-img" alt="Portada" />
              }
              <div class="dv-cover-gradient"></div>
              <div class="dv-cover-overlay">
                <div class="dv-mission-tag">🎯 {{ doc.missionTitle }}</div>
                <h1 class="dv-title">{{ doc.title }}</h1>
                <div class="dv-meta">{{ doc.items.length }} evidencias · {{ submitterFromDoc(doc) }}</div>
              </div>
            </div>

            <!-- Narrativa IA -->
            @if (doc.aiNarrative) {
              <div class="dv-narrative">
                <div class="dv-nar-badge">🧠 Narrativa · Sentinel AI</div>
                <p class="dv-nar-text">{{ doc.aiNarrative }}</p>
              </div>
            }

            <!-- Galería de evidencias -->
            <div class="dv-section-title">📸 Evidencias de la misión</div>
            <div class="dv-gallery">
              @for (item of doc.items; track item.id) {

                @if (item.type === 'PHOTO' && item.mediaData && item.mediaMime) {
                  <div class="dv-polaroid">
                    <img [src]="'data:' + item.mediaMime + ';base64,' + item.mediaData"
                         class="dv-polaroid-img" alt="Foto" />
                    <div class="dv-polaroid-caption">{{ cleanLabel(item.label) }}</div>
                  </div>
                }

                @if (item.type === 'VIDEO' && item.mediaData && item.mediaMime) {
                  <div class="dv-polaroid">
                    <video [src]="'data:' + item.mediaMime + ';base64,' + item.mediaData"
                           class="dv-polaroid-img" controls muted></video>
                    <div class="dv-polaroid-caption">{{ cleanLabel(item.label) }}</div>
                  </div>
                }

                @if (item.type === 'AUDIO' && item.mediaData && item.mediaMime) {
                  <div class="dv-audio-card">
                    <div class="dv-audio-icon">🎙️</div>
                    <audio [src]="'data:' + item.mediaMime + ';base64,' + item.mediaData"
                           controls class="dv-audio-player"></audio>
                    <div class="dv-audio-label">{{ cleanLabel(item.label) }}</div>
                  </div>
                }

                @if (item.type === 'SELF_REFLECTION' && item.textContent) {
                  <div class="dv-note-card">
                    <div class="dv-note-mark">"</div>
                    <p class="dv-note-text">{{ item.textContent }}</p>
                    @if (item.emotion) {
                      <span class="dv-note-emotion">{{ emotionEmoji(item.emotion) }}</span>
                    }
                    <div class="dv-note-label">{{ cleanLabel(item.label) }}</div>
                  </div>
                }

                @if (item.type === 'DOCUMENT' && item.latitude && item.longitude) {
                  <div class="dv-location-card">
                    <div class="dv-loc-icon">📍</div>
                    <div class="dv-loc-coords">{{ item.latitude.toFixed(4) }}, {{ item.longitude.toFixed(4) }}</div>
                    <a [href]="'https://maps.google.com/?q=' + item.latitude + ',' + item.longitude"
                       target="_blank" class="dv-maps-link">Abrir en Google Maps →</a>
                  </div>
                }

              }
            </div>

            <!-- Bitácora (reflexiones) -->
            @if (doc.reflections.length > 0) {
              <div class="dv-section-title">📖 Bitácora de la misión</div>
              <div class="dv-logbook">
                @for (r of doc.reflections; track r.q) {
                  <div class="dv-lb-entry">
                    <div class="dv-lb-q">{{ r.q }}</div>
                    <div class="dv-lb-a" [class.highlight]="r.q.includes('aprendimos')">{{ r.a }}</div>
                  </div>
                }
              </div>
            }

            <!-- Banner Película Familiar -->
            <div class="dv-movie-banner">
              <div class="dv-mb-icon">🎬</div>
              <div>
                <div class="dv-mb-title">Insumo de la Película Familiar</div>
                <div class="dv-mb-hint">Este documental se suma al trimestre actual. Cada 3 meses la IA los reunirá en la Película de tu familia.</div>
              </div>
            </div>

            <div class="dv-actions">
              <button class="btn-back-gallery" (click)="viewingDoc.set(null)">← Otros documentales</button>
              <button class="btn-new-doc" (click)="goToDocumentary()">+ Nuevo documental</button>
            </div>

          </div>
        }

      }

      <!-- ══ ZONA DE CAPTURA ══════════════════════════════════════════════ -->
      @if (mainTab() === 'capture') {

      <!-- ── Menú de tipos ────────────────────────────────── -->
      @if (mode() === 'menu') {
        <div class="ec-menu">
          <button class="ec-type-btn" (click)="startPhoto()">
            <span class="tb-icon">📷</span>
            <span class="tb-label">Foto</span>
            <span class="tb-hint">Captura o sube una imagen</span>
          </button>
          <button class="ec-type-btn" (click)="startAudio()">
            <span class="tb-icon">🎤</span>
            <span class="tb-label">Audio</span>
            <span class="tb-hint">Graba hasta 90 segundos</span>
          </button>
          <button class="ec-type-btn" (click)="mode.set('text')">
            <span class="tb-icon">✍️</span>
            <span class="tb-label">Reflexión</span>
            <span class="tb-hint">Escribe lo que sientes</span>
          </button>
          <button class="ec-type-btn" (click)="captureLocation()">
            <span class="tb-icon">📍</span>
            <span class="tb-label">Ubicación</span>
            <span class="tb-hint">Marca dónde están</span>
          </button>
        </div>
      }

      <!-- ── Captura de foto ──────────────────────────────── -->
      @if (mode() === 'photo') {
        <div class="ec-capture">
          <div class="cap-title">📷 Captura o sube una foto</div>

          <!-- Input nativo — funciona en desktop y móvil -->
          <label class="file-drop">
            @if (!photoPreview()) {
              <span class="fd-icon">📷</span>
              <span class="fd-text">Toca para abrir la cámara o elegir una foto</span>
            } @else {
              <img [src]="photoPreview()!" class="photo-preview" alt="Vista previa" />
            }
            <input
              type="file"
              accept="image/*"
              capture="environment"
              class="file-input"
              (change)="onPhotoSelected($event)"
            />
          </label>

          @if (photoPreview()) {
            <button class="btn-retake" (click)="clearPhoto()">🔄 Tomar otra</button>
          }

          <div class="cap-nav">
            <button class="btn-back" (click)="mode.set('menu')">← Volver</button>
            @if (photoPreview()) {
              <button class="btn-next" (click)="goToPreview()">Continuar →</button>
            }
          </div>
        </div>
      }

      <!-- ── Grabación de audio ───────────────────────────── -->
      @if (mode() === 'audio') {
        <div class="ec-capture">
          <div class="cap-title">🎤 Graba un mensaje de voz</div>

          <div class="audio-recorder">
            @if (!recording() && !audioBlob()) {
              <button class="btn-record" (click)="startRecording()">
                <span class="rec-dot"></span> Iniciar grabación
              </button>
            }
            @if (recording()) {
              <div class="rec-live">
                <span class="rec-pulse"></span>
                <span class="rec-time">{{ formatSeconds(recordSeconds()) }}</span>
                <button class="btn-stop-rec" (click)="stopRecording()">⏹ Detener</button>
              </div>
            }
            @if (audioBlob() && !recording()) {
              <div class="audio-preview">
                <audio [src]="audioUrl()!" controls class="audio-player" (loadedmetadata)="onAudioLoaded($event)"></audio>
                <button class="btn-retake" (click)="clearAudio()">🔄 Grabar de nuevo</button>
              </div>
            }
          </div>

          @if (audioError()) {
            <div class="cap-error">{{ audioError() }}</div>
          }

          <div class="cap-nav">
            <button class="btn-back" (click)="mode.set('menu')">← Volver</button>
            @if (audioBlob()) {
              <button class="btn-next" (click)="goToPreview()">Continuar →</button>
            }
          </div>
        </div>
      }

      <!-- ── Reflexión escrita ────────────────────────────── -->
      @if (mode() === 'text') {
        <div class="ec-capture">
          <div class="cap-title">✍️ ¿Qué momento merece ser recordado?</div>
          <textarea
            class="text-area"
            [(ngModel)]="textContent"
            placeholder="Describe lo que vivieron hoy, un logro, una conversación, un aprendizaje…"
            rows="6"
            maxlength="1200"
          ></textarea>
          <div class="char-count">{{ textContent.length }} / 1200</div>
          <div class="cap-nav">
            <button class="btn-back" (click)="mode.set('menu')">← Volver</button>
            @if (textContent.trim()) {
              <button class="btn-next" (click)="goToPreview()">Continuar →</button>
            }
          </div>
        </div>
      }

      <!-- ── Ubicación ────────────────────────────────────── -->
      @if (mode() === 'location') {
        <div class="ec-capture">
          <div class="cap-title">📍 Ubicación del momento</div>
          @if (locationError()) {
            <div class="cap-error">{{ locationError() }}</div>
          }
          @if (latitude() && longitude()) {
            <div class="location-card">
              <span class="loc-icon">📍</span>
              <div>
                <div class="loc-coords">{{ latitude()!.toFixed(4) }}, {{ longitude()!.toFixed(4) }}</div>
                <div class="loc-hint">Ubicación capturada correctamente</div>
              </div>
            </div>
          }
          @if (!latitude()) {
            <button class="btn-locate" (click)="captureLocation()" [disabled]="locating()">
              {{ locating() ? 'Obteniendo ubicación…' : '📡 Obtener mi ubicación' }}
            </button>
          }
          <div class="cap-nav">
            <button class="btn-back" (click)="mode.set('menu')">← Volver</button>
            @if (latitude()) {
              <button class="btn-next" (click)="goToPreview()">Continuar →</button>
            }
          </div>
        </div>
      }

      <!-- ── Vista previa + emoción + envío ─────────────── -->
      @if (mode() === 'preview' && !submitted()) {
        <div class="ec-preview">

          <!-- Vista previa del contenido capturado -->
          @if (photoPreview()) {
            <img [src]="photoPreview()!" class="prev-photo" alt="Foto capturada" />
          }
          @if (audioUrl()) {
            <audio [src]="audioUrl()!" controls class="prev-audio" (loadedmetadata)="onAudioLoaded($event)"></audio>
          }
          @if (textContent) {
            <div class="prev-text">{{ textContent }}</div>
          }
          @if (latitude()) {
            <div class="prev-location">📍 {{ latitude()!.toFixed(4) }}, {{ longitude()!.toFixed(4) }}</div>
          }

          <!-- Título -->
          <div class="field-group">
            <label class="field-label">Título del momento</label>
            <input class="field-input" [(ngModel)]="title" placeholder="Ej: Cena sin celulares" maxlength="120" />
          </div>

          <!-- Quién capturó -->
          <div class="field-group">
            <label class="field-label">¿Quién lo captura?</label>
            <input class="field-input" [(ngModel)]="memberName" placeholder="Tu nombre" maxlength="80" />
          </div>

          <div class="cap-title" style="margin-top: 8px;">¿Cómo se sintieron? (Opcional)</div>

          <!-- Selector de emoción -->
          <div class="emotion-grid">
            @for (em of emotions; track em.key) {
              <button
                class="em-btn"
                [class.selected]="selectedEmotion() === em.key"
                (click)="selectedEmotion.set(em.key)"
              >
                <span class="em-emoji">{{ em.emoji }}</span>
                <span class="em-label">{{ em.label }}</span>
              </button>
            }
          </div>

          <!-- Selección de tarea (misión) -->
          @if (tasks().length) {
            <div class="field-group">
              <label class="field-label">Misión asociada (opcional)</label>
              <select class="field-input" [(ngModel)]="selectedTaskId">
                <option [ngValue]="null">— Momento espontáneo —</option>
                @for (t of tasks(); track t.id) {
                  <option [ngValue]="t.id">{{ t.title }}</option>
                }
              </select>
            </div>
          }

          @if (submitError()) {
            <div class="cap-error">{{ submitError() }}</div>
          }

          <div class="cap-nav">
            <button class="btn-back" (click)="goBack()">← Volver</button>
            <button class="btn-submit" (click)="submit()" [disabled]="submitting() || !title.trim()">
              {{ submitting() ? '⏳ Enviando…' : '✨ Enviar cápsula' }}
            </button>
          </div>
        </div>
      }

      <!-- ── Éxito ──────────────────────────────────────── -->
      @if (submitted()) {
        <div class="ec-success-container" style="animation: fadeIn 0.4s ease-out;">
          
          <!-- Sondeo activo: escaneando coherencia -->
          @if (pollingActive()) {
            <div class="cognitive-scanner-box">
              <div class="scanner-laser-line"></div>
              <div class="brain-pulse-glow">🧠</div>
              <h2 class="scanner-title">Analizando Coherencia Conductual</h2>
              <p class="scanner-subtitle">Sentinel AI (Claude) está realizando la decodificación cognitiva del momento familiar y los acuerdos de la misión...</p>
              <div class="spinner-premium" style="border-top-color: #8b5cf6; margin-top: 24px; width: 36px; height: 36px;"></div>
              <div class="scanner-eta-label">Analizando evidencias multimodales — máx. 45s</div>
            </div>
          }

          <!-- Sello y Contemplación Inline -->
          @if (!pollingActive() && validatedEvidence(); as ev) {
            <div class="validation-congratulations" style="text-align: center; margin-bottom: 24px; animation: scaleUp 0.3s cubic-bezier(0.175, 0.885, 0.32, 1.275);">
              <span style="font-size: 56px;">🎉</span>
              <h2 style="font-size: 24px; font-weight: 800; color: #fff; margin: 8px 0 4px 0;">¡Momento Sellado y Validado!</h2>
              <p style="color: #10b981; font-size: 13.5px; font-weight: 700; margin: 0; text-transform: uppercase; letter-spacing: 0.5px;">✓ Coherencia conductual certificada por Sentinel AI</p>
            </div>

            <!-- Portal de Contemplación Inline -->
            <div class="contemplation-inline-card"
                 [style.--emotion-color]="getEmotionDetails(ev.emotion).color"
                 [style.--emotion-glow]="getEmotionDetails(ev.emotion).glow">
              
              <div class="emotion-glow-bar" [style.background]="getEmotionDetails(ev.emotion).color"></div>

              <div style="display: flex; justify-content: space-between; align-items: center; border-bottom: 1px solid rgba(255,255,255,0.08); padding-bottom: 12px; margin-bottom: 20px; flex-wrap: wrap; gap: 8px;">
                <div style="display: flex; align-items: center; gap: 10px;">
                  <span class="emotion-badge-pill" 
                        [style.background]="getEmotionDetails(ev.emotion).glow" 
                        [style.borderColor]="getEmotionDetails(ev.emotion).color" 
                        [style.color]="getEmotionDetails(ev.emotion).color" 
                        style="display: inline-flex; align-items: center; gap: 6px; font-size: 11px; padding: 4px 12px; border-radius: 20px; border: 1px solid; font-weight: bold; text-transform: uppercase; letter-spacing: 0.5px;">
                    <span>{{ getEmotionDetails(ev.emotion).emoji }}</span>
                    <span>{{ getEmotionDetails(ev.emotion).label }}</span>
                  </span>
                  <h3 style="margin: 0; font-size: 16px; color: #fff; font-weight: 700;">{{ ev.title }}</h3>
                </div>
                <span style="font-size: 11.5px; color: #8b949e;">Liderado por: <strong style="color: #cbd5e1;">{{ ev.submittedBy }}</strong></span>
              </div>

              <div class="contemplation-grid-body">
                
                <!-- Left Pane: Polaroid & Audio -->
                <div class="contemplation-left-pane">
                  @if (ev.mediaData && ev.mediaMime && ev.mediaMime.startsWith('image/')) {
                    <div class="polaroid-large-wrapper" style="max-width: 260px; padding: 10px 10px 22px 10px;">
                      <img [src]="'data:' + ev.mediaMime + ';base64,' + ev.mediaData" style="max-width: 240px; height: 200px; object-fit: cover; border: 1px solid #e0e0e0; display: block;" alt="Foto Polaroid" />
                      <div class="polaroid-large-tape" style="width: 50px; height: 16px; top: -10px;"></div>
                    </div>
                  } @else if (ev.mediaData && ev.mediaMime && ev.mediaMime.startsWith('video/')) {
                    <div class="polaroid-large-wrapper" style="max-width: 260px; padding: 10px 10px 22px 10px;">
                      <video [src]="'data:' + ev.mediaMime + ';base64,' + ev.mediaData" controls style="max-width: 240px; height: 200px; object-fit: cover; border: 1px solid #e0e0e0; display: block; background: #000;"></video>
                      <div class="polaroid-large-tape" style="width: 50px; height: 16px; top: -10px;"></div>
                    </div>
                  } @else {
                    <div class="memory-artwork-placeholder">
                      <span style="font-size: 48px; text-shadow: 0 0 15px var(--emotion-glow);">{{ getEmotionDetails(ev.emotion).emoji }}</span>
                      <span style="font-size: 11px; font-weight: bold; text-transform: uppercase; letter-spacing: 0.5px; margin-top: 8px; color: #8b949e;">Acuerdo Sellado</span>
                    </div>
                  }

                  @if (ev.mediaData && ev.mediaMime && ev.mediaMime.startsWith('audio/')) {
                    <div class="retro-audio-card" style="max-width: 260px; padding: 12px; background: linear-gradient(135deg, #1f2937 0%, #111827 100%); border: 1px solid rgba(255,255,255,0.08); border-radius: 10px; margin-top: 10px;">
                      <div class="cassette-decor" style="display: flex; justify-content: center; gap: 24px; margin-bottom: 8px; opacity: 0.15;">
                        <span style="animation: spin 3s linear infinite; display: inline-block;">⚙️</span>
                        <span style="animation: spin 3s linear infinite; display: inline-block;">⚙️</span>
                      </div>
                      <audio [src]="'data:' + ev.mediaMime + ';base64,' + ev.mediaData" controls style="width: 100%; border-radius: 4px; height: 32px;" (loadedmetadata)="onAudioLoaded($event)"></audio>
                    </div>
                  }
                </div>

                <!-- Right Pane: Written Note, Map, Sentinel AI -->
                <div class="contemplation-right-pane">
                  <div class="handwritten-note">
                    <div class="handwritten-paper">
                      <div class="handwritten-lines">
                        <span style="font-size: 32px; line-height: 0.1; vertical-align: middle; margin-right: 4px; color: var(--emotion-color);">“</span>
                        <p style="margin: 0; display: inline; font-style: italic;">
                          {{ ev.textContent || 'Vivimos un momento de unión, conexión y reflexión conjunta que fortalece los cimientos y hábitos de nuestro núcleo familiar.' }}
                        </p>
                      </div>
                    </div>
                  </div>

                  @if (ev.latitude && ev.longitude) {
                    <div class="contemplation-map-box">
                      <div style="display: flex; justify-content: space-between; font-size: 10.5px; font-weight: 700; color: #fff; margin-bottom: 8px;">
                        <span>📍 Ubicación Geográfica</span>
                        <small class="text-muted">Coords: {{ ev.latitude.toFixed(4) }}, {{ ev.longitude.toFixed(4) }}</small>
                      </div>
                      <div id="contemplation-map-capture"></div>
                    </div>
                  }

                  @if (ev.description) {
                    <div class="contemplation-seal-card" [style.borderLeftColor]="getEmotionDetails(ev.emotion).color">
                      <div class="sentinel-wax-seal" [style.background]="getEmotionDetails(ev.emotion).color" [style.boxShadow]="'0 0 10px ' + getEmotionDetails(ev.emotion).glow">
                        🧠
                      </div>
                      <div style="flex: 1;">
                        <h4 style="margin: 0 0 2px 0; color: #fff; font-size: 11px; font-weight: 700; text-transform: uppercase;">Sentinel AI seal</h4>
                        <p style="margin: 0; font-size: 11px; color: #cbd5e1; line-height: 1.4; opacity: 0.85;">{{ ev.description }}</p>
                      </div>
                    </div>
                  }

                </div>

              </div>

            </div>

            <!-- CTA Película Familiar -->
            <div style="margin-top: 20px; padding: 16px; background: linear-gradient(135deg, rgba(139,92,246,0.15), rgba(236,72,153,0.1)); border: 1px solid rgba(139,92,246,0.3); border-radius: 14px; text-align: center;">
              <div style="font-size: 28px; margin-bottom: 6px;">🎬</div>
              <p style="font-size: 13px; color: #c084fc; font-weight: 700; margin: 0 0 4px 0;">Esta cápsula forma parte de tu Película Familiar</p>
              <p style="font-size: 11.5px; color: #8b949e; margin: 0 0 12px 0;">Cada momento que capturas se teje en el documental de tu familia</p>
              <a href="/family-movie" style="display: inline-block; padding: 10px 22px; background: linear-gradient(135deg, #7c3aed, #db2777); border-radius: 10px; color: #fff; font-size: 13px; font-weight: 700; text-decoration: none; letter-spacing: 0.3px;">
                🎬 Ver Película Familiar →
              </a>
            </div>

            <div style="display: flex; gap: 12px; justify-content: center; margin-top: 16px;">
              <button class="btn-new" (click)="reset()">+ Nueva cápsula</button>
              <a href="/checklist" class="btn-back" style="text-decoration: none; padding: 12px 24px; border-radius: 10px; font-size: 14px; font-weight: 700; display: inline-flex; align-items: center; border: 1px solid rgba(255,255,255,0.1); color: #8b949e; background: transparent; transition: all 0.2s;">
                Ir a Bitácora
              </a>
            </div>
          }

        </div>
      }

      } <!-- /tab capture -->

    </div>
  `,
  styles: [`
    .ec-page {
      max-width: 640px;
      margin: 0 auto;
      padding: 24px 20px 60px;
      font-family: inherit;
      color: var(--if-text-primary, #e0e0e0);
    }

    /* Header */
    .ec-header { display: flex; align-items: center; gap: 14px; margin-bottom: 28px; }
    .ec-icon   { font-size: 42px; }
    .ec-title  { font-size: 24px; font-weight: 800; margin: 0 0 4px; }
    .ec-sub    { font-size: 13px; color: var(--if-text-secondary, #888); margin: 0; }

    /* Menú de tipos */
    .ec-menu {
      display: grid;
      grid-template-columns: 1fr 1fr;
      gap: 14px;
      margin-bottom: 24px;
    }
    .ec-type-btn {
      display: flex; flex-direction: column; align-items: center;
      gap: 6px; padding: 22px 16px;
      background: var(--if-surface, rgba(255,255,255,0.04));
      border: 1px solid rgba(255,255,255,0.09);
      border-radius: 16px;
      cursor: pointer; transition: all 0.2s;
      color: var(--if-text-primary, #ddd);
    }
    .ec-type-btn:hover { background: rgba(255,255,255,0.08); border-color: rgba(139,92,246,0.35); transform: translateY(-2px); }
    .tb-icon  { font-size: 30px; }
    .tb-label { font-size: 14px; font-weight: 700; }
    .tb-hint  { font-size: 11px; color: var(--if-text-secondary, #888); text-align: center; }

    /* Secciones de captura */
    .ec-capture, .ec-preview { display: flex; flex-direction: column; gap: 16px; }
    .cap-title { font-size: 16px; font-weight: 700; margin-bottom: 4px; }

    /* Foto */
    .file-drop {
      display: flex; flex-direction: column; align-items: center; justify-content: center;
      gap: 12px; padding: 32px;
      border: 2px dashed rgba(255,255,255,0.15);
      border-radius: 16px;
      cursor: pointer; transition: border-color 0.2s;
      position: relative; min-height: 180px;
    }
    .file-drop:hover { border-color: rgba(139,92,246,0.5); }
    .fd-icon  { font-size: 36px; }
    .fd-text  { font-size: 13px; color: var(--if-text-secondary, #888); text-align: center; }
    .file-input { position: absolute; inset: 0; opacity: 0; cursor: pointer; }
    .photo-preview { max-width: 100%; max-height: 300px; border-radius: 12px; object-fit: cover; }
    .btn-retake {
      background: rgba(255,255,255,0.06); border: 1px solid rgba(255,255,255,0.12);
      color: var(--if-text-secondary, #aaa); padding: 8px 16px;
      border-radius: 8px; font-size: 13px; cursor: pointer;
      align-self: flex-start;
    }

    /* Audio */
    .audio-recorder { display: flex; flex-direction: column; align-items: center; gap: 16px; padding: 24px; }
    .btn-record {
      display: flex; align-items: center; gap: 10px;
      background: rgba(239,68,68,0.12); border: 1px solid rgba(239,68,68,0.3);
      color: #fca5a5; padding: 14px 28px; border-radius: 99px;
      font-size: 15px; font-weight: 700; cursor: pointer; transition: all 0.2s;
    }
    .btn-record:hover { background: rgba(239,68,68,0.2); }
    .rec-dot {
      width: 12px; height: 12px; border-radius: 50%;
      background: #ef4444;
      animation: pulse 1.2s ease-in-out infinite;
    }
    @keyframes pulse {
      0%, 100% { opacity: 1; transform: scale(1); }
      50%       { opacity: 0.5; transform: scale(0.8); }
    }
    .rec-live { display: flex; align-items: center; gap: 16px; }
    .rec-pulse {
      width: 14px; height: 14px; border-radius: 50%;
      background: #ef4444;
      animation: pulse 0.8s ease-in-out infinite;
    }
    .rec-time { font-size: 24px; font-weight: 800; font-variant-numeric: tabular-nums; color: #fca5a5; }
    .btn-stop-rec {
      background: rgba(239,68,68,0.15); border: 1px solid rgba(239,68,68,0.3);
      color: #fca5a5; padding: 8px 18px; border-radius: 8px; font-size: 13px; cursor: pointer;
    }
    .audio-preview { display: flex; flex-direction: column; align-items: center; gap: 12px; width: 100%; }
    .audio-player { width: 100%; border-radius: 10px; }

    /* Texto */
    .text-area {
      width: 100%; padding: 14px; border-radius: 12px;
      background: rgba(255,255,255,0.05);
      border: 1px solid rgba(255,255,255,0.1);
      color: var(--if-text-primary, #e0e0e0);
      font-size: 14px; line-height: 1.6; resize: vertical;
      font-family: inherit;
    }
    .text-area:focus { outline: none; border-color: rgba(139,92,246,0.4); }
    .char-count { font-size: 11px; color: var(--if-text-secondary, #777); text-align: right; }

    /* Ubicación */
    .location-card {
      display: flex; align-items: center; gap: 14px;
      background: rgba(16,185,129,0.08);
      border: 1px solid rgba(16,185,129,0.25);
      border-radius: 12px; padding: 16px;
    }
    .loc-icon   { font-size: 28px; }
    .loc-coords { font-size: 14px; font-weight: 700; font-variant-numeric: tabular-nums; }
    .loc-hint   { font-size: 12px; color: #6ee7b7; }
    .btn-locate {
      background: rgba(16,185,129,0.1); border: 1px solid rgba(16,185,129,0.3);
      color: #6ee7b7; padding: 12px 24px; border-radius: 10px;
      font-size: 14px; font-weight: 600; cursor: pointer; transition: all 0.2s;
    }
    .btn-locate:disabled { opacity: 0.5; cursor: not-allowed; }

    /* Preview / emoción */
    .emotion-grid {
      display: grid; grid-template-columns: repeat(4, 1fr); gap: 8px;
    }
    .em-btn {
      display: flex; flex-direction: column; align-items: center; gap: 4px;
      padding: 10px 6px; border-radius: 12px;
      background: rgba(255,255,255,0.04);
      border: 1px solid rgba(255,255,255,0.08);
      cursor: pointer; transition: all 0.18s;
      color: var(--if-text-primary, #ddd);
    }
    .em-btn:hover    { background: rgba(255,255,255,0.09); }
    .em-btn.selected { background: rgba(139,92,246,0.18); border-color: rgba(139,92,246,0.4); }
    .em-emoji { font-size: 22px; }
    .em-label { font-size: 10px; font-weight: 600; }

    /* Campos */
    .field-group { display: flex; flex-direction: column; gap: 6px; }
    .field-label { font-size: 12px; font-weight: 700; color: var(--if-text-secondary, #aaa); text-transform: uppercase; letter-spacing: 0.06em; }
    .field-input {
      padding: 10px 14px; border-radius: 10px;
      background: rgba(255,255,255,0.05);
      border: 1px solid rgba(255,255,255,0.1);
      color: var(--if-text-primary, #e0e0e0);
      font-size: 14px; font-family: inherit;
    }
    .field-input:focus { outline: none; border-color: rgba(139,92,246,0.4); }
    select.field-input { cursor: pointer; }

    /* Vista previa de contenido */
    .prev-photo  { max-width: 100%; max-height: 180px; border-radius: 12px; object-fit: cover; }
    .prev-audio  { width: 100%; border-radius: 10px; }
    .prev-text   { background: rgba(255,255,255,0.04); border-radius: 10px; padding: 14px; font-size: 14px; line-height: 1.6; color: var(--if-text-secondary, #ccc); }
    .prev-location { font-size: 13px; color: #6ee7b7; }

    /* Navegación */
    .cap-nav { display: flex; justify-content: space-between; align-items: center; margin-top: 8px; }
    .btn-back {
      background: transparent; border: 1px solid rgba(255,255,255,0.1);
      color: var(--if-text-secondary, #aaa); padding: 10px 18px;
      border-radius: 9px; font-size: 13px; cursor: pointer;
    }
    .btn-next {
      background: rgba(139,92,246,0.18); border: 1px solid rgba(139,92,246,0.35);
      color: #c4b5fd; padding: 10px 22px;
      border-radius: 9px; font-size: 13px; font-weight: 700; cursor: pointer;
    }
    .btn-submit {
      background: linear-gradient(135deg, #7c3aed, #4f46e5);
      border: none; color: white;
      padding: 12px 28px; border-radius: 10px;
      font-size: 14px; font-weight: 700; cursor: pointer;
      transition: opacity 0.2s;
    }
    .btn-submit:disabled { opacity: 0.5; cursor: not-allowed; }

    /* Error */
    .cap-error {
      background: rgba(239,68,68,0.1); border: 1px solid rgba(239,68,68,0.25);
      border-radius: 10px; padding: 12px 16px;
      font-size: 13px; color: #fca5a5;
    }

    /* Éxito */
    .ec-success-container { padding: 20px 0; }
    .btn-new {
      background: linear-gradient(135deg, #7c3aed, #4f46e5);
      border: none; color: white; padding: 12px 28px;
      border-radius: 10px; font-size: 14px; font-weight: 700; cursor: pointer;
      box-shadow: 0 4px 15px rgba(124, 58, 237, 0.3);
      transition: transform 0.2s;
    }
    .btn-new:hover { transform: translateY(-2px); }

    /* Cognitive scanner animation box */
    .cognitive-scanner-box {
      background: rgba(13, 17, 28, 0.5);
      border: 1px solid rgba(139, 92, 246, 0.2);
      border-radius: 16px;
      padding: 40px 24px;
      text-align: center;
      position: relative;
      overflow: hidden;
      box-shadow: 0 10px 30px rgba(0,0,0,0.4);
    }
    .scanner-laser-line {
      position: absolute;
      left: 0; right: 0; height: 3px;
      background: linear-gradient(90deg, transparent, #8b5cf6, transparent);
      box-shadow: 0 0 10px #8b5cf6;
      animation: scanLaser 2.2s infinite ease-in-out;
    }
    @keyframes scanLaser {
      0% { top: 0%; }
      50% { top: 100%; }
      100% { top: 0%; }
    }
    .brain-pulse-glow {
      font-size: 64px;
      line-height: 1;
      margin-bottom: 24px;
      display: inline-block;
      animation: brainGlow 1.8s infinite ease-in-out;
    }
    @keyframes brainGlow {
      0%, 100% { transform: scale(1); filter: drop-shadow(0 0 2px rgba(139, 92, 246, 0.3)); }
      50% { transform: scale(1.1); filter: drop-shadow(0 0 15px rgba(139, 92, 246, 0.7)); }
    }
    .scanner-title { font-size: 20px; font-weight: 800; color: #fff; margin: 0 0 10px 0; }
    .scanner-subtitle { font-size: 13px; color: var(--if-text-secondary, #aaa); line-height: 1.6; max-width: 480px; margin: 0 auto; }
    .scanner-eta-label { font-family: monospace; font-size: 11px; color: #ab7df6; margin-top: 14px; font-weight: bold; }

    /* Inline Contemplation Portal layout */
    .contemplation-inline-card {
      background: rgba(13, 17, 28, 0.85);
      border: 1px solid rgba(255,255,255,0.06);
      border-left: 5px solid var(--emotion-color) !important;
      box-shadow: 0 15px 40px rgba(0, 0, 0, 0.5), 0 0 25px var(--emotion-glow) !important;
      border-radius: 14px;
      padding: 24px;
      position: relative;
      overflow: hidden;
      text-align: left;
    }
    .contemplation-inline-card .contemplation-grid-body {
      display: grid;
      grid-template-columns: 1fr;
      gap: 24px;
      align-items: start;
    }
    @media (min-width: 600px) {
      .contemplation-inline-card .contemplation-grid-body {
        grid-template-columns: auto 1fr;
      }
    }

    /* Polaroid and tape styles */
    .polaroid-large-wrapper {
      background: #ffffff;
      border-radius: 3px;
      box-shadow: 0 6px 16px rgba(0, 0, 0, 0.4);
      transform: rotate(-1.5deg);
      transition: transform 0.3s;
      position: relative;
      display: inline-block;
    }
    .polaroid-large-wrapper:hover {
      transform: rotate(1deg) scale(1.03);
    }
    .polaroid-large-tape {
      position: absolute;
      left: 50%;
      transform: translateX(-50%) rotate(3deg);
      background: rgba(255, 255, 255, 0.25);
      backdrop-filter: blur(2px);
      -webkit-backdrop-filter: blur(2px);
      border: 1px solid rgba(255, 255, 255, 0.15);
      box-shadow: 0 2px 4px rgba(0, 0, 0, 0.08);
    }

    .memory-artwork-placeholder {
      width: 100%;
      max-width: 260px;
      height: 200px;
      background: rgba(255,255,255,0.01);
      border: 2px dashed rgba(255,255,255,0.05);
      border-radius: 12px;
      display: flex;
      flex-direction: column;
      align-items: center;
      justify-content: center;
      text-align: center;
    }

    .handwritten-paper {
      padding: 16px;
      border-left: 2px solid #ff7b72;
      background: #fefcf3;
      border-radius: 8px;
      box-shadow: 0 4px 10px rgba(0,0,0,0.1);
    }

    .handwritten-lines {
      font-family: 'Caveat', 'Comic Sans MS', cursive;
      font-size: 20px;
      line-height: 1.4;
      color: #3b2a1a;
    }

    .contemplation-map-box {
      background: rgba(0,0,0,0.2);
      border: 1px solid rgba(255,255,255,0.08);
      border-radius: 10px;
      padding: 12px;
    }

    #contemplation-map-capture {
      height: 120px;
      border-radius: 6px;
      border: 1px solid rgba(255,255,255,0.05);
      background: #0d1117;
    }

    .contemplation-seal-card {
      background: rgba(255,255,255,0.015);
      border: 1px solid rgba(255,255,255,0.05);
      border-left: 3px solid #58a6ff;
      border-radius: 10px;
      padding: 12px;
      display: flex;
      gap: 10px;
      align-items: start;
    }

    .sentinel-wax-seal {
      width: 24px;
      height: 24px;
      border-radius: 50%;
      display: flex;
      align-items: center;
      justify-content: center;
      font-size: 12px;
      flex-shrink: 0;
      color: #000;
      border: 1px dashed rgba(255,255,255,0.3);
    }

    .spinner-premium {
      border: 3px solid rgba(255, 255, 255, 0.05);
      border-top-color: #8b5cf6;
      border-radius: 50%;
      animation: spin 1s linear infinite;
      margin: 0 auto;
    }

    @keyframes spin {
      to { transform: rotate(360deg); }
    }

    @keyframes fadeIn {
      from { opacity: 0; }
      to { opacity: 1; }
    }

    @keyframes scaleUp {
      from { transform: scale(0.9); opacity: 0; }
      to { transform: scale(1); opacity: 1; }
    }

    /* ══ TABS ══════════════════════════════════════════════════════════════ */
    .main-tabs {
      display: flex; gap: 6px; margin-bottom: 24px;
      border-bottom: 1px solid rgba(255,255,255,0.08);
      padding-bottom: 0;
    }
    .mtab {
      display: flex; align-items: center; gap: 6px;
      padding: 9px 16px; border-radius: 10px 10px 0 0;
      background: transparent; border: 1px solid transparent;
      border-bottom: none;
      color: var(--if-text-secondary, #888);
      font-size: 13px; font-weight: 600; cursor: pointer;
      transition: all 0.2s; position: relative; bottom: -1px;
    }
    .mtab:hover   { color: #c4b5fd; background: rgba(124,58,237,0.06); }
    .mtab.active  {
      color: #c4b5fd; background: rgba(13,17,28,1);
      border-color: rgba(255,255,255,0.08);
      border-bottom-color: rgba(13,17,28,1);
    }
    .mtab-badge {
      background: rgba(124,58,237,0.25); color: #c4b5fd;
      border-radius: 20px; padding: 1px 7px; font-size: 10px; font-weight: 800;
    }

    /* ══ GALERÍA DE DOCUMENTALES ══════════════════════════════════════════ */
    .docs-loading {
      display: flex; align-items: center; gap: 10px;
      color: var(--if-text-secondary, #999); font-size: 13px;
      padding: 40px 0; justify-content: center;
    }
    .dl-spinner {
      width: 18px; height: 18px; border-radius: 50%;
      border: 2px solid rgba(255,255,255,0.08); border-top-color: #8b5cf6;
      animation: spin 0.9s linear infinite;
    }
    .docs-empty {
      text-align: center; padding: 52px 20px;
    }
    .de-icon { font-size: 52px; margin-bottom: 14px; }
    .docs-empty h3 { font-size: 18px; font-weight: 700; margin: 0 0 10px; }
    .docs-empty p  { font-size: 13px; color: var(--if-text-secondary, #999); max-width: 380px; margin: 0 auto 20px; line-height: 1.6; }
    .btn-go-doc {
      background: linear-gradient(135deg, #7c3aed, #4f46e5);
      border: none; color: #fff; padding: 12px 24px; border-radius: 10px;
      font-size: 14px; font-weight: 700; cursor: pointer;
    }

    .docs-grid { display: flex; flex-direction: column; gap: 18px; }

    /* Tarjeta de documental en galería */
    .doc-card {
      background: rgba(255,255,255,0.03);
      border: 1px solid rgba(255,255,255,0.08);
      border-radius: 18px; overflow: hidden; cursor: pointer;
      transition: all 0.25s;
    }
    .doc-card:hover { border-color: rgba(124,58,237,0.35); transform: translateY(-2px); box-shadow: 0 8px 24px rgba(0,0,0,0.3); }

    .dc-cover {
      position: relative; height: 180px;
      background: linear-gradient(135deg, #1e1b4b, #4c1d95);
      overflow: hidden;
    }
    .dc-cover-img {
      width: 100%; height: 100%; object-fit: cover; opacity: 0.45;
    }
    .dc-cover-placeholder {
      position: absolute; inset: 0; display: flex; align-items: center; justify-content: center;
      font-size: 52px;
    }
    .dc-cover-gradient {
      position: absolute; inset: 0;
      background: linear-gradient(to top, rgba(0,0,0,0.8) 0%, transparent 60%);
    }
    .dc-cover-label {
      position: absolute; bottom: 14px; left: 16px; right: 16px; z-index: 2;
    }
    .dc-mission { font-size: 10px; font-weight: 700; color: #a78bfa; text-transform: uppercase; letter-spacing: 0.1em; margin-bottom: 5px; }
    .dc-title   { font-size: 17px; font-weight: 800; color: #fff; line-height: 1.2; }

    .dc-info { padding: 14px 16px; }
    .dc-meta { display: flex; justify-content: space-between; align-items: center; margin-bottom: 8px; }
    .dc-items { font-size: 11px; color: var(--if-text-secondary, #888); font-weight: 600; }
    .dc-date  { font-size: 11px; color: var(--if-text-secondary, #777); }
    .dc-narrative-preview {
      font-size: 12px; font-style: italic; color: var(--if-text-secondary, #bbb);
      line-height: 1.5; margin: 0 0 12px;
    }
    .btn-ver-doc {
      font-size: 12px; font-weight: 700; color: #a78bfa;
      background: none; border: none; cursor: pointer; padding: 0;
      transition: color 0.2s;
    }
    .btn-ver-doc:hover { color: #c4b5fd; }

    /* ══ VISOR DE DOCUMENTAL COMPLETO ════════════════════════════════════ */
    .doc-viewer { display: flex; flex-direction: column; gap: 24px; animation: fadeIn 0.3s ease-out; }

    .btn-back-gallery {
      background: transparent; border: 1px solid rgba(255,255,255,0.1);
      color: var(--if-text-secondary, #aaa); padding: 9px 16px;
      border-radius: 9px; font-size: 12px; font-weight: 600; cursor: pointer;
      align-self: flex-start;
    }

    .dv-seal {
      display: flex; align-items: center; gap: 12px;
      background: linear-gradient(135deg, rgba(16,185,129,0.1), rgba(5,150,105,0.07));
      border: 1px solid rgba(16,185,129,0.25);
      border-radius: 12px; padding: 14px 18px;
      font-size: 28px;
    }
    .dv-seal-label { font-size: 12px; font-weight: 800; color: #6ee7b7; text-transform: uppercase; letter-spacing: 0.1em; }
    .dv-seal-date  { font-size: 11px; color: var(--if-text-secondary, #999); margin-top: 2px; }

    .dv-cover {
      position: relative; border-radius: 18px; overflow: hidden;
      min-height: 220px;
      background: linear-gradient(135deg, #1e1b4b 0%, #312e81 50%, #4c1d95 100%);
    }
    .dv-cover-img { position: absolute; inset: 0; width: 100%; height: 100%; object-fit: cover; opacity: 0.35; }
    .dv-cover-gradient {
      position: absolute; inset: 0;
      background: linear-gradient(to top, rgba(0,0,0,0.85) 0%, transparent 55%);
      z-index: 1;
    }
    .dv-cover-overlay {
      position: relative; z-index: 2;
      display: flex; flex-direction: column; align-items: center; justify-content: flex-end;
      padding: 24px 20px; min-height: 220px; text-align: center;
    }
    .dv-mission-tag { font-size: 10px; font-weight: 700; color: #a78bfa; text-transform: uppercase; letter-spacing: 0.15em; margin-bottom: 8px; }
    .dv-title { font-size: 26px; font-weight: 900; color: #fff; margin: 0 0 7px; line-height: 1.2; }
    .dv-meta  { font-size: 12px; color: rgba(255,255,255,0.55); }

    .dv-narrative {
      background: linear-gradient(135deg, rgba(124,58,237,0.08), rgba(79,70,229,0.05));
      border: 1px solid rgba(124,58,237,0.2);
      border-radius: 14px; padding: 20px;
    }
    .dv-nar-badge {
      display: inline-block; font-size: 10px; font-weight: 700; color: #a78bfa;
      background: rgba(124,58,237,0.12); border: 1px solid rgba(124,58,237,0.25);
      padding: 3px 10px; border-radius: 20px; text-transform: uppercase; letter-spacing: 0.06em;
      margin-bottom: 12px;
    }
    .dv-nar-text { font-size: 14px; line-height: 1.8; color: var(--if-text-primary, #ddd); margin: 0; font-style: italic; }

    .dv-section-title { font-size: 14px; font-weight: 800; border-bottom: 1px solid rgba(255,255,255,0.07); padding-bottom: 9px; }

    /* Galería del visor */
    .dv-gallery { display: grid; grid-template-columns: repeat(auto-fill, minmax(150px, 1fr)); gap: 14px; }

    .dv-polaroid {
      background: #fff; padding: 7px 7px 20px;
      border-radius: 3px; box-shadow: 0 6px 18px rgba(0,0,0,0.4);
      transform: rotate(-1.5deg); transition: transform 0.3s;
      cursor: default;
    }
    .dv-polaroid:hover { transform: rotate(1deg) scale(1.04); }
    .dv-polaroid-img { width: 100%; height: 100px; object-fit: cover; border-radius: 2px; display: block; }
    .dv-polaroid-caption { font-size: 10px; text-align: center; color: #555; margin-top: 6px; font-style: italic; padding: 0 4px; }

    .dv-audio-card {
      background: rgba(255,255,255,0.04); border: 1px solid rgba(255,255,255,0.08);
      border-radius: 12px; padding: 14px;
      display: flex; flex-direction: column; align-items: center; gap: 8px;
    }
    .dv-audio-icon  { font-size: 32px; }
    .dv-audio-player { width: 100%; }
    .dv-audio-label { font-size: 10px; color: var(--if-text-secondary, #888); font-weight: 600; }

    .dv-note-card {
      background: #fefcf3; border-radius: 10px; padding: 12px;
      min-height: 100px; box-shadow: 0 4px 10px rgba(0,0,0,0.15);
    }
    .dv-note-mark  { font-size: 40px; line-height: 0.5; color: rgba(124,58,237,0.25); }
    .dv-note-text  { font-size: 12px; line-height: 1.5; color: #3b2a1a; font-style: italic; margin: 6px 0 0; }
    .dv-note-emotion { font-size: 22px; display: block; margin-top: 6px; }
    .dv-note-label { font-size: 10px; color: #777; margin-top: 6px; font-weight: 600; }

    .dv-location-card {
      background: rgba(16,185,129,0.07); border: 1px solid rgba(16,185,129,0.2);
      border-radius: 12px; padding: 14px;
      display: flex; flex-direction: column; align-items: center; gap: 8px;
      text-align: center;
    }
    .dv-loc-icon   { font-size: 28px; }
    .dv-loc-coords { font-size: 10px; font-variant-numeric: tabular-nums; color: #6ee7b7; font-weight: 700; }
    .dv-maps-link  { font-size: 11px; color: #6ee7b7; text-decoration: none; border-bottom: 1px dotted #6ee7b7; }

    /* Bitácora del visor */
    .dv-logbook { display: flex; flex-direction: column; gap: 14px; }
    .dv-lb-entry { border-left: 3px solid rgba(124,58,237,0.4); padding-left: 14px; }
    .dv-lb-q { font-size: 10px; font-weight: 700; color: #a78bfa; text-transform: uppercase; letter-spacing: 0.08em; margin-bottom: 4px; }
    .dv-lb-a { font-size: 13px; line-height: 1.7; color: var(--if-text-primary, #ddd); }
    .dv-lb-a.highlight { color: #c4b5fd; font-style: italic; font-weight: 600; }

    /* Banner película */
    .dv-movie-banner {
      display: flex; align-items: center; gap: 14px;
      background: linear-gradient(135deg, rgba(245,158,11,0.1), rgba(217,119,6,0.06));
      border: 1px solid rgba(245,158,11,0.2);
      border-radius: 14px; padding: 16px 18px;
    }
    .dv-mb-icon  { font-size: 32px; flex-shrink: 0; }
    .dv-mb-title { font-size: 13px; font-weight: 700; color: #fbbf24; margin-bottom: 3px; }
    .dv-mb-hint  { font-size: 12px; color: var(--if-text-secondary, #999); line-height: 1.5; }

    .dv-actions { display: flex; gap: 10px; flex-wrap: wrap; }
    .btn-new-doc {
      flex: 1; padding: 11px 18px; border-radius: 10px;
      background: rgba(124,58,237,0.12); border: 1px solid rgba(124,58,237,0.3);
      color: #c4b5fd; font-size: 13px; font-weight: 700; cursor: pointer;
    }
  `]
})
export class EvidenceCaptureComponent implements OnInit, OnDestroy {
  private readonly evidenceService = inject(EvidenceService);
  private readonly familyState     = inject(FamilyStateService);
  private readonly http             = inject(HttpClient);
  private readonly api              = inject(ApiService);

  @Input() preselectedTaskId: number | null = null;

  readonly emotions = EMOTIONS;

  // ── Tab principal ────────────────────────────────────────────────────
  readonly mainTab       = signal<MainTab>('capture');
  readonly documentals   = signal<SavedDocumental[]>([]);
  readonly loadingDocs   = signal(false);
  readonly viewingDoc    = signal<SavedDocumental | null>(null);

  switchTab(tab: MainTab): void {
    this.mainTab.set(tab);
    if (tab === 'gallery' && this.documentals().length === 0) {
      this.loadDocumentals();
    }
  }

  private loadDocumentals(): void {
    const fid = this.familyState.getSelectedFamilyId();
    if (!fid) return;
    this.loadingDocs.set(true);
    this.http.get<any>(`${this.api.base}/evidences/family/${fid}`).pipe(
      catchError(() => of(null))
    ).subscribe(res => {
      this.loadingDocs.set(false);
      const all: any[] = res?.data ?? [];
      this.documentals.set(this.groupIntoDocumentals(all));
    });
  }

  private groupIntoDocumentals(evidences: any[]): SavedDocumental[] {
    // La evidencia BITACORA maestra tiene título: "🎬 Mini Documental: {title}"
    const masters = evidences.filter(e =>
      e.evidenceType === 'BITACORA' && e.title?.startsWith('🎬 Mini Documental:')
    );

    return masters.map(master => {
      // Extraer título del documental
      const docTitle = master.title.replace('🎬 Mini Documental:', '').trim();

      // Ítems asociados: los que tienen "[Documental] {docTitle} —" en el título
      const prefix = `[Documental] ${docTitle} — `;
      const items: SavedEvidenceItem[] = evidences
        .filter(e => e.title?.startsWith(prefix))
        .map(e => ({
          id: e.id,
          type: e.evidenceType,
          label: e.title.replace(prefix, ''),
          mediaData: e.mediaData,
          mediaMime: e.mediaMime,
          textContent: e.textContent,
          latitude: e.latitude,
          longitude: e.longitude,
          emotion: e.emotion,
          createdAt: e.createdAt,
        }));

      // Portada: primera foto
      const firstPhoto = items.find(i => i.type === 'PHOTO' && i.mediaData);

      // Reflexiones: parsear el textContent del master
      const reflections = this.parseReflections(master.description ?? master.textContent ?? '');

      // Narrativa IA: si el master fue validado, description es la narrativa
      const aiNarrative = (master.status === 'VALIDATED' && master.description
        && !master.description.startsWith('DOCUMENTAL:'))
        ? master.description : null;

      return {
        id: String(master.id),
        title: docTitle,
        missionTitle: this.extractMissionTitle(master.description ?? ''),
        date: master.createdAt,
        coverMime: firstPhoto?.mediaMime ?? null,
        coverData: firstPhoto?.mediaData ?? null,
        aiNarrative,
        items,
        reflections,
      } as SavedDocumental;
    }).sort((a, b) => new Date(b.date).getTime() - new Date(a.date).getTime());
  }

  private extractMissionTitle(text: string): string {
    const match = text.match(/MISIÓN:\s*(.+)/);
    return match ? match[1].trim() : 'Misión familiar';
  }

  private parseReflections(text: string): { q: string; a: string }[] {
    const sections = [
      { marker: '¿Qué hicimos?',       q: '¿Qué hicimos?' },
      { marker: '¿Quién participó?',    q: '¿Quién participó?' },
      { marker: '¿Cómo nos sentimos?',  q: '¿Cómo nos sentimos?' },
      { marker: '¿Qué aprendimos?',     q: '¿Qué aprendimos?' },
      { marker: '¿Qué mejoraríamos?',   q: '¿Qué mejoraríamos?' },
    ];
    const result: { q: string; a: string }[] = [];
    for (const s of sections) {
      const idx = text.indexOf(s.marker);
      if (idx === -1) continue;
      const start = idx + s.marker.length;
      const nextIdx = sections
        .map(x => text.indexOf(x.marker, start))
        .filter(i => i > start)
        .reduce((min, i) => (i < min ? i : min), text.length);
      const answer = text.slice(start, nextIdx).replace(/^\n+/, '').trim();
      if (answer) result.push({ q: s.q, a: answer });
    }
    return result;
  }

  openDoc(doc: SavedDocumental): void {
    this.viewingDoc.set(doc);
    window.scrollTo({ top: 0, behavior: 'smooth' });
  }

  submitterFromDoc(doc: SavedDocumental): string {
    return doc.items[0] ? '' : 'La familia';
  }

  cleanLabel(label: string): string {
    return label.replace(/^\[Documental\].*?— /, '');
  }

  emotionEmoji(key: string): string {
    return EMOTIONS.find(e => e.key === key)?.emoji ?? '💭';
  }

  formatDocDate(iso: string): string {
    try {
      return new Date(iso).toLocaleDateString('es-CO', { day: '2-digit', month: 'long', year: 'numeric' });
    } catch { return iso; }
  }

  goToDocumentary(): void {
    window.location.href = '/evidence/documentary';
  }

  // Estado de flujo
  readonly mode           = signal<CaptureMode>('menu');
  readonly submitted      = signal(false);
  readonly submitting     = signal(false);
  readonly submitError    = signal<string | null>(null);
  readonly aiResponsePreview = signal<string>('');
  readonly pollingActive = signal(false);
  readonly validatedEvidence = signal<any | null>(null);
  private pollingTimer: any = null;
  private mapInstance: any = null;

  // Foto
  readonly photoPreview   = signal<string | null>(null);
  photoBase64: string | null = null;
  photoMime: string = 'image/jpeg';

  // Audio
  readonly recording      = signal(false);
  readonly recordSeconds  = signal(0);
  readonly audioBlob      = signal<Blob | null>(null);
  readonly audioUrl       = signal<string | null>(null);
  readonly audioError     = signal<string | null>(null);
  private mediaRecorder: MediaRecorder | null = null;
  private audioChunks: Blob[] = [];
  private recordTimer: any;

  // Ubicación
  readonly latitude       = signal<number | null>(null);
  readonly longitude      = signal<number | null>(null);
  readonly locating       = signal(false);
  readonly locationError  = signal<string | null>(null);

  // Form
  readonly selectedEmotion = signal<string | null>(null);
  selectedTaskId: number | null = null;
  memberName  = '';
  title       = '';
  textContent = '';

  // Tareas disponibles
  readonly tasks = signal<{ id: number; title: string }[]>([]);

  ngOnInit(): void {
    this.selectedTaskId = this.preselectedTaskId;
    this.loadTasks();
  }

  ngOnDestroy(): void {
    this.stopRecording();
    clearInterval(this.recordTimer);
    if (this.pollingTimer) clearInterval(this.pollingTimer);
    if (this.audioUrl()) URL.revokeObjectURL(this.audioUrl()!);
    if (this.mapInstance) {
      try { this.mapInstance.remove(); } catch {}
    }
  }

  goToPreview(): void {
    if (!this.title.trim()) {
      if (this.selectedTaskId) {
        const selectedTask = this.tasks().find(t => t.id === this.selectedTaskId);
        this.title = selectedTask ? selectedTask.title : 'Momento familiar';
      } else {
        this.title = this.photoBase64 ? 'Foto familiar'
                   : this.audioBlob() ? 'Audio familiar'
                   : this.latitude()  ? 'Ubicación familiar'
                   : 'Reflexión familiar';
      }
    }
    this.mode.set('preview');
  }

  private loadTasks(): void {
    const fid = this.familyState.getSelectedFamilyId();
    if (!fid) return;
    // Carga tareas activas desde la API del backend
    this.http.get<any>(`${this.api.base}/plans/family/${fid}`).subscribe({
      next: (res) => {
        const plans = res?.data ?? res;
        if (plans && plans.length > 0) {
          const latestPlan = plans[plans.length - 1];
          const activeTasks = (latestPlan.tasks || [])
            .map((t: any) => ({ id: t.id, title: t.title }));
          this.tasks.set(activeTasks);
        }
      },
      error: (err) => console.error('Error al cargar tareas en Cápsula Familiar:', err)
    });
  }

  // ── Foto ─────────────────────────────────────────────────────────────────

  startPhoto(): void { this.mode.set('photo'); }

  onPhotoSelected(event: Event): void {
    const file = (event.target as HTMLInputElement).files?.[0];
    if (!file) return;
    this.photoMime = file.type || 'image/jpeg';
    const reader = new FileReader();
    reader.onload = (e) => {
      const dataUrl = e.target?.result as string;
      this.photoPreview.set(dataUrl);
      this.photoBase64 = dataUrl.split(',')[1]; // solo la parte base64
    };
    reader.readAsDataURL(file);
  }

  clearPhoto(): void {
    this.photoPreview.set(null);
    this.photoBase64 = null;
  }

  // ── Audio ─────────────────────────────────────────────────────────────────

  startAudio(): void {
    this.mode.set('audio');
    this.audioError.set(null);
  }

  async startRecording(): Promise<void> {
    try {
      const stream = await navigator.mediaDevices.getUserMedia({ audio: true });
      this.audioChunks = [];
      
      let mimeType = 'audio/webm';
      if (typeof MediaRecorder !== 'undefined') {
        if (!MediaRecorder.isTypeSupported(mimeType)) {
          mimeType = 'audio/ogg';
          if (!MediaRecorder.isTypeSupported(mimeType)) {
            mimeType = 'audio/mp4';
            if (!MediaRecorder.isTypeSupported(mimeType)) {
              mimeType = ''; // Browser default
            }
          }
        }
      }

      const options = mimeType ? { mimeType } : {};
      this.mediaRecorder = new MediaRecorder(stream, options);
      
      this.mediaRecorder.ondataavailable = (e) => {
        if (e.data && e.data.size > 0) {
          this.audioChunks.push(e.data);
        }
      };

      this.mediaRecorder.onstop = () => {
        const finalMime = this.mediaRecorder?.mimeType || 'audio/webm';
        const blob = new Blob(this.audioChunks, { type: finalMime });
        this.audioBlob.set(blob);
        this.audioUrl.set(URL.createObjectURL(blob));
        stream.getTracks().forEach(t => t.stop());
      };

      this.mediaRecorder.start();
      this.recording.set(true);
      this.recordSeconds.set(0);
      this.recordTimer = setInterval(() => {
        this.recordSeconds.update(s => s + 1);
        if (this.recordSeconds() >= 90) this.stopRecording();
      }, 1000);
    } catch (err) {
      console.error('Error starting audio recording:', err);
      this.audioError.set('No se pudo acceder al micrófono. Verifica los permisos del navegador.');
    }
  }

  stopRecording(): void {
    clearInterval(this.recordTimer);
    if (this.mediaRecorder && this.recording()) {
      this.mediaRecorder.stop();
      this.recording.set(false);
    }
  }

  clearAudio(): void {
    if (this.audioUrl()) URL.revokeObjectURL(this.audioUrl()!);
    this.audioBlob.set(null);
    this.audioUrl.set(null);
    this.recordSeconds.set(0);
  }

  formatSeconds(s: number): string {
    const m = Math.floor(s / 60);
    const sec = s % 60;
    return `${m}:${sec.toString().padStart(2, '0')}`;
  }

  // ── Ubicación ─────────────────────────────────────────────────────────────

  captureLocation(): void {
    this.mode.set('location');
    this.locationError.set(null);
    if (!navigator.geolocation) {
      this.locationError.set('Tu navegador no soporta geolocalización.');
      return;
    }
    this.locating.set(true);
    navigator.geolocation.getCurrentPosition(
      (pos) => {
        this.latitude.set(pos.coords.latitude);
        this.longitude.set(pos.coords.longitude);
        this.locating.set(false);
      },
      () => {
        this.locationError.set('No se pudo obtener la ubicación. Verifica los permisos.');
        this.locating.set(false);
      }
    );
  }

  // ── Navegación ────────────────────────────────────────────────────────────

  goBack(): void {
    if (this.photoBase64) { this.mode.set('photo'); return; }
    if (this.audioBlob()) { this.mode.set('audio'); return; }
    if (this.textContent) { this.mode.set('text'); return; }
    if (this.latitude())  { this.mode.set('location'); return; }
    this.mode.set('menu');
  }

  // ── Envío ─────────────────────────────────────────────────────────────────

  async submit(): Promise<void> {
    const familyId = this.familyState.getSelectedFamilyId();
    if (!familyId) { this.submitError.set('No hay familia seleccionada.'); return; }
    if (!this.title.trim()) { this.submitError.set('Escribe un título para este momento.'); return; }

    let taskId = this.selectedTaskId ?? this.preselectedTaskId;
    // Si es un momento espontáneo (taskId es null) y tenemos misiones cargadas, asociamos al primer task ID de forma transparente para cumplir con el esquema BD
    if (!taskId && this.tasks().length > 0) {
      taskId = this.tasks()[0].id;
    }
    if (!taskId) { this.submitError.set('Selecciona una misión o elige "Momento espontáneo".'); return; }

    this.submitting.set(true);
    this.submitError.set(null);

    const type = this.photoBase64 ? 'PHOTO'
               : this.audioBlob() ? 'AUDIO'
               : this.latitude()  ? 'DOCUMENT'
               : 'SELF_REFLECTION';

    // Convierte audio a base64
    let audioBase64: string | null = null;
    if (this.audioBlob()) {
      audioBase64 = await this.blobToBase64(this.audioBlob()!);
    }

    const req: SubmitEvidenceRequest = {
      taskId,
      familyId,
      evidenceType: type as any,
      title: this.title.trim(),
      description: this.textContent || undefined,
      textContent: this.textContent || undefined,
      submittedBy: this.memberName || 'Familia',
      memberName: this.memberName || undefined,
      emotion: this.selectedEmotion() ?? undefined,
      latitude: this.latitude() ?? undefined,
      longitude: this.longitude() ?? undefined,
      mediaData: this.photoBase64 ?? audioBase64 ?? undefined,
      mediaMime: this.photoBase64 ? this.photoMime
               : this.audioBlob() ? 'audio/webm'
               : undefined,
    };

    this.evidenceService.submit(req).pipe(
      catchError(() => {
        this.submitError.set('Error al enviar. Verifica tu conexión e inténtalo de nuevo.');
        this.submitting.set(false);
        return of(null);
      })
    ).subscribe(res => {
      if (res && res.data) {
        this.submitted.set(true);
        this.pollingActive.set(true);
        this.startPolling(res.data.id);
      }
      this.submitting.set(false);
    });
  }

  private startPolling(evidenceId: number): void {
    const fid = this.familyState.getSelectedFamilyId();
    if (!fid) return;

    if (this.pollingTimer) clearInterval(this.pollingTimer);

    let attempts = 0;
    const MAX_ATTEMPTS = 15; // 45 segundos máximo (15 × 3s)

    this.pollingTimer = setInterval(() => {
      attempts++;

      this.http.get<any>(`${this.api.base}/evidences/family/${fid}`).subscribe({
        next: (res) => {
          if (res && res.data) {
            const matching = res.data.find((e: any) => e.id === evidenceId);

            // Éxito: evidencia validada por IA
            if (matching && matching.status === 'VALIDATED') {
              clearInterval(this.pollingTimer);
              this.pollingActive.set(false);
              this.validatedEvidence.set(matching);
              if (matching.latitude && matching.longitude) {
                this.initMap(matching.latitude, matching.longitude);
              }
              return;
            }

            // Timeout: mostrar resultado aunque la IA no haya terminado
            if (attempts >= MAX_ATTEMPTS) {
              clearInterval(this.pollingTimer);
              this.pollingActive.set(false);
              // Mostrar la evidencia con lo que hay (sin narrativa IA aún)
              this.validatedEvidence.set(matching ?? {
                id: evidenceId,
                title: this.title,
                submittedBy: this.memberName || 'Familia',
                emotion: this.selectedEmotion(),
                description: 'La IA está procesando tu cápsula en segundo plano. Podrás ver el análisis completo en Evidencias.',
                mediaData: this.photoBase64,
                mediaMime: this.photoBase64 ? this.photoMime : null,
                latitude: this.latitude(),
                longitude: this.longitude(),
                textContent: this.textContent,
              });
            }
          }
        },
        error: () => {
          if (attempts >= MAX_ATTEMPTS) {
            clearInterval(this.pollingTimer);
            this.pollingActive.set(false);
            this.validatedEvidence.set({
              id: evidenceId,
              title: this.title,
              submittedBy: this.memberName || 'Familia',
              emotion: this.selectedEmotion(),
              description: 'Cápsula guardada. La IA finalizará el análisis en breve.',
              mediaData: this.photoBase64,
              mediaMime: this.photoBase64 ? this.photoMime : null,
              latitude: this.latitude(),
              longitude: this.longitude(),
              textContent: this.textContent,
            });
          }
        }
      });
    }, 3000);
  }

  private initMap(lat: number, lng: number): void {
    if (!document.getElementById('leaflet-css')) {
      const link = document.createElement('link');
      link.id = 'leaflet-css';
      link.rel = 'stylesheet';
      link.href = 'https://unpkg.com/leaflet@1.9.4/dist/leaflet.css';
      document.head.appendChild(link);
    }

    if (typeof (window as any).L === 'undefined') {
      const script = document.createElement('script');
      script.src = 'https://unpkg.com/leaflet@1.9.4/dist/leaflet.js';
      script.onload = () => this.setupLeafletMap(lat, lng);
      document.head.appendChild(script);
    } else {
      setTimeout(() => this.setupLeafletMap(lat, lng), 100);
    }
  }

  private setupLeafletMap(lat: number, lng: number): void {
    if (!this.submitted() || this.pollingActive()) return;

    if (this.mapInstance) {
      try {
        this.mapInstance.remove();
      } catch {}
      this.mapInstance = null;
    }

    setTimeout(() => {
      const container = document.getElementById('contemplation-map-capture');
      if (!container) return;

      try {
        const L = (window as any).L;
        this.mapInstance = L.map('contemplation-map-capture', {
          zoomControl: false,
          attributionControl: false
        }).setView([lat, lng], 13);

        L.tileLayer('https://{s}.basemaps.cartocdn.com/dark_all/{z}/{x}/{y}{r}.png', {
          maxZoom: 20
        }).addTo(this.mapInstance);

        const customIcon = L.divIcon({
          className: 'leaflet-custom-marker',
          html: `<div style="background-color: ${this.getEmotionDetails(this.validatedEvidence()?.emotion).color}; width: 14px; height: 14px; border-radius: 50%; border: 3px solid #fff; box-shadow: 0 0 10px rgba(0,0,0,0.5);"></div>`,
          iconSize: [20, 20],
          iconAnchor: [10, 10]
        });

        L.marker([lat, lng], { icon: customIcon }).addTo(this.mapInstance);
      } catch (e) {
        console.error('Error setting up map in capture screen:', e);
      }
    }, 150);
  }

  getEmotionDetails(emotionKey: string | null | undefined) {
    const emotionsMap: { [key: string]: { label: string; emoji: string; color: string; glow: string } } = {
      alegria:   { label: 'Alegría',     emoji: '😊', color: '#ffb300', glow: 'rgba(255, 179, 0, 0.2)' },
      gratitud:  { label: 'Gratitud',    emoji: '🙏', color: '#a78bfa', glow: 'rgba(167, 139, 250, 0.2)' },
      amor:      { label: 'Amor',        emoji: '❤️', color: '#fb7185', glow: 'rgba(251, 113, 133, 0.2)' },
      orgullo:   { label: 'Orgullo',     emoji: '🌟', color: '#fbbf24', glow: 'rgba(251, 191, 36, 0.2)' },
      calma:     { label: 'Calma',       emoji: '😌', color: '#2dd4bf', glow: 'rgba(45, 212, 191, 0.2)' },
      esperanza: { label: 'Esperanza',   emoji: '🌱', color: '#34d399', glow: 'rgba(52, 211, 153, 0.2)' },
      tristeza:  { label: 'Tristeza',    emoji: '😢', color: '#60a5fa', glow: 'rgba(96, 165, 250, 0.2)' },
      tension:   { label: 'Tensión',     emoji: '😤', color: '#f87171', glow: 'rgba(248, 113, 113, 0.2)' },
    };
    return emotionsMap[emotionKey || ''] || { label: 'Reflexión', emoji: '📸', color: '#58a6ff', glow: 'rgba(88, 166, 255, 0.15)' };
  }

  private blobToBase64(blob: Blob): Promise<string> {
    return new Promise((resolve, reject) => {
      const reader = new FileReader();
      reader.onload  = () => resolve((reader.result as string).split(',')[1]);
      reader.onerror = reject;
      reader.readAsDataURL(blob);
    });
  }

  reset(): void {
    this.mode.set('menu');
    this.submitted.set(false);
    this.pollingActive.set(false);
    this.validatedEvidence.set(null);
    if (this.pollingTimer) clearInterval(this.pollingTimer);
    if (this.mapInstance) {
      try { this.mapInstance.remove(); } catch {}
      this.mapInstance = null;
    }
    this.photoPreview.set(null);
    this.photoBase64 = null;
    this.clearAudio();
    this.latitude.set(null);
    this.longitude.set(null);
    this.textContent = '';
    this.title = '';
    this.memberName = '';
    this.selectedEmotion.set(null);
    this.submitError.set(null);
  }

  onAudioLoaded(event: Event): void {
    const audio = event.target as HTMLAudioElement;
    if (audio) {
      if (audio.duration === Infinity || isNaN(audio.duration) || audio.duration === 0) {
        audio.currentTime = 1e101;
        const onTimeUpdate = () => {
          audio.currentTime = 0;
          audio.removeEventListener('timeupdate', onTimeUpdate);
        };
        audio.addEventListener('timeupdate', onTimeUpdate);
      }
    }
  }
}
