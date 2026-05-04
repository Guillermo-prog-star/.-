import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import {
  CreateFamilyLogbookEntryRequest,
  FamilyLogbookEntry,
  LogbookStatus
} from './family-logbook.model';
import { FamilyLogbookService } from './family-logbook.service';
import { AuthService } from '../../core/services/auth.service';

@Component({
  selector: 'app-family-logbook',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './family-logbook.component.html',
  styleUrl: './family-logbook.component.css'
})
export class FamilyLogbookComponent implements OnInit {
  familyId = 0; // Se inicializará desde el auth context

  statusFilter: 'ALL' | LogbookStatus = 'ALL';

  entries: FamilyLogbookEntry[] = [];

  form: CreateFamilyLogbookEntryRequest = {
    familyId: 0,
    situation: '',
    difficultyDetected: '',
    emotionIdentified: '',
    understanding: '',
    correctionAction: '',
    familyAgreement: '',
    createdBy: ''
  };

  resolveEvidence: Record<number, string> = {};

  loading = false;
  errorMessage = '';

  constructor(
    private readonly service: FamilyLogbookService,
    private readonly authService: AuthService
  ) {}

  ngOnInit(): void {
    const user = this.authService.user();
    if (user && user.familyId) {
      this.familyId = user.familyId;
      this.form.familyId = user.familyId;
      this.form.createdBy = user.fullName; // Pre-poblar autor
      this.loadEntries();
    } else {
      this.errorMessage = 'No se encontró una familia asociada a tu cuenta.';
    }
  }

  loadEntries(): void {
    this.loading = true;
    this.errorMessage = '';

    const request$ = this.statusFilter === 'ALL'
      ? this.service.findByFamily(this.familyId)
      : this.service.findByFamilyAndStatus(this.familyId, this.statusFilter);

    request$.subscribe({
      next: entries => {
        this.entries = entries;
        this.loading = false;
      },
      error: () => {
        this.errorMessage = 'No fue posible cargar la bitácora familiar.';
        this.loading = false;
      }
    });
  }

  createEntry(): void {
    if (!this.isFormValid()) {
      this.errorMessage = 'Todos los campos principales son obligatorios.';
      return;
    }

    this.loading = true;
    this.errorMessage = '';

    this.form.familyId = this.familyId;

    this.service.create(this.form).subscribe({
      next: () => {
        this.resetForm();
        this.loadEntries();
      },
      error: () => {
        this.errorMessage = 'No fue posible crear la entrada de bitácora.';
        this.loading = false;
      }
    });
  }

  resolveEntry(entry: FamilyLogbookEntry): void {
    const evidence = this.resolveEvidence[entry.id];

    if (!evidence || !evidence.trim()) {
      this.errorMessage = 'La evidencia de avance es obligatoria.';
      return;
    }

    this.loading = true;
    this.errorMessage = '';

    this.service.resolve(entry.id, {
      progressEvidence: evidence,
      resolvedBy: this.form.createdBy || 'Familia'
    }).subscribe({
      next: () => {
        this.resolveEvidence[entry.id] = '';
        this.loadEntries();
      },
      error: () => {
        this.errorMessage = 'No fue posible cerrar la entrada.';
        this.loading = false;
      }
    });
  }

  private isFormValid(): boolean {
    return Boolean(
      this.form.situation.trim() &&
      this.form.difficultyDetected.trim() &&
      this.form.emotionIdentified.trim() &&
      this.form.understanding.trim() &&
      this.form.correctionAction.trim() &&
      this.form.familyAgreement.trim()
    );
  }

  private resetForm(): void {
    this.form = {
      familyId: this.familyId,
      situation: '',
      difficultyDetected: '',
      emotionIdentified: '',
      understanding: '',
      correctionAction: '',
      familyAgreement: '',
      createdBy: ''
    };
  }
}
