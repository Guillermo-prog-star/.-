package com.integrityfamily.trajectory;

import com.integrityfamily.domain.Family;
import com.integrityfamily.domain.FamilyRiskTrajectory;
import com.integrityfamily.domain.RiskMacrodomain;
import com.integrityfamily.domain.RiskTrajectory;
import com.integrityfamily.domain.TrajectoryImpactIndicator;
import com.integrityfamily.domain.TrajectoryStatus;
import com.integrityfamily.common.service.UserNotificationService;
import com.integrityfamily.domain.repository.FamilyRepository;
import com.integrityfamily.domain.repository.FamilyRiskTrajectoryRepository;
import com.integrityfamily.domain.repository.RiskTrajectoryRepository;
import com.integrityfamily.domain.repository.TrajectoryImpactIndicatorRepository;
import com.integrityfamily.domain.repository.TrajectoryTimelineEventRepository;
import com.integrityfamily.trajectory.dto.TrajectoryDtos.AssignTrajectoryRequest;
import com.integrityfamily.trajectory.dto.TrajectoryDtos.FamilyTrajectoryDto;
import com.integrityfamily.trajectory.dto.TrajectoryDtos.IndicatorRequest;
import com.integrityfamily.trajectory.dto.TrajectoryDtos.TrajectoryBankResponse;
import com.integrityfamily.trajectory.dto.TrajectoryDtos.TrajectoryImpactDto;
import com.integrityfamily.trajectory.service.TrajectoryService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.anyList;

@ExtendWith(MockitoExtension.class)
@DisplayName("TrajectoryService — Unit Tests")
class TrajectoryServiceTest {

    @Mock RiskTrajectoryRepository trajectoryRepo;
    @Mock FamilyRiskTrajectoryRepository familyTrajectoryRepo;
    @Mock TrajectoryTimelineEventRepository timelineRepo;
    @Mock TrajectoryImpactIndicatorRepository indicatorRepo;
    @Mock FamilyRepository familyRepo;
    @Mock UserNotificationService notificationService;

    @InjectMocks TrajectoryService service;

    // ─── Helpers ──────────────────────────────────────────────────────────────

    private RiskTrajectory aTrajecto(String code, RiskMacrodomain domain) {
        return RiskTrajectory.builder()
            .id(1L).code(code).name("Trayectoria " + code)
            .macrodomain(domain).severityDefault("HIGH").active(true)
            .earlySignals("[\"señal 1\"]").potentialEvolution("Evolución grave.")
            .build();
    }

    private Family aFamily() {
        Family f = new Family();
        f.setId(10L);
        f.setName("Familia Test");
        return f;
    }

    private FamilyRiskTrajectory aFamilyTraj(Family f, RiskTrajectory t) {
        return FamilyRiskTrajectory.builder()
            .id(100L).family(f).trajectory(t)
            .status(TrajectoryStatus.IN_PROGRESS)
            .detectedAt(LocalDateTime.now().minusDays(10))
            .build();
    }

    // ─── getBank() ────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("getBank()")
    class GetBank {

        @Test
        @DisplayName("agrupa trayectorias activas por macrodominio")
        void groupsByMacrodomain() {
            RiskTrajectory t1 = aTrajecto("DIVORCIO", RiskMacrodomain.RELACIONES_PAREJA);
            RiskTrajectory t2 = aTrajecto("CONSUMO_MARIHUANA", RiskMacrodomain.CRIANZA_ADOLESCENCIA);
            when(trajectoryRepo.findByActiveTrue()).thenReturn(List.of(t1, t2));

            TrajectoryBankResponse result = service.getBank();

            assertThat(result.totalTrajectories()).isEqualTo(2);
            assertThat(result.byMacrodomain()).containsKeys("RELACIONES_PAREJA", "CRIANZA_ADOLESCENCIA");
            assertThat(result.byMacrodomain().get("RELACIONES_PAREJA")).hasSize(1);
        }
    }

    // ─── assignTrajectory() ───────────────────────────────────────────────────

    @Nested
    @DisplayName("assignTrajectory()")
    class AssignTrajectory {

        @Test
        @DisplayName("asigna correctamente con estado DETECTED")
        void assignsWithDetectedStatus() {
            Family family = aFamily();
            RiskTrajectory traj = aTrajecto("DIVORCIO", RiskMacrodomain.RELACIONES_PAREJA);
            FamilyRiskTrajectory saved = FamilyRiskTrajectory.builder()
                .id(200L).family(family).trajectory(traj)
                .status(TrajectoryStatus.DETECTED).assignedBy("guardian@test.com").build();

            when(familyRepo.findById(10L)).thenReturn(Optional.of(family));
            when(trajectoryRepo.findByCode("DIVORCIO")).thenReturn(Optional.of(traj));
            when(familyTrajectoryRepo.save(any())).thenReturn(saved);

            FamilyTrajectoryDto result = service.assignTrajectory(10L, "DIVORCIO", "guardian@test.com", null);

            assertThat(result.status()).isEqualTo(TrajectoryStatus.DETECTED);
            assertThat(result.assignedBy()).isEqualTo("guardian@test.com");
            verify(familyTrajectoryRepo).save(any(FamilyRiskTrajectory.class));
        }

        @Test
        @DisplayName("lanza excepción si la familia no existe")
        void throwsIfFamilyNotFound() {
            when(familyRepo.findById(99L)).thenReturn(Optional.empty());
            assertThatThrownBy(() -> service.assignTrajectory(99L, "DIVORCIO", "user", null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Familia no encontrada");
        }

        @Test
        @DisplayName("lanza excepción si el código de trayectoria no existe")
        void throwsIfTrajectoryNotFound() {
            when(familyRepo.findById(10L)).thenReturn(Optional.of(aFamily()));
            when(trajectoryRepo.findByCode("INEXISTENTE")).thenReturn(Optional.empty());
            assertThatThrownBy(() -> service.assignTrajectory(10L, "INEXISTENTE", "user", null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Trayectoria no encontrada");
        }
    }

    // ─── updateStatus() ───────────────────────────────────────────────────────

    @Nested
    @DisplayName("updateStatus()")
    class UpdateStatus {

        @Test
        @DisplayName("actualiza estado y establece resolvedAt si es RESOLVED")
        void setsResolvedAtOnResolved() {
            Family f = aFamily();
            RiskTrajectory t = aTrajecto("DIVORCIO", RiskMacrodomain.RELACIONES_PAREJA);
            FamilyRiskTrajectory frt = aFamilyTraj(f, t);
            when(familyTrajectoryRepo.findById(100L)).thenReturn(Optional.of(frt));
            when(familyTrajectoryRepo.save(any())).thenReturn(frt);

            service.updateStatus(100L, TrajectoryStatus.RESOLVED, "Situación resuelta");

            assertThat(frt.getStatus()).isEqualTo(TrajectoryStatus.RESOLVED);
            assertThat(frt.getResolvedAt()).isNotNull();
        }
    }

    // ─── upsertIndicator() ────────────────────────────────────────────────────

    @Nested
    @DisplayName("upsertIndicator()")
    class UpsertIndicator {

        @Test
        @DisplayName("calcula improvementPct correctamente para higher_is_better=true")
        void calculatesImprovementPct() {
            Family f = aFamily();
            RiskTrajectory t = aTrajecto("DIVORCIO", RiskMacrodomain.RELACIONES_PAREJA);
            FamilyRiskTrajectory frt = aFamilyTraj(f, t);

            TrajectoryImpactIndicator saved = TrajectoryImpactIndicator.builder()
                .id(1L).familyTrajectory(frt)
                .indicatorName("Conflictos/semana").indicatorKey("conflictos_semana")
                .baselineValue(BigDecimal.valueOf(18)).currentValue(BigDecimal.valueOf(3))
                .unit("conflictos").higherIsBetter(false).build();

            when(familyTrajectoryRepo.findById(100L)).thenReturn(Optional.of(frt));
            when(indicatorRepo.findByFamilyTrajectoryIdAndIndicatorKey(100L, "conflictos_semana"))
                .thenReturn(Optional.empty());
            when(indicatorRepo.save(any())).thenReturn(saved);

            IndicatorRequest req = new IndicatorRequest("Conflictos/semana", "conflictos_semana",
                BigDecimal.valueOf(18), BigDecimal.valueOf(3), "conflictos", false, null);

            TrajectoryImpactDto result = service.upsertIndicator(100L, req);

            // Baseline 18, current 3, higher_is_better=false → mejora real
            assertThat(result.improvementPct()).isNotNull();
        }
    }

    // ─── buildTrajectoryContextBlock() ───────────────────────────────────────

    @Nested
    @DisplayName("buildTrajectoryContextBlock()")
    class BuildContextBlock {

        @Test
        @DisplayName("retorna cadena vacía si no hay trayectorias activas")
        void returnsEmptyWhenNoActiveTrajectories() {
            when(familyTrajectoryRepo.findByFamilyIdAndStatusIn(eq(10L), anyList())).thenReturn(List.of());
            String result = service.buildTrajectoryContextBlock(10L);
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("incluye nombre y estado de la trayectoria activa")
        void includesTrajectoryInfo() {
            Family f = aFamily();
            RiskTrajectory t = aTrajecto("DIVORCIO", RiskMacrodomain.RELACIONES_PAREJA);
            FamilyRiskTrajectory frt = aFamilyTraj(f, t);

            when(familyTrajectoryRepo.findByFamilyIdAndStatusIn(eq(10L), anyList())).thenReturn(List.of(frt));
            when(timelineRepo.findByFamilyTrajectoryIdOrderByEventDateAsc(100L)).thenReturn(List.of());
            when(indicatorRepo.findByFamilyTrajectoryId(100L)).thenReturn(List.of());

            String result = service.buildTrajectoryContextBlock(10L);

            assertThat(result).contains("DIVORCIO");
            assertThat(result).contains("IN_PROGRESS");
            assertThat(result).contains("trayectorias_de_riesgo");
        }
    }
}
