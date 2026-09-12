package com.integrityfamily.familyhome.adapter;

import com.integrityfamily.bitacora.dto.SprintDtos.CreateSprintRequest;
import com.integrityfamily.bitacora.dto.SprintDtos.SprintResponse;
import com.integrityfamily.bitacora.service.SprintService;
import com.integrityfamily.dto.home.AcceptFirstSprintRequest;
import com.integrityfamily.dto.home.SprintDisplayStatus;
import com.integrityfamily.familyhome.application.exception.FamilyHomeDataIncompleteException;
import com.integrityfamily.familyhome.port.FamilyInterventionCommandPort;
import com.integrityfamily.familyhome.port.SprintSummarySnapshot;
import com.integrityfamily.familyhome.security.FamilyIdentifierBridge;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;

/**
 * Delega la creación real del sprint en {@link SprintService} (módulo bitácora) — el Family
 * Action Engine no reimplementa la lógica de negocio de sprints, solo la envuelve con
 * autorización, validación de journey e idempotencia (ver {@code FamilyActionEngine}).
 */
@Component
public class FamilyInterventionCommandPortAdapter implements FamilyInterventionCommandPort {

    private static final String DEFAULT_OBJECTIVE = "Primer sprint de transformación familiar";

    private final SprintService sprintService;
    private final FamilyIdentifierBridge idBridge;

    public FamilyInterventionCommandPortAdapter(SprintService sprintService, FamilyIdentifierBridge idBridge) {
        this.sprintService = sprintService;
        this.idBridge = idBridge;
    }

    @Override
    public SprintSummarySnapshot acceptFirstSprint(UUID familyId, AcceptFirstSprintRequest request) {
        Long fId = idBridge.resolveFamilyId(familyId)
                .orElseThrow(() -> new FamilyHomeDataIncompleteException("Family not found: " + familyId));

        CreateSprintRequest createRequest = new CreateSprintRequest(
                request.objective() != null && !request.objective().isBlank() ? request.objective() : DEFAULT_OBJECTIVE,
                request.riskDimension(),
                request.durationDays(),
                request.missions() != null ? request.missions() : List.of(),
                null
        );

        SprintResponse sprint = sprintService.createSprint(fId, createRequest);

        int total = sprint.missions() != null ? sprint.missions().size() : 0;
        int completed = sprint.missions() != null
                ? (int) sprint.missions().stream().filter(m -> m.completedAt() != null).count()
                : 0;

        return new SprintSummarySnapshot(
                UUID.nameUUIDFromBytes(("sprint-" + sprint.id()).getBytes(StandardCharsets.UTF_8)),
                SprintDisplayStatus.ACTIVE,
                sprint.objective(),
                completed,
                total,
                null,
                null,
                null
        );
    }
}
