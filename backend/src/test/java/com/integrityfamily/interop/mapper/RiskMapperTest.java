package com.integrityfamily.interop.mapper;

import com.integrityfamily.domain.*;
import com.integrityfamily.interop.canonical.Risk;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("RiskMapper")
class RiskMapperTest {

    @Test
    @DisplayName("mapea trayectoria activa + item del banco")
    void shouldMapActiveTrajectory() {
        Family family = Family.builder().id(1L).build();
        RiskTrajectory bankItem = RiskTrajectory.builder()
                .id(5L).code("VIOLENCIA_INTRAFAMILIAR").name("Violencia intrafamiliar")
                .macrodomain(RiskMacrodomain.SALUD_MENTAL)
                .severityDefault("CRITICAL")
                .requiresSafetyProtocol(true)
                .active(true)
                .build();

        FamilyRiskTrajectory active = FamilyRiskTrajectory.builder()
                .id(20L).family(family).trajectory(bankItem)
                .status(TrajectoryStatus.DETECTED)
                .detectedAt(LocalDateTime.of(2026, 7, 1, 9, 0))
                .build();

        Risk risk = RiskMapper.toCanonical(active);

        assertThat(risk.canonicalId()).isEqualTo("family-risk-trajectory-20");
        assertThat(risk.subjectId()).isEqualTo("family-1");
        assertThat(risk.code()).isEqualTo("VIOLENCIA_INTRAFAMILIAR");
        assertThat(risk.display()).isEqualTo("Violencia intrafamiliar");
        assertThat(risk.macrodomain()).isEqualTo("SALUD_MENTAL");
        assertThat(risk.severity()).isEqualTo("CRITICAL");
        assertThat(risk.requiresSafetyProtocol()).isTrue();
        assertThat(risk.status()).isEqualTo("DETECTED");
        assertThat(risk.resolvedAt()).isNull();
    }

    @Test
    @DisplayName("requiresSafetyProtocol null en el banco → se mapea como false, no NPE")
    void shouldDefaultRequiresSafetyProtocolToFalse() {
        Family family = Family.builder().id(1L).build();
        RiskTrajectory bankItem = RiskTrajectory.builder()
                .id(6L).code("OTRA").name("Otra trayectoria")
                .macrodomain(RiskMacrodomain.GOBERNANZA)
                .severityDefault("LOW")
                .requiresSafetyProtocol(null)
                .build();
        FamilyRiskTrajectory active = FamilyRiskTrajectory.builder()
                .id(21L).family(family).trajectory(bankItem).status(TrajectoryStatus.RESOLVED)
                .resolvedAt(LocalDateTime.now())
                .build();

        Risk risk = RiskMapper.toCanonical(active);

        assertThat(risk.requiresSafetyProtocol()).isFalse();
        assertThat(risk.resolvedAt()).isNotNull();
    }
}
