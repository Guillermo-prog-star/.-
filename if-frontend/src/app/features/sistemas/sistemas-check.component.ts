import { Component, OnInit, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { FourWeekCheckResponse, apiErrorMessage, formatIsoDate } from './sistemas.model';
import { SistemasService } from './sistemas.service';

@Component({
  selector: 'app-sistemas-check',
  standalone: true,
  imports: [FormsModule, RouterLink],
  templateUrl: './sistemas-check.component.html',
  styleUrl: './sistemas.css'
})
export class SistemasCheckComponent implements OnInit {
  private readonly sistemas = inject(SistemasService);

  readonly formatDate = formatIsoDate;
  readonly loading = signal(true);
  readonly saving = signal(false);
  readonly error = signal('');
  readonly notice = signal('');
  readonly check = signal<FourWeekCheckResponse | null>(null);
  answer: 'si' | 'no' | '' = '';

  ngOnInit(): void {
    this.load();
  }

  save(): void {
    if (!this.answer) {
      this.error.set('Responde si llegó algún compromiso a la fecha sin estar registrado.');
      return;
    }
    this.saving.set(true);
    this.error.set('');
    this.sistemas.ensureFamily().subscribe({
      next: familyId => {
        this.sistemas.saveCheck(familyId, this.answer === 'si').subscribe({
          next: check => {
            this.check.set(check);
            this.notice.set('Respuesta guardada. Las otras dos preguntas se calculan de las revisiones.');
            this.saving.set(false);
          },
          error: err => {
            this.error.set(apiErrorMessage(err, 'No fue posible guardar la prueba.'));
            this.saving.set(false);
          }
        });
      },
      error: err => {
        this.error.set(err?.message || 'No se encontró una familia activa.');
        this.saving.set(false);
      }
    });
  }

  private load(): void {
    this.sistemas.ensureFamily().subscribe({
      next: familyId => {
        this.sistemas.fourWeekCheck(familyId).subscribe({
          next: check => {
            this.check.set(check);
            this.answer = check.unregisteredCommitment == null ? '' : check.unregisteredCommitment ? 'si' : 'no';
            this.loading.set(false);
          },
          error: err => {
            this.error.set(apiErrorMessage(err, 'No fue posible calcular la prueba.'));
            this.loading.set(false);
          }
        });
      },
      error: err => {
        this.error.set(err?.message || 'No se encontró una familia activa.');
        this.loading.set(false);
      }
    });
  }
}
