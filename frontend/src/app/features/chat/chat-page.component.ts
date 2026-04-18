import { Component, OnInit, inject, ElementRef, ViewChild } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { HttpClient } from '@angular/common/http';
import { ApiService } from '../../core/services/api.service';
import { FamilyStateService } from '../../core/services/family-state.service';
import { Router } from '@angular/router';

interface Msg { role: 'user' | 'ai'; text: string; time: string; }

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
  private api = inject(ApiService);
  private familyState = inject(FamilyStateService);
  private router = inject(Router);

  msgs: Msg[] = []; 
  input = ''; 
  loading = false;
  
  get familyId() { return this.familyState.currentFamilyId(); }
  familyName = localStorage.getItem('selectedFamilyName') ?? 'Familia';
  milestone  = localStorage.getItem('currentMilestone') ?? 'Inicio';

  ngOnInit() {
    this.msgs.push({ role:'ai', time: this.now(),
      text: `Hola, soy tu consultor de Integrity Family. Acompaño a ${this.familyName} en su proceso de crecimiento. ¿En qué puedo ayudarte hoy?` });
  }

  send() {
    const text = this.input.trim();
    if (!text || this.loading) return;
    this.msgs.push({ role:'user', text, time: this.now() });
    this.input = ''; this.loading = true;
    this.scroll();
    this.http.post<any>(`${this.api.base}/chat`, { familyId: this.familyId, message: text })
      .subscribe({
        next: (res: any) => {
          let reply = res.data.reply;
          
          if (reply.includes('[COMMAND:CHANGE_FAMILY]')) {
            reply = reply.replace('[COMMAND:CHANGE_FAMILY]', '');
            this.msgs.push({ role:'ai', text: reply, time: this.now() });
            setTimeout(() => this.router.navigate(['/families']), 1500);
          } else {
            this.msgs.push({ role:'ai', text: reply, time: this.now() });
          }
          
          this.loading = false; this.scroll();
        },
        error: () => {
          this.msgs.push({ role:'ai', text:'Hubo un problema conectando con el consultor.', time: this.now() });
          this.loading = false; this.scroll();
        }
      });
  }

  private now() { return new Date().toLocaleTimeString('es-CO', { hour:'2-digit', minute:'2-digit' }); }
  private scroll() { setTimeout(() => this.anchor?.nativeElement?.scrollIntoView({ behavior:'smooth' }), 60); }
}
