package com.integrityfamily.familyhome.adapter;

import com.integrityfamily.domain.FamilyRiskTrajectory;
import com.integrityfamily.domain.repository.FamilyRiskTrajectoryRepository;
import com.integrityfamily.domain.repository.SafetyProtocolActivationRepository;
import com.integrityfamily.dto.home.SafetyMode;
import com.integrityfamily.familyhome.port.FamilySafetyQueryPort;
import com.integrityfamily.familyhome.port.SafetyPresentationCandidate;
import com.integrityfamily.familyhome.security.FamilyIdentifierBridge;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

/** Se apoya en el Protocolo de Seguridad (V97-V101): trayectoria con activación abierta -> alerta visible. */
@Component
public class FamilySafetyQueryPortAdapter implements FamilySafetyQueryPort {

    private static final SafetyPresentationCandidate NONE =
            new SafetyPresentationCandidate(SafetyMode.NONE, null, null);

    private final FamilyRiskTrajectoryRepository riskTrajectoryRepository;
    private final SafetyProtocolActivationRepository safetyProtocolActivationRepository;
    private final FamilyIdentifierBridge idBridge;

    public FamilySafetyQueryPortAdapter(
            FamilyRiskTrajectoryRepository riskTrajectoryRepository,
            SafetyProtocolActivationRepository safetyProtocolActivationRepository,
            FamilyIdentifierBridge idBridge) {
        this.riskTrajectoryRepository = riskTrajectoryRepository;
        this.safetyProtocolActivationRepository = safetyProtocolActivationRepository;
        this.idBridge = idBridge;
    }

    @Override
    public SafetyPresentationCandidate getSafetyAlert(UUID familyId) {
        Optional<Long> fId = idBridge.resolveFamilyId(familyId);
        if (fId.isEmpty()) {
            return NONE;
        }

        boolean hasOpenActivation = riskTrajectoryRepository.findByFamilyId(fId.get()).stream()
                .map(FamilyRiskTrajectory::getId)
                .anyMatch(trajectoryId ->
                        !safetyProtocolActivationRepository.findByFamilyTrajectoryIdAndClosedFalse(trajectoryId).isEmpty());

        if (!hasOpenActivation) {
            return NONE;
        }

        return new SafetyPresentationCandidate(
                SafetyMode.URGENT_GUIDANCE,
                "Protocolo de seguridad activo",
                "Tu familia cuenta con acompañamiento activo por parte de un profesional. Revisa las indicaciones junto a tu red de apoyo."
        );
    }
}
