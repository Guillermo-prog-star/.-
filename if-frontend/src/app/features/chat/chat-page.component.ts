import { Component, OnInit, inject, ElementRef, ViewChild } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { HttpClient } from '@angular/common/http';
import { FamilyStateService } from '../../core/services/family-state.service';
import { MarkdownPipe } from '../../shared/pipes/markdown.pipe';
import { SessionContext } from '../../core/models/models';

@Component({
  selector: 'app-chat-page',
  standalone: true,
  imports: [CommonModule, FormsModule, MarkdownPipe],
  templateUrl: './chat-page.component.html',
  styleUrls: ['./chat-page.component.css']
})
export class ChatPageComponent implements OnInit {
  @ViewChild('anchor') anchor!: ElementRef;
  private http = inject(HttpClient);
  private familyState = inject(FamilyStateService);

  messages: any[] = [];
  inputText = '';
  loading = false;
  recording = false;
  micError = '';
  sessionContext: SessionContext | null = null;
  private mediaRecorder: any;
  private audioChunks: any[] = [];

  get familyId()   { return this.familyState.currentFamilyId(); }
  get familyName() { return this.familyState.currentFamilyName() || 'Familia'; }
  get memberId()   { return this.familyState.currentMemberId(); }

  ngOnInit() {
    this.loadHistory();
    this.loadSessionContext();
  }

  loadHistory() {
    this.http.get<any>(`/api/chat/family/${this.familyId}`).subscribe({
      next: (res) => {
        // FIX Bug #16: ChatMessage/ChatMessageSummary serialize boolean field as "ai" (not "isAi")
        // Jackson strips 'is' prefix from boolean getters: isAi() → "ai" in JSON
        this.messages = (res.data || []).map((m: any) => ({ ...m, isAi: m.ai ?? m.isAi ?? false }));
        if (this.messages.length === 0) {
          this.messages.push({
            content: `Hola familia ${this.familyName}. Soy su Mentor de Integridad Proactiva. Estoy analizando su hito actual para guiarlos. ¿Tienen alguna duda sobre sus misiones o el diagnóstico?`,
            isAi: true,
            createdAt: new Date()
          });
        }
        this.scroll();
      }
    });
  }

  loadSessionContext() {
    if (!this.familyId || !this.memberId) return;
    this.http.get<any>(`/api/chat/session/active?familyId=${this.familyId}&memberId=${this.memberId}`)
      .subscribe({ next: (res) => { this.sessionContext = res?.data ?? null; } });
  }

  goalLabel(goal: string): string {
    const map: Record<string, string> = {
      GENERAL: 'General', SUPPORT: 'Acompañamiento',
      PLANNING: 'Planificación', CRISIS_CONTAINMENT: 'Contención', REFLECTION: 'Reflexión'
    };
    return map[goal] ?? goal;
  }

  arcLabel(arc: string): string {
    const map: Record<string, string> = {
      STABLE: 'Estable', MILD_TENSION: 'Leve Tensión',
      ESCALATING: 'En Alza', ESCALATED: 'Tensión Alta', DE_ESCALATING: 'Calmándose'
    };
    return map[arc] ?? arc;
  }

  arcColor(arc: string): string {
    const map: Record<string, string> = {
      STABLE: 'text-teal-400 border-teal-500/40',
      MILD_TENSION: 'text-yellow-400 border-yellow-500/40',
      ESCALATING: 'text-orange-400 border-orange-500/40',
      ESCALATED: 'text-red-400 border-red-500/40',
      DE_ESCALATING: 'text-cyan-400 border-cyan-500/40'
    };
    return map[arc] ?? 'text-white/40 border-white/20';
  }

  send() {
    const text = this.inputText.trim();
    if (!text || this.loading) return;

    // UI Optimista
    this.messages.push({ content: text, isAi: false, createdAt: new Date() });
    this.inputText = '';
    this.loading = true;
    this.scroll();

    this.http.post<any>(`/api/chat/send`, { familyId: this.familyId, message: text, memberId: this.memberId })
      .subscribe({
        next: (res: any) => {
          const msg = res.data;
          this.messages.push({ ...msg, isAi: msg?.ai ?? msg?.isAi ?? false });
          this.loading = false;
          this.scroll();
          this.loadSessionContext();
        },
        error: () => {
          this.messages.push({ content: 'Disculpen, hay una interferencia en la red neuronal. Inténtenlo de nuevo en un momento.', isAi: true, createdAt: new Date() });
          this.loading = false;
          this.scroll();
        }
      });
  }

  toggleRecording() {
    if (this.recording) {
      this.stopRecording();
    } else {
      this.startRecording();
    }
  }

  private startRecording() {
    navigator.mediaDevices.getUserMedia({ audio: true }).then(stream => {
      this.mediaRecorder = new MediaRecorder(stream);
      this.audioChunks = [];
      this.mediaRecorder.ondataavailable = (event: any) => this.audioChunks.push(event.data);
      this.mediaRecorder.onstop = () => {
        const audioBlob = new Blob(this.audioChunks, { type: 'audio/mpeg' });
        this.sendVoice(audioBlob);
      };
      this.mediaRecorder.start();
      this.recording = true;
    }).catch(err => {
      console.error("Mic access denied", err);
      this.micError = "Se requiere permiso de micrófono para esta función.";
      setTimeout(() => { this.micError = ''; }, 4000);
    });
  }

  private stopRecording() {
    if (this.mediaRecorder) {
      this.mediaRecorder.stop();
      this.recording = false;
      this.loading = true; // Empieza a procesar
    }
  }

  private sendVoice(blob: Blob) {
    const formData = new FormData();
    formData.append('audio', blob, 'voice_message.mp3');
    if (this.memberId != null) formData.append('memberId', String(this.memberId));

    this.http.post<any>(`/api/chat/voice/${this.familyId}`, formData).subscribe({
      next: (res) => {
        // FIX Bug #15: VoiceController returns SonicResponse (transcript/assistantReply),
        // NOT VoiceChatResponse (transcription/aiResponseText). Use the correct field names.
        this.messages.push({ content: res.transcript, isAi: false, createdAt: new Date() });
        this.messages.push({ content: res.assistantReply, isAi: true, createdAt: new Date() });
        this.loading = false;
        this.scroll();
        this.loadSessionContext();
        if (res.audioBase64) {
          this.playAudio(res.audioBase64);
        }
      },
      error: () => {
        this.loading = false;
        this.messages.push({ content: 'Error al procesar el audio.', isAi: true, createdAt: new Date() });
      }
    });
  }

  private playAudio(base64: String) {
    const audio = new Audio('data:audio/mpeg;base64,' + base64);
    audio.play().catch(err => console.error("Error playing audio", err));
  }

  scroll() {
    setTimeout(() => {
      const chatContainer = document.querySelector('.chat-messages');
      if (chatContainer) {
        chatContainer.scrollTop = chatContainer.scrollHeight;
      }
    }, 100);
  }
}
