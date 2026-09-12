package com.integrityfamily.hud.resolver;

import com.integrityfamily.dto.home.FamilyHomeView;
import com.integrityfamily.dto.home.ActiveHomeView;
import com.integrityfamily.domain.FamilyLongitudinalState;
import com.integrityfamily.domain.repository.FamilyLongitudinalStateRepository;
import com.integrityfamily.familyhome.application.FamilyHomeProjectionService;
import com.integrityfamily.familyhome.application.ProjectionRequestContext;
import com.integrityfamily.familyhome.security.FamilyIdentifierBridge;
import com.integrityfamily.hud.dto.FamilyHudView;
import com.integrityfamily.hud.dto.HudType;
import com.integrityfamily.hud.policy.FamilyPresentationPolicy;
import org.springframework.stereotype.Component;
import java.util.*;

@Component
public class FamilyHudProjectionResolver {

    private final FamilyHomeProjectionService familyHomeService;
    private final FamilyLongitudinalStateRepository ltsRepository;
    private final FamilyIdentifierBridge idBridge;

    public FamilyHudProjectionResolver(
            FamilyHomeProjectionService familyHomeService,
            FamilyLongitudinalStateRepository ltsRepository,
            FamilyIdentifierBridge idBridge) {
        this.familyHomeService = familyHomeService;
        this.ltsRepository = ltsRepository;
        this.idBridge = idBridge;
    }

    public FamilyHudView resolve(UUID familyId, UUID authenticatedUserId, ProjectionRequestContext requestContext) {
        FamilyHomeView homeView = familyHomeService.project(familyId, authenticatedUserId, requestContext);

        List<String> navigation = List.of("Hoy", "Crecemos", "Recordamos", "Somos", "Conversamos");

        // Antes: valores hardcodeados (63, 0.61, "MEDIUM") sin relación con la familia
        // real (ver ADR-002, action item 9). Ahora se leen de FamilyLongitudinalState
        // cuando existe; si la familia todavía no tiene estado longitudinal (no ha
        // completado evaluaciones), se usa un mensaje honesto en vez de fabricar cifras.
        Optional<FamilyLongitudinalState> lts = idBridge.resolveFamilyId(familyId)
                .flatMap(ltsRepository::findByFamilyId);

        String statusMessage;
        String paceMessage;
        String communicationMessage;
        if (lts.isPresent() && lts.get().getIcfCurrent() != null) {
            statusMessage = FamilyPresentationPolicy.formatICaF(lts.get().getIcfCurrent().intValue());
            communicationMessage = FamilyPresentationPolicy.formatRisk(lts.get().getCurrentRiskLevel());
            paceMessage = "El ritmo recomendado para esta semana es tranquilo y equilibrado.";
        } else {
            statusMessage = "Aún no hay suficientes datos de esta familia — completen su primera evaluación para ver su estado aquí.";
            paceMessage = "Empiecen con calma: lo importante hoy es dar el primer paso.";
            communicationMessage = "";
        }

        if (homeView instanceof ActiveHomeView activeView) {
            return new FamilyHudView(
                "1.0.0-AdaptiveHUD",
                HudType.FAMILY,
                homeView.viewer(),
                familyId,
                navigation,
                statusMessage,
                paceMessage,
                communicationMessage,
                activeView.today(),
                activeView.activeSprint(),
                activeView.dimensions(),
                homeView.safetyPresentation(),
                homeView.responseMetadata()
            );
        }

        // Fallback for onboarding/other views mapped to family layout
        return new FamilyHudView(
            "1.0.0-AdaptiveHUD",
            HudType.FAMILY,
            homeView.viewer(),
            familyId,
            navigation,
            statusMessage,
            paceMessage,
            communicationMessage,
            null,
            null,
            Map.of(),
            homeView.safetyPresentation(),
            homeView.responseMetadata()
        );
    }
}
