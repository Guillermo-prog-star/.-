import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, ActivatedRoute } from '@angular/router';
import { HttpClient } from '@angular/common/http';
import { GuardianService } from '../../core/services/guardian.service';
import { TransformationFlowService } from '../../core/services/transformation-flow.service';
import { GuardianStatusResponse, VoteCount } from '../../core/models/models';
import { environment } from '../../../environments/environment';

interface MemberCandidate {
  memberId: number;
  fullName: string;
  email: string;
  role: string;
  votes: number;
}

@Component({
  selector: 'app-guardian-election',
  standalone: true,
  imports: [CommonModule],
  template: `
<div class="guardian-election-shell">

  <!-- Fondo decorativo -->
  <div class="bg-decoration">
    <div class="circle c1"></div>
    <div class="circle c2"></div>
    <div class="circle c3"></div>
  </div>

  <div class="election-card" *ngIf="!loading && status">

    <!-- Guardián ya elegido -->
    <ng-container *ngIf="status.hasGuardian; else electionFlow">
      <div class="guardian-confirmed">
        <div class="confirmed-icon">🌱</div>
        <h2>¡Guardián Familiar elegido!</h2>
        <div class="guardian-badge">
          <span class="guardian-name">{{ status.guardianFullName }}</span>
          <span class="guardian-label">Guardián Familiar</span>
        </div>
        <p class="confirmed-desc">
          Gracias por elegir quién guiará la evolución de su familia.
          Esta persona impulsará el bienestar de todos con amor y compromiso.
        </p>
        <button class="btn-primary" (click)="goToDashboard()">Comenzar la aventura →</button>
      </div>
    </ng-container>

    <!-- Flujo de elección -->
    <ng-template #electionFlow>
      <div class="election-header">
        <div class="header-icon">🏡</div>
        <h1>¿Quién será el <span class="highlight">Guardián Familiar</span>?</h1>
        <p class="subtitle">
          El Guardián motiva, acompaña y ayuda a mantener viva la evolución de la familia.
          <strong>No controla a nadie.</strong> Solo impulsa el bienestar y la participación.
        </p>
      </div>

      <!-- Lista de candidatos -->
      <div class="members-grid" *ngIf="members.length > 0">
        <div
          class="member-card"
          *ngFor="let m of members"
          [class.selected]="selectedMemberId === m.memberId"
          [class.leading]="isLeading(m)"
          (click)="selectCandidate(m.memberId)">

          <div class="member-avatar">{{ getInitials(m.fullName) }}</div>
          <div class="member-info">
            <span class="member-name">{{ m.fullName }}</span>
            <span class="vote-count" *ngIf="m.votes > 0">
              {{ m.votes }} voto{{ m.votes !== 1 ? 's' : '' }} 🗳️
            </span>
          </div>
          <div class="leading-badge" *ngIf="isLeading(m)">🌟 Preferido</div>
        </div>
      </div>

      <p class="vote-hint" *ngIf="status.currentUserHasVoted">
        ✓ Ya votaste. Puedes cambiar tu voto cuando quieras.
      </p>

      <!-- Acciones -->
      <div class="actions">
        <button
          class="btn-vote"
          [disabled]="!selectedMemberId || voting"
          (click)="castVote()">
          {{ voting ? 'Votando...' : '🗳️ Votar por este miembro' }}
        </button>

        <button
          class="btn-confirm"
          *ngIf="canConfirm()"
          (click)="confirmGuardian()">
          ✅ Confirmar como Guardián
        </button>

        <button class="btn-skip" (click)="goToDashboard()">
          Decidir más tarde →
        </button>
      </div>

      <!-- Barra de progreso de votos -->
      <div class="votes-summary" *ngIf="status.totalVotes > 0">
        <span class="votes-label">{{ status.totalVotes }} de {{ totalMembers }} votos emitidos</span>
        <div class="progress-bar">
          <div class="progress-fill"
               [style.width.%]="(status.totalVotes / totalMembers) * 100"></div>
        </div>
      </div>
    </ng-template>

  </div>

  <!-- Loader -->
  <div class="loader" *ngIf="loading">
    <div class="spinner"></div>
    <p>Cargando familia...</p>
  </div>

</div>
  `,
  styles: [`
    .guardian-election-shell {
      min-height: 100vh;
      display: flex;
      align-items: center;
      justify-content: center;
      background: linear-gradient(135deg, #0f172a 0%, #1e1b4b 50%, #0f172a 100%);
      position: relative;
      overflow: hidden;
      padding: 2rem;
    }
    .bg-decoration .circle {
      position: absolute; border-radius: 50%; opacity: 0.08;
      background: radial-gradient(circle, #818cf8, transparent);
    }
    .c1 { width: 500px; height: 500px; top: -100px; right: -100px; }
    .c2 { width: 300px; height: 300px; bottom: -50px; left: -50px; }
    .c3 { width: 200px; height: 200px; top: 50%; left: 50%; transform: translate(-50%,-50%); }

    .election-card {
      background: rgba(30,27,75,0.85);
      backdrop-filter: blur(20px);
      border: 1px solid rgba(129,140,248,0.2);
      border-radius: 24px;
      padding: 3rem 2.5rem;
      max-width: 680px;
      width: 100%;
      position: relative;
      z-index: 1;
    }

    .election-header { text-align: center; margin-bottom: 2.5rem; }
    .header-icon { font-size: 3.5rem; margin-bottom: 1rem; }
    h1 { font-size: 1.75rem; font-weight: 700; color: #e2e8f0; margin: 0 0 1rem; line-height: 1.3; }
    .highlight { color: #818cf8; }
    .subtitle { color: #94a3b8; font-size: 0.95rem; line-height: 1.6; }
    .subtitle strong { color: #c7d2fe; }

    .members-grid {
      display: grid;
      grid-template-columns: repeat(auto-fill, minmax(180px, 1fr));
      gap: 1rem;
      margin-bottom: 1.5rem;
    }
    .member-card {
      background: rgba(15,23,42,0.6);
      border: 2px solid rgba(99,102,241,0.2);
      border-radius: 16px;
      padding: 1.25rem 1rem;
      cursor: pointer;
      transition: all 0.2s ease;
      text-align: center;
      position: relative;
    }
    .member-card:hover { border-color: rgba(129,140,248,0.5); transform: translateY(-2px); }
    .member-card.selected {
      border-color: #818cf8;
      background: rgba(129,140,248,0.12);
      box-shadow: 0 0 20px rgba(129,140,248,0.2);
    }
    .member-card.leading { border-color: #fbbf24; }
    .member-avatar {
      width: 52px; height: 52px;
      background: linear-gradient(135deg, #6366f1, #8b5cf6);
      border-radius: 50%;
      display: flex; align-items: center; justify-content: center;
      font-size: 1.1rem; font-weight: 700; color: white;
      margin: 0 auto 0.75rem;
    }
    .member-name { display: block; color: #e2e8f0; font-weight: 600; font-size: 0.9rem; }
    .vote-count  { display: block; color: #818cf8; font-size: 0.8rem; margin-top: 0.25rem; }
    .leading-badge {
      position: absolute; top: -10px; right: -10px;
      background: #fbbf24; color: #1e1b4b;
      font-size: 0.7rem; font-weight: 700;
      padding: 2px 8px; border-radius: 20px;
    }

    .vote-hint { text-align: center; color: #6ee7b7; font-size: 0.85rem; margin-bottom: 1rem; }

    .actions { display: flex; flex-direction: column; gap: 0.75rem; margin-bottom: 1.5rem; }
    button {
      width: 100%; padding: 0.85rem 1.5rem;
      border: none; border-radius: 12px;
      font-weight: 600; font-size: 0.95rem;
      cursor: pointer; transition: all 0.2s;
    }
    .btn-vote {
      background: linear-gradient(135deg, #6366f1, #8b5cf6);
      color: white;
    }
    .btn-vote:disabled { opacity: 0.4; cursor: not-allowed; }
    .btn-vote:not(:disabled):hover { transform: translateY(-1px); box-shadow: 0 4px 20px rgba(99,102,241,0.4); }
    .btn-confirm {
      background: linear-gradient(135deg, #10b981, #059669);
      color: white;
    }
    .btn-primary {
      background: linear-gradient(135deg, #6366f1, #8b5cf6);
      color: white; margin-top: 1.5rem;
    }
    .btn-skip {
      background: transparent; border: 1px solid rgba(148,163,184,0.3);
      color: #64748b;
    }
    .btn-skip:hover { color: #94a3b8; }

    .votes-summary { text-align: center; }
    .votes-label { color: #64748b; font-size: 0.8rem; display: block; margin-bottom: 0.5rem; }
    .progress-bar {
      height: 6px; background: rgba(99,102,241,0.15);
      border-radius: 99px; overflow: hidden;
    }
    .progress-fill {
      height: 100%; background: linear-gradient(90deg, #6366f1, #8b5cf6);
      border-radius: 99px; transition: width 0.4s ease;
    }

    /* Guardián confirmado */
    .guardian-confirmed { text-align: center; }
    .confirmed-icon { font-size: 4rem; margin-bottom: 1rem; }
    h2 { font-size: 1.6rem; font-weight: 700; color: #6ee7b7; margin-bottom: 1.5rem; }
    .guardian-badge {
      display: inline-flex; flex-direction: column; align-items: center;
      background: rgba(99,102,241,0.15);
      border: 1px solid rgba(129,140,248,0.3);
      border-radius: 16px; padding: 1rem 2rem; margin-bottom: 1.5rem;
    }
    .guardian-name { font-size: 1.4rem; font-weight: 700; color: #c7d2fe; }
    .guardian-label { font-size: 0.8rem; color: #818cf8; margin-top: 0.25rem; text-transform: uppercase; letter-spacing: 1px; }
    .confirmed-desc { color: #94a3b8; line-height: 1.6; margin-bottom: 1rem; }

    /* Loader */
    .loader { text-align: center; color: #64748b; }
    .spinner {
      width: 40px; height: 40px;
      border: 3px solid rgba(99,102,241,0.2);
      border-top-color: #6366f1;
      border-radius: 50%;
      animation: spin 0.8s linear infinite;
      margin: 0 auto 1rem;
    }
    @keyframes spin { to { transform: rotate(360deg); } }
  `]
})
export class GuardianElectionComponent implements OnInit {

  familyId!: number;
  currentMemberId?: number;
  status?: GuardianStatusResponse;
  members: MemberCandidate[] = [];   // Todos los miembros de la familia
  selectedMemberId?: number;
  voting = false;
  loading = true;

  private flow = inject(TransformationFlowService);

  constructor(
    private guardianSvc: GuardianService,
    private router: Router,
    private route: ActivatedRoute,
    private http: HttpClient
  ) {}

  ngOnInit() {
    this.familyId = Number(this.route.snapshot.paramMap.get('familyId'));
    const memberParam = this.route.snapshot.queryParamMap.get('memberId');
    if (memberParam) this.currentMemberId = Number(memberParam);
    this.loadFamilyMembers();
  }

  /** Carga los miembros reales de la familia y luego el estado del guardián */
  loadFamilyMembers() {
    this.loading = true;
    this.http.get<any>(`${environment.apiBaseUrl}/families/${this.familyId}`).subscribe({
      next: res => {
        const family = res?.data ?? res;
        const rawMembers: any[] = family?.members ?? [];

        // Resolver el currentMemberId desde el email del usuario autenticado
        if (!this.currentMemberId) {
          try {
            const authUser = JSON.parse(localStorage.getItem('auth_user') || '{}');
            const matched = rawMembers.find((m: any) => m.email === authUser.email);
            if (matched) this.currentMemberId = matched.id;
          } catch { /* ignorar */ }
        }

        // Construir candidatos con votos en 0 por defecto
        this.members = rawMembers.map((m: any) => ({
          memberId: m.id,
          fullName: m.fullName,
          email: m.email ?? '',
          role: m.role ?? '',
          votes: 0
        }));

        this.loadStatus();
      },
      error: () => this.loadStatus()   // Si falla, intenta solo con el estado
    });
  }

  loadStatus() {
    this.guardianSvc.getStatus(this.familyId, this.currentMemberId).subscribe({
      next: s => {
        this.status = s;
        // Combinar: actualizar votos en la lista de miembros existente
        s.voteCounts.forEach(vc => {
          const m = this.members.find(x => x.memberId === vc.memberId);
          if (m) m.votes = vc.votes;
          else this.members.push({ memberId: vc.memberId, fullName: vc.fullName, email: '', role: '', votes: vc.votes });
        });
        this.loading = false;
      },
      error: () => { this.loading = false; }
    });
  }

  get totalMembers(): number {
    return this.members.length || 1;
  }

  selectCandidate(id: number) {
    this.selectedMemberId = id;
  }

  isLeading(m: MemberCandidate): boolean {
    if (this.members.length === 0) return false;
    const max = Math.max(...this.members.map(x => x.votes));
    return m.votes === max && max > 0;
  }

  getInitials(name: string): string {
    return name.split(' ').slice(0, 2).map(p => p[0]).join('').toUpperCase();
  }

  castVote() {
    if (!this.selectedMemberId || !this.currentMemberId) return;
    this.voting = true;
    this.guardianSvc.vote(this.familyId, {
      voterMemberId: this.currentMemberId,
      nominatedMemberId: this.selectedMemberId
    }).subscribe({
      next: s => {
        this.status = s;
        s.voteCounts.forEach(vc => {
          const m = this.members.find(x => x.memberId === vc.memberId);
          if (m) m.votes = vc.votes;
        });
        this.voting = false;
      },
      error: () => { this.voting = false; }
    });
  }

  canConfirm(): boolean {
    if (!this.selectedMemberId || !this.status) return false;
    const leading = this.members.find(m => m.memberId === this.selectedMemberId);
    return !!leading && leading.votes === Math.max(...this.members.map(m => m.votes));
  }

  confirmGuardian() {
    if (!this.selectedMemberId) return;
    this.guardianSvc.confirmGuardian(this.familyId, this.selectedMemberId).subscribe({
      next: s => {
        this.status = s;
        // Guardián elegido → avanzar onboarding al diagnóstico
        this.flow.advanceOnboarding('diagnosis');
      }
    });
  }

  goToDashboard() {
    // Si ya tenemos guardián, marcamos onboarding como diagnosis y vamos al diagnóstico
    if (this.status?.hasGuardian) {
      this.flow.advanceOnboarding('diagnosis');
      this.router.navigate(['/evaluations/start']);
    } else {
      this.router.navigate(['/dashboard']);
    }
  }
}
