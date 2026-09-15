package com.integrityfamily.hud.api;

import com.integrityfamily.familyhome.api.AuthenticatedUserResolver;
import com.integrityfamily.familyhome.application.ProjectionChannel;
import com.integrityfamily.familyhome.application.ProjectionRequestContext;
import com.integrityfamily.hud.application.AdaptiveHudProjectionService;
import com.integrityfamily.hud.dto.FamilyHudView;
import com.integrityfamily.hud.dto.ProfessionalHudView;
import com.integrityfamily.hud.dto.HudType;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.time.Clock;
import java.util.Locale;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/families/{familyId}/hud")
public final class AdaptiveHudController {

    private final AdaptiveHudProjectionService hudProjectionService;
    private final AuthenticatedUserResolver authenticatedUserResolver;
    private final Clock clock;

    @Autowired
    public AdaptiveHudController(
            AdaptiveHudProjectionService hudProjectionService,
            AuthenticatedUserResolver authenticatedUserResolver,
            @Autowired(required = false) Clock clock
    ) {
        this.hudProjectionService = hudProjectionService;
        this.authenticatedUserResolver = authenticatedUserResolver;
        this.clock = clock != null ? clock : Clock.systemDefaultZone();
    }

    @GetMapping("/family")
    public ResponseEntity<FamilyHudView> getFamilyHud(
            @PathVariable UUID familyId,
            Locale locale,
            @RequestHeader(name = "X-Correlation-Id", required = false) String requestedCorrelationId
    ) {
        UUID authenticatedUserId = authenticatedUserResolver.requireAuthenticatedUserId();
        ProjectionRequestContext requestContext = buildContext(locale, requestedCorrelationId);

        FamilyHudView view = (FamilyHudView) hudProjectionService.project(
            familyId, authenticatedUserId, requestContext, HudType.FAMILY
        );

        return ResponseEntity.ok()
                .header("X-Correlation-Id", requestContext.correlationId().toString())
                .header("X-Family-Home-Contract-Version", view.contractVersion())
                .cacheControl(CacheControl.noStore())
                .body(view);
    }

    @GetMapping("/professional")
    public ResponseEntity<ProfessionalHudView> getProfessionalHud(
            @PathVariable UUID familyId,
            Locale locale,
            @RequestHeader(name = "X-Correlation-Id", required = false) String requestedCorrelationId
    ) {
        UUID authenticatedUserId = authenticatedUserResolver.requireAuthenticatedUserId();
        ProjectionRequestContext requestContext = buildContext(locale, requestedCorrelationId);

        ProfessionalHudView view = (ProfessionalHudView) hudProjectionService.project(
            familyId, authenticatedUserId, requestContext, HudType.PROFESSIONAL
        );

        return ResponseEntity.ok()
                .header("X-Correlation-Id", requestContext.correlationId().toString())
                .header("X-Family-Home-Contract-Version", view.contractVersion())
                .cacheControl(CacheControl.noStore())
                .body(view);
    }

    private ProjectionRequestContext buildContext(Locale locale, String requestedCorrelationId) {
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

        return new ProjectionRequestContext(
                locale != null ? locale : Locale.getDefault(),
                ProjectionChannel.WEB,
                correlationId,
                clock.instant(),
                "1.0.0-AdaptiveHUD"
        );
    }
}
