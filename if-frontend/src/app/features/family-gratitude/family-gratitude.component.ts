import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { CreateFamilyGratitudeRequest, FamilyGratitude } from './family-gratitude.model';
import { FamilyGratitudeService } from './family-gratitude.service';
import { AuthService } from '../../core/services/auth.service';
import { ScrollPolicyService } from '../../shared/directives/scroll-policy.service';

@Component({
  selector: 'app-family-gratitude',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './family-gratitude.component.html',
  styleUrl: './family-gratitude.component.css'
})
export class FamilyGratitudeComponent implements OnInit {
  private router       = inject(Router);
  private scrollPolicy = inject(ScrollPolicyService);

  familyId = 0;
  entries: FamilyGratitude[] = [];
  /** true cuando el usuario llega desde el cierre de un sprint */
  sprintJustCompleted = false;

  form: CreateFamilyGratitudeRequest = {
    familyId: 0,
    fromMember: '',
    toMember: '',
    description: ''
  };

  loading = false;
  errorMessage = '';
  successMessage = '';

  constructor(
    private readonly service: FamilyGratitudeService,
    private readonly authService: AuthService
  ) {}

  ngOnInit(): void {
    this.scrollPolicy.set('scroll-to-new');
    // Detectar si viene del cierre de un sprint (queryParam o navigation state)
    const nav = window.history.state;
    if (nav?.sprintCompleted) this.sprintJustCompleted = true;

    const user = this.authService.user();
    if (user && user.familyId) {
      this.familyId = user.familyId;
      this.form.familyId = user.familyId;
      this.form.fromMember = user.fullName;
      this.loadGratitudes();
    } else {
      this.errorMessage = 'No se encontró una familia asociada a tu cuenta.';
    }
  }

  loadGratitudes(): void {
    this.loading = true;
    this.errorMessage = '';

    this.service.findByFamily(this.familyId).subscribe({
      next: entries => {
        this.entries = entries;
        this.loading = false;
      },
      error: () => {
        this.errorMessage = 'No fue posible cargar los agradecimientos familiares.';
        this.loading = false;
      }
    });
  }

  createGratitude(): void {
    if (!this.isFormValid()) {
      this.errorMessage = 'Todos los campos son obligatorios.';
      return;
    }

    this.loading = true;
    this.errorMessage = '';
    this.successMessage = '';

    this.form.familyId = this.familyId;

    this.service.create(this.form).subscribe({
      next: () => {
        this.successMessage = '¡Agradecimiento enviado con éxito! Un paso más hacia la reconexión.';
        const prevFrom = this.form.fromMember;
        this.resetForm();
        this.form.fromMember = prevFrom; // Conservar el nombre del autor
        this.loadGratitudes();
        setTimeout(() => this.successMessage = '', 4000);
      },
      error: () => {
        this.errorMessage = 'No fue posible enviar la nota de gratitud.';
        this.loading = false;
      }
    });
  }

  isFormValid(): boolean {
    return Boolean(
      this.form.fromMember.trim() &&
      this.form.toMember.trim() &&
      this.form.description.trim()
    );
  }

  private resetForm(): void {
    this.form = {
      familyId: this.familyId,
      fromMember: '',
      toMember: '',
      description: ''
    };
  }
}
