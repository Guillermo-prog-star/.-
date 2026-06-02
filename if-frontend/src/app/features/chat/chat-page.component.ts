import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { HttpClient } from '@angular/common/http';
import { FamilyStateService } from '../../core/services/family-state.service';
import { TransformationFlowService } from '../../core/services/transformation-flow.service';
import { MarkdownPipe } from '../../shared/pipes/markdown.pipe';
import { SessionContext } from '../../core/models/models';
import { ScrollPolicyDirective } from '../../shared/directives/scroll-policy.directive';

const SpeechRecognition = (window as any).SpeechRecognition || (window as any).webkitSpeechRecognition;

@Component({
  selector: 'app-chat-page',
  standalone: true,
  imports: [CommonModule, FormsModule, MarkdownPipe, ScrollPolicyDirective],
  templateUrl: './chat-page.component.html',
  styleUrls: ['./chat-page.component.css']
})
export class ChatPageComponent implements OnInit {
  private http        = inject(HttpClient);
  private familyState = inject(FamilyStateService);
  private flow        = inject(TransformationFlowService);

  messages: any[] = [];
  inputText = '';
  loading = false;
  recording = false;
  micError = '';
  sessionContext: SessionContext | null = null;
  private mediaRecorder: any;
  private audioChunks: any[] = [];
  private recognition: any;
  private clientTranscript = '';
  private audioBlobReady = false;
  private recognitionReady = false;
  private recordedBlob: Blob | null = null;

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

    // Construir contexto de transformación para enriquecer la respuesta del IA
    const transformationContext = {
      currentPillar:       this.flow.currentPillar(),
      currentMonth:        this.flow.currentMonth(),
      milestoneLabel:      this.flow.milestoneLabel(),
      currentPhase:        this.flow.currentPhaseLabel(),
      sprintNumber:        this.flow.currentSprintNumber(),
      activeMissionId:     this.flow.activeMissionId(),
      progressPercent:     this.flow.progressPercent(),
      onboardingCompleted: this.flow.isOnboardingDone(),
    };

    this.http.post<any>(`/api/chat/send`, {
      familyId: this.familyId,
      message:  text,
      memberId: this.memberId,
      transformationContext,
    })
      .subscribe({
        next: (res: any) => {
          const msg = res.data;
          this.messages.push({ ...msg, isAi: msg?.ai ?? msg?.isAi ?? false });
          this.loading = false;
  
          this.loadSessionContext();
        },
        error: () => {
          this.messages.push({ content: 'Disculpen, hay una interferencia en la red neuronal. Inténtenlo de nuevo en un momento.', isAi: true, createdAt: new Date() });
          this.loading = false;
  
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
        this.recordedBlob = audioBlob;
        this.audioBlobReady = true;
        this.maybeSendVoice();
      };
      
      this.mediaRecorder.start();
      this.recording = true;

      // Start client-side speech recognition if supported
      this.clientTranscript = '';
      this.audioBlobReady = false;
      this.recognitionReady = !SpeechRecognition;
      this.recordedBlob = null;

      if (SpeechRecognition) {
        try {
          this.recognition = new SpeechRecognition();
          this.recognition.lang = 'es-ES';
          this.recognition.continuous = true;
          this.recognition.interimResults = false;
          
          this.recognition.onresult = (event: any) => {
            let chunkText = '';
            for (let i = event.resultIndex; i < event.results.length; ++i) {
              if (event.results[i].isFinal) {
                chunkText += event.results[i][0].transcript;
              }
            }
            if (chunkText) {
              this.clientTranscript += (this.clientTranscript ? ' ' : '') + chunkText;
            }
          };

          this.recognition.onerror = (event: any) => {
            console.warn("SpeechRecognition error:", event.error);
            this.recognitionReady = true;
            this.maybeSendVoice();
          };

          this.recognition.onend = () => {
            console.log("SpeechRecognition ended. Final transcript:", this.clientTranscript);
            this.recognitionReady = true;
            this.maybeSendVoice();
          };

          this.recognition.start();
        } catch (e) {
          console.error("SpeechRecognition startup failed", e);
          this.recognitionReady = true;
        }
      }
    }).catch(err => {
      console.error("Mic access denied", err);
      this.micError = "Se requiere permiso de micrófono para esta función.";
      setTimeout(() => { this.micError = ''; }, 4000);
    });
  }

  private stopRecording() {
    if (this.mediaRecorder) {
      this.mediaRecorder.stop();
      if (this.recognition) {
        try {
          this.recognition.stop();
        } catch (e) {
          console.warn("Failed to stop recognition:", e);
          this.recognitionReady = true;
          this.maybeSendVoice();
        }
      }
      this.recording = false;
      this.loading = true; // Empieza a procesar
    }
  }

  private maybeSendVoice() {
    if (this.audioBlobReady && this.recognitionReady) {
      if (this.recordedBlob) {
        this.sendVoice(this.recordedBlob);
      }
      this.audioBlobReady = false;
      this.recognitionReady = false;
      this.recordedBlob = null;
    }
  }

  private sendVoice(blob: Blob) {
    const formData = new FormData();
    formData.append('audio', blob, 'voice_message.mp3');
    if (this.memberId != null) formData.append('memberId', String(this.memberId));
    if (this.clientTranscript) {
      formData.append('clientTranscript', this.clientTranscript.trim());
    }

    // Append transformation context parameters to voice chat request
    const currentPillar = this.flow.currentPillar();
    if (currentPillar != null) formData.append('currentPillar', currentPillar);

    const currentMonth = this.flow.currentMonth();
    if (currentMonth != null) formData.append('currentMonth', String(currentMonth));

    const milestoneLabel = this.flow.milestoneLabel();
    if (milestoneLabel != null) formData.append('milestoneLabel', milestoneLabel);

    const currentPhase = this.flow.currentPhaseLabel();
    if (currentPhase != null) formData.append('currentPhase', currentPhase);

    const sprintNumber = this.flow.currentSprintNumber();
    if (sprintNumber != null) formData.append('sprintNumber', String(sprintNumber));

    const activeMissionId = this.flow.activeMissionId();
    if (activeMissionId != null) formData.append('activeMissionId', activeMissionId);

    const progressPercent = this.flow.progressPercent();
    if (progressPercent != null) formData.append('progressPercent', String(progressPercent));

    const onboardingCompleted = this.flow.isOnboardingDone();
    if (onboardingCompleted != null) formData.append('onboardingCompleted', String(onboardingCompleted));

    // UI Optimista
    const tempUserMsg = {
      content: this.clientTranscript ? this.clientTranscript.trim() : '🎤 Procesando audio...',
      isAi: false,
      createdAt: new Date()
    };
    this.messages.push(tempUserMsg);

    this.http.post<any>(`/api/chat/voice/${this.familyId}`, formData).subscribe({
      next: (res) => {
        // Actualizar la transcripción real y agregar la respuesta del Mentor
        tempUserMsg.content = res.transcript;
        this.messages.push({ content: res.assistantReply, isAi: true, createdAt: new Date() });
        this.loading = false;

        this.loadSessionContext();
        if (res.audioBase64) {
          this.playAudio(res.audioBase64);
        }
      },
      error: () => {
        this.loading = false;
        if (tempUserMsg.content === '🎤 Procesando audio...') {
          tempUserMsg.content = '🎤 Error en grabación de voz';
        }
        this.messages.push({ content: 'Error al procesar el audio.', isAi: true, createdAt: new Date() });

      }
    });
  }

  private playAudio(base64: String) {
    const audio = new Audio('data:audio/mpeg;base64,' + base64);
    audio.play().catch(err => console.error("Error playing audio", err));
  }

}
