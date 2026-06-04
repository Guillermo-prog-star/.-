import { Component, OnInit, OnDestroy, inject, signal, Input } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { HttpClient } from '@angular/common/http';
import { EvidenceService, SubmitEvidenceRequest } from '../../core/services/evidence.service';
import { FamilyStateService } from '../../core/services/family-state.service';
import { ApiService } from '../../core/services/api.service';
import { catchError, of } from 'rxjs';

type CaptureMode = 'menu' | 'photo' | 'audio' | 'text' | 'location' | 'preview';
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
              <button class="btn-next" (click)="mode.set('preview')">Continuar →</button>
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
                <audio [src]="audioUrl()!" controls class="audio-player"></audio>
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
              <button class="btn-next" (click)="mode.set('preview')">Continuar →</button>
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
              <button class="btn-next" (click)="mode.set('preview')">Continuar →</button>
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
              <button class="btn-next" (click)="mode.set('preview')">Continuar →</button>
            }
          </div>
        </div>
      }

      <!-- ── Vista previa + emoción + envío ─────────────── -->
      @if (mode() === 'preview') {
        <div class="ec-preview">

          <div class="cap-title">¿Cómo se sintieron?</div>

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

          <!-- Quién capturó -->
          <div class="field-group">
            <label class="field-label">¿Quién lo captura?</label>
            <input class="field-input" [(ngModel)]="memberName" placeholder="Tu nombre" maxlength="80" />
          </div>

          <!-- Título -->
          <div class="field-group">
            <label class="field-label">Título del momento</label>
            <input class="field-input" [(ngModel)]="title" placeholder="Ej: Cena sin celulares" maxlength="120" />
          </div>

          <!-- Vista previa del contenido capturado -->
          @if (photoPreview()) {
            <img [src]="photoPreview()!" class="prev-photo" alt="Foto capturada" />
          }
          @if (audioUrl()) {
            <audio [src]="audioUrl()!" controls class="prev-audio"></audio>
          }
          @if (textContent) {
            <div class="prev-text">{{ textContent }}</div>
          }
          @if (latitude()) {
            <div class="prev-location">📍 {{ latitude()!.toFixed(4) }}, {{ longitude()!.toFixed(4) }}</div>
          }

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
        <div class="ec-success">
          <div class="suc-icon">🎉</div>
          <h2 class="suc-title">¡Cápsula enviada!</h2>
          <p class="suc-text">La IA está analizando este momento. En unos instantes recibirán una respuesta.</p>
          <div class="suc-meta">{{ aiResponsePreview() }}</div>
          <button class="btn-new" (click)="reset()">+ Nueva cápsula</button>
        </div>
      }

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
    .prev-photo  { max-width: 100%; border-radius: 12px; object-fit: cover; }
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
    .ec-success { text-align: center; padding: 40px 20px; }
    .suc-icon   { font-size: 56px; margin-bottom: 16px; }
    .suc-title  { font-size: 22px; font-weight: 800; margin: 0 0 10px; }
    .suc-text   { font-size: 14px; color: var(--if-text-secondary, #aaa); margin: 0 0 20px; line-height: 1.6; }
    .suc-meta   {
      background: rgba(139,92,246,0.08); border: 1px solid rgba(139,92,246,0.2);
      border-radius: 12px; padding: 16px; font-size: 13px; color: #c4b5fd;
      margin-bottom: 24px; line-height: 1.6; text-align: left;
    }
    .btn-new {
      background: linear-gradient(135deg, #7c3aed, #4f46e5);
      border: none; color: white; padding: 12px 28px;
      border-radius: 10px; font-size: 14px; font-weight: 700; cursor: pointer;
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

  // Estado de flujo
  readonly mode           = signal<CaptureMode>('menu');
  readonly submitted      = signal(false);
  readonly submitting     = signal(false);
  readonly submitError    = signal<string | null>(null);
  readonly aiResponsePreview = signal<string>('');

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
    if (this.audioUrl()) URL.revokeObjectURL(this.audioUrl()!);
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
      this.mediaRecorder = new MediaRecorder(stream);
      this.mediaRecorder.ondataavailable = (e) => this.audioChunks.push(e.data);
      this.mediaRecorder.onstop = () => {
        const blob = new Blob(this.audioChunks, { type: 'audio/webm' });
        this.audioBlob.set(blob);
        this.audioUrl.set(URL.createObjectURL(blob));
        stream.getTracks().forEach(t => t.stop());
      };
      this.mediaRecorder.start(100);
      this.recording.set(true);
      this.recordSeconds.set(0);
      this.recordTimer = setInterval(() => {
        this.recordSeconds.update(s => s + 1);
        if (this.recordSeconds() >= 90) this.stopRecording();
      }, 1000);
    } catch {
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
      if (res) {
        this.submitted.set(true);
        this.aiResponsePreview.set('La IA analizará este momento en los próximos segundos y lo valorará dentro de tu plan de transformación familiar.');
      }
      this.submitting.set(false);
    });
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
}
