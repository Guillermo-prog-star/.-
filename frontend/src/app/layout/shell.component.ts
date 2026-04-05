import { Component } from '@angular/core';
import { RouterOutlet } from '@angular/router';
import { SidebarComponent } from '../shared/components/sidebar.component';
import { NavbarComponent } from '../shared/components/navbar.component';
@Component({
  selector: 'app-shell', standalone: true,
  imports: [RouterOutlet, SidebarComponent, NavbarComponent],
  template: `
    <div style="display:grid;grid-template-columns:240px 1fr;min-height:100vh;">
      <app-sidebar/>
      <div style="display:flex;flex-direction:column;min-width:0;">
        <app-navbar/>
        <main style="padding:28px;flex:1;overflow-y:auto;">
          <router-outlet/>
        </main>
      </div>
    </div>`
})
export class ShellComponent {}
