package com.integrityfamily.familyhome.application;

import com.integrityfamily.common.event.DomainEvent;
import com.integrityfamily.common.event.EventPublisher;
import com.integrityfamily.domain.AuditEventType;
import com.integrityfamily.domain.FamilyActionExecution;
import com.integrityfamily.domain.User;
import com.integrityfamily.domain.repository.FamilyActionExecutionRepository;
import com.integrityfamily.domain.repository.UserRepository;
import com.integrityfamily.auth.service.AuditService;
import com.integrityfamily.dto.home.AcceptFirstSprintRequest;
import com.integrityfamily.dto.home.FamilyActionResult;
import com.integrityfamily.dto.home.JourneyStage;
import com.integrityfamily.familyhome.application.exception.FamilyHomeAccessDeniedException;
import com.integrityfamily.familyhome.application.exception.UnsupportedJourneyStageException;
import com.integrityfamily.familyhome.port.FamilyInterventionCommandPort;
import com.integrityfamily.familyhome.port.FamilyJourneyQueryPort;
import com.integrityfamily.familyhome.port.FamilyMembershipQueryPort;
import com.integrityfamily.familyhome.port.SprintSummarySnapshot;
import com.integrityfamily.familyhome.security.FamilyIdentifierBridge;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
public class FamilyActionEngineImpl implements FamilyActionEngine {

    private static final String ACTION_ACCEPT_FIRST_SPRINT = "accept-first-sprint";
    private static final Set<JourneyStage> ACCEPT_FIRST_SPRINT_ALLOWED_STAGES =
            EnumSet.of(JourneyStage.RETURN_AVAILABLE, JourneyStage.FIRST_SPRINT_PENDING);

    private final FamilyMembershipQueryPort membershipPort;
    private final FamilyJourneyQueryPort journeyPort;
    private final FamilyInterventionCommandPort interventionCommandPort;
    private final FamilyActionExecutionRepository executionRepository;
    private final EventPublisher eventPublisher;
    private final AuditService auditService;
    private final UserRepository userRepository;
    private final FamilyIdentifierBridge idBridge;

    public FamilyActionEngineImpl(
            FamilyMembershipQueryPort membershipPort,
            FamilyJourneyQueryPort journeyPort,
            FamilyInterventionCommandPort interventionCommandPort,
            FamilyActionExecutionRepository executionRepository,
            EventPublisher eventPublisher,
            AuditService auditService,
            UserRepository userRepository,
            FamilyIdentifierBridge idBridge) {
        this.membershipPort = membershipPort;
        this.journeyPort = journeyPort;
        this.interventionCommandPort = interventionCommandPort;
        this.executionRepository = executionRepository;
        this.eventPublisher = eventPublisher;
        this.auditService = auditService;
        this.userRepository = userRepository;
        this.idBridge = idBridge;
    }

    @Override
    @Transactional
    public FamilyActionResult acceptFirstSprint(
            UUID familyId,
            UUID authenticatedUserId,
            AcceptFirstSprintRequest request,
            String idempotencyKey) {

        // 1. Autorización
        if (!membershipPort.isMember(familyId, authenticatedUserId)) {
            throw new FamilyHomeAccessDeniedException(
                    "User " + authenticatedUserId + " is not a member of family " + familyId);
        }

        // 2. Idempotencia: un reintento con la misma clave no vuelve a crear el sprint
        FamilyActionExecution existing = executionRepository
                .findByFamilyIdAndActionAndIdempotencyKey(familyId.toString(), ACTION_ACCEPT_FIRST_SPRINT, idempotencyKey)
                .orElse(null);
        if (existing != null) {
            return new FamilyActionResult(
                    ACTION_ACCEPT_FIRST_SPRINT,
                    existing.getStatus(),
                    existing.getResultSprintRef() != null ? UUID.fromString(existing.getResultSprintRef()) : null,
                    existing.getCreatedAt().atZone(java.time.ZoneId.systemDefault()).toInstant(),
                    true
            );
        }

        // 3. Validación de journey: solo se puede aceptar el primer sprint en estos estados
        JourneyStage currentStage = journeyPort.getJourneyStatus(familyId).stage();
        if (!ACCEPT_FIRST_SPRINT_ALLOWED_STAGES.contains(currentStage)) {
            throw new UnsupportedJourneyStageException(currentStage);
        }

        // 4. Ejecutar la acción de dominio real
        SprintSummarySnapshot sprint = interventionCommandPort.acceptFirstSprint(familyId, request);

        // 5. Registrar ejecución (idempotencia futura)
        executionRepository.save(FamilyActionExecution.builder()
                .familyId(familyId.toString())
                .action(ACTION_ACCEPT_FIRST_SPRINT)
                .idempotencyKey(idempotencyKey)
                .status("COMPLETED")
                .resultSprintRef(sprint.sprintId().toString())
                .build());

        // 6. Evento de dominio (sin contenido sensible: solo IDs, no texto libre de la familia)
        eventPublisher.publish(DomainEvent.of(
                "FAMILY_FIRST_SPRINT_ACCEPTED",
                "Family",
                null,
                Map.of("familyId", familyId.toString(), "sprintId", sprint.sprintId().toString())
        ));

        // 7. Auditoría (sin contenido sensible)
        String actorEmail = idBridge.resolveUserId(authenticatedUserId)
                .flatMap(userRepository::findById)
                .map(User::getEmail)
                .orElse("UNKNOWN");
        auditService.registerSystemEvent(
                actorEmail,
                AuditEventType.FAMILY_HOME_ACTION_EXECUTED,
                "{\"action\":\"" + ACTION_ACCEPT_FIRST_SPRINT + "\",\"familyId\":\"" + familyId + "\"}"
        );

        return new FamilyActionResult(ACTION_ACCEPT_FIRST_SPRINT, "COMPLETED", sprint.sprintId(), Instant.now(), false);
    }
}
