package com.integrityfamily.support.controller;

import com.integrityfamily.domain.Family;
import com.integrityfamily.domain.Role;
import com.integrityfamily.domain.User;
import com.integrityfamily.domain.repository.FamilyRepository;
import com.integrityfamily.domain.repository.RoleRepository;
import com.integrityfamily.domain.repository.UserRepository;
import com.integrityfamily.ecosystem.domain.EcosystemLinkStatus;
import com.integrityfamily.ecosystem.domain.EcosystemParticipant;
import com.integrityfamily.ecosystem.domain.FamilyEcosystemLink;
import com.integrityfamily.ecosystem.domain.NetworkType;
import com.integrityfamily.ecosystem.repository.EcosystemParticipantRepository;
import com.integrityfamily.ecosystem.repository.FamilyEcosystemLinkRepository;
import com.integrityfamily.security.JwtTokenProvider;
import com.integrityfamily.support.domain.AssignmentStatus;
import com.integrityfamily.support.domain.DraftStatus;
import com.integrityfamily.support.domain.FamilySupportAssignment;
import com.integrityfamily.support.domain.SupportNetworkMember;
import com.integrityfamily.support.domain.SupportSpecialty;
import com.integrityfamily.support.repository.FamilySupportAssignmentRepository;
import com.integrityfamily.support.repository.ProfessionalFollowUpDraftRepository;
import com.integrityfamily.support.repository.SupportNetworkMemberRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Test de integración de POST /api/families/{familyId}/support/follow-up-drafts
 * (ADR-006, Action Item 7) ejercitando la cadena HTTP real: SecurityFilterChain,
 * JwtAuthenticationFilter, CustomUserDetailsService y el endpoint completo contra
 * un usuario y datos persistidos de verdad -- mismo patrón que
 * {@code FamilyHomeControllerJwtIntegrationTest}.
 *
 * Cubre deliberadamente, como regresión, los dos bugs reales encontrados al
 * verificar esta feature en vivo (2026-07-18):
 * - usuario con más de un rol (ROLE_USER + ROLE_THERAPIST) debe poder pasar
 *   @PreAuthorize -- antes bloqueado por CustomUserDetailsService empaquetando
 *   todos los roles en una sola authority separada por comas.
 * - profesional conectado vía FamilyEcosystemLink (no FamilySupportAssignment)
 *   debe poder generar el borrador -- antes bloqueado por la FK de V107,
 *   corregida en V108.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class ProfessionalFollowUpDraftControllerIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private FamilyRepository familyRepository;
    @Autowired private RoleRepository roleRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private SupportNetworkMemberRepository memberRepository;
    @Autowired private FamilySupportAssignmentRepository assignmentRepository;
    @Autowired private EcosystemParticipantRepository participantRepository;
    @Autowired private FamilyEcosystemLinkRepository linkRepository;
    @Autowired private ProfessionalFollowUpDraftRepository draftRepository;
    @Autowired private JwtTokenProvider jwtTokenProvider;

    private Role roleOrCreate(String name) {
        return roleRepository.findByName(name)
                .orElseGet(() -> roleRepository.save(Role.builder().name(name).build()));
    }

    /** Usuario con dos roles a propósito -- ver regresión en el javadoc de la clase. */
    private String tokenForMultiRoleTherapist(String email) {
        User user = userRepository.save(User.builder()
                .email(email)
                .passwordHash("irrelevant-hash")
                .fullName("Dra. Test Integración")
                .roles(List.of(roleOrCreate("ROLE_USER"), roleOrCreate("ROLE_THERAPIST")))
                .build());
        return jwtTokenProvider.generate(user);
    }

    @Test
    @DisplayName("profesional con asignación tradicional ACTIVE y rol múltiple → 200 y persiste GENERATED")
    void generatesDraft_forTraditionalAssignment_withMultiRoleUser() throws Exception {
        Family family = familyRepository.save(Family.builder().name("Familia Test").build());
        String email = "dra.integracion@if-test.com";

        SupportNetworkMember member = memberRepository.save(SupportNetworkMember.builder()
                .fullName("Dra. Test Integración")
                .email(email)
                .specialty(SupportSpecialty.THERAPIST)
                .build());

        FamilySupportAssignment assignment = assignmentRepository.save(FamilySupportAssignment.builder()
                .familyId(family.getId())
                .supportMember(member)
                .specialty(SupportSpecialty.THERAPIST)
                .status(AssignmentStatus.ACTIVE)
                .invitedByEmail("admin@if-test.com")
                .invitedAt(LocalDateTime.now())
                .canViewIcfScore(true)
                .canViewRiskLevel(true)
                .canViewPlanSummary(true)
                .canViewSprintProgress(true)
                .canViewCrisisHistory(true)
                .canLeaveNotes(true)
                .build());

        String token = tokenForMultiRoleTherapist(email);

        mockMvc.perform(post("/api/families/{familyId}/support/follow-up-drafts", family.getId())
                        .param("assignmentId", String.valueOf(assignment.getId()))
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.draftId").exists())
                .andExpect(jsonPath("$.familyId").value(family.getId()))
                .andExpect(jsonPath("$.generatorType").value("RULE_BASED_TEMPLATE"))
                .andExpect(jsonPath("$.templateVersion").value("professional-follow-up-v1.0"))
                .andExpect(jsonPath("$.narrativeText").value(org.hamcrest.Matchers.containsString("No registrado")))
                .andExpect(jsonPath("$.warnings", org.hamcrest.Matchers.hasItems(
                        "REQUIRES_PROFESSIONAL_REVIEW", "NOT_A_CLINICAL_DIAGNOSIS")));

        assertThat(draftRepository.findByAssignmentIdAndStatus(assignment.getId(), DraftStatus.GENERATED))
                .hasSize(1);
    }

    @Test
    @DisplayName("regenerar → el borrador anterior queda VOIDED y el nuevo GENERATED")
    void regenerating_voidsPreviousDraft() throws Exception {
        Family family = familyRepository.save(Family.builder().name("Familia Test").build());
        String email = "dra.regenera@if-test.com";

        SupportNetworkMember member = memberRepository.save(SupportNetworkMember.builder()
                .fullName("Dra. Regenera").email(email).specialty(SupportSpecialty.THERAPIST).build());
        FamilySupportAssignment assignment = assignmentRepository.save(FamilySupportAssignment.builder()
                .familyId(family.getId()).supportMember(member).specialty(SupportSpecialty.THERAPIST)
                .status(AssignmentStatus.ACTIVE).invitedByEmail("admin@if-test.com").invitedAt(LocalDateTime.now())
                .canViewIcfScore(true).canViewRiskLevel(true).build());

        String token = tokenForMultiRoleTherapist(email);

        mockMvc.perform(post("/api/families/{familyId}/support/follow-up-drafts", family.getId())
                        .param("assignmentId", String.valueOf(assignment.getId()))
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/families/{familyId}/support/follow-up-drafts", family.getId())
                        .param("assignmentId", String.valueOf(assignment.getId()))
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());

        List<com.integrityfamily.support.domain.ProfessionalFollowUpDraft> all =
                draftRepository.findByAssignmentIdOrderByGeneratedAtDesc(assignment.getId());
        assertThat(all).hasSize(2);
        assertThat(all.get(0).getStatus()).isEqualTo(DraftStatus.GENERATED);
        assertThat(all.get(1).getStatus()).isEqualTo(DraftStatus.VOIDED);
    }

    @Test
    @DisplayName("profesional conectado vía Ecosistema de Apoyo (no asignación tradicional) → 200")
    void generatesDraft_forEcosystemLinkedProfessional() throws Exception {
        Family family = familyRepository.save(Family.builder().name("Familia Test Ecosistema").build());
        String email = "dr.ecosistema@if-test.com";

        EcosystemParticipant participant = participantRepository.save(EcosystemParticipant.builder()
                .name("Dr. Ecosistema")
                .networkType(NetworkType.PROFESSIONAL)
                .contactEmail(email)
                .active(true)
                .createdAt(LocalDateTime.now())
                .build());

        FamilyEcosystemLink link = linkRepository.save(FamilyEcosystemLink.builder()
                .familyId(family.getId())
                .participant(participant)
                .networkType(NetworkType.PROFESSIONAL)
                .accessLevel(2)
                .status(EcosystemLinkStatus.ACTIVE)
                .canViewIcfScore(true).canViewRiskLevel(true).canViewPlanSummary(true)
                .canViewSprintProgress(true).canViewCrisisHistory(true)
                .invitedByEmail("admin@if-test.com")
                .invitedAt(LocalDateTime.now())
                .build());

        String token = tokenForMultiRoleTherapist(email);

        mockMvc.perform(post("/api/families/{familyId}/support/follow-up-drafts", family.getId())
                        .param("assignmentId", String.valueOf(link.getId()))
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.draftId").exists());

        assertThat(draftRepository.findByAssignmentIdAndStatus(link.getId(), DraftStatus.GENERATED)).hasSize(1);
    }

    @Test
    @DisplayName("usuario sin rol THERAPIST/ORIENTADOR/ADMIN → 403, ningún borrador se persiste")
    void rejectsUser_withoutProfessionalRole() throws Exception {
        Family family = familyRepository.save(Family.builder().name("Familia Test").build());
        User plainUser = userRepository.save(User.builder()
                .email("solo-usuario@if-test.com")
                .passwordHash("irrelevant-hash")
                .fullName("Usuario Sin Rol Profesional")
                .roles(List.of(roleOrCreate("ROLE_USER")))
                .build());
        String token = jwtTokenProvider.generate(plainUser);

        mockMvc.perform(post("/api/families/{familyId}/support/follow-up-drafts", family.getId())
                        .param("assignmentId", "1")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());

        assertThat(draftRepository.findAll()).isEmpty();
    }
}
