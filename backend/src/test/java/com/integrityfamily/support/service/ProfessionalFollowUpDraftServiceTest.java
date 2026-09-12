package com.integrityfamily.support.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.integrityfamily.auth.service.AuditService;
import com.integrityfamily.domain.AuditEventType;
import com.integrityfamily.support.domain.DraftStatus;
import com.integrityfamily.support.domain.ProfessionalFollowUpDraft;
import com.integrityfamily.support.domain.SupportSpecialty;
import com.integrityfamily.support.dto.SupportNetworkDtos.FamilyDataView;
import com.integrityfamily.support.dto.SupportNetworkDtos.FollowUpDraftResponse;
import com.integrityfamily.support.repository.ProfessionalFollowUpDraftRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("ProfessionalFollowUpDraftService — Unit Tests")
class ProfessionalFollowUpDraftServiceTest {

    @Mock SupportNetworkService supportNetworkService;
    @Mock ProfessionalFollowUpDraftRepository draftRepository;
    @Mock AuditService auditService;

    ProfessionalFollowUpDraftService service;

    private static final Long FAMILY_ID = 1L;
    private static final Long ASSIGNMENT_ID = 100L;
    private static final String EMAIL = "profesional@test.com";

    @BeforeEach
    void setUp() {
        service = new ProfessionalFollowUpDraftService(
                supportNetworkService, draftRepository, auditService, new ObjectMapper());
    }

    private FamilyDataView.FamilyDataViewBuilder baseView() {
        return FamilyDataView.builder()
                .familyId(FAMILY_ID)
                .familyName("Familia Test")
                .assignmentId(ASSIGNMENT_ID)
                .specialty(SupportSpecialty.THERAPIST)
                .accessLevel(5);
    }

    private void stubSave() {
        when(draftRepository.findByAssignmentIdAndStatus(ASSIGNMENT_ID, DraftStatus.GENERATED))
                .thenReturn(List.of());
        when(draftRepository.save(any(ProfessionalFollowUpDraft.class))).thenAnswer(inv -> {
            ProfessionalFollowUpDraft d = inv.getArgument(0);
            d.setId(999L);
            return d;
        });
    }

    @Nested
    @DisplayName("generate()")
    class Generate {

        @Test
        @DisplayName("arma el borrador con los datos de FamilyDataView y persiste con templateVersion")
        void buildsAndPersistsDraft() {
            FamilyDataView view = baseView()
                    .icfScore(72.5).icfLabel("Creciendo").icfDirection("STABLE")
                    .riskLevel("MODERADO").sentinelActive(false)
                    .hasActiveSprint(true).activeSprintStatus("ACTIVE")
                    .planSummaryAvailable(true).crisisHistoryAvailable(true)
                    .build();
            when(supportNetworkService.getDataView(FAMILY_ID, ASSIGNMENT_ID, EMAIL)).thenReturn(view);
            stubSave();

            FollowUpDraftResponse resp = service.generate(FAMILY_ID, ASSIGNMENT_ID, EMAIL);

            assertThat(resp.getDraftId()).isEqualTo(999L);
            assertThat(resp.getTemplateVersion()).isEqualTo(ProfessionalFollowUpDraftService.TEMPLATE_VERSION);
            assertThat(resp.getGeneratorType()).isEqualTo("RULE_BASED_TEMPLATE");
            assertThat(resp.getWarnings()).contains("REQUIRES_PROFESSIONAL_REVIEW", "NOT_A_CLINICAL_DIAGNOSIS");
            assertThat(resp.getNarrativeText())
                    .contains("72.5 (Creciendo)")
                    .contains("MODERADO")
                    .contains("Activo (Estado: ACTIVE)")
                    .contains("Monitorear indicadores de salud familiar");

            ArgumentCaptor<ProfessionalFollowUpDraft> captor = ArgumentCaptor.forClass(ProfessionalFollowUpDraft.class);
            verify(draftRepository).save(captor.capture());
            assertThat(captor.getValue().getStatus()).isEqualTo(DraftStatus.GENERATED);
            assertThat(captor.getValue().getSourceSnapshot()).contains("\"riskLevel\":\"MODERADO\"");
        }

        @Test
        @DisplayName("marca VOIDED cualquier borrador GENERATED previo de la misma asignación")
        void voidsPreviousActiveDraft() {
            ProfessionalFollowUpDraft previous = ProfessionalFollowUpDraft.builder()
                    .id(1L).familyId(FAMILY_ID).assignmentId(ASSIGNMENT_ID)
                    .status(DraftStatus.GENERATED).build();
            when(supportNetworkService.getDataView(FAMILY_ID, ASSIGNMENT_ID, EMAIL)).thenReturn(baseView().build());
            when(draftRepository.findByAssignmentIdAndStatus(ASSIGNMENT_ID, DraftStatus.GENERATED))
                    .thenReturn(List.of(previous));
            when(draftRepository.save(any(ProfessionalFollowUpDraft.class))).thenAnswer(inv -> {
                ProfessionalFollowUpDraft d = inv.getArgument(0);
                d.setId(999L);
                return d;
            });

            service.generate(FAMILY_ID, ASSIGNMENT_ID, EMAIL);

            ArgumentCaptor<List<ProfessionalFollowUpDraft>> captor = ArgumentCaptor.forClass(List.class);
            verify(draftRepository).saveAll(captor.capture());
            assertThat(captor.getValue()).hasSize(1);
            assertThat(captor.getValue().get(0).getStatus()).isEqualTo(DraftStatus.VOIDED);
        }

        @Test
        @DisplayName("ICF ausente produce 'No registrado' sin lanzar excepción")
        void handlesMissingIcfScore() {
            FamilyDataView view = baseView().build(); // icfScore null
            when(supportNetworkService.getDataView(FAMILY_ID, ASSIGNMENT_ID, EMAIL)).thenReturn(view);
            stubSave();

            FollowUpDraftResponse resp = service.generate(FAMILY_ID, ASSIGNMENT_ID, EMAIL);

            assertThat(resp.getNarrativeText()).contains("• ICaF: No registrado");
        }

        @Test
        @DisplayName("tendencia DECLINING recomienda Consejo Familiar en vez de reforzar rituales")
        void decliningTrendRecommendsFamilyCouncil() {
            FamilyDataView view = baseView().icfDirection("DECLINING").riskLevel("BAJO").build();
            when(supportNetworkService.getDataView(FAMILY_ID, ASSIGNMENT_ID, EMAIL)).thenReturn(view);
            stubSave();

            FollowUpDraftResponse resp = service.generate(FAMILY_ID, ASSIGNMENT_ID, EMAIL);

            assertThat(resp.getNarrativeText())
                    .contains("Recomendar la realización de un Consejo Familiar")
                    .doesNotContain("Reforzar el cumplimiento de rituales diarios");
        }

        @Test
        @DisplayName("registra auditoría PROFESSIONAL_DRAFT_GENERATED exactamente una vez")
        void registersAuditEvent() {
            when(supportNetworkService.getDataView(FAMILY_ID, ASSIGNMENT_ID, EMAIL)).thenReturn(baseView().build());
            stubSave();

            service.generate(FAMILY_ID, ASSIGNMENT_ID, EMAIL);

            verify(auditService, times(1))
                    .registerSystemEvent(eq(EMAIL), eq(AuditEventType.PROFESSIONAL_DRAFT_GENERATED), anyString());
            verify(auditService, never())
                    .registerSystemEvent(anyString(), eq(AuditEventType.PROFESSIONAL_DRAFT_USED_AS_NOTE), anyString());
        }
    }
}
