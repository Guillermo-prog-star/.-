import { Component, OnInit, inject, ElementRef, ViewChild } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { HttpClient } from '@angular/common/http';
import { ApiService } from '../../core/services/api.service';
interface Msg { role: 'user' | 'ai'; text: string; time: string; }
@Component({
  selector: 'app-chat-page', standalone: true, imports: [CommonModule, FormsModule],
  styles: [`
    .messages{flex:1;overflow-y:auto;display:flex;flex-direction:column;gap:12px;padding:4px 0;}
    .bubble-user{align-self:flex-end;max-width:72%;background:#1A3A2A;color:#fff;padding:12px 16px;border-radius:18px 18px 4px 18px;font-size:14px;line-height:1.6;}
    .bubble-ai{align-self:flex-start;max-width:72%;background:#F5F4F0;border:1px solid var(--border);padding:12px 16px;border-radius:18px 18px 18px 4px;font-size:14px;line-height:1.6;}
    .dot{display:inline-block;width:8px;height:8px;background:var(--muted);border-radius:50%;margin:0 2px;animation:bop .9s infinite;}
    .dot:nth-child(2){animation-delay:.15s;}.dot:nth-child(3){animation-delay:.3s;}
    @keyframes bop{0%,100%{transform:translateY(0)}50%{transform:translateY(-5px)}}
  `],
  template: `
    <div class="page-header">
      <div><h1>Consultor IA</h1><p>Claude · {{ familyName }} · {{ milestone }}</p></div>
    </div>
    <div class="card" style="display:flex;flex-direction:column;height:calc(100vh - 200px);min-height:500px;">
      <div class="messages" #msgBox>
        @if (msgs.length === 0) {
          <div style="text-align:center;margin:auto;color:var(--muted);padding:32px;">
            <div style="font-size:36px;margin-bottom:12px;">◉</div>
            <p>Hola, soy el consultor de Integrity Family.</p>
            <p>Pregúntame sobre el estado familiar, los planes, el hito actual o cómo avanzar.</p>
          </div>
        }
        @for (m of msgs; track $index) {
          <div>
            @if (m.role === 'ai') {
              <div style="font-size:11px;color:var(--muted);font-weight:600;margin-bottom:4px;">◉ Consultor Integrity Family · {{ m.time }}</div>
            }
            <div [class]="m.role === 'user' ? 'bubble-user' : 'bubble-ai'">{{ m.text }}</div>
            @if (m.role === 'user') {
              <div style="font-size:11px;color:var(--muted);text-align:right;margin-top:4px;">{{ m.time }}</div>
            }
          </div>
        }
        @if (loading) {
          <div>
            <div style="font-size:11px;color:var(--muted);font-weight:600;margin-bottom:4px;">◉ Consultor</div>
            <div class="bubble-ai"><span class="dot"></span><span class="dot"></span><span class="dot"></span></div>
          </div>
        }
        <div #anchor></div>
      </div>
      <div style="border-top:1px solid var(--border);padding-top:14px;display:flex;gap:10px;margin-top:auto;">
        <input [(ngModel)]="input" name="msg" placeholder="Escribe tu consulta sobre el bienestar familiar..."
               style="flex:1;" (keyup.enter)="send()" [disabled]="loading"/>
        <button class="btn btn-primary" (click)="send()" [disabled]="loading || !input.trim()">Enviar</button>
      </div>
    </div>`
})
export class ChatPageComponent implements OnInit {
  @ViewChild('anchor') anchor!: ElementRef;
  private http = inject(HttpClient); private api = inject(ApiService);
  msgs: Msg[] = []; input = ''; loading = false;
  familyId   = Number(localStorage.getItem('selectedFamilyId') ?? 1);
  familyName = localStorage.getItem('selectedFamilyName') ?? 'Familia';
  milestone  = localStorage.getItem('currentMilestone') ?? 'Inicio';

  ngOnInit() {
    this.msgs.push({ role:'ai', time: this.now(),
      text: `Hola, soy tu consultor de Integrity Family. Acompaño a ${this.familyName} en su proceso de crecimiento con autonomía y responsabilidad compartida. ¿En qué puedo ayudarte hoy?` });
  }

  send() {
    const text = this.input.trim();
    if (!text || this.loading) return;
    this.msgs.push({ role:'user', text, time: this.now() });
    this.input = ''; this.loading = true;
    this.scroll();
    this.http.post<any>(`${this.api.base}/chat`, { familyId: this.familyId, message: text })
      .subscribe({
        next: ({ data }) => {
          this.msgs.push({ role:'ai', text: data.reply, time: this.now() });
          this.loading = false; this.scroll();
        },
        error: () => {
          this.msgs.push({ role:'ai', text:'Hubo un problema conectando con el consultor. Intenta de nuevo.', time: this.now() });
          this.loading = false; this.scroll();
        }
      });
  }

  private now() { return new Date().toLocaleTimeString('es-CO', { hour:'2-digit', minute:'2-digit' }); }
  private scroll() { setTimeout(() => this.anchor?.nativeElement?.scrollIntoView({ behavior:'smooth' }), 60); }
}
