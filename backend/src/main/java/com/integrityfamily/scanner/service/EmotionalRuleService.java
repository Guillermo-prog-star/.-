package com.integrityfamily.scanner.service;

import com.integrityfamily.scanner.domain.EmotionalRule;
import com.integrityfamily.scanner.dto.EmotionalRuleDto;
import com.integrityfamily.scanner.dto.EmotionalRuleRequest;
import com.integrityfamily.scanner.repository.EmotionalRuleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmotionalRuleService {

    private final EmotionalRuleRepository repo;

    public List<EmotionalRuleDto> findAll() {
        return repo.findAllWithSignals().stream().map(this::toDto).toList();
    }

    public List<EmotionalRuleDto> findActive() {
        return repo.findByActiveTrue().stream().map(this::toDto).toList();
    }

    @Transactional
    public EmotionalRuleDto toggleActive(Long id) {
        EmotionalRule rule = repo.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Regla no encontrada: " + id));
        rule.setActive(!rule.isActive());
        log.info("[EEDSL] Regla '{}' v{} → active={}", rule.getRuleKey(), rule.getVersion(), rule.isActive());
        return toDto(repo.save(rule));
    }

    @Transactional
    public EmotionalRuleDto create(EmotionalRuleRequest req) {
        // Determinar la siguiente versión para esta ruleKey
        List<EmotionalRule> existing = repo.findByRuleKeyOrderByVersionDesc(req.ruleKey());
        int nextVersion = existing.isEmpty() ? 1 : existing.get(0).getVersion() + 1;

        EmotionalRule rule = EmotionalRule.builder()
                .ruleKey(req.ruleKey())
                .version(nextVersion)
                .active(true)
                .milestoneScope(req.milestoneScope() != null ? req.milestoneScope() : "*")
                .memberRole(req.memberRole()     != null ? req.memberRole()     : "*")
                .requiredSignals(req.requiredSignals() != null ? new ArrayList<>(req.requiredSignals()) : new ArrayList<>())
                .temporalWindowDays(req.temporalWindowDays() != null ? req.temporalWindowDays() : 14)
                .projectionLabel(req.projectionLabel())
                .confidenceBase(req.confidenceBase() != null ? req.confidenceBase() : 0.70)
                .riskOutput(req.riskOutput())
                .createdBy("ADMIN")
                .build();

        log.info("[EEDSL] Nueva regla creada: '{}' v{}", rule.getRuleKey(), rule.getVersion());
        return toDto(repo.save(rule));
    }

    @Transactional
    public EmotionalRuleDto update(Long id, EmotionalRuleRequest req) {
        EmotionalRule rule = repo.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Regla no encontrada: " + id));

        if (req.milestoneScope()    != null) rule.setMilestoneScope(req.milestoneScope());
        if (req.memberRole()        != null) rule.setMemberRole(req.memberRole());
        if (req.requiredSignals()   != null) rule.setRequiredSignals(new ArrayList<>(req.requiredSignals()));
        if (req.temporalWindowDays()!= null) rule.setTemporalWindowDays(req.temporalWindowDays());
        if (req.projectionLabel()   != null) rule.setProjectionLabel(req.projectionLabel());
        if (req.confidenceBase()    != null) rule.setConfidenceBase(req.confidenceBase());
        if (req.riskOutput()        != null) rule.setRiskOutput(req.riskOutput());

        log.info("[EEDSL] Regla '{}' v{} actualizada", rule.getRuleKey(), rule.getVersion());
        return toDto(repo.save(rule));
    }

    // ── Mapper ───────────────────────────────────────────────────────────────

    private EmotionalRuleDto toDto(EmotionalRule r) {
        return new EmotionalRuleDto(
                r.getId(),
                r.getRuleKey(),
                r.getVersion(),
                r.isActive(),
                r.getMilestoneScope(),
                r.getMemberRole(),
                r.getRequiredSignals() != null ? List.copyOf(r.getRequiredSignals()) : List.of(),
                r.getTemporalWindowDays(),
                r.getProjectionLabel(),
                r.getConfidenceBase(),
                r.getRiskOutput(),
                r.getCreatedBy(),
                r.getCreatedAt()
        );
    }
}
