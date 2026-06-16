package com.integrityfamily.documentary.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.integrityfamily.ai.provider.AiProvider;
import com.integrityfamily.documentary.domain.DocumentaryProduction;
import com.integrityfamily.documentary.domain.DocumentaryScope;
import com.integrityfamily.documentary.domain.ProductionStatus;
import com.integrityfamily.documentary.repository.DocumentaryProductionRepository;
import com.integrityfamily.domain.Family;
import com.integrityfamily.domain.TaskEvidence;
import com.integrityfamily.domain.repository.FamilyRepository;
import com.integrityfamily.domain.repository.TaskEvidenceRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("DocumentaryProductionService — Unit Tests")
class DocumentaryProductionServiceTest {

    @Mock DocumentaryProductionRepository productionRepository;
    @Mock FamilyRepository                familyRepository;
    @Mock TaskEvidenceRepository          evidenceRepository;
    @Mock AiProvider                      aiProvider;
    @Mock ObjectMapper                    objectMapper;

    @InjectMocks DocumentaryProductionService service;

    private Family             family;
    private DocumentaryProduction draftProduction;

    @BeforeEach
    void setUp() {
        family = Family.builder().id(1L).name("Familia Test").build();
        draftProduction = DocumentaryProduction.builder()
                .id(10L).family(family).title("Mi documental")
                .scope(DocumentaryScope.MISSION)
                .status(ProductionStatus.DRAFT)
                .curatedEvidences(new ArrayList<>())
                .build();
    }

    // ═══════════════════════════════════════════════════════════════
    //  createDraft()
    // ═══════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("createDraft()")
    class CreateDraft {

        @Test
        @DisplayName("familia existe → crea producción en estado DRAFT")
        void shouldCreateDraft_withCorrectStatus() {
            when(familyRepository.findById(1L)).thenReturn(Optional.of(family));
            when(productionRepository.save(any())).thenAnswer(i -> i.getArgument(0));

            service.createDraft(1L, "Documental de misión", DocumentaryScope.MISSION, 5L);

            ArgumentCaptor<DocumentaryProduction> captor = ArgumentCaptor.forClass(DocumentaryProduction.class);
            verify(productionRepository).save(captor.capture());
            assertThat(captor.getValue().getStatus()).isEqualTo(ProductionStatus.DRAFT);
            assertThat(captor.getValue().getTitle()).isEqualTo("Documental de misión");
            assertThat(captor.getValue().getScope()).isEqualTo(DocumentaryScope.MISSION);
            assertThat(captor.getValue().getReferenceId()).isEqualTo(5L);
            assertThat(captor.getValue().getFamily()).isSameAs(family);
        }

        @Test
        @DisplayName("familia no encontrada → IllegalArgumentException")
        void shouldThrow_whenFamilyNotFound() {
            when(familyRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.createDraft(99L, "Título", DocumentaryScope.SPRINT, null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Family not found");
        }
    }

    // ═══════════════════════════════════════════════════════════════
    //  updateCuration()
    // ═══════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("updateCuration()")
    class UpdateCuration {

        @Test
        @DisplayName("producción DRAFT → actualiza evidencias y pasa a CURATED")
        void shouldUpdateCuration_fromDraft() {
            TaskEvidence ev1 = TaskEvidence.builder().id(1L).build();
            TaskEvidence ev2 = TaskEvidence.builder().id(2L).build();

            when(productionRepository.findById(10L)).thenReturn(Optional.of(draftProduction));
            when(evidenceRepository.findAllById(List.of(1L, 2L))).thenReturn(List.of(ev1, ev2));
            when(productionRepository.save(any())).thenAnswer(i -> i.getArgument(0));

            service.updateCuration(10L, List.of(1L, 2L));

            assertThat(draftProduction.getStatus()).isEqualTo(ProductionStatus.CURATED);
            assertThat(draftProduction.getCuratedEvidences()).containsExactly(ev1, ev2);
        }

        @Test
        @DisplayName("producción APPROVED → IllegalStateException (no editable)")
        void shouldThrow_whenProductionIsApproved() {
            draftProduction.setStatus(ProductionStatus.APPROVED);
            when(productionRepository.findById(10L)).thenReturn(Optional.of(draftProduction));

            assertThatThrownBy(() -> service.updateCuration(10L, List.of(1L)))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("approved or published");
        }

        @Test
        @DisplayName("producción PUBLISHED → IllegalStateException (no editable)")
        void shouldThrow_whenProductionIsPublished() {
            draftProduction.setStatus(ProductionStatus.PUBLISHED);
            when(productionRepository.findById(10L)).thenReturn(Optional.of(draftProduction));

            assertThatThrownBy(() -> service.updateCuration(10L, List.of(1L)))
                    .isInstanceOf(IllegalStateException.class);
        }

        @Test
        @DisplayName("producción no encontrada → IllegalArgumentException")
        void shouldThrow_whenProductionNotFound() {
            when(productionRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.updateCuration(99L, List.of(1L)))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Production not found");
        }
    }

    // ═══════════════════════════════════════════════════════════════
    //  generateScript()
    // ═══════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("generateScript()")
    class GenerateScript {

        @Test
        @DisplayName("producción CURATED + IA responde → guarda script y pasa a GENERATED")
        void shouldGenerateScript_whenCurated() {
            draftProduction.setStatus(ProductionStatus.CURATED);

            when(productionRepository.findById(10L)).thenReturn(Optional.of(draftProduction));
            when(aiProvider.generateRawResponse(any())).thenReturn("Capítulo 1: El inicio...");
            when(productionRepository.save(any())).thenAnswer(i -> i.getArgument(0));

            service.generateScript(10L);

            assertThat(draftProduction.getStatus()).isEqualTo(ProductionStatus.GENERATED);
            assertThat(draftProduction.getScriptData()).isEqualTo("Capítulo 1: El inicio...");
        }

        @Test
        @DisplayName("producción DRAFT → IllegalStateException (debe estar CURATED)")
        void shouldThrow_whenNotCurated() {
            when(productionRepository.findById(10L)).thenReturn(Optional.of(draftProduction));

            assertThatThrownBy(() -> service.generateScript(10L))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("curated");
        }

        @Test
        @DisplayName("IA lanza excepción → RuntimeException propagada")
        void shouldThrow_whenAiFails() {
            draftProduction.setStatus(ProductionStatus.CURATED);
            when(productionRepository.findById(10L)).thenReturn(Optional.of(draftProduction));
            when(aiProvider.generateRawResponse(any())).thenThrow(new RuntimeException("IA no disponible"));

            assertThatThrownBy(() -> service.generateScript(10L))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("guion");
        }
    }

    // ═══════════════════════════════════════════════════════════════
    //  approveProduction() / publishProduction()
    // ═══════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("approveProduction() / publishProduction()")
    class ApproveAndPublish {

        @Test
        @DisplayName("approve → estado APPROVED")
        void shouldApprove() {
            draftProduction.setStatus(ProductionStatus.GENERATED);
            when(productionRepository.findById(10L)).thenReturn(Optional.of(draftProduction));
            when(productionRepository.save(any())).thenAnswer(i -> i.getArgument(0));

            service.approveProduction(10L);

            assertThat(draftProduction.getStatus()).isEqualTo(ProductionStatus.APPROVED);
        }

        @Test
        @DisplayName("publish → estado PUBLISHED con exportUrl")
        void shouldPublish_withExportUrl() {
            draftProduction.setStatus(ProductionStatus.APPROVED);
            when(productionRepository.findById(10L)).thenReturn(Optional.of(draftProduction));
            when(productionRepository.save(any())).thenAnswer(i -> i.getArgument(0));

            service.publishProduction(10L, "https://cdn.integrityfamily.com/doc-10.mp4");

            assertThat(draftProduction.getStatus()).isEqualTo(ProductionStatus.PUBLISHED);
            assertThat(draftProduction.getExportUrl()).isEqualTo("https://cdn.integrityfamily.com/doc-10.mp4");
        }

        @Test
        @DisplayName("approve producción no encontrada → IllegalArgumentException")
        void shouldThrow_whenApproveNotFound() {
            when(productionRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.approveProduction(99L))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("publish producción no encontrada → IllegalArgumentException")
        void shouldThrow_whenPublishNotFound() {
            when(productionRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.publishProduction(99L, "url"))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    // ═══════════════════════════════════════════════════════════════
    //  getProductionsByFamily() / getProduction()
    // ═══════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("getProductionsByFamily() / getProduction()")
    class Queries {

        @Test
        @DisplayName("getProductionsByFamily → delega en repositorio")
        void shouldReturnProductionList() {
            when(productionRepository.findByFamilyIdOrderByCreatedAtDesc(1L))
                    .thenReturn(List.of(draftProduction));

            List<DocumentaryProduction> result = service.getProductionsByFamily(1L);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getId()).isEqualTo(10L);
        }

        @Test
        @DisplayName("getProduction existente → retorna la producción")
        void shouldReturnProduction_whenFound() {
            when(productionRepository.findById(10L)).thenReturn(Optional.of(draftProduction));

            DocumentaryProduction result = service.getProduction(10L);

            assertThat(result.getId()).isEqualTo(10L);
        }

        @Test
        @DisplayName("getProduction no encontrada → IllegalArgumentException")
        void shouldThrow_whenNotFound() {
            when(productionRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.getProduction(99L))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Production not found");
        }
    }
}
