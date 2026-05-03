package com.integrityfamily.risk.dto;
import com.integrityfamily.domain.RiskLevel;
import java.math.BigDecimal; import java.time.LocalDateTime;
public class RiskDtos {
    public record RiskHistoryResponse(
            Long id, Long evaluationId, RiskLevel riskLevel,
            BigDecimal scoreEmotions, BigDecimal scoreCommunication,
            BigDecimal scoreHabits, BigDecimal scoreTimes,
            BigDecimal globalScore, LocalDateTime createdAt) {}
}


