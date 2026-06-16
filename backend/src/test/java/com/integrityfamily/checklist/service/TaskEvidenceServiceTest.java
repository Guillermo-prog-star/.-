package com.integrityfamily.checklist.service;

import com.integrityfamily.common.exception.BusinessException;
import com.integrityfamily.domain.*;
import com.integrityfamily.domain.repository.FamilyRepository;
import com.integrityfamily.domain.repository.PlanTaskRepository;
import com.integrityfamily.domain.repository.TaskEvidenceRepository;
import org.springframework.http.HttpStatus;
import com.integrityfamily.participation.service.ParticipationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Pruebas unitarias para {@link TaskEvidenceService}.
 *
 * No levanta contexto Spring — usa Mockito strict stubs.
 * El RabbitTemplate es mockeado; se prueba que el evento se intenta publicar
 * y que los errores de publicación no propagan la excepción al caller.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("TaskEvidenceService — Unit Tests")
class TaskEvidenceServiceTest {

    @Mock TaskEvidenceRepository taskEvidenceRepository;
    @Mock PlanTaskRepository     planTaskRepository;
    @Mock FamilyRepository       familyRepository;
    @Mock RabbitTemplate         rabbitTemplate;
    @Mock ParticipationService   participationService;
    @Mock com.integrityfamily.auth.service.AuditService auditService;

    @InjectMocks TaskEvidenceService service;

    private Family   family;
    private PlanTask task;

    @BeforeEach
    void setUp() {
        family = Family.builder().id(1L).name("Los García").build();
        task   = PlanTask.builder().id(42L).completed(false).build();
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  Queries simples
    // ═══════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("getFamilyEvidences() / getEvidencesByTask()")
    class Queries {

        @Test
        @DisplayName("getFamilyEvidences delega al repositorio")
        void getFamilyEvidences_delegatesToRepo() {
            TaskEvidence e = TaskEvidence.builder().id(1L).family(family).task(task)
                    .evidenceType(EvidenceType.DOCUMENT).build();

            when(taskEvidenceRepository.findByFamilyId(1L)).thenReturn(List.of(e));

            List<TaskEvidence> result = service.getFamilyEvidences(1L);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getId()).isEqualTo(1L);
        }

        @Test
        @DisplayName("getEvidencesByTask delega al repositorio")
        void getEvidencesByTask_delegatesToRepo() {
            when(taskEvidenceRepository.findByTaskId(42L)).thenReturn(List.of());

            assertThat(service.getEvidencesByTask(42L)).isEmpty();
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  submitEvidence()
    // ═══════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("submitEvidence() — subir evidencia")
    class SubmitEvidence {

        @Test
        @DisplayName("éxito: construye la evidencia con todos los campos y la persiste")
        void submitEvidence_success() {
            TaskEvidence saved = TaskEvidence.builder()
                    .id(100L).task(task).family(family)
                    .evidenceType(EvidenceType.SELF_REFLECTION).status(EvidenceStatus.SUBMITTED)
                    .title("Mi evidencia").submittedBy("Papá").build();

            when(planTaskRepository.findById(42L)).thenReturn(Optional.of(task));
            when(familyRepository.findById(1L)).thenReturn(Optional.of(family));
            when(taskEvidenceRepository.save(any())).thenReturn(saved);

            TaskEvidence result = service.submitEvidence(
                    42L, 1L, EvidenceType.SELF_REFLECTION,
                    "Mi evidencia", "Descripción", null, "Texto libre", "Papá"
            );

            assertThat(result.getId()).isEqualTo(100L);
            assertThat(result.getStatus()).isEqualTo(EvidenceStatus.SUBMITTED);
            verify(taskEvidenceRepository).save(any(TaskEvidence.class));
        }

        @Test
        @DisplayName("los campos de la evidencia guardada reflejan los argumentos")
        void submitEvidence_fieldMapping() {
            ArgumentCaptor<TaskEvidence> captor = ArgumentCaptor.forClass(TaskEvidence.class);

            when(planTaskRepository.findById(42L)).thenReturn(Optional.of(task));
            when(familyRepository.findById(1L)).thenReturn(Optional.of(family));
            when(taskEvidenceRepository.save(captor.capture())).thenAnswer(inv -> inv.getArgument(0));

            service.submitEvidence(
                    42L, 1L, EvidenceType.PHOTO,
                    "Foto del jardín", "Descripción foto",
                    "http://s3/foto.jpg", null, "Mamá"
            );

            TaskEvidence captured = captor.getValue();
            assertThat(captured.getTask()).isSameAs(task);
            assertThat(captured.getFamily()).isSameAs(family);
            assertThat(captured.getEvidenceType()).isEqualTo(EvidenceType.PHOTO);
            assertThat(captured.getStatus()).isEqualTo(EvidenceStatus.SUBMITTED);
            assertThat(captured.getTitle()).isEqualTo("Foto del jardín");
            assertThat(captured.getFileUrl()).isEqualTo("http://s3/foto.jpg");
            assertThat(captured.getSubmittedBy()).isEqualTo("Mamá");
            assertThat(captured.isValidated()).isFalse();
        }

        @Test
        @DisplayName("tarea no existe → BusinessException TASK_NOT_FOUND 404")
        void submitEvidence_taskNotFound() {
            when(planTaskRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.submitEvidence(
                    99L, 1L, EvidenceType.SELF_REFLECTION,
                    "T", "D", null, null, "User"
            )).isInstanceOf(BusinessException.class)
              .satisfies(ex -> {
                  BusinessException be = (BusinessException) ex;
                  assertThat(be.getCode()).isEqualTo("TASK_NOT_FOUND");
                  assertThat(be.getStatus()).isEqualTo(HttpStatus.NOT_FOUND);
              });

            verify(taskEvidenceRepository, never()).save(any());
        }

        @Test
        @DisplayName("familia no existe → BusinessException FAMILY_NOT_FOUND 404")
        void submitEvidence_familyNotFound() {
            when(planTaskRepository.findById(42L)).thenReturn(Optional.of(task));
            when(familyRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.submitEvidence(
                    42L, 99L, EvidenceType.SELF_REFLECTION,
                    "T", "D", null, null, "User"
            )).isInstanceOf(BusinessException.class)
              .satisfies(ex -> {
                  BusinessException be = (BusinessException) ex;
                  assertThat(be.getCode()).isEqualTo("FAMILY_NOT_FOUND");
                  assertThat(be.getStatus()).isEqualTo(HttpStatus.NOT_FOUND);
              });

            verify(taskEvidenceRepository, never()).save(any());
        }

        @Test
        @DisplayName("error en RabbitMQ → no propaga la excepción (try/catch interno)")
        void submitEvidence_rabbitError_doesNotPropagate() {
            TaskEvidence saved = TaskEvidence.builder()
                    .id(50L).task(task).family(family)
                    .evidenceType(EvidenceType.SELF_REFLECTION).status(EvidenceStatus.SUBMITTED).build();

            when(planTaskRepository.findById(42L)).thenReturn(Optional.of(task));
            when(familyRepository.findById(1L)).thenReturn(Optional.of(family));
            when(taskEvidenceRepository.save(any())).thenReturn(saved);
            doThrow(new RuntimeException("broker down"))
                    .when(rabbitTemplate).convertAndSend(anyString(), anyString(), any(Object.class));

            // No debe lanzar excepción al caller
            TaskEvidence result = service.submitEvidence(
                    42L, 1L, EvidenceType.SELF_REFLECTION, "T", "D", null, null, "User"
            );

            assertThat(result).isNotNull();
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  validateEvidence()
    // ═══════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("validateEvidence() — aprobar evidencia")
    class ValidateEvidence {

        @Test
        @DisplayName("éxito: llama a validate(), persiste evidencia y marca tarea completada")
        void validateEvidence_success() {
            TaskEvidence evidence = TaskEvidence.builder()
                    .id(10L).task(task).family(family)
                    .evidenceType(EvidenceType.SELF_REFLECTION).status(EvidenceStatus.SUBMITTED)
                    .validated(false).build();

            when(taskEvidenceRepository.findById(10L)).thenReturn(Optional.of(evidence));
            when(taskEvidenceRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(planTaskRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            TaskEvidence result = service.validateEvidence(10L, 95.0, "Supervisor");

            assertThat(result.isValidated()).isTrue();
            assertThat(result.getStatus()).isEqualTo(EvidenceStatus.VALIDATED);
            assertThat(result.getHumanScore()).isEqualTo(95.0);
            assertThat(result.getValidatedAt()).isNotNull();
            assertThat(task.isCompleted()).isTrue();
            verify(planTaskRepository).save(task);
        }

        @Test
        @DisplayName("evidencia no encontrada → BusinessException EVIDENCE_NOT_FOUND 404")
        void validateEvidence_notFound() {
            when(taskEvidenceRepository.findById(999L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.validateEvidence(999L, 80.0, "Admin"))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(ex -> {
                        BusinessException be = (BusinessException) ex;
                        assertThat(be.getCode()).isEqualTo("EVIDENCE_NOT_FOUND");
                        assertThat(be.getStatus()).isEqualTo(HttpStatus.NOT_FOUND);
                    });
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  rejectEvidence()
    // ═══════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("rejectEvidence() — rechazar evidencia")
    class RejectEvidence {

        @Test
        @DisplayName("éxito: pone REJECTED, validated=false y tarea=incompleta")
        void rejectEvidence_success() {
            task.setCompleted(true); // Venía de una validación previa

            TaskEvidence evidence = TaskEvidence.builder()
                    .id(20L).task(task).family(family)
                    .evidenceType(EvidenceType.SELF_REFLECTION).status(EvidenceStatus.SUBMITTED)
                    .validated(true).build();

            when(taskEvidenceRepository.findById(20L)).thenReturn(Optional.of(evidence));
            when(taskEvidenceRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(planTaskRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            TaskEvidence result = service.rejectEvidence(20L, "Supervisor");

            assertThat(result.getStatus()).isEqualTo(EvidenceStatus.REJECTED);
            assertThat(result.isValidated()).isFalse();
            assertThat(result.getValidatedAt()).isNotNull();
            assertThat(task.isCompleted()).isFalse();
            verify(planTaskRepository).save(task);
        }

        @Test
        @DisplayName("evidencia no encontrada → BusinessException EVIDENCE_NOT_FOUND 404")
        void rejectEvidence_notFound() {
            when(taskEvidenceRepository.findById(888L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.rejectEvidence(888L, "Admin"))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(ex -> {
                        BusinessException be = (BusinessException) ex;
                        assertThat(be.getCode()).isEqualTo("EVIDENCE_NOT_FOUND");
                        assertThat(be.getStatus()).isEqualTo(HttpStatus.NOT_FOUND);
                    });
        }
    }
}
