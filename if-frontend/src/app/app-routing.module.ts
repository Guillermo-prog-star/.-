import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';
import { EvaluationComponent } from './features/evaluation/evaluation.component';

const routes: Routes = [
  // 1. Ruta principal del Test
  { 
    path: 'evaluation', 
    component: EvaluationComponent 
  },

  // 2. Redirección por defecto (cuando entras a localhost:4200)
  { 
    path: '', 
    redirectTo: 'evaluation', 
    pathMatch: 'full' 
  },

  // 3. Ruta de escape (puedes crear un componente 404 luego)
  { 
    path: '**', 
    redirectTo: 'evaluation' 
  }
];

@NgModule({
  imports: [RouterModule.forRoot(routes)],
  exports: [RouterModule]
})
export class AppRoutingModule { }