import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { ReviewResponse, apiErrorMessage, formatIsoDate } from './sistemas.model';
import { SistemasService } from './sistemas.service';

@Component({
  selector: 'app-sistemas-history',
  standalone: true,
  imports: [RouterLink],
  templateUrl: './sistemas-history.component.html',
  styleUrl: './sistemas.css'
})
export class SistemasHistoryComponent implements OnInit {
  private readonly sistemas = inject(SistemasService);

  readonly formatDate = formatIsoDate;
  readonly loading = signal(true);
  readonly error = signal('');
  readonly reviews = signal<ReviewResponse[]>([]);
  readonly filter = signal('ALL');
  readonly openId = signal<number | null>(null);

  readonly filters = [
    { code: 'ALL', label: 'Todas' },
    { code: 'DAILY', label: 'Diaria' },
    { code: 'WEEKLY', label: 'Semanal' },
    { code: 'MONTHLY', label: 'Mensual' },
    { code: 'ANNUAL', label: 'Anual' },
    { code: 'CHANGE', label: 'Por cambio' }
  ];

  readonly visible = computed(() => {
    const filter = this.filter();
    const reviews = this.reviews();
    return filter === 'ALL' ? reviews : reviews.filter(review => review.type === filter);
  });

  ngOnInit(): void {
    this.sistemas.ensureFamily().subscribe({
      next: familyId => {
        this.sistemas.history(familyId).subscribe({
          next: reviews => {
            this.reviews.set(reviews);
            this.loading.set(false);
          },
          error: err => {
            this.error.set(apiErrorMessage(err, 'No fue posible cargar el historial.'));
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

  toggle(id: number): void {
    this.openId.set(this.openId() === id ? null : id);
  }
}
