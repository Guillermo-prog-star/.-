package com.integrityfamily.familyhome.api;

import com.integrityfamily.dto.home.FamilyHomeView;
import com.integrityfamily.familyhome.application.FamilyHomeProjectionService;
import com.integrityfamily.familyhome.application.ProjectionChannel;
import com.integrityfamily.familyhome.application.ProjectionRequestContext;
import com.integrityfamily.familyhome.application.exception.UnsupportedContractVersionException;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Clock;
import java.util.Locale;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/families/{familyId}/home")
public final class FamilyHomeController {

    private final FamilyHomeProjectionService projectionService;
    private final AuthenticatedUserResolver authenticatedUserResolver;
    private final Clock clock;

    @Autowired
    public FamilyHomeController(
            FamilyHomeProjectionService projectionService,
            AuthenticatedUserResolver authenticatedUserResolver,
            @Autowired(required = false) Clock clock
    ) {
        this.projectionService = projectionService;
        this.authenticatedUserResolver = authenticatedUserResolver;
        this.clock = clock != null ? clock : Clock.systemDefaultZone();
    }

    @GetMapping
    public ResponseEntity<FamilyHomeView> getFamilyHome(
            @PathVariable UUID familyId,
            Locale locale,
            @RequestHeader(name = "X-Family-Home-Contract-Version", required = false) String requestedContractVersion,
            @RequestHeader(name = "X-Correlation-Id", required = false) String requestedCorrelationId
    ) {
        // 1. Force context security authentication lookup
        UUID authenticatedUserId = authenticatedUserResolver.requireAuthenticatedUserId();

        // 2. Validate contract version compatibility if client requested specific version
        if (requestedContractVersion != null && !requestedContractVersion.equals("0.9.0-Candidate")) {
            throw new UnsupportedContractVersionException(requestedContractVersion);
        }

        // 3. Resolve request correlation ID
        String currentMdcId = MDC.get("correlationId");
        UUID correlationId;
        if (currentMdcId != null) {
            correlationId = UUID.fromString(currentMdcId);
        } else if (requestedCorrelationId != null && requestedCorrelationId.trim().length() == 36) {
            try {
                correlationId = UUID.fromString(requestedCorrelationId.trim());
            } catch (IllegalArgumentException ex) {
                correlationId = UUID.randomUUID();
            }
        } else {
            correlationId = UUID.randomUUID();
        }

        // 4. Build context
        ProjectionRequestContext context = new ProjectionRequestContext(
                locale != null ? locale : Locale.getDefault(),
                ProjectionChannel.WEB,
                correlationId,
                clock.instant(),
                "0.9.0-Candidate"
        );

        // 5. Delegate projection resolution to domain service
        FamilyHomeView view = projectionService.project(familyId, authenticatedUserId, context);

        return ResponseEntity.ok()
                .header("X-Correlation-Id", correlationId.toString())
                .header("X-Family-Home-Contract-Version", view.contractVersion())
                .cacheControl(CacheControl.noStore())
                .body(view);
    }
}
