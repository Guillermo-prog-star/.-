package com.integrityfamily.familyhome.api;

import com.integrityfamily.dto.home.AcceptFirstSprintRequest;
import com.integrityfamily.dto.home.FamilyActionResult;
import com.integrityfamily.familyhome.application.FamilyActionEngine;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * IFRM-D Hito 5 — punto de entrada HTTP del Family Action Engine. Delgado a propósito:
 * toda la orquestación (autorización, validación de journey, idempotencia, evento,
 * auditoría) vive en {@link FamilyActionEngine}.
 */
@RestController
@RequestMapping("/api/v1/families/{familyId}/actions")
public final class FamilyActionController {

    private final FamilyActionEngine actionEngine;
    private final AuthenticatedUserResolver authenticatedUserResolver;

    public FamilyActionController(FamilyActionEngine actionEngine, AuthenticatedUserResolver authenticatedUserResolver) {
        this.actionEngine = actionEngine;
        this.authenticatedUserResolver = authenticatedUserResolver;
    }

    @PostMapping("/accept-first-sprint")
    public ResponseEntity<FamilyActionResult> acceptFirstSprint(
            @PathVariable UUID familyId,
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @RequestBody(required = false) AcceptFirstSprintRequest request
    ) {
        UUID authenticatedUserId = authenticatedUserResolver.requireAuthenticatedUserId();
        AcceptFirstSprintRequest safeRequest =
                request != null ? request : new AcceptFirstSprintRequest(null, null, null, null);

        FamilyActionResult result =
                actionEngine.acceptFirstSprint(familyId, authenticatedUserId, safeRequest, idempotencyKey);

        HttpStatus status = result.replayed() ? HttpStatus.OK : HttpStatus.CREATED;
        return ResponseEntity.status(status).body(result);
    }
}
