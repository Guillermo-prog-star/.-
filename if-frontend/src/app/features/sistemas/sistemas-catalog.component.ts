import { Component, OnInit, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { SistemasService } from './sistemas.service';
import { CatalogResponse, apiErrorMessage } from './sistemas.model';

@Component({
  selector: 'app-sistemas-catalog',
  standalone: true,
  imports: [RouterLink],
  templateUrl: './sistemas-catalog.component.html',
  styleUrl: './sistemas.css'
})
export class SistemasCatalogComponent implements OnInit {
  private readonly sistemas = inject(SistemasService);

  readonly catalog = signal<CatalogResponse | null>(null);
  readonly loading = signal(true);
  readonly error = signal('');

  ngOnInit(): void {
    this.sistemas.getCatalog().subscribe({
      next: catalog => {
        this.catalog.set(catalog);
        this.loading.set(false);
      },
      error: err => {
        this.error.set(apiErrorMessage(err, 'No fue posible cargar los sistemas.'));
        this.loading.set(false);
      }
    });
  }
}
