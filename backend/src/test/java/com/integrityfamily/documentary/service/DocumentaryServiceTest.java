package com.integrityfamily.documentary.service;

import com.integrityfamily.common.exception.BusinessException;
import com.integrityfamily.documentary.dto.SubmitDocumentaryRequest;
import com.integrityfamily.domain.*;
import com.integrityfamily.domain.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("DocumentaryService — Unit Tests")
class DocumentaryServiceTest {

    @Mock FamilyDocumentaryRepository documentaryRepository;
    @Mock FamilyRepository            familyRepository;
    @Mock PlanTaskRepository          planTaskRepository;
    @Mock TaskEvidenceRepository      taskEvidenceRepository;

    @InjectMocks DocumentaryService service;

    private Family   family;
    private PlanTask task;

    @BeforeEach
    void setUp() {
        family = Family.builder().id(1L).name("Familia López").build();
        task   = PlanTask.builder().id(5L).build();
    }

    // ═══════════════════════════════════════════════════════════════
    //  createDocumentary()
    // ═══════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("createDocumentary()")
    class CreateDocumentary {

        @Test
        @DisplayName("con taskId → crea documental COMPLETED vinculado a la tarea")
        void shouldCreateDocumentary_withTask() {
            SubmitDocumentaryRequest req = new SubmitDocumentaryRequest();
            req.setFamilyId(1L);
            req.setTaskId(5L);
            req.setTitle("Misión cumplida");
            req.setContent("Narrativa del logro familiar");
            req.setSourceType(DocumentarySourceType.MISSION);
            req.setEvidenceIds(null);

            when(familyRepository.findById(1L)).thenReturn(Optional.of(family));
            when(planTaskRepository.findById(5L)).thenReturn(Optional.of(task));
            when(documentaryRepository.save(any())).thenAnswer(i -> i.getArgument(0));

            service.createDocumentary(req);

            ArgumentCaptor<FamilyDocumentary> captor = ArgumentCaptor.forClass(FamilyDocumentary.class);
            verify(documentaryRepository).save(captor.capture());
            assertThat(captor.getValue().getTitle()).isEqualTo("Misión cumplida");
            assertThat(captor.getValue().getStatus()).isEqualTo(DocumentaryStatus.COMPLETED);
            assertThat(captor.getValue().getTask()).isSameAs(task);
            assertThat(captor.getValue().getFamily()).isSameAs(family);
        }

        @Test
        @DisplayName("sin taskId → tipo SPONTANEOUS por defecto, sin tarea")
        void shouldCreateDocumentary_spontaneous() {
            SubmitDocumentaryRequest req = new SubmitDocumentaryRequest();
            req.setFamilyId(1L);
            req.setTaskId(null);
            req.setTitle("Momento espontáneo");
            req.setContent("Descripción del momento");
            req.setSourceType(null);
            req.setEvidenceIds(null);

            when(familyRepository.findById(1L)).thenReturn(Optional.of(family));
            when(documentaryRepository.save(any())).thenAnswer(i -> i.getArgument(0));

            service.createDocumentary(req);

            ArgumentCaptor<FamilyDocumentary> captor = ArgumentCaptor.forClass(FamilyDocumentary.class);
            verify(documentaryRepository).save(captor.capture());
            assertThat(captor.getValue().getTask()).isNull();
            assertThat(captor.getValue().getSourceType()).isEqualTo(DocumentarySourceType.SPONTANEOUS);
            verify(planTaskRepository, never()).findById(any());
        }

        @Test
        @DisplayName("con evidenceIds → vincula evidencias de la misma familia")
        void shouldLinkEvidences_whenEvidenceIdsProvided() {
            TaskEvidence ev1 = TaskEvidence.builder().id(10L).family(family).build();
            TaskEvidence ev2 = TaskEvidence.builder().id(11L).family(family).build();

            SubmitDocumentaryRequest req = new SubmitDocumentaryRequest();
            req.setFamilyId(1L);
            req.setTitle("Con evidencias");
            req.setContent("Contenido");
            req.setEvidenceIds(List.of(10L, 11L));

            FamilyDocumentary saved = FamilyDocumentary.builder()
                    .id(99L).family(family).title("Con evidencias")
                    .status(DocumentaryStatus.COMPLETED).build();

            when(familyRepository.findById(1L)).thenReturn(Optional.of(family));
            when(documentaryRepository.save(any())).thenReturn(saved);
            when(taskEvidenceRepository.findById(10L)).thenReturn(Optional.of(ev1));
            when(taskEvidenceRepository.findById(11L)).thenReturn(Optional.of(ev2));
            when(taskEvidenceRepository.save(any())).thenAnswer(i -> i.getArgument(0));

            service.createDocumentary(req);

            assertThat(ev1.getDocumentary()).isSameAs(saved);
            assertThat(ev2.getDocumentary()).isSameAs(saved);
            verify(taskEvidenceRepository, times(2)).save(any(TaskEvidence.class));
        }

        @Test
        @DisplayName("evidencia de otra familia → no se vincula")
        void shouldSkipEvidence_fromDifferentFamily() {
            Family otherFamily = Family.builder().id(99L).name("Otra familia").build();
            TaskEvidence evOther = TaskEvidence.builder().id(20L).family(otherFamily).build();

            SubmitDocumentaryRequest req = new SubmitDocumentaryRequest();
            req.setFamilyId(1L);
            req.setTitle("Filtrado");
            req.setContent("Contenido");
            req.setEvidenceIds(List.of(20L));

            FamilyDocumentary saved = FamilyDocumentary.builder()
                    .id(100L).family(family).status(DocumentaryStatus.COMPLETED).build();

            when(familyRepository.findById(1L)).thenReturn(Optional.of(family));
            when(documentaryRepository.save(any())).thenReturn(saved);
            when(taskEvidenceRepository.findById(20L)).thenReturn(Optional.of(evOther));

            service.createDocumentary(req);

            verify(taskEvidenceRepository, never()).save(any());
        }

        @Test
        @DisplayName("familia no encontrada → BusinessException FAMILY_NOT_FOUND")
        void shouldThrow_whenFamilyNotFound() {
            SubmitDocumentaryRequest req = new SubmitDocumentaryRequest();
            req.setFamilyId(99L);

            when(familyRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.createDocumentary(req))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("Familia no encontrada");
        }

        @Test
        @DisplayName("tarea no encontrada → BusinessException TASK_NOT_FOUND")
        void shouldThrow_whenTaskNotFound() {
            SubmitDocumentaryRequest req = new SubmitDocumentaryRequest();
            req.setFamilyId(1L);
            req.setTaskId(99L);

            when(familyRepository.findById(1L)).thenReturn(Optional.of(family));
            when(planTaskRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.createDocumentary(req))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("Tarea no encontrada");
        }
    }

    // ═══════════════════════════════════════════════════════════════
    //  getFamilyDocumentaries()
    // ═══════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("getFamilyDocumentaries()")
    class GetFamilyDocumentaries {

        @Test
        @DisplayName("delega en repositorio y mapea a DTOs")
        void shouldDelegateAndMapDtos() {
            FamilyDocumentary doc = FamilyDocumentary.builder()
                    .id(1L).family(family).title("Doc 1")
                    .status(DocumentaryStatus.COMPLETED)
                    .sourceType(DocumentarySourceType.SPONTANEOUS)
                    .build();
            when(documentaryRepository.findByFamilyIdOrderByCreatedAtDesc(1L)).thenReturn(List.of(doc));

            var result = service.getFamilyDocumentaries(1L);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getTitle()).isEqualTo("Doc 1");
        }

        @Test
        @DisplayName("familia sin documentales → lista vacía")
        void shouldReturnEmptyList_whenNoDocumentaries() {
            when(documentaryRepository.findByFamilyIdOrderByCreatedAtDesc(1L)).thenReturn(List.of());

            var result = service.getFamilyDocumentaries(1L);

            assertThat(result).isEmpty();
        }
    }
}
