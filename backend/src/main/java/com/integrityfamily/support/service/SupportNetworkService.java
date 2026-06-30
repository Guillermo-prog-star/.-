package com.integrityfamily.support.service;

import com.integrityfamily.common.exception.BusinessException;
import com.integrityfamily.domain.*;
import com.integrityfamily.domain.repository.*;
import com.integrityfamily.support.domain.*;
import com.integrityfamily.support.dto.SupportNetworkDtos.*;
import com.integrityfamily.support.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Red de Apoyo Humano.
 *
 * Principio rector: la familia decide.
 * Ningún profesional externo accede a datos familiares sin consentimiento explícito.
 * Flujo: familia invita → familia consiente → profesional acompaña → familia revoca cuando decide.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SupportNetworkService {

    private final FamilyRepository familyRepository;
    private final SupportNetworkMemberRepository memberRepository;
    private final FamilySupportAssignmentRepository assignmentRepository;
    private final SupportProfessionalNoteRepository noteRepository;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final EvaluationRepository evaluationRepository;

    // ─────────────────────────────────────────────────────────────────────
    // Registro de profesionales (lo hace el admin o el propio profesional)
    // ─────────────────────────────────────────────────────────────────────

    @Transactional
    public ProfessionalResponse registerProfessional(RegisterProfessionalRequest req) {
        if (memberRepository.existsByEmail(req.getEmail())) {
            throw new BusinessException("Ya existe un profesional registrado con ese email.", "SUPPORT_CONFLICT", HttpStatus.CONFLICT);
        }

        SupportNetworkMember member = SupportNetworkMember.builder()
                .fullName(req.getFullName())
                .email(req.getEmail())
                .phone(req.getPhone())
                .specialty(req.getSpecialty())
                .licenseNumber(req.getLicenseNumber())
                .institutionName(req.getInstitutionName())
                .bio(req.getBio())
                .build();
        memberRepository.save(member);

        // Crear cuenta de usuario para que el profesional pueda autenticarse
        String roleName = resolveRoleName(req.getSpecialty());
        Role role = roleRepository.findByName(roleName)
                .orElseThrow(() -> new BusinessException("Rol profesional no configurado: " + roleName,
                        "ROLE_NOT_FOUND", HttpStatus.INTERNAL_SERVER_ERROR));

        String tempPassword = UUID.randomUUID().toString().substring(0, 12);

        if (!userRepository.existsByEmail(req.getEmail())) {
            User user = User.builder()
                    .email(req.getEmail())
                    .fullName(req.getFullName())
                    .passwordHash(passwordEncoder.encode(tempPassword))
                    .roles(new ArrayList<>(List.of(role)))
                    .enabled(true)
                    .build();
            userRepository.save(user);
            log.info("[SUPPORT] Cuenta creada para profesional {} con rol {}", req.getEmail(), roleName);

            ProfessionalResponse resp = toResponse(member);
            resp.setTemporaryPassword(tempPassword);
            return resp;
        }

        return toResponse(member);
    }

    private String resolveRoleName(SupportSpecialty specialty) {
        return switch (specialty) {
            case THERAPIST -> "ROLE_THERAPIST";
            case ORIENTADOR -> "ROLE_ORIENTADOR";
            case SOCIAL_WORKER -> "ROLE_SOCIAL_WORKER";
            default -> "ROLE_THERAPIST";
        };
    }

    @Transactional(readOnly = true)
    public List<ProfessionalResponse> listProfessionals(SupportSpecialty specialty) {
        List<SupportNetworkMember> members = specialty != null
                ? memberRepository.findBySpecialtyAndActiveTrue(specialty)
                : memberRepository.findByActiveTrue();
        return members.stream().map(this::toResponse).toList();
    }

    // ─────────────────────────────────────────────────────────────────────
    // La familia invita a un profesional
    // ─────────────────────────────────────────────────────────────────────

    @Transactional
    public AssignmentResponse invite(Long familyId, InviteRequest req, String invitedByEmail) {
        getFamily(familyId);

        SupportNetworkMember professional = memberRepository.findById(req.getSupportMemberId())
                .orElseThrow(() -> new BusinessException("Profesional no encontrado.", "SUPPORT_NOT_FOUND", HttpStatus.NOT_FOUND));

        if (!professional.isActive()) {
            throw new BusinessException("El profesional no está disponible.", "SUPPORT_UNPROCESSABLE_ENTITY", HttpStatus.UNPROCESSABLE_ENTITY);
        }

        // No permitir duplicado activo/invitado
        if (assignmentRepository.existsByFamilyIdAndSupportMemberIdAndStatusNot(
                familyId, professional.getId(), AssignmentStatus.REVOKED)) {
            throw new BusinessException("Este profesional ya tiene una asignación activa o pendiente para esta familia.",
                    "SUPPORT_CONFLICT", HttpStatus.CONFLICT);
        }

        AccessScopeDto scope = req.getAccessScope() != null ? req.getAccessScope() : new AccessScopeDto();

        FamilySupportAssignment assignment = FamilySupportAssignment.builder()
                .familyId(familyId)
                .supportMember(professional)
                .specialty(professional.getSpecialty())
                .status(AssignmentStatus.INVITED)
                .invitedByEmail(invitedByEmail)
                .invitedAt(LocalDateTime.now())
                .canViewIcfScore(scope.isCanViewIcfScore())
                .canViewRiskLevel(scope.isCanViewRiskLevel())
                .canViewPlanSummary(scope.isCanViewPlanSummary())
                .canViewSprintProgress(scope.isCanViewSprintProgress())
                .canViewCrisisHistory(scope.isCanViewCrisisHistory())
                .canLeaveNotes(scope.isCanLeaveNotes())
                .notes(req.getInitialNote())
                .build();

        FamilySupportAssignment saved = assignmentRepository.save(assignment);
        log.info("[SUPPORT] Familia {} invitó a {} ({})", familyId, professional.getFullName(), professional.getSpecialty());
        return toAssignmentResponse(saved);
    }

    // ─────────────────────────────────────────────────────────────────────
    // La familia otorga consentimiento explícito (activa el acceso)
    // ─────────────────────────────────────────────────────────────────────

    @Transactional
    public AssignmentResponse giveConsent(Long familyId, ConsentRequest req, String consentedByEmail) {
        FamilySupportAssignment assignment = getAssignment(req.getAssignmentId(), familyId);

        if (assignment.getStatus() != AssignmentStatus.INVITED) {
            throw new BusinessException("Solo se puede consentir una invitación pendiente.", "SUPPORT_UNPROCESSABLE_ENTITY", HttpStatus.UNPROCESSABLE_ENTITY);
        }

        // La familia puede ajustar el alcance al momento del consentimiento
        if (req.getAccessScope() != null) {
            AccessScopeDto scope = req.getAccessScope();
            assignment.setCanViewIcfScore(scope.isCanViewIcfScore());
            assignment.setCanViewRiskLevel(scope.isCanViewRiskLevel());
            assignment.setCanViewPlanSummary(scope.isCanViewPlanSummary());
            assignment.setCanViewSprintProgress(scope.isCanViewSprintProgress());
            assignment.setCanViewCrisisHistory(scope.isCanViewCrisisHistory());
            assignment.setCanLeaveNotes(scope.isCanLeaveNotes());
        }

        assignment.setStatus(AssignmentStatus.ACTIVE);
        assignment.setConsentedByEmail(consentedByEmail);
        assignment.setConsentedAt(LocalDateTime.now());

        log.info("[SUPPORT] Familia {} otorgó consentimiento a {} para asignación {}",
                familyId, assignment.getSupportMember().getFullName(), assignment.getId());
        return toAssignmentResponse(assignmentRepository.save(assignment));
    }

    // ─────────────────────────────────────────────────────────────────────
    // La familia revoca el acceso (en cualquier momento, sin justificación obligatoria)
    // ─────────────────────────────────────────────────────────────────────

    @Transactional
    public AssignmentResponse revoke(Long familyId, RevokeRequest req, String revokedByEmail) {
        FamilySupportAssignment assignment = getAssignment(req.getAssignmentId(), familyId);

        if (assignment.getStatus() == AssignmentStatus.REVOKED) {
            throw new BusinessException("El acceso ya fue revocado.", "SUPPORT_UNPROCESSABLE_ENTITY", HttpStatus.UNPROCESSABLE_ENTITY);
        }

        assignment.setStatus(AssignmentStatus.REVOKED);
        assignment.setRevokedByEmail(revokedByEmail);
        assignment.setRevokedAt(LocalDateTime.now());
        assignment.setRevocationReason(req.getReason());

        log.info("[SUPPORT] Familia {} revocó acceso de {} (asignación {})",
                familyId, assignment.getSupportMember().getFullName(), assignment.getId());
        return toAssignmentResponse(assignmentRepository.save(assignment));
    }

    // ─────────────────────────────────────────────────────────────────────
    // Profesional deja una nota clínica
    // ─────────────────────────────────────────────────────────────────────

    @Transactional
    public NoteResponse addNote(Long familyId, AddNoteRequest req, Long supportMemberId) {
        FamilySupportAssignment assignment = getAssignment(req.getAssignmentId(), familyId);

        if (assignment.getStatus() != AssignmentStatus.ACTIVE) {
            throw new BusinessException("Solo se pueden agregar notas en asignaciones activas.", "SUPPORT_FORBIDDEN", HttpStatus.FORBIDDEN);
        }
        if (!assignment.isCanLeaveNotes()) {
            throw new BusinessException("Este profesional no tiene permiso para dejar notas.", "SUPPORT_FORBIDDEN", HttpStatus.FORBIDDEN);
        }
        if (!assignment.getSupportMember().getId().equals(supportMemberId)) {
            throw new BusinessException("No autorizado para esta asignación.", "SUPPORT_FORBIDDEN", HttpStatus.FORBIDDEN);
        }

        SupportProfessionalNote note = SupportProfessionalNote.builder()
                .assignmentId(assignment.getId())
                .familyId(familyId)
                .supportMemberId(supportMemberId)
                .content(req.getContent())
                .visibleToFamily(req.isVisibleToFamily())
                .build();

        return toNoteResponse(noteRepository.save(note));
    }

    // ─────────────────────────────────────────────────────────────────────
    // Consultas
    // ─────────────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public FamilySupportSummary getSummary(Long familyId) {
        getFamily(familyId);
        List<FamilySupportAssignment> all = assignmentRepository.findByFamilyId(familyId);
        long active = all.stream().filter(a -> a.getStatus() == AssignmentStatus.ACTIVE).count();

        List<AssignmentResponse> responses = all.stream().map(a -> {
            AssignmentResponse r = toAssignmentResponse(a);
            if (a.getStatus() == AssignmentStatus.ACTIVE) {
                r.setVisibleNotes(noteRepository
                        .findByFamilyIdAndVisibleToFamilyTrueOrderByCreatedAtDesc(familyId)
                        .stream().map(this::toNoteResponse).toList());
            }
            return r;
        }).toList();

        return FamilySupportSummary.builder()
                .familyId(familyId)
                .totalProfessionals(all.size())
                .activeProfessionals((int) active)
                .assignments(responses)
                .build();
    }

    @Transactional(readOnly = true)
    public List<AssignmentResponse> getActive(Long familyId) {
        return assignmentRepository.findByFamilyIdAndStatus(familyId, AssignmentStatus.ACTIVE)
                .stream().map(this::toAssignmentResponse).toList();
    }

    // ─────────────────────────────────────────────────────────────────────
    // Panel del profesional: mis familias + perfil + vista de datos
    // ─────────────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<AssignmentResponse> getMyAssignments(String email) {
        SupportNetworkMember member = memberRepository.findByEmail(email)
                .orElseThrow(() -> new BusinessException(
                        "No existe un perfil profesional para este usuario.", "SUPPORT_NOT_FOUND", HttpStatus.NOT_FOUND));
        return assignmentRepository.findBySupportMemberIdAndStatus(member.getId(), AssignmentStatus.ACTIVE)
                .stream().map(this::toAssignmentResponse).toList();
    }

    @Transactional(readOnly = true)
    public ProfessionalResponse getMyProfile(String email) {
        SupportNetworkMember member = memberRepository.findByEmail(email)
                .orElseThrow(() -> new BusinessException(
                        "No existe un perfil profesional para este usuario.", "SUPPORT_NOT_FOUND", HttpStatus.NOT_FOUND));
        return toResponse(member);
    }

    @Transactional
    public ProfessionalResponse updateMyProfile(String email, UpdateProfileRequest req) {
        SupportNetworkMember member = memberRepository.findByEmail(email)
                .orElseThrow(() -> new BusinessException(
                        "No existe un perfil profesional para este usuario.", "SUPPORT_NOT_FOUND", HttpStatus.NOT_FOUND));
        if (req.getFullName() != null)       member.setFullName(req.getFullName());
        if (req.getPhone() != null)          member.setPhone(req.getPhone());
        if (req.getBio() != null)            member.setBio(req.getBio());
        if (req.getInstitutionName() != null) member.setInstitutionName(req.getInstitutionName());
        if (req.getLicenseNumber() != null)  member.setLicenseNumber(req.getLicenseNumber());
        return toResponse(memberRepository.save(member));
    }

    @Transactional(readOnly = true)
    public FamilyDataView getDataView(Long familyId, Long assignmentId, String email) {
        SupportNetworkMember member = memberRepository.findByEmail(email)
                .orElseThrow(() -> new BusinessException("Perfil profesional no encontrado.", "SUPPORT_NOT_FOUND", HttpStatus.NOT_FOUND));

        FamilySupportAssignment assignment = assignmentRepository.findById(assignmentId)
                .orElseThrow(() -> new BusinessException("Asignación no encontrada.", "SUPPORT_NOT_FOUND", HttpStatus.NOT_FOUND));

        if (!assignment.getFamilyId().equals(familyId) || !assignment.getSupportMember().getId().equals(member.getId())) {
            throw new BusinessException("No autorizado.", "SUPPORT_FORBIDDEN", HttpStatus.FORBIDDEN);
        }
        if (assignment.getStatus() != AssignmentStatus.ACTIVE) {
            throw new BusinessException("La asignación no está activa.", "SUPPORT_FORBIDDEN", HttpStatus.FORBIDDEN);
        }

        Family family = getFamily(familyId);
        FamilyDataView.FamilyDataViewBuilder view = FamilyDataView.builder()
                .familyId(familyId)
                .familyName(family.getName())
                .assignmentId(assignmentId)
                .specialty(assignment.getSpecialty())
                .sentinelActive(Boolean.TRUE.equals(family.getSentinelActive()));

        int level = 0;

        if (assignment.isCanViewIcfScore()) {
            level++;
            evaluationRepository.findTopByFamilyIdAndStatusOrderByFinalizedAtDesc(familyId, EvaluationStatus.FINALIZED)
                    .ifPresent(ev -> {
                        view.icfScore(ev.getIcf() != null ? ev.getIcf().doubleValue() : null);
                        view.riskLevel(ev.getRiskLevel());
                        view.icfLabel(labelFromIcf(ev.getIcf()));
                        view.icfDirection("STABLE");
                    });
        }
        if (assignment.isCanViewRiskLevel()) {
            level++;
        }
        if (assignment.isCanViewPlanSummary()) {
            level++;
            view.planSummaryAvailable(true);
        }
        if (assignment.isCanViewSprintProgress()) {
            level++;
            view.hasActiveSprint(false);
        }
        if (assignment.isCanViewCrisisHistory()) {
            level++;
            view.crisisHistoryAvailable(true);
        }

        view.accessLevel(level);
        return view.build();
    }

    private String labelFromIcf(Number icf) {
        if (icf == null) return "Sin datos";
        double v = icf.doubleValue();
        if (v >= 80) return "Fortaleza";
        if (v >= 60) return "Creciendo";
        if (v >= 40) return "Atención";
        return "Crítico";
    }

    // ─────────────────────────────────────────────────────────────────────
    // Helpers privados
    // ─────────────────────────────────────────────────────────────────────

    private Family getFamily(Long familyId) {
        return familyRepository.findById(familyId)
                .orElseThrow(() -> new BusinessException("Familia no encontrada.", "SUPPORT_NOT_FOUND", HttpStatus.NOT_FOUND));
    }

    private FamilySupportAssignment getAssignment(Long assignmentId, Long familyId) {
        FamilySupportAssignment a = assignmentRepository.findById(assignmentId)
                .orElseThrow(() -> new BusinessException("Asignación no encontrada.", "SUPPORT_NOT_FOUND", HttpStatus.NOT_FOUND));
        if (!a.getFamilyId().equals(familyId)) {
            throw new BusinessException("No autorizado.", "SUPPORT_FORBIDDEN", HttpStatus.FORBIDDEN);
        }
        return a;
    }

    private ProfessionalResponse toResponse(SupportNetworkMember m) {
        return ProfessionalResponse.builder()
                .id(m.getId()).fullName(m.getFullName()).email(m.getEmail())
                .specialty(m.getSpecialty()).licenseNumber(m.getLicenseNumber())
                .institutionName(m.getInstitutionName()).bio(m.getBio())
                .build();
    }

    private AssignmentResponse toAssignmentResponse(FamilySupportAssignment a) {
        AccessScopeDto scope = new AccessScopeDto();
        scope.setCanViewIcfScore(a.isCanViewIcfScore());
        scope.setCanViewRiskLevel(a.isCanViewRiskLevel());
        scope.setCanViewPlanSummary(a.isCanViewPlanSummary());
        scope.setCanViewSprintProgress(a.isCanViewSprintProgress());
        scope.setCanViewCrisisHistory(a.isCanViewCrisisHistory());
        scope.setCanLeaveNotes(a.isCanLeaveNotes());

        return AssignmentResponse.builder()
                .id(a.getId()).familyId(a.getFamilyId())
                .professional(toResponse(a.getSupportMember()))
                .specialty(a.getSpecialty()).status(a.getStatus())
                .invitedByEmail(a.getInvitedByEmail()).invitedAt(a.getInvitedAt())
                .consentedByEmail(a.getConsentedByEmail()).consentedAt(a.getConsentedAt())
                .accessScope(scope)
                .build();
    }

    private NoteResponse toNoteResponse(SupportProfessionalNote n) {
        return NoteResponse.builder()
                .id(n.getId()).content(n.getContent())
                .visibleToFamily(n.isVisibleToFamily()).createdAt(n.getCreatedAt())
                .build();
    }
}
