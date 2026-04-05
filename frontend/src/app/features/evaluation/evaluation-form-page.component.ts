import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, ActivatedRoute } from '@angular/router';
import { HttpClient } from '@angular/common/http';
import { ApiService } from '../../core/services/api.service';
import { QuestionResponse } from '../../core/models/models';
const DIM_STYLE: Record<string, { bg: string; text: string; dot: string }> = {
  EMOTIONS:      { bg:'#FDF2F8', text:'#9D174D', dot:'#EC4899' },
  COMMUNICATION: { bg:'#EFF6FF', text:'#1E40AF', dot:'#3B82F6' },
  HABITS:        { bg:'#F0FDF4', text:'#166534', dot:'#22C55E' },
  TIMES:         { bg:'#FFFBEB', text:'#92400E', dot:'#F59E0B' },
};
const DIM_LABEL: Record<string, string> = { EMOTIONS:'Emociones', COMMUNICATION:'Comunicación', HABITS:'Hábitos', TIMES:'Tiempos' };
@Component({
  selector: 'app-evaluation-form-page', standalone: true, imports: [CommonModule],
  template: `
    <div class="page-header">
      <div><h1>Evaluación familiar</h1><p>Responde con honestidad — cada respuesta importa</p></div>
      <span class="badge badge-green" style="font-size:14px;padding:6px 14px;">{{ answered }}/{{ questions.length }}</span>
    </div>
    @if (loading) { <div class="loading">Cargando preguntas...</div> }
    @else {
      <div class="stack" style="max-width:700px;">
        @for (q of questions; track q.id; let i = $index) {
          <div class="card">
            <div style="display:flex;justify-content:space-between;margin-bottom:12px;">
              <span class="badge" [style.background]="dimStyle(q.dimension).bg" [style.color]="dimStyle(q.dimension).text">
                {{ dimLabel(q.dimension) }}
              </span>
              <span class="muted">Pregunta {{ i+1 }}</span>
            </div>
            <p style="font-size:15px;color:var(--text);margin-bottom:16px;line-height:1.5;">{{ q.questionText }}</p>
            <div style="display:flex;gap:8px;">
              @for (v of [1,2,3,4,5]; track v) {
                <button (click)="setAnswer(q.id, v)"
                        [style.border-color]="answers[q.id]===v ? dimStyle(q.dimension).dot : 'var(--border)'"
                        [style.background]="answers[q.id]===v ? dimStyle(q.dimension).bg : 'var(--surface)'"
                        [style.color]="answers[q.id]===v ? dimStyle(q.dimension).text : 'var(--muted)'"
                        style="flex:1;padding:10px;border:2px solid;border-radius:10px;font-weight:700;font-size:15px;cursor:pointer;transition:all .1s;">
                  {{ v }}
                </button>
              }
            </div>
            <div style="display:flex;justify-content:space-between;margin-top:6px;font-size:11px;color:var(--muted);"><span>Nunca</span><span>Siempre</span></div>
          </div>
        }
      </div>
      <div style="margin-top:24px;max-width:700px;">
        <button class="btn btn-primary" style="width:100%;justify-content:center;padding:14px;font-size:15px;"
                [disabled]="answered < questions.length || submitting" (click)="submit()">
          {{ submitting ? 'Procesando evaluación y consultando IA...' : answered < questions.length ? 'Responde todas las preguntas (' + (questions.length - answered) + ' pendientes)' : 'Finalizar evaluación →' }}
        </button>
      </div>
    }`
})
export class EvaluationFormPageComponent implements OnInit {
  private http = inject(HttpClient); private api = inject(ApiService);
  private router = inject(Router); private route = inject(ActivatedRoute);
  evalId = Number(this.route.snapshot.paramMap.get('id'));
  familyId = Number(localStorage.getItem('selectedFamilyId') ?? 1);
  questions: QuestionResponse[] = []; answers: Record<number,number> = {};
  loading = false; submitting = false;
  get answered() { return Object.keys(this.answers).length; }
  ngOnInit() {
    this.loading = true;
    this.http.get<any>(`${this.api.base}/evaluations/questions/family/${this.familyId}`)
      .subscribe({ next: ({ data }) => { this.questions = data; this.loading = false; }, error: () => this.loading = false });
  }
  setAnswer(qId: number, v: number) { this.answers[qId] = v; }
  dimStyle(d: string) { return DIM_STYLE[d] ?? DIM_STYLE['EMOTIONS']; }
  dimLabel(d: string) { return DIM_LABEL[d] ?? d; }
  submit() {
    this.submitting = true;
    const payload = { answers: Object.entries(this.answers).map(([qId,v]) => ({ questionId: Number(qId), answerValue: v })) };
    this.http.post<any>(`${this.api.base}/evaluations/${this.evalId}/finalize`, payload)
      .subscribe({
        next: () => { this.submitting = false; this.router.navigate(['/evaluations', this.evalId, 'result']); },
        error: () => this.submitting = false
      });
  }
}
