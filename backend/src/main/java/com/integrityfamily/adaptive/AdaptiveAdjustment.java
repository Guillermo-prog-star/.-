package com.integrityfamily.adaptive;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

public class AdaptiveAdjustment {

    private final UUID id;
    private final Long familyId;
    private final AdaptiveRuleType ruleType;
    private final String reason;
    private AdjustmentStatus status;
    private final LocalDateTime createdAt;

    public AdaptiveAdjustment(Long familyId, AdaptiveRuleType ruleType, String reason) {
        this.id = UUID.randomUUID();
        this.familyId = Objects.requireNonNull(familyId);
        this.ruleType = Objects.requireNonNull(ruleType);
        this.reason = Objects.requireNonNull(reason);
        this.status = AdjustmentStatus.PROPOSED;
        this.createdAt = LocalDateTime.now();
    }

    public UUID getId() {
        return id;
    }

    public Long getFamilyId() {
        return familyId;
    }

    public AdaptiveRuleType getRuleType() {
        return ruleType;
    }

    public String getReason() {
        return reason;
    }

    public AdjustmentStatus getStatus() {
        return status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void approve() {
        if (status != AdjustmentStatus.PROPOSED) {
            throw new IllegalStateException("Only proposed adjustments can be approved.");
        }
        this.status = AdjustmentStatus.APPROVED;
    }

    public void apply() {
        if (status != AdjustmentStatus.APPROVED) {
            throw new IllegalStateException("Only approved adjustments can be applied.");
        }
        this.status = AdjustmentStatus.APPLIED;
    }

    public void reject() {
        if (status != AdjustmentStatus.PROPOSED) {
            throw new IllegalStateException("Only proposed adjustments can be rejected.");
        }
        this.status = AdjustmentStatus.REJECTED;
    }
}
