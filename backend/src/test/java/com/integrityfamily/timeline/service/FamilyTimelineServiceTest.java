package com.integrityfamily.timeline.service;

import com.integrityfamily.domain.*;
import com.integrityfamily.domain.repository.*;
import com.integrityfamily.dna.domain.FamilyDna;
import com.integrityfamily.dna.repository.FamilyDnaRepository;
import com.integrityfamily.timeline.dto.TimelineEventDto;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("FamilyTimelineService")
class FamilyTimelineServiceTest {

    @Mock EvaluationRepository            evaluationRepository;
    @Mock FamilyGratitudeEntryRepository  gratitudeRepository;
    @Mock FamilyLogbookRepository         logbookRepository;
    @Mock TaskEvidenceRepository          evidenceRepository;
    @Mock CriticalDayRepository           criticalDayRepository;
    @Mock FamilySprintRepository          sprintRepository;
    @Mock FamilyDnaRepository             dnaRepository;
    @Mock FamilyRepository                familyRepository;
    @InjectMocks FamilyTimelineService service;

    private static final long FAM_ID = 1L;

    // ── vacío ─────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("todas las fuentes vacías → lista vacía")
    void allSourcesEmpty_returnsEmpty() {
        List<TimelineEventDto> result = service.getTimeline(FAM_ID);

        assertThat(result).isEmpty();
    }

    // ── evaluaciones ──────────────────────────────────────────────────────────

    @Nested
    @DisplayName("fromEvaluations")
    class FromEvaluations {

        @Test
        @DisplayName("evaluación FINALIZED con finalizedAt → incluida con tipo EVALUATION")
        void finalizedEval_included() {
            Evaluation eval = Evaluation.builder().id(1L)
                    .status(EvaluationStatus.FINALIZED)
                    .finalizedAt(LocalDateTime.now()).icf(75.5)
                    .build();
            when(evaluationRepository.findByFamilyId(FAM_ID)).thenReturn(List.of(eval));

            List<TimelineEventDto> result = service.getTimeline(FAM_ID);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).type()).isEqualTo(TimelineEventDto.EventType.EVALUATION);
        }

        @Test
        @DisplayName("evaluación no FINALIZED → excluida del timeline")
        void nonFinalizedEval_excluded() {
            Evaluation eval = Evaluation.builder().id(2L)
                    .status(EvaluationStatus.IN_PROGRESS)
                    .finalizedAt(null).build();
            when(evaluationRepository.findByFamilyId(FAM_ID)).thenReturn(List.of(eval));

            assertThat(service.getTimeline(FAM_ID)).isEmpty();
        }

        @Test
        @DisplayName("evaluación con ICF → descripción incluye 'ICF:'")
        void evalWithIcf_descriptionContainsIcf() {
            Evaluation eval = Evaluation.builder().id(3L)
                    .status(EvaluationStatus.FINALIZED)
                    .finalizedAt(LocalDateTime.now()).icf(80.0)
                    .riskLevel("BAJO").build();
            when(evaluationRepository.findByFamilyId(FAM_ID)).thenReturn(List.of(eval));

            TimelineEventDto event = service.getTimeline(FAM_ID).get(0);

            assertThat(event.description()).contains("ICF:");
        }

        @Test
        @DisplayName("evaluación sin ICF → descripción genérica")
        void evalWithoutIcf_genericDescription() {
            Evaluation eval = Evaluation.builder().id(4L)
                    .status(EvaluationStatus.FINALIZED)
                    .finalizedAt(LocalDateTime.now()).icf(null).build();
            when(evaluationRepository.findByFamilyId(FAM_ID)).thenReturn(List.of(eval));

            TimelineEventDto event = service.getTimeline(FAM_ID).get(0);

            assertThat(event.description()).doesNotContain("ICF:");
        }
    }

    // ── gratitudes ────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("fromGratitudes")
    class FromGratitudes {

        @Test
        @DisplayName("gratitud → título incluye 'agradeció a'")
        void gratitude_titleContainsAgradecioa() {
            FamilyGratitudeEntry g = FamilyGratitudeEntry.builder()
                    .id(1L).fromMember("Ana").toMember("Luis")
                    .description("Gracias por tu apoyo")
                    .createdAt(LocalDateTime.now()).build();
            when(gratitudeRepository.findByFamilyIdOrderByCreatedAtDesc(FAM_ID)).thenReturn(List.of(g));

            TimelineEventDto event = service.getTimeline(FAM_ID).get(0);

            assertThat(event.type()).isEqualTo(TimelineEventDto.EventType.GRATITUDE);
            assertThat(event.title()).contains("Ana").contains("agradeció a").contains("Luis");
        }
    }

    // ── bitácora ──────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("fromLogbook")
    class FromLogbook {

        @Test
        @DisplayName("entrada RESOLVED → título 'Desafío superado y registrado en bitácora'")
        void resolvedEntry_specificTitle() {
            FamilyLogbookEntry entry = FamilyLogbookEntry.builder()
                    .id(1L).status(LogbookStatus.RESOLVED).situation("Conflicto resuelto")
                    .createdAt(LocalDateTime.now()).build();
            when(logbookRepository.findByFamilyIdOrderByCreatedAtDesc(FAM_ID)).thenReturn(List.of(entry));

            TimelineEventDto event = service.getTimeline(FAM_ID).get(0);

            assertThat(event.type()).isEqualTo(TimelineEventDto.EventType.LOGBOOK);
            assertThat(event.title()).isEqualTo("Desafío superado y registrado en bitácora");
        }

        @Test
        @DisplayName("entrada no RESOLVED → título 'Momento de transformación registrado'")
        void openEntry_genericTitle() {
            FamilyLogbookEntry entry = FamilyLogbookEntry.builder()
                    .id(2L).status(LogbookStatus.OPEN).situation("Situación difícil")
                    .createdAt(LocalDateTime.now()).build();
            when(logbookRepository.findByFamilyIdOrderByCreatedAtDesc(FAM_ID)).thenReturn(List.of(entry));

            assertThat(service.getTimeline(FAM_ID).get(0).title())
                    .isEqualTo("Momento de transformación registrado");
        }
    }

    // ── evidencias ────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("fromEvidences")
    class FromEvidences {

        @Test
        @DisplayName("evidencia sin createdAt → filtrada del timeline")
        void evidenceWithNullCreatedAt_filtered() {
            TaskEvidence ev = TaskEvidence.builder().id(1L).createdAt(null).build();
            when(evidenceRepository.findByFamilyId(FAM_ID)).thenReturn(List.of(ev));

            assertThat(service.getTimeline(FAM_ID)).isEmpty();
        }

        @Test
        @DisplayName("evidencia tipo PHOTO → título 'Foto subida como evidencia de misión'")
        void photoEvidence_specificTitle() {
            TaskEvidence ev = TaskEvidence.builder()
                    .id(2L).evidenceType(EvidenceType.PHOTO).createdAt(LocalDateTime.now()).build();
            when(evidenceRepository.findByFamilyId(FAM_ID)).thenReturn(List.of(ev));

            assertThat(service.getTimeline(FAM_ID).get(0).title())
                    .isEqualTo("Foto subida como evidencia de misión");
        }
    }

    // ── ADN familiar ──────────────────────────────────────────────────────────

    @Nested
    @DisplayName("fromDna")
    class FromDna {

        @Test
        @DisplayName("ADN versión 1 → título 'sintetizado por primera vez'")
        void dnaVersion1_firstSynthesisTitle() {
            FamilyDna dna = FamilyDna.builder().id(1L).version(1)
                    .updatedAt(LocalDateTime.now()).build();
            when(dnaRepository.findByFamilyId(FAM_ID)).thenReturn(Optional.of(dna));

            TimelineEventDto event = service.getTimeline(FAM_ID).get(0);

            assertThat(event.type()).isEqualTo(TimelineEventDto.EventType.DNA);
            assertThat(event.title()).contains("primera vez");
        }

        @Test
        @DisplayName("ADN versión 3 → título contiene 'actualizado (v3)'")
        void dnaVersion3_updatedTitle() {
            FamilyDna dna = FamilyDna.builder().id(2L).version(3)
                    .updatedAt(LocalDateTime.now()).build();
            when(dnaRepository.findByFamilyId(FAM_ID)).thenReturn(Optional.of(dna));

            assertThat(service.getTimeline(FAM_ID).get(0).title()).contains("v3");
        }

        @Test
        @DisplayName("ADN sin updatedAt → filtrado (no incluido en lista)")
        void dnaWithNullUpdatedAt_excluded() {
            FamilyDna dna = FamilyDna.builder().id(3L).version(1).updatedAt(null).build();
            when(dnaRepository.findByFamilyId(FAM_ID)).thenReturn(Optional.of(dna));

            assertThat(service.getTimeline(FAM_ID)).isEmpty();
        }
    }

    // ── miembros ──────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("fromMemberJoins")
    class FromMemberJoins {

        @Test
        @DisplayName("miembro activo con joinedAt → incluido con tipo MEMBER_JOINED")
        void activeMemberWithJoinedAt_included() {
            FamilyMember m = FamilyMember.builder()
                    .id(1L).fullName("Sofía García").role("GUARDIAN")
                    .active(true).joinedAt(LocalDateTime.now()).build();
            Family family = Family.builder().id(FAM_ID).name("García").build();
            family.getMembers().add(m);
            when(familyRepository.findById(FAM_ID)).thenReturn(Optional.of(family));

            TimelineEventDto event = service.getTimeline(FAM_ID).get(0);

            assertThat(event.type()).isEqualTo(TimelineEventDto.EventType.MEMBER_JOINED);
            assertThat(event.title()).contains("Sofía García");
        }
    }

    // ── ordenamiento y límite ─────────────────────────────────────────────────

    @Nested
    @DisplayName("ordenamiento y límite")
    class SortingAndLimit {

        @Test
        @DisplayName("eventos de distintas fuentes → ordenados por occurredAt DESC")
        void multiSourceEvents_sortedByOccurredAtDesc() {
            LocalDateTime earlier = LocalDateTime.now().minusDays(2);
            LocalDateTime later   = LocalDateTime.now().minusHours(1);

            FamilyGratitudeEntry g = FamilyGratitudeEntry.builder()
                    .id(1L).fromMember("A").toMember("B").description("Gracias")
                    .createdAt(earlier).build();
            FamilyGratitudeEntry g2 = FamilyGratitudeEntry.builder()
                    .id(2L).fromMember("C").toMember("D").description("Gracias 2")
                    .createdAt(later).build();
            when(gratitudeRepository.findByFamilyIdOrderByCreatedAtDesc(FAM_ID))
                    .thenReturn(List.of(g, g2));

            List<TimelineEventDto> result = service.getTimeline(FAM_ID);

            assertThat(result).hasSize(2);
            assertThat(result.get(0).occurredAt()).isAfterOrEqualTo(result.get(1).occurredAt());
        }

        @Test
        @DisplayName("más de 100 eventos → resultado limitado a 100")
        void over100Events_limitedTo100() {
            List<FamilyGratitudeEntry> gratitudes = IntStream.range(0, 110)
                    .mapToObj(i -> FamilyGratitudeEntry.builder()
                            .id((long) i).fromMember("A").toMember("B").description("G" + i)
                            .createdAt(LocalDateTime.now().minusMinutes(i))
                            .build())
                    .toList();
            when(gratitudeRepository.findByFamilyIdOrderByCreatedAtDesc(FAM_ID)).thenReturn(gratitudes);

            assertThat(service.getTimeline(FAM_ID)).hasSize(100);
        }
    }
}
