package com.integrityfamily.support.service;

import com.integrityfamily.common.exception.BusinessException;
import com.integrityfamily.domain.Family;
import com.integrityfamily.domain.repository.FamilyRepository;
import com.integrityfamily.support.domain.*;
import com.integrityfamily.support.dto.SupportNetworkDtos.*;
import com.integrityfamily.support.repository.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("SupportNetworkService — Unit Tests")
class SupportNetworkServiceTest {

    @Mock FamilyRepository familyRepository;
    @Mock SupportNetworkMemberRepository memberRepository;
    @Mock FamilySupportAssignmentRepository assignmentRepository;
    @Mock SupportProfessionalNoteRepository noteRepository;

    @InjectMocks SupportNetworkService service;

    // ── Fixtures ──────────────────────────────────────────────────────────

    private static final Long FAMILY_ID = 1L;
    private static final Long MEMBER_ID = 10L;
    private static final Long ASSIGNMENT_ID = 100L;
    private static final String EMAIL = "familia@test.com";

    private Family family() {
        Family f = new Family();
        f.setId(FAMILY_ID);
        return f;
    }

    private SupportNetworkMember activeProfessional() {
        return SupportNetworkMember.builder()
                .id(MEMBER_ID)
                .fullName("Dra. Ana Torres")
                .email("ana@clinic.com")
                .specialty(SupportSpecialty.THERAPIST)
                .active(true)
                .build();
    }

    private SupportNetworkMember inactiveProfessional() {
        return SupportNetworkMember.builder()
                .id(MEMBER_ID)
                .fullName("Dr. Inactivo")
                .email("inactivo@clinic.com")
                .specialty(SupportSpecialty.DOCTOR)
                .active(false)
                .build();
    }

    private FamilySupportAssignment invitedAssignment(SupportNetworkMember pro) {
        return FamilySupportAssignment.builder()
                .id(ASSIGNMENT_ID)
                .familyId(FAMILY_ID)
                .supportMember(pro)
                .specialty(pro.getSpecialty())
                .status(AssignmentStatus.INVITED)
                .invitedByEmail(EMAIL)
                .invitedAt(LocalDateTime.now())
                .canLeaveNotes(true)
                .build();
    }

    private FamilySupportAssignment activeAssignment(SupportNetworkMember pro) {
        FamilySupportAssignment a = invitedAssignment(pro);
        a.setStatus(AssignmentStatus.ACTIVE);
        a.setConsentedByEmail(EMAIL);
        a.setConsentedAt(LocalDateTime.now());
        return a;
    }

    // ─────────────────────────────────────────────────────────────────────
    // registerProfessional()
    // ─────────────────────────────────────────────────────────────────────

    @Nested @DisplayName("registerProfessional()")
    class RegisterProfessional {

        @Test @DisplayName("registra un nuevo profesional correctamente")
        void registra_profesional_nuevo() {
            RegisterProfessionalRequest req = new RegisterProfessionalRequest();
            req.setFullName("Dra. Ana Torres");
            req.setEmail("ana@clinic.com");
            req.setSpecialty(SupportSpecialty.THERAPIST);

            when(memberRepository.existsByEmail("ana@clinic.com")).thenReturn(false);
            when(memberRepository.save(any())).thenAnswer(inv -> {
                SupportNetworkMember m = inv.getArgument(0);
                m.setId(MEMBER_ID);
                return m;
            });

            ProfessionalResponse resp = service.registerProfessional(req);

            assertThat(resp.getFullName()).isEqualTo("Dra. Ana Torres");
            assertThat(resp.getSpecialty()).isEqualTo(SupportSpecialty.THERAPIST);
            verify(memberRepository).save(any());
        }

        @Test @DisplayName("lanza CONFLICT si el email ya existe")
        void lanza_conflict_si_email_duplicado() {
            RegisterProfessionalRequest req = new RegisterProfessionalRequest();
            req.setEmail("duplicado@clinic.com");
            req.setSpecialty(SupportSpecialty.SOCIAL_WORKER);

            when(memberRepository.existsByEmail("duplicado@clinic.com")).thenReturn(true);

            assertThatThrownBy(() -> service.registerProfessional(req))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("Ya existe");
        }
    }

    // ─────────────────────────────────────────────────────────────────────
    // listProfessionals()
    // ─────────────────────────────────────────────────────────────────────

    @Nested @DisplayName("listProfessionals()")
    class ListProfessionals {

        @Test @DisplayName("sin filtro devuelve todos los activos")
        void sin_filtro_devuelve_activos() {
            when(memberRepository.findByActiveTrue()).thenReturn(List.of(activeProfessional()));

            List<ProfessionalResponse> list = service.listProfessionals(null);

            assertThat(list).hasSize(1);
            assertThat(list.get(0).getSpecialty()).isEqualTo(SupportSpecialty.THERAPIST);
        }

        @Test @DisplayName("con filtro de specialty llama al método correspondiente")
        void con_filtro_specialty_filtra() {
            when(memberRepository.findBySpecialtyAndActiveTrue(SupportSpecialty.DOCTOR))
                    .thenReturn(List.of());

            List<ProfessionalResponse> list = service.listProfessionals(SupportSpecialty.DOCTOR);

            assertThat(list).isEmpty();
            verify(memberRepository).findBySpecialtyAndActiveTrue(SupportSpecialty.DOCTOR);
            verify(memberRepository, never()).findByActiveTrue();
        }
    }

    // ─────────────────────────────────────────────────────────────────────
    // invite()
    // ─────────────────────────────────────────────────────────────────────

    @Nested @DisplayName("invite()")
    class Invite {

        @Test @DisplayName("la familia invita a un profesional activo correctamente")
        void invita_profesional_activo() {
            InviteRequest req = new InviteRequest();
            req.setSupportMemberId(MEMBER_ID);

            when(familyRepository.findById(FAMILY_ID)).thenReturn(Optional.of(family()));
            when(memberRepository.findById(MEMBER_ID)).thenReturn(Optional.of(activeProfessional()));
            when(assignmentRepository.existsByFamilyIdAndSupportMemberIdAndStatusNot(
                    FAMILY_ID, MEMBER_ID, AssignmentStatus.REVOKED)).thenReturn(false);
            when(assignmentRepository.save(any())).thenAnswer(inv -> {
                FamilySupportAssignment a = inv.getArgument(0);
                a.setId(ASSIGNMENT_ID);
                return a;
            });

            AssignmentResponse resp = service.invite(FAMILY_ID, req, EMAIL);

            assertThat(resp.getStatus()).isEqualTo(AssignmentStatus.INVITED);
            assertThat(resp.getInvitedByEmail()).isEqualTo(EMAIL);
        }

        @Test @DisplayName("invitar con scope personalizado aplica los permisos")
        void invita_con_scope_personalizado() {
            InviteRequest req = new InviteRequest();
            req.setSupportMemberId(MEMBER_ID);
            AccessScopeDto scope = new AccessScopeDto();
            scope.setCanViewCrisisHistory(true);
            scope.setCanViewPlanSummary(true);
            req.setAccessScope(scope);

            when(familyRepository.findById(FAMILY_ID)).thenReturn(Optional.of(family()));
            when(memberRepository.findById(MEMBER_ID)).thenReturn(Optional.of(activeProfessional()));
            when(assignmentRepository.existsByFamilyIdAndSupportMemberIdAndStatusNot(any(), any(), any())).thenReturn(false);
            when(assignmentRepository.save(any())).thenAnswer(inv -> {
                FamilySupportAssignment a = inv.getArgument(0);
                a.setId(ASSIGNMENT_ID);
                return a;
            });

            AssignmentResponse resp = service.invite(FAMILY_ID, req, EMAIL);

            assertThat(resp.getAccessScope().isCanViewCrisisHistory()).isTrue();
            assertThat(resp.getAccessScope().isCanViewPlanSummary()).isTrue();
        }

        @Test @DisplayName("lanza NOT_FOUND si la familia no existe")
        void lanza_not_found_familia() {
            InviteRequest req = new InviteRequest();
            req.setSupportMemberId(MEMBER_ID);
            when(familyRepository.findById(FAMILY_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.invite(FAMILY_ID, req, EMAIL))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("Familia no encontrada");
        }

        @Test @DisplayName("lanza NOT_FOUND si el profesional no existe")
        void lanza_not_found_profesional() {
            InviteRequest req = new InviteRequest();
            req.setSupportMemberId(99L);
            when(familyRepository.findById(FAMILY_ID)).thenReturn(Optional.of(family()));
            when(memberRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.invite(FAMILY_ID, req, EMAIL))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("Profesional no encontrado");
        }

        @Test @DisplayName("lanza UNPROCESSABLE_ENTITY si el profesional está inactivo")
        void lanza_error_profesional_inactivo() {
            InviteRequest req = new InviteRequest();
            req.setSupportMemberId(MEMBER_ID);
            when(familyRepository.findById(FAMILY_ID)).thenReturn(Optional.of(family()));
            when(memberRepository.findById(MEMBER_ID)).thenReturn(Optional.of(inactiveProfessional()));

            assertThatThrownBy(() -> service.invite(FAMILY_ID, req, EMAIL))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("no está disponible");
        }

        @Test @DisplayName("lanza CONFLICT si ya hay asignación activa o pendiente")
        void lanza_conflict_asignacion_duplicada() {
            InviteRequest req = new InviteRequest();
            req.setSupportMemberId(MEMBER_ID);
            when(familyRepository.findById(FAMILY_ID)).thenReturn(Optional.of(family()));
            when(memberRepository.findById(MEMBER_ID)).thenReturn(Optional.of(activeProfessional()));
            when(assignmentRepository.existsByFamilyIdAndSupportMemberIdAndStatusNot(
                    FAMILY_ID, MEMBER_ID, AssignmentStatus.REVOKED)).thenReturn(true);

            assertThatThrownBy(() -> service.invite(FAMILY_ID, req, EMAIL))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("ya tiene una asignación activa");
        }
    }

    // ─────────────────────────────────────────────────────────────────────
    // giveConsent()
    // ─────────────────────────────────────────────────────────────────────

    @Nested @DisplayName("giveConsent()")
    class GiveConsent {

        @Test @DisplayName("activa una invitación pendiente con consentimiento")
        void activa_invitacion_pendiente() {
            ConsentRequest req = new ConsentRequest();
            req.setAssignmentId(ASSIGNMENT_ID);

            FamilySupportAssignment assignment = invitedAssignment(activeProfessional());
            when(assignmentRepository.findById(ASSIGNMENT_ID)).thenReturn(Optional.of(assignment));
            when(assignmentRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            AssignmentResponse resp = service.giveConsent(FAMILY_ID, req, EMAIL);

            assertThat(resp.getStatus()).isEqualTo(AssignmentStatus.ACTIVE);
            assertThat(resp.getConsentedByEmail()).isEqualTo(EMAIL);
        }

        @Test @DisplayName("la familia puede ajustar el scope al consentir")
        void puede_ajustar_scope_al_consentir() {
            ConsentRequest req = new ConsentRequest();
            req.setAssignmentId(ASSIGNMENT_ID);
            AccessScopeDto scope = new AccessScopeDto();
            scope.setCanViewCrisisHistory(true);
            scope.setCanViewPlanSummary(false);
            req.setAccessScope(scope);

            FamilySupportAssignment assignment = invitedAssignment(activeProfessional());
            when(assignmentRepository.findById(ASSIGNMENT_ID)).thenReturn(Optional.of(assignment));
            when(assignmentRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            AssignmentResponse resp = service.giveConsent(FAMILY_ID, req, EMAIL);

            assertThat(resp.getAccessScope().isCanViewCrisisHistory()).isTrue();
            assertThat(resp.getAccessScope().isCanViewPlanSummary()).isFalse();
        }

        @Test @DisplayName("lanza error si la asignación no está en estado INVITED")
        void lanza_error_si_no_es_invited() {
            ConsentRequest req = new ConsentRequest();
            req.setAssignmentId(ASSIGNMENT_ID);

            FamilySupportAssignment assignment = activeAssignment(activeProfessional());
            when(assignmentRepository.findById(ASSIGNMENT_ID)).thenReturn(Optional.of(assignment));

            assertThatThrownBy(() -> service.giveConsent(FAMILY_ID, req, EMAIL))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("Solo se puede consentir");
        }

        @Test @DisplayName("lanza FORBIDDEN si la asignación no pertenece a la familia")
        void lanza_forbidden_si_familia_distinta() {
            ConsentRequest req = new ConsentRequest();
            req.setAssignmentId(ASSIGNMENT_ID);

            FamilySupportAssignment assignment = invitedAssignment(activeProfessional());
            assignment.setFamilyId(999L); // otra familia
            when(assignmentRepository.findById(ASSIGNMENT_ID)).thenReturn(Optional.of(assignment));

            assertThatThrownBy(() -> service.giveConsent(FAMILY_ID, req, EMAIL))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("No autorizado");
        }
    }

    // ─────────────────────────────────────────────────────────────────────
    // revoke()
    // ─────────────────────────────────────────────────────────────────────

    @Nested @DisplayName("revoke()")
    class Revoke {

        @Test @DisplayName("la familia puede revocar un acceso activo")
        void revoca_acceso_activo() {
            RevokeRequest req = new RevokeRequest();
            req.setAssignmentId(ASSIGNMENT_ID);
            req.setReason("Decisión de la familia");

            FamilySupportAssignment assignment = activeAssignment(activeProfessional());
            when(assignmentRepository.findById(ASSIGNMENT_ID)).thenReturn(Optional.of(assignment));
            when(assignmentRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            AssignmentResponse resp = service.revoke(FAMILY_ID, req, EMAIL);

            assertThat(resp.getStatus()).isEqualTo(AssignmentStatus.REVOKED);
            assertThat(assignment.getRevokedByEmail()).isEqualTo(EMAIL);
            assertThat(assignment.getRevocationReason()).isEqualTo("Decisión de la familia");
        }

        @Test @DisplayName("la familia puede revocar una invitación pendiente")
        void revoca_invitacion_pendiente() {
            RevokeRequest req = new RevokeRequest();
            req.setAssignmentId(ASSIGNMENT_ID);

            FamilySupportAssignment assignment = invitedAssignment(activeProfessional());
            when(assignmentRepository.findById(ASSIGNMENT_ID)).thenReturn(Optional.of(assignment));
            when(assignmentRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            AssignmentResponse resp = service.revoke(FAMILY_ID, req, EMAIL);

            assertThat(resp.getStatus()).isEqualTo(AssignmentStatus.REVOKED);
        }

        @Test @DisplayName("lanza error si ya fue revocado")
        void lanza_error_si_ya_revocado() {
            RevokeRequest req = new RevokeRequest();
            req.setAssignmentId(ASSIGNMENT_ID);

            FamilySupportAssignment assignment = activeAssignment(activeProfessional());
            assignment.setStatus(AssignmentStatus.REVOKED);
            when(assignmentRepository.findById(ASSIGNMENT_ID)).thenReturn(Optional.of(assignment));

            assertThatThrownBy(() -> service.revoke(FAMILY_ID, req, EMAIL))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("ya fue revocado");
        }

        @Test @DisplayName("la revocación no requiere motivo — reason puede ser null")
        void revocacion_sin_motivo_es_valida() {
            RevokeRequest req = new RevokeRequest();
            req.setAssignmentId(ASSIGNMENT_ID);
            req.setReason(null);

            FamilySupportAssignment assignment = activeAssignment(activeProfessional());
            when(assignmentRepository.findById(ASSIGNMENT_ID)).thenReturn(Optional.of(assignment));
            when(assignmentRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            assertThatCode(() -> service.revoke(FAMILY_ID, req, EMAIL))
                    .doesNotThrowAnyException();
        }
    }

    // ─────────────────────────────────────────────────────────────────────
    // addNote()
    // ─────────────────────────────────────────────────────────────────────

    @Nested @DisplayName("addNote()")
    class AddNote {

        private SupportProfessionalNote note(Long assignmentId) {
            return SupportProfessionalNote.builder()
                    .id(200L)
                    .assignmentId(assignmentId)
                    .familyId(FAMILY_ID)
                    .supportMemberId(MEMBER_ID)
                    .content("Observación clínica")
                    .visibleToFamily(true)
                    .createdAt(LocalDateTime.now())
                    .build();
        }

        @Test @DisplayName("el profesional deja una nota en asignación activa")
        void profesional_deja_nota_activa() {
            AddNoteRequest req = new AddNoteRequest();
            req.setAssignmentId(ASSIGNMENT_ID);
            req.setContent("Observación clínica");
            req.setVisibleToFamily(true);

            FamilySupportAssignment assignment = activeAssignment(activeProfessional());
            when(assignmentRepository.findById(ASSIGNMENT_ID)).thenReturn(Optional.of(assignment));
            when(noteRepository.save(any())).thenReturn(note(ASSIGNMENT_ID));

            NoteResponse resp = service.addNote(FAMILY_ID, req, MEMBER_ID);

            assertThat(resp.getContent()).isEqualTo("Observación clínica");
            assertThat(resp.isVisibleToFamily()).isTrue();
        }

        @Test @DisplayName("lanza FORBIDDEN si la asignación no está activa")
        void lanza_forbidden_asignacion_no_activa() {
            AddNoteRequest req = new AddNoteRequest();
            req.setAssignmentId(ASSIGNMENT_ID);

            FamilySupportAssignment assignment = invitedAssignment(activeProfessional());
            when(assignmentRepository.findById(ASSIGNMENT_ID)).thenReturn(Optional.of(assignment));

            assertThatThrownBy(() -> service.addNote(FAMILY_ID, req, MEMBER_ID))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("Solo se pueden agregar notas en asignaciones activas");
        }

        @Test @DisplayName("lanza FORBIDDEN si el profesional no tiene permiso de notas")
        void lanza_forbidden_sin_permiso_notas() {
            AddNoteRequest req = new AddNoteRequest();
            req.setAssignmentId(ASSIGNMENT_ID);

            SupportNetworkMember pro = activeProfessional();
            FamilySupportAssignment assignment = activeAssignment(pro);
            assignment.setCanLeaveNotes(false);
            when(assignmentRepository.findById(ASSIGNMENT_ID)).thenReturn(Optional.of(assignment));

            assertThatThrownBy(() -> service.addNote(FAMILY_ID, req, MEMBER_ID))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("no tiene permiso para dejar notas");
        }

        @Test @DisplayName("lanza FORBIDDEN si el memberId no coincide con la asignación")
        void lanza_forbidden_si_memberid_no_coincide() {
            AddNoteRequest req = new AddNoteRequest();
            req.setAssignmentId(ASSIGNMENT_ID);

            FamilySupportAssignment assignment = activeAssignment(activeProfessional());
            when(assignmentRepository.findById(ASSIGNMENT_ID)).thenReturn(Optional.of(assignment));

            // supportMemberId 999 ≠ MEMBER_ID 10
            assertThatThrownBy(() -> service.addNote(FAMILY_ID, req, 999L))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("No autorizado para esta asignación");
        }
    }

    // ─────────────────────────────────────────────────────────────────────
    // getSummary()
    // ─────────────────────────────────────────────────────────────────────

    @Nested @DisplayName("getSummary()")
    class GetSummary {

        @Test @DisplayName("devuelve resumen con conteos correctos")
        void devuelve_resumen_con_conteos() {
            SupportNetworkMember pro = activeProfessional();
            FamilySupportAssignment active = activeAssignment(pro);
            FamilySupportAssignment invited = invitedAssignment(pro);

            when(familyRepository.findById(FAMILY_ID)).thenReturn(Optional.of(family()));
            when(assignmentRepository.findByFamilyId(FAMILY_ID)).thenReturn(List.of(active, invited));
            when(noteRepository.findByFamilyIdAndVisibleToFamilyTrueOrderByCreatedAtDesc(FAMILY_ID))
                    .thenReturn(List.of());

            FamilySupportSummary summary = service.getSummary(FAMILY_ID);

            assertThat(summary.getFamilyId()).isEqualTo(FAMILY_ID);
            assertThat(summary.getTotalProfessionals()).isEqualTo(2);
            assertThat(summary.getActiveProfessionals()).isEqualTo(1);
            assertThat(summary.getAssignments()).hasSize(2);
        }

        @Test @DisplayName("lanza NOT_FOUND si la familia no existe")
        void lanza_not_found_familia() {
            when(familyRepository.findById(FAMILY_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.getSummary(FAMILY_ID))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("Familia no encontrada");
        }
    }

    // ─────────────────────────────────────────────────────────────────────
    // getActive()
    // ─────────────────────────────────────────────────────────────────────

    @Nested @DisplayName("getActive()")
    class GetActive {

        @Test @DisplayName("devuelve solo las asignaciones en estado ACTIVE")
        void devuelve_solo_activas() {
            FamilySupportAssignment active = activeAssignment(activeProfessional());

            when(assignmentRepository.findByFamilyIdAndStatus(FAMILY_ID, AssignmentStatus.ACTIVE))
                    .thenReturn(List.of(active));

            List<AssignmentResponse> list = service.getActive(FAMILY_ID);

            assertThat(list).hasSize(1);
            assertThat(list.get(0).getStatus()).isEqualTo(AssignmentStatus.ACTIVE);
        }

        @Test @DisplayName("devuelve lista vacía si no hay activos")
        void devuelve_vacia_si_no_hay_activos() {
            when(assignmentRepository.findByFamilyIdAndStatus(FAMILY_ID, AssignmentStatus.ACTIVE))
                    .thenReturn(List.of());

            assertThat(service.getActive(FAMILY_ID)).isEmpty();
        }
    }

    // ─────────────────────────────────────────────────────────────────────
    // Flujo completo: invite → consent → note → revoke
    // ─────────────────────────────────────────────────────────────────────

    @Nested @DisplayName("Flujo completo de ciclo de vida")
    class FlujoCicloDeVida {

        @Test @DisplayName("invite → consent → revoke transiciona estados correctamente")
        void flujo_invite_consent_revoke() {
            SupportNetworkMember pro = activeProfessional();
            FamilySupportAssignment assignment = invitedAssignment(pro);

            // 1. invite
            InviteRequest invReq = new InviteRequest();
            invReq.setSupportMemberId(MEMBER_ID);

            when(familyRepository.findById(FAMILY_ID)).thenReturn(Optional.of(family()));
            when(memberRepository.findById(MEMBER_ID)).thenReturn(Optional.of(pro));
            when(assignmentRepository.existsByFamilyIdAndSupportMemberIdAndStatusNot(any(), any(), any())).thenReturn(false);
            when(assignmentRepository.save(any())).thenAnswer(inv -> {
                FamilySupportAssignment a = inv.getArgument(0);
                if (a.getId() == null) a.setId(ASSIGNMENT_ID);
                return a;
            });

            AssignmentResponse invited = service.invite(FAMILY_ID, invReq, EMAIL);
            assertThat(invited.getStatus()).isEqualTo(AssignmentStatus.INVITED);

            // 2. consent
            ConsentRequest conReq = new ConsentRequest();
            conReq.setAssignmentId(ASSIGNMENT_ID);

            assignment.setStatus(AssignmentStatus.INVITED);
            when(assignmentRepository.findById(ASSIGNMENT_ID)).thenReturn(Optional.of(assignment));

            AssignmentResponse consented = service.giveConsent(FAMILY_ID, conReq, EMAIL);
            assertThat(consented.getStatus()).isEqualTo(AssignmentStatus.ACTIVE);

            // 3. revoke
            RevokeRequest revReq = new RevokeRequest();
            revReq.setAssignmentId(ASSIGNMENT_ID);

            AssignmentResponse revoked = service.revoke(FAMILY_ID, revReq, EMAIL);
            assertThat(revoked.getStatus()).isEqualTo(AssignmentStatus.REVOKED);
        }
    }
}
