import { Component, inject, OnInit, signal, computed } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { EvidenceService } from '../../core/services/evidence.service';
import { DocumentaryProductionService, DocumentaryScope, ProductionStatus, DocumentaryProductionDTO } from '../../core/services/documentary-production.service';
import { FamilyStateService } from '../../core/services/family-state.service';
import { Router } from '@angular/router';

@Component({
  selector: 'app-documentary-maker',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './documentary-maker.component.html',
  styleUrls: ['./documentary-maker.component.css']
})
export class DocumentaryMakerComponent implements OnInit {
  private evidenceService = inject(EvidenceService);
  private docService = inject(DocumentaryProductionService);
  private familyState = inject(FamilyStateService);
  private router = inject(Router);

  familyId = 1;

  productions = signal<DocumentaryProductionDTO[]>([]);
  selectedProduction = signal<DocumentaryProductionDTO | null>(null);

  // Formularios
  newTitle = '';
  newScope: DocumentaryScope = 'MISSION';
  newRefId: number | null = null;

  availableEvidences = signal<any[]>([]);
  selectedEvidenceIds = signal<Set<number>>(new Set());

  // Search
  searchQuery = signal('');
  filteredEvidences = computed(() => {
    const q = this.searchQuery().toLowerCase();
    if (!q) return this.availableEvidences();
    return this.availableEvidences().filter(ev => {
      const title = (ev.title || ev.label || '').toLowerCase();
      const type = (ev.evidenceType || ev.type || '').toLowerCase();
      return title.includes(q) || type.includes(q);
    });
  });

  // Modal Upload
  showUploadModal = signal(false);
  uploadTitle = '';
  uploadType: 'PHOTO' | 'DOCUMENT' | 'AUDIO' = 'PHOTO';
  uploadFileBase64 = '';
  uploadMime = '';

  loading = signal(false);

  ngOnInit() {
    this.familyId = this.familyState.getSelectedFamilyId() || 1;
    this.loadProductions();
  }

  loadProductions() {
    this.docService.getProductions(this.familyId).subscribe(res => {
      this.productions.set(res.data);
    });
  }

  createDraft() {
    this.loading.set(true);
    this.docService.createDraft({
      familyId: this.familyId,
      title: this.newTitle,
      scope: this.newScope,
      referenceId: this.newRefId
    }).subscribe({
      next: (res) => {
        this.loadProductions();
        this.selectProduction(res.data);
        this.loading.set(false);
        this.newTitle = '';
      },
      error: (err) => {
        console.error("Error creating draft", err);
        this.loading.set(false);
      }
    });
  }

  selectProduction(prod: DocumentaryProductionDTO) {
    this.selectedProduction.set(prod);
    
    // Si está en DRAFT, cargamos todas las evidencias de la familia para curaduría
    if (prod.status === 'DRAFT') {
      this.evidenceService.getByFamily(this.familyId).subscribe((res: any) => {
        this.availableEvidences.set(res.data || []);
        
        // Cargar los ya seleccionados si los hay
        const currentSet = new Set<number>();
        prod.curatedEvidences?.forEach(e => currentSet.add(e.id));
        this.selectedEvidenceIds.set(currentSet);
      });
    }
  }

  toggleEvidence(evId: number) {
    const s = this.selectedEvidenceIds();
    if (s.has(evId)) s.delete(evId);
    else s.add(evId);
    this.selectedEvidenceIds.set(new Set(s));
  }

  saveCuration() {
    const prod = this.selectedProduction();
    if (!prod) return;
    this.loading.set(true);
    this.docService.updateCuration(prod.id, Array.from(this.selectedEvidenceIds())).subscribe({
      next: (res) => {
        this.selectProduction(res.data);
        this.loadProductions();
        this.loading.set(false);
      },
      error: () => this.loading.set(false)
    });
  }

  generateScript() {
    const prod = this.selectedProduction();
    if (!prod) return;
    this.loading.set(true);
    this.docService.generateScript(prod.id).subscribe({
      next: (res) => {
        this.selectProduction(res.data);
        this.loadProductions();
        this.loading.set(false);
      },
      error: () => this.loading.set(false)
    });
  }

  approve() {
    const prod = this.selectedProduction();
    if (!prod) return;
    this.loading.set(true);
    this.docService.approveProduction(prod.id).subscribe({
      next: (res) => {
        this.selectProduction(res.data);
        this.loadProductions();
        this.loading.set(false);
      },
      error: () => this.loading.set(false)
    });
  }

  playDocumentary() {
    const prod = this.selectedProduction();
    if (!prod) return;
    this.router.navigate(['/evidence/capture'], { queryParams: { doc: 'prod-' + prod.id } });
  }

  // --- Quick Upload Logic ---
  onFileSelected(event: any) {
    const file = event.target.files[0];
    if (!file) return;
    this.uploadMime = file.type;
    const reader = new FileReader();
    reader.onload = (e) => {
      this.uploadFileBase64 = (e.target?.result as string).split(',')[1];
    };
    reader.readAsDataURL(file);
  }

  submitQuickUpload() {
    if (!this.uploadTitle || !this.uploadFileBase64) return;
    this.loading.set(true);
    this.evidenceService.submit({
      familyId: this.familyId,
      evidenceType: this.uploadType,
      title: this.uploadTitle,
      submittedBy: 'Curador',
      mediaData: this.uploadFileBase64,
      mediaMime: this.uploadMime
    }).subscribe({
      next: () => {
        this.showUploadModal.set(false);
        this.uploadTitle = '';
        this.uploadFileBase64 = '';
        this.loading.set(false);
        if (this.selectedProduction()) {
          this.selectProduction(this.selectedProduction()!);
        }
      },
      error: () => this.loading.set(false)
    });
  }
}
// force rebuild
