import { Component, OnInit, inject, signal, computed } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { FamilyStateService } from '../../core/services/family-state.service';
import { AuthService } from '../../core/services/auth.service';
import {
  EcosystemService, EcosystemSummary, EcosystemLink,
  EcosystemParticipant, NetworkType, EcosystemAccessScope, AuditEntry,
  RegisterParticipantRequest
} from '../../core/services/ecosystem.service';
import { catchError, of } from 'rxjs';

type ActiveTab = 'network' | 'catalog' | 'audit';

interface InviteForm {
  participantId: number | null;
  objective: string;
  responsibilities: string;
  validFrom: string;
  validUntil: string;
  scope: EcosystemAccessScope;
}

interface ConsentForm {
  linkId: number;
  scope: EcosystemAccessScope;
}

const DEFAULT_SCOPE: EcosystemAccessScope = {
  canViewIcfScore: false,
  canViewRiskLevel: false,
  canViewPlanSummary: false,
  canViewSprintProgress: false,
  canViewCrisisHistory: false,
  canReceiveAlerts: false
};

const DEFAULT_REGISTER: RegisterParticipantRequest = {
  name: '', networkType: 'PROFESSIONAL', description: '',
  contactEmail: '', contactPhone: '', website: ''
};

@Component({
  selector: 'app-ecosystem-page',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './ecosystem-page.component.html',
  styleUrls: ['./ecosystem-page.component.css']
})
export class EcosystemPageComponent implements OnInit {
  private familyState = inject(FamilyStateService);
  private auth        = inject(AuthService);
  private svc         = inject(EcosystemService);

  readonly isAdmin = computed(() => this.auth.user()?.role === 'ADMIN');

  readonly activeTab    = signal<ActiveTab>('network');
  readonly summary      = signal<EcosystemSummary | null>(null);
  readonly catalog      = signal<EcosystemParticipant[]>([]);
  readonly auditLog     = signal<AuditEntry[]>([]);
  readonly loading      = signal(true);
  readonly loadingCat   = signal(false);
  readonly loadingAudit = signal(false);
  readonly error        = signal<string | null>(null);
  readonly actionMsg    = signal<string | null>(null);
  readonly actionErr    = signal<string | null>(null);

  readonly catalogFilter  = signal<NetworkType | 'ALL'>('ALL');
  readonly catalogSearch  = signal('');
  readonly showInviteModal   = signal(false);
  readonly showConsentModal  = signal(false);
  readonly showRevokeModal   = signal(false);
  readonly revokeReason      = signal('');
  readonly pendingRevokeId   = signal<number | null>(null);
  readonly pendingConsentLink = signal<EcosystemLink | null>(null);
  readonly pendingInviteParticipant = signal<EcosystemParticipant | null>(null);
  readonly actionLoading     = signal(false);

  // ── Admin: registro y edición de participante ───────────────────────
  readonly showRegisterModal  = signal(false);
  registerForm: RegisterParticipantRequest = { ...DEFAULT_REGISTER };
  readonly registerLoading    = signal(false);

  readonly showEditModal      = signal(false);
  editForm: RegisterParticipantRequest = { ...DEFAULT_REGISTER };
  editingParticipantId: number | null = null;
  readonly editLoading        = signal(false);

  inviteForm: InviteForm = {
    participantId: null,
    objective: '',
    responsibilities: '',
    validFrom: '',
    validUntil: '',
    scope: { ...DEFAULT_SCOPE }
  };

  readonly showEditLinkModal  = signal(false);
  editLinkForm: InviteForm = {
    participantId: null,
    objective: '',
    responsibilities: '',
    validFrom: '',
    validUntil: '',
    scope: { ...DEFAULT_SCOPE }
  };
  editingLinkId: number | null = null;
  readonly editLinkLoading     = signal(false);

  readonly consentForm = signal<ConsentForm>({
    linkId: 0,
    scope: { ...DEFAULT_SCOPE }
  });

  readonly filteredCatalog = computed(() => {
    const type   = this.catalogFilter();
    const search = this.catalogSearch().toLowerCase().trim();
    return this.catalog()
      .filter(p => type === 'ALL' || p.networkType === type)
      .filter(p => !search
        || p.name.toLowerCase().includes(search)
        || (p.description ?? '').toLowerCase().includes(search));
  });

  readonly totalActive  = computed(() => this.summary()?.activeLinks ?? 0);
  readonly totalLinks   = computed(() => this.summary()?.totalLinks ?? 0);
  readonly totalPending = computed(() => this.pendingLinks().length);

  readonly networkSections = computed(() => [
    { key: 'familiar',      label: 'Red Familiar',   icon: '👨‍👩‍👧', links: this.summary()?.familiar      ?? [] },
    { key: 'institutional', label: 'Institucional',  icon: '🏛️',   links: this.summary()?.institutional ?? [] },
    { key: 'community',     label: 'Comunitaria',    icon: '🤝',   links: this.summary()?.community     ?? [] },
    { key: 'territorial',   label: 'Territorial',    icon: '🌍',   links: this.summary()?.territorial   ?? [] },
  ].filter(s => s.links.some(l => l.status !== 'INVITED')));

  private get familyId(): number {
    return this.familyState.getSelectedFamilyId();
  }

  ngOnInit() { this.loadSummary(); }

  setTab(tab: ActiveTab) {
    this.activeTab.set(tab);
    if (tab === 'catalog' && this.catalog().length === 0) this.loadCatalog();
    if (tab === 'audit'   && this.auditLog().length === 0) this.loadAudit();
  }

  private loadSummary() {
    this.loading.set(true);
    this.error.set(null);
    this.svc.getSummary(this.familyId).pipe(
      catchError(() => { this.error.set('No se pudo cargar el ecosistema.'); return of(null); })
    ).subscribe(s => { this.summary.set(s); this.loading.set(false); });
  }

  private loadCatalog() {
    this.loadingCat.set(true);
    this.svc.getParticipants().pipe(catchError(() => of([])))
      .subscribe(list => { this.catalog.set(list); this.loadingCat.set(false); });
  }

  private loadAudit() {
    this.loadingAudit.set(true);
    this.svc.getAuditLog(this.familyId).pipe(catchError(() => of([])))
      .subscribe(log => { this.auditLog.set(log); this.loadingAudit.set(false); });
  }

  openInviteModal(participant: EcosystemParticipant) {
    this.pendingInviteParticipant.set(participant);
    this.inviteForm = {
      participantId: participant.id,
      objective: '',
      responsibilities: '',
      validFrom: '',
      validUntil: '',
      scope: { ...DEFAULT_SCOPE }
    };
    this.showInviteModal.set(true);
    this.clearMessages();
  }

  closeInviteModal() { this.showInviteModal.set(false); this.pendingInviteParticipant.set(null); }

  submitInvite() {
    const f = this.inviteForm;
    if (!f.participantId) return;
    this.actionLoading.set(true);
    this.clearMessages();
    this.svc.link(this.familyId, {
      participantId: f.participantId,
      objective: f.objective || undefined,
      responsibilities: f.responsibilities || undefined,
      validFrom: f.validFrom || undefined,
      validUntil: f.validUntil || undefined,
      accessScope: f.scope
    }).pipe(
      catchError(err => { this.actionErr.set(err?.error?.message ?? 'Error al crear la conexión.'); return of(null); })
    ).subscribe(link => {
      this.actionLoading.set(false);
      if (link) {
        this.showInviteModal.set(false);
        this.actionMsg.set('Invitación enviada. Ahora puedes dar consentimiento en la pestaña Mi Red.');
        this.loadSummary();
      }
    });
  }

  // ── Editar Detalles de Conexión en Mi Red ──────────────────────────────
  openEditLinkModal(link: EcosystemLink) {
    this.editingLinkId = link.id;
    this.editLinkForm = {
      participantId: link.participant?.id ?? null,
      objective: link.objective ?? '',
      responsibilities: link.responsibilities ?? '',
      validFrom: link.validFrom ?? '',
      validUntil: link.validUntil ?? '',
      scope: link.accessScope ? { ...link.accessScope } : { ...DEFAULT_SCOPE }
    };
    this.showEditLinkModal.set(true);
    this.clearMessages();
  }

  closeEditLinkModal() {
    this.showEditLinkModal.set(false);
    this.editingLinkId = null;
  }

  submitEditLink() {
    if (this.editingLinkId == null) return;
    const f = this.editLinkForm;
    this.editLinkLoading.set(true);
    this.clearMessages();
    this.svc.updateLink(this.familyId, this.editingLinkId, {
      objective: f.objective || undefined,
      responsibilities: f.responsibilities || undefined,
      validFrom: f.validFrom || null,
      validUntil: f.validUntil || null,
      accessScope: f.scope
    }).pipe(
      catchError(err => {
        this.actionErr.set(err?.error?.message ?? 'Error al actualizar los detalles de la conexión.');
        return of(null);
      })
    ).subscribe(link => {
      this.editLinkLoading.set(false);
      if (link) {
        this.showEditLinkModal.set(false);
        this.actionMsg.set('Detalles de conexión actualizados correctamente.');
        this.loadSummary();
      }
    });
  }

  toggleEditLinkScope(key: keyof EcosystemAccessScope) {
    this.editLinkForm.scope[key] = !this.editLinkForm.scope[key];
  }

  openConsentModal(link: EcosystemLink) {
    this.pendingConsentLink.set(link);
    this.consentForm.set({ linkId: link.id, scope: link.accessScope ? { ...link.accessScope } : { ...DEFAULT_SCOPE } });
    this.showConsentModal.set(true);
    this.clearMessages();
  }

  closeConsentModal() { this.showConsentModal.set(false); this.pendingConsentLink.set(null); }

  submitConsent() {
    const f = this.consentForm();
    this.actionLoading.set(true);
    this.clearMessages();
    this.svc.giveConsent(this.familyId, f.linkId, f.scope).pipe(
      catchError(err => { this.actionErr.set(err?.error?.message ?? 'Error al dar consentimiento.'); return of(null); })
    ).subscribe(link => {
      this.actionLoading.set(false);
      if (link) {
        this.showConsentModal.set(false);
        this.actionMsg.set('Consentimiento otorgado. El participante ahora tiene acceso activo.');
        this.loadSummary();
      }
    });
  }

  openRevokeModal(linkId: number) {
    this.pendingRevokeId.set(linkId);
    this.revokeReason.set('');
    this.showRevokeModal.set(true);
    this.clearMessages();
  }

  closeRevokeModal() { this.showRevokeModal.set(false); this.pendingRevokeId.set(null); }

  submitRevoke() {
    const id = this.pendingRevokeId();
    if (!id) return;
    this.actionLoading.set(true);
    this.clearMessages();
    this.svc.revoke(this.familyId, id, this.revokeReason() || undefined).pipe(
      catchError(err => { this.actionErr.set(err?.error?.message ?? 'Error al revocar el acceso.'); return of(null); })
    ).subscribe(link => {
      this.actionLoading.set(false);
      if (link) {
        this.showRevokeModal.set(false);
        this.actionMsg.set('Acceso revocado. El participante ya no puede ver datos de tu familia.');
        this.loadSummary();
      }
    });
  }

  toggleInviteScope(key: keyof EcosystemAccessScope) {
    this.inviteForm.scope[key] = !this.inviteForm.scope[key];
  }

  toggleConsentScope(key: keyof EcosystemAccessScope) {
    this.consentForm.update(f => ({ ...f, scope: { ...f.scope, [key]: !f.scope[key] } }));
  }

  setCatalogFilter(f: NetworkType | 'ALL') { this.catalogFilter.set(f); }
  setCatalogSearch(v: string)              { this.catalogSearch.set(v); }
  private clearMessages() { this.actionMsg.set(null); this.actionErr.set(null); }

  // ── Admin: registro de participante ──────────────────────────────────
  openRegisterModal() {
    this.registerForm = { ...DEFAULT_REGISTER };
    this.showRegisterModal.set(true);
    this.clearMessages();
  }
  closeRegisterModal() { this.showRegisterModal.set(false); }

  submitRegister() {
    const f = this.registerForm;
    if (!f.name.trim()) return;
    this.registerLoading.set(true);
    this.clearMessages();
    this.svc.registerParticipant(f).pipe(
      catchError(err => {
        this.actionErr.set(err?.error?.message ?? 'Error al registrar el participante.');
        return of(null);
      })
    ).subscribe(p => {
      this.registerLoading.set(false);
      if (p) {
        this.showRegisterModal.set(false);
        this.actionMsg.set(`"${p.name}" agregado al catálogo correctamente.`);
        this.catalog.update(list => [p, ...list]);
      }
    });
  }

  // ── Admin: edición y desactivación de participante ──────────────────
  openEditModal(p: EcosystemParticipant) {
    this.editingParticipantId = p.id;
    this.editForm = {
      name: p.name,
      networkType: p.networkType,
      description: p.description || '',
      contactEmail: p.contactEmail || '',
      contactPhone: p.contactPhone || '',
      website: p.website || ''
    };
    this.showEditModal.set(true);
    this.clearMessages();
  }

  closeEditModal() {
    this.showEditModal.set(false);
    this.editingParticipantId = null;
  }

  submitEdit() {
    if (this.editingParticipantId == null) return;
    const f = this.editForm;
    if (!f.name.trim()) return;
    this.editLoading.set(true);
    this.clearMessages();
    this.svc.updateParticipant(this.editingParticipantId, f).pipe(
      catchError(err => {
        this.actionErr.set(err?.error?.message ?? 'Error al actualizar el participante.');
        return of(null);
      })
    ).subscribe(p => {
      this.editLoading.set(false);
      if (p) {
        this.showEditModal.set(false);
        this.actionMsg.set(`"${p.name}" actualizado en el catálogo correctamente.`);
        this.catalog.update(list => list.map(item => item.id === p.id ? p : item));
      }
    });
  }

  deleteParticipant(id: number, name: string) {
    if (!confirm(`¿Estás seguro de que deseas eliminar a "${name}" del catálogo?`)) {
      return;
    }
    this.actionLoading.set(true);
    this.clearMessages();
    this.svc.deleteParticipant(id).pipe(
      catchError(err => {
        this.actionErr.set(err?.error?.message ?? 'Error al eliminar el participante.');
        return of(null);
      })
    ).subscribe(() => {
      this.actionLoading.set(false);
      this.actionMsg.set(`Participante eliminado del catálogo.`);
      this.catalog.update(list => list.filter(item => item.id !== id));
    });
  }

  setRevokeReason(v: string)           { this.revokeReason.set(v); }

  networkLabel(t: NetworkType | string): string {
    const m: Record<string, string> = {
      FAMILIAR: 'Familiar', PROFESSIONAL: 'Profesional',
      INSTITUTIONAL: 'Institucional', COMMUNITY: 'Comunitaria', TERRITORIAL: 'Territorial'
    };
    return m[t] ?? t;
  }

  networkIcon(t: NetworkType | string): string {
    const m: Record<string, string> = {
      FAMILIAR: '👨‍👩‍👧', PROFESSIONAL: '🩺',
      INSTITUTIONAL: '🏛️', COMMUNITY: '🤝', TERRITORIAL: '🌍'
    };
    return m[t] ?? '🔗';
  }

  statusLabel(s: string): string {
    const m: Record<string, string> = {
      INVITED: 'Pendiente', ACTIVE: 'Activo', SUSPENDED: 'Suspendido', REVOKED: 'Revocado'
    };
    return m[s] ?? s;
  }

  statusIcon(s: string): string {
    const m: Record<string, string> = {
      INVITED: '⏳', ACTIVE: '✅', SUSPENDED: '⏸️', REVOKED: '🚫'
    };
    return m[s] ?? '•';
  }

  formatDate(iso: any): string {
    if (!iso) return '—';
    if (Array.isArray(iso)) {
      if (iso.length >= 3) {
        const year = iso[0];
        const month = String(iso[1]).padStart(2, '0');
        const day = String(iso[2]).padStart(2, '0');
        return `${day}/${month}/${year}`;
      }
      return '—';
    }
    const d = new Date(iso);
    if (isNaN(d.getTime())) {
      return String(iso);
    }
    return d.toLocaleDateString('es-CO', { day: '2-digit', month: 'short', year: 'numeric' });
  }

  scopeKeys(): (keyof EcosystemAccessScope)[] {
    return ['canViewIcfScore', 'canViewRiskLevel', 'canViewPlanSummary',
            'canViewSprintProgress', 'canViewCrisisHistory', 'canReceiveAlerts'];
  }

  scopeLabel(k: keyof EcosystemAccessScope): string {
    const m: Record<string, string> = {
      canViewIcfScore:       'ICF Score',
      canViewRiskLevel:      'Nivel de riesgo',
      canViewPlanSummary:    'Resumen del plan',
      canViewSprintProgress: 'Progreso sprint',
      canViewCrisisHistory:  'Historial de crisis',
      canReceiveAlerts:      'Recibir alertas'
    };
    return m[k] ?? k;
  }

  scopeIcon(k: keyof EcosystemAccessScope): string {
    const m: Record<string, string> = {
      canViewIcfScore:       '🔢',
      canViewRiskLevel:      '⚠️',
      canViewPlanSummary:    '📋',
      canViewSprintProgress: '🏃',
      canViewCrisisHistory:  '🔴',
      canReceiveAlerts:      '🔔'
    };
    return m[k] ?? '•';
  }

  allLinksInOrder(): EcosystemLink[] {
    const s = this.summary();
    if (!s) return [];
    return [...(s.familiar ?? []), ...(s.institutional ?? []), ...(s.community ?? []), ...(s.territorial ?? [])];
  }

  pendingLinks(): EcosystemLink[] {
    return this.allLinksInOrder().filter(l => l.status === 'INVITED');
  }

  activeLinks(): EcosystemLink[] {
    return this.allLinksInOrder().filter(l => l.status === 'ACTIVE');
  }

  activeScopeCount(link: EcosystemLink): number {
    if (!link.accessScope) return 0;
    return Object.values(link.accessScope).filter(Boolean).length;
  }

  getParticipantLink(participantId: number): EcosystemLink | undefined {
    const links = this.allLinksInOrder().filter(l => l.participant?.id === participantId);
    if (links.length === 0) return undefined;
    const activeOrInvited = links.find(l => l.status !== 'REVOKED');
    return activeOrInvited || links[0];
  }
}
