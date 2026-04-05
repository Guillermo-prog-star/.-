package com.integrityfamily.risk.service;

import com.integrityfamily.family.domain.Family;
import com.integrityfamily.risk.domain.RiskSnapshot;
import com.integrityfamily.risk.repository.RiskSnapshotRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class RiskService {

    private final RiskSnapshotRepository riskSnapshotRepository;

    public RiskService(RiskSnapshotRepository riskSnapshotRepository) {
        this.riskSnapshotRepository = riskSnapshotRepository;
    }

    public List<RiskSnapshot> findAll() {
        return riskSnapshotRepository.findAll();
    }

    public RiskSnapshot findById(Long id) {
        return riskSnapshotRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("RiskSnapshot no encontrado"));
    }

    public RiskSnapshot create(RiskSnapshot snapshot) {
        return riskSnapshotRepository.save(snapshot);
    }

    public RiskSnapshot createDefaultForFamily(Family family) {
        RiskSnapshot snapshot = new RiskSnapshot();
        snapshot.setFamily(family);
        snapshot.setRiskLevel("MEDIO");
        snapshot.setGlobalScore(75.0);
        return riskSnapshotRepository.save(snapshot);
    }

    public void delete(Long id) {
        if (!riskSnapshotRepository.existsById(id)) {
            throw new RuntimeException("RiskSnapshot no encontrado");
        }
        riskSnapshotRepository.deleteById(id);
    }
}