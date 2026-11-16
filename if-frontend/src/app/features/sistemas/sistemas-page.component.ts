import { Component, OnInit, inject, signal } from '@angular/core';
import { NavigationEnd, Router, RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { filter } from 'rxjs';

@Component({
  selector: 'app-sistemas-page',
  standalone: true,
  imports: [RouterOutlet, RouterLink, RouterLinkActive],
  templateUrl: './sistemas-page.component.html',
  styleUrl: './sistemas.css'
})
export class SistemasPageComponent implements OnInit {
  private readonly router = inject(Router);
  readonly url = signal(this.router.url);

  ngOnInit(): void {
    this.router.events.pipe(filter(event => event instanceof NavigationEnd)).subscribe(() => {
      this.url.set(this.router.url);
    });
  }

  onReviews(): boolean {
    return this.url().includes('/sistemas/revision');
  }
}
