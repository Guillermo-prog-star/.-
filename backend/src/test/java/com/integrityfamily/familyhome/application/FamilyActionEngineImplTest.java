package com.integrityfamily.familyhome.application;

import com.integrityfamily.auth.service.AuditService;
import com.integrityfamily.common.event.EventPublisher;
import com.integrityfamily.domain.FamilyActionExecution;
import com.integrityfamily.domain.User;
import com.integrityfamily.domain.repository.FamilyActionExecutionRepository;
import com.integrityfamily.domain.repository.UserRepository;
import com.integrityfamily.dto.home.AcceptFirstSprintRequest;
import com.integrityfamily.dto.home.FamilyActionResult;
import com.integrityfamily.dto.home.JourneyStage;
import com.integrityfamily.dto.home.SprintDisplayStatus;
import com.integrityfamily.familyhome.application.exception.FamilyHomeAccessDeniedException;
import com.integrityfamily.familyhome.application.exception.UnsupportedJourneyStageException;
import com.integrityfamily.familyhome.port.*;
import com.integrityfamily.familyhome.security.FamilyIdentifierBridge;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("FamilyActionEngineImpl — Unit Tests")
class FamilyActionEngineImplTest {

    @Mock FamilyMembershipQueryPort membershipPort;
    @Mock FamilyJourneyQueryPort journeyPort;
    @Mock FamilyInterventionCommandPort interventionCommandPort;
    @Mock FamilyActionExecutionRepository executionRepository;
    @Mock EventPublisher eventPublisher;
    @Mock AuditService auditService;
    @Mock UserRepository userRepository;
    @Mock FamilyIdentifierBridge idBridge;

    FamilyActionEngineImpl engine;

    UUID familyId;
    UUID userId;
    AcceptFirstSprintRequest request;

    @BeforeEach
    void setUp() {
        engine = new FamilyActionEngineImpl(
                membershipPort, journeyPort, interventionCommandPort,
                executionRepository, eventPublisher, auditService, userRepository, idBridge);

        familyId = UUID.randomUUID();
        userId = UUID.randomUUID();
        request = new AcceptFirstSprintRequest(null, null, null, null);
    }

    @Nested
    @DisplayName("acceptFirstSprint()")
    class AcceptFirstSprint {

        @Test
        @DisplayName("usuario que no pertenece a la familia → FamilyHomeAccessDeniedException, sin tocar journey ni sprint")
        void nonMember_isDenied() {
            when(membershipPort.isMember(familyId, userId)).thenReturn(false);

            assertThatThrownBy(() -> engine.acceptFirstSprint(familyId, userId, request, "key-1"))
                    .isInstanceOf(FamilyHomeAccessDeniedException.class);

            verifyNoInteractions(journeyPort, interventionCommandPort, executionRepository, eventPublisher, auditService);
        }

        @Test
        @DisplayName("journeyStage fuera de RETURN_AVAILABLE/FIRST_SPRINT_PENDING → UnsupportedJourneyStageException")
        void wrongStage_isRejected() {
            when(membershipPort.isMember(familyId, userId)).thenReturn(true);
            when(executionRepository.findByFamilyIdAndActionAndIdempotencyKey(
                    familyId.toString(), "accept-first-sprint", "key-1")).thenReturn(Optional.empty());
            when(journeyPort.getJourneyStatus(familyId))
                    .thenReturn(new JourneySnapshot(JourneyStage.ACTIVE_HOME, 1, 3));

            assertThatThrownBy(() -> engine.acceptFirstSprint(familyId, userId, request, "key-1"))
                    .isInstanceOf(UnsupportedJourneyStageException.class);

            verifyNoInteractions(interventionCommandPort);
        }

        @Test
        @DisplayName("ejecución nueva → crea sprint, guarda registro de idempotencia, publica evento y audita")
        void freshExecution_runsFullPipeline() {
            UUID sprintId = UUID.randomUUID();
            when(membershipPort.isMember(familyId, userId)).thenReturn(true);
            when(executionRepository.findByFamilyIdAndActionAndIdempotencyKey(
                    familyId.toString(), "accept-first-sprint", "key-1")).thenReturn(Optional.empty());
            when(journeyPort.getJourneyStatus(familyId))
                    .thenReturn(new JourneySnapshot(JourneyStage.RETURN_AVAILABLE, 0, 0));
            when(interventionCommandPort.acceptFirstSprint(eq(familyId), any()))
                    .thenReturn(new SprintSummarySnapshot(sprintId, SprintDisplayStatus.ACTIVE, "Objetivo", 0, 0, null, null, null));
            when(idBridge.resolveUserId(userId)).thenReturn(Optional.of(9L));
            when(userRepository.findById(9L)).thenReturn(Optional.of(User.builder().id(9L).email("mama@if.com").build()));

            FamilyActionResult result = engine.acceptFirstSprint(familyId, userId, request, "key-1");

            assertThat(result.action()).isEqualTo("accept-first-sprint");
            assertThat(result.status()).isEqualTo("COMPLETED");
            assertThat(result.sprintId()).isEqualTo(sprintId);
            assertThat(result.replayed()).isFalse();

            verify(executionRepository).save(argThat(exec ->
                    exec.getFamilyId().equals(familyId.toString())
                    && exec.getAction().equals("accept-first-sprint")
                    && exec.getIdempotencyKey().equals("key-1")
                    && exec.getResultSprintRef().equals(sprintId.toString())));
            verify(eventPublisher).publish(any());
            verify(auditService).registerSystemEvent(eq("mama@if.com"), any(), any());
        }

        @Test
        @DisplayName("reintento con la misma Idempotency-Key → no vuelve a ejecutar, responde con replayed=true")
        void replay_doesNotReexecute() {
            UUID sprintId = UUID.randomUUID();
            when(membershipPort.isMember(familyId, userId)).thenReturn(true);
            FamilyActionExecution existing = FamilyActionExecution.builder()
                    .familyId(familyId.toString())
                    .action("accept-first-sprint")
                    .idempotencyKey("key-1")
                    .status("COMPLETED")
                    .resultSprintRef(sprintId.toString())
                    .createdAt(LocalDateTime.now().minus(1, ChronoUnit.MINUTES))
                    .build();
            when(executionRepository.findByFamilyIdAndActionAndIdempotencyKey(
                    familyId.toString(), "accept-first-sprint", "key-1")).thenReturn(Optional.of(existing));

            FamilyActionResult result = engine.acceptFirstSprint(familyId, userId, request, "key-1");

            assertThat(result.replayed()).isTrue();
            assertThat(result.sprintId()).isEqualTo(sprintId);
            verifyNoInteractions(journeyPort, interventionCommandPort, eventPublisher, auditService);
            verify(executionRepository, never()).save(any());
        }
    }
}
