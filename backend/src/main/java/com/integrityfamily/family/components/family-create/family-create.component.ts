import { Component, OnInit } from '@angular/core';
import { Router } from '@angular/router'; // Herramienta de navegación
import { FamilyService } from '../../services/family.service';
import { Family } from '../../models/family.model';
import { ApiResponse } from '../../../../core/models/api-response.model';

@Component({
  selector: 'app-family-create',
  templateUrl: './family-create.component.html',
  styleUrls: ['./family-create.component.scss']
})
export class FamilyCreateComponent implements OnInit {

  // Objeto vinculado al formulario de la Familia López Rivera
  family: Family = {
    name: '',
    description: '',
    municipio: 'Armenia', // Valor por defecto para tu nodo
    whatsapp: '',
    pin: ''
  };

  constructor(
    private familyService: FamilyService,
    private router: Router // El "conductor" hacia el Dashboard
  ) { }

  ngOnInit(): void { }

  /**
   * Acción del botón "Crear familia"
   */
  onCreateFamily(): void {
    this.familyService.create(this.family).subscribe({
      next: (response: ApiResponse<Family>) => {
        if (response.success) {
          console.log('Familia creada con éxito en el Nodo Armenia');
          
          // LA CLAVE: Redirección inmediata al Dashboard
          this.router.navigate(['/dashboard']);
        }
      },
      error: (err) => {
        console.error('Error al conectar con el Backend:', err);
        alert('Error al crear familia. Verifica la conexión del contenedor if-backend.');
      }
    });
  }
}