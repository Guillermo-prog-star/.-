import { Component, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { FeedbackService } from '../../../core/services/feedback.service';
import { FamilyStateService } from '../../../core/services/family-state.service';

@Component({
  selector: 'app-feedback-dialog',
  standalone: true,
  imports: [CommonModule, FormsModule],
  template: `
    <div class="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/60 backdrop-blur-sm" *ngIf="visible">
      <div class="glass-premium p-8 rounded-[2rem] border border-white/10 w-full max-w-md shadow-2xl relative animate-fade-in">
        <button (click)="close()" class="absolute top-6 right-6 text-white/40 hover:text-white">✕</button>
        
        <h2 class="text-2xl font-black text-white mb-2">Voz del Beta-Tester</h2>
        <p class="text-xs text-white/40 mb-6 uppercase tracking-widest font-bold">Ayúdanos a evolucionar el sistema</p>

        <div class="space-y-6">
          <!-- Type Toggle -->
          <div class="flex gap-2 p-1 bg-white/5 rounded-xl border border-white/5">
            <button 
                *ngFor="let t of types" 
                (click)="type = t.id"
                [ngClass]="type === t.id ? 'bg-indigo-600 text-white' : 'text-white/40'"
                class="flex-1 py-2 text-[10px] font-bold rounded-lg transition-all uppercase">
              {{ t.label }}
            </button>
          </div>

          <!-- Stars -->
          <div class="flex justify-center gap-4 text-3xl">
            <button 
                *ngFor="let s of [1,2,3,4,5]" 
                (click)="score = s"
                [ngClass]="score >= s ? 'text-yellow-400' : 'text-white/10'"
                class="hover:scale-125 transition-transform">
              ★
            </button>
          </div>

          <!-- Comment -->
          <textarea 
            [(ngModel)]="comment"
            placeholder="¿Qué podríamos mejorar? ¿Algún error detectado?"
            class="w-full bg-black/20 border border-white/10 rounded-2xl p-4 text-white text-sm outline-none focus:border-indigo-500 min-h-[100px]"
          ></textarea>

          <div *ngIf="feedbackMsg" [style]="feedbackSuccess
              ? 'padding:10px 14px;background:rgba(52,211,153,0.1);border:1px solid rgba(52,211,153,0.3);border-radius:12px;color:#6ee7b7;font-size:0.82rem;font-weight:600;text-align:center;'
              : 'padding:10px 14px;background:rgba(239,68,68,0.1);border:1px solid rgba(239,68,68,0.3);border-radius:12px;color:#fca5a5;font-size:0.82rem;font-weight:600;text-align:center;'">
            {{ feedbackMsg }}
          </div>

          <button
            (click)="submit()"
            [disabled]="loading || !comment.trim() || feedbackSuccess"
            class="w-full bg-indigo-600 hover:bg-indigo-500 text-white font-bold py-4 rounded-2xl transition-all shadow-lg active:scale-95 disabled:opacity-50">
            {{ loading ? 'Sincronizando...' : 'Enviar Feedback' }}
          </button>
        </div>
      </div>
    </div>

    <!-- Floating Trigger -->
    <button (click)="visible = true" class="fixed bottom-8 right-8 w-14 h-14 bg-yellow-500 rounded-2xl flex items-center justify-center text-2xl shadow-xl hover:scale-110 active:scale-95 transition-all z-40 group">
      💡
      <span class="absolute right-16 bg-black/80 text-white text-[10px] py-1 px-3 rounded-lg opacity-0 group-hover:opacity-100 transition-opacity whitespace-nowrap uppercase font-bold border border-white/10">Dar Feedback</span>
    </button>
  `,
  styles: [`
    .glass-premium {
      background: rgba(15, 23, 42, 0.8);
      backdrop-filter: blur(40px);
    }
    @keyframes fadeIn {
      from { opacity: 0; transform: translateY(20px); }
      to { opacity: 1; transform: translateY(0); }
    }
    .animate-fade-in { animation: fadeIn 0.3s ease-out forwards; }
  `]
})
export class FeedbackDialogComponent {
  constructor(
    private feedbackService: FeedbackService,
    private familyState: FamilyStateService
  ) {}

  visible = false;
  loading = false;
  feedbackMsg = '';
  feedbackSuccess = false;

  type = 'EXPERIENCE';
  score = 5;
  comment = '';

  types = [
    { id: 'BUG', label: 'Error' },
    { id: 'SUGGESTION', label: 'Idea' },
    { id: 'EXPERIENCE', label: 'Sentir' }
  ];

  close() {
    this.visible = false;
    this.comment = '';
    this.score = 5;
    this.feedbackMsg = '';
  }

  submit() {
    this.loading = true;
    const data = {
      familyId: this.familyState.currentFamilyId(),
      score: this.score,
      comment: this.comment,
      type: this.type
    };

    this.feedbackService.sendFeedback(data).subscribe({
      next: () => {
        this.loading = false;
        this.feedbackSuccess = true;
        this.feedbackMsg = '¡Gracias! Tu voz ha sido sincronizada con el equipo de desarrollo.';
        setTimeout(() => { this.close(); }, 2200);
      },
      error: () => {
        this.loading = false;
        this.feedbackSuccess = false;
        this.feedbackMsg = 'Error al enviar el feedback. Reintenta en un momento.';
      }
    });
  }
}
