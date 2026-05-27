import { Component, OnInit, inject, ElementRef, ViewChild } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { HttpClient } from '@angular/common/http';
import { FamilyStateService } from '../../core/services/family-state.service';

@Component({
  selector: 'app-chat-page',
  standalone: true,
  imports: [CommonModule, FormsModule],
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
  private mediaRecorder: any;
  private audioChunks: any[] = [];

  get familyId()   { return this.familyState.currentFamilyId(); }
  get familyName() { return this.familyState.currentFamilyName() || 'Familia'; }

  ngOnInit() {
    this.loadHistory();
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

  send() {
    const text = this.inputText.trim();
    if (!text || this.loading) return;

    // UI Optimista
    this.messages.push({ content: text, isAi: false, createdAt: new Date() });
    this.inputText = '';
    this.loading = true;
    this.scroll();

    this.http.post<any>(`/api/chat/send`, { familyId: this.familyId, message: text })
      .subscribe({
        next: (res: any) => {
          // FIX Bug #16: ChatMessage entity serializes boolean 'ai' field, not 'isAi'
          const msg = res.data;
          this.messages.push({ ...msg, isAi: msg?.ai ?? msg?.isAi ?? false });
          this.loading = false;
          this.scroll();
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

    this.http.post<any>(`/api/chat/voice/${this.familyId}`, formData).subscribe({
      next: (res) => {
        // FIX Bug #15: VoiceController returns SonicResponse (transcript/assistantReply),
        // NOT VoiceChatResponse (transcription/aiResponseText). Use the correct field names.
        this.messages.push({ content: res.transcript, isAi: false, createdAt: new Date() });
        this.messages.push({ content: res.assistantReply, isAi: true, createdAt: new Date() });

        this.loading = false;
        this.scroll();
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
