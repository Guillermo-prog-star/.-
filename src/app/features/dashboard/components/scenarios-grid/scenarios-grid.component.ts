import { Component, Input } from '@angular/core';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-scenarios-grid',
  standalone: true,
  imports: [CommonModule],
  template: ''
})
export class ScenariosGridComponent {
  @Input() data: any;

  // [SDD] Tipado explícito 'any' inyectado para superar el AOT
  processActions() {
    if (this.data?.suggestedActions) {
      this.data.suggestedActions.forEach((action: any, index: number) => {
        console.log(action);
      });
    }
  }
}
