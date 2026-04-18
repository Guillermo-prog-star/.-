package com.integrityfamily.risk.service;

import com.integrityfamily.family.domain.Family;
import com.integrityfamily.risk.domain.RiskSnapshot;
import com.integrityfamily.risk.repository.RiskSnapshotRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class RiskService {

    private static final Logger log = LoggerFactory.getLogger(RiskService.class);
    private final RiskSnapshotRepository riskSnapshotRepository;

    public RiskService(RiskSnapshotRepository riskSnapshotRepository) {
        this.riskSnapshotRepository = riskSnapshotRepository;
    }

    public RiskSnapshot findById(Long id) {
        return riskSnapshotRepository.findById(id).orElseThrow();
    }

    public java.util.List<RiskSnapshot> findAll() {
        return riskSnapshotRepository.findAll();
    }

    public RiskSnapshot create(RiskSnapshot snapshot) {
        return riskSnapshotRepository.save(snapshot);
    }

    @Transactional
    public RiskSnapshot calculateAndCreate(Family family, Double icf, boolean hasCrisis) {
        log.info("🛡️ [SENTINEL-ENGINE] Evaluando integridad para familia: {}", family.getName());

        int conLevel = calculateConsciousnessLevel(icf);
        
        RiskSnapshot snapshot = new RiskSnapshot();
        snapshot.setFamily(family);
        snapshot.setIcf(icf);
        snapshot.setRiskLevel(calculateRiskLevel(icf, hasCrisis));
        snapshot.setHasCrisis(hasCrisis);
        snapshot.setConsciousnessLevel(conLevel);
        snapshot.setConsciousnessLabel(getLabel(conLevel));
        
        return riskSnapshotRepository.save(snapshot);
    }

    private String calculateRiskLevel(Double icf, boolean hasCrisis) {
        if (hasCrisis) return "ALTO"; // REGLA ORO: Crisis > Promedio
        if (icf < 50) return "ALTO";
        if (icf < 75) return "MEDIO";
        return "BAJO";
    }

    private int calculateConsciousnessLevel(Double icf) {
        if (icf < 20) return 1;
        if (icf < 40) return 2;
        if (icf < 60) return 3;
        if (icf < 80) return 4;
        return 5;
    }

    private String getLabel(int level) {
        return switch (level) {
            case 1 -> "Inconsciente";
            case 2 -> "Reactiva";
            case 3 -> "Consciente";
            case 4 -> "Madurando";
            case 5 -> "Plena";
            default -> "Indefinido";
        };
    }
}