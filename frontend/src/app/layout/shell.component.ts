import { Component } from '@angular/core';
import { RouterOutlet } from '@angular/router';
import { SidebarComponent } from '../shared/components/sidebar.component';
import { NavbarComponent } from '../shared/components/navbar.component';
@Component({
  selector: 'app-shell', standalone: true,
  imports: [RouterOutlet, SidebarComponent, NavbarComponent],
  template: `
    <div style="display: flex; min-height: 100vh;">
      <app-sidebar/>
      <div style="flex: 1; margin-left: 280px; display: flex; flex-direction: column; min-width: 0; background: var(--bg-main);">
        <app-navbar/>
        <main style="padding: 40px; flex: 1; overflow-y: auto;">
          <router-outlet/>
        </main>
      </div>
    </div>`
})
export class ShellComponent {}
