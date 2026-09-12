package com.integrityfamily.vitality.service;

import com.integrityfamily.common.exception.BusinessException;
import com.integrityfamily.domain.EvidenceSource;
import com.integrityfamily.domain.Family;
import com.integrityfamily.domain.FamilyMember;
import com.integrityfamily.domain.HypothesisEvidence;
import com.integrityfamily.domain.repository.HypothesisEvidenceRepository;
import com.integrityfamily.domain.repository.MemberRepository;
import com.integrityfamily.vitality.domain.DailyVitalityLog;
import com.integrityfamily.vitality.repository.DailyVitalityLogRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("RecoveryIndexService — Unit Tests")
class RecoveryIndexServiceTest {

    @Mock DailyVitalityLogRepository logRepository;
    @Mock HypothesisEvidenceRepository hypothesisEvidenceRepository;
    @Mock MemberRepository memberRepository;

    @InjectMocks
    RecoveryIndexService recoveryIndexService;

    private Family family;
    private FamilyMember member;

    @BeforeEach
    void setUp() {
        family = Family.builder().id(1L).name("Familia López").build();
        member = FamilyMember.builder().id(10L).fullName("Ana López").family(family).build();
    }

    // ═══════════════════════════════════════════════════════════════════
    //  compute() — fórmula v1, sin tocar repositorios
    // ═══════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("compute()")
    class Compute {

        @Test
        @DisplayName("Sin logs → null (ningún componente presente)")
        void shouldReturnNull_whenNoLogs() {
            assertThat(recoveryIndexService.compute(List.of())).isNull();
        }

        @Test
        @DisplayName("Solo sleepHours=8.0 → sleepComponent=100, resto ausente → índice=100")
        void shouldComputeSleepOnly() {
            DailyVitalityLog log = DailyVitalityLog.builder().sleepHours(8.0).build();
            assertThat(recoveryIndexService.compute(List.of(log))).isEqualTo(100.0);
        }

        @Test
        @DisplayName("sleepHours=4.0 (mitad de 8h) → sleepComponent=50")
        void shouldCapAndScaleSleepHours() {
            DailyVitalityLog log = DailyVitalityLog.builder().sleepHours(4.0).build();
            assertThat(recoveryIndexService.compute(List.of(log))).isEqualTo(50.0);
        }

        @Test
        @DisplayName("sleepHours=10.0 (por encima de 8h) → cap en 100, no supera el máximo")
        void shouldCapSleepHoursAt100() {
            DailyVitalityLog log = DailyVitalityLog.builder().sleepHours(10.0).build();
            assertThat(recoveryIndexService.compute(List.of(log))).isEqualTo(100.0);
        }

        @Test
        @DisplayName("fatigueLevel=1 (sin fatiga) → componente invertido=100; fatigueLevel=5 → componente=20")
        void shouldInvertFatigueScore() {
            DailyVitalityLog lowFatigue = DailyVitalityLog.builder().fatigueLevel(1).build();
            DailyVitalityLog highFatigue = DailyVitalityLog.builder().fatigueLevel(5).build();

            assertThat(recoveryIndexService.compute(List.of(lowFatigue))).isEqualTo(100.0);
            assertThat(recoveryIndexService.compute(List.of(highFatigue))).isEqualTo(20.0);
        }

        @Test
        @DisplayName("Todos los campos completos → media de los 4 componentes")
        void shouldAverageAllComponents_whenFullyPopulated() {
            // sleepHours=8 -> 100, sleepQuality=5 -> 100 => sleepComponent = 100
            // exerciseMinutes=30 -> 100
            // nutritionQuality=5 -> 100
            // fatigueLevel=1 -> 100
            DailyVitalityLog log = DailyVitalityLog.builder()
                    .sleepHours(8.0).sleepQuality(5)
                    .exerciseMinutes(30)
                    .nutritionQuality(5)
                    .fatigueLevel(1)
                    .build();

            assertThat(recoveryIndexService.compute(List.of(log))).isEqualTo(100.0);
        }

        @Test
        @DisplayName("Promedia entre varios días de la ventana, ignorando campos ausentes por día")
        void shouldAverageAcrossMultipleDays() {
            DailyVitalityLog day1 = DailyVitalityLog.builder().sleepHours(8.0).build(); // 100
            DailyVitalityLog day2 = DailyVitalityLog.builder().sleepHours(4.0).build(); // 50

            // promedio de sleepHours = 6.0 -> sleepComponent = 6/8*100 = 75
            assertThat(recoveryIndexService.compute(List.of(day1, day2))).isEqualTo(75.0);
        }
    }

    // ═══════════════════════════════════════════════════════════════════
    //  calculateAndRecord()
    // ═══════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("calculateAndRecord()")
    class CalculateAndRecord {

        @Test
        @DisplayName("Miembro inexistente → BusinessException")
        void shouldThrow_whenMemberNotFound() {
            when(memberRepository.findById(10L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> recoveryIndexService.calculateAndRecord(1L, 10L, 7))
                    .isInstanceOf(BusinessException.class);
        }

        @Test
        @DisplayName("Miembro de otra familia → BusinessException")
        void shouldThrow_whenMemberBelongsToDifferentFamily() {
            when(memberRepository.findById(10L)).thenReturn(Optional.of(member));

            assertThatThrownBy(() -> recoveryIndexService.calculateAndRecord(99L, 10L, 7))
                    .isInstanceOf(BusinessException.class);
        }

        @Test
        @DisplayName("Ventana sin logs → retorna null y NO escribe evidencia")
        void shouldReturnNullAndSkipEvidence_whenWindowEmpty() {
            when(memberRepository.findById(10L)).thenReturn(Optional.of(member));
            when(logRepository.findByFamilyMemberIdAndLogDateBetweenOrderByLogDateAsc(eq(10L), any(), any()))
                    .thenReturn(List.of());

            Double result = recoveryIndexService.calculateAndRecord(1L, 10L, 7);

            assertThat(result).isNull();
            verify(hypothesisEvidenceRepository, never()).save(any());
        }

        @Test
        @DisplayName("Con datos → escribe fila en hypothesis_evidence con los campos correctos")
        void shouldWriteEvidence_whenDataPresent() {
            DailyVitalityLog log = DailyVitalityLog.builder().sleepHours(8.0).build();
            when(memberRepository.findById(10L)).thenReturn(Optional.of(member));
            when(logRepository.findByFamilyMemberIdAndLogDateBetweenOrderByLogDateAsc(eq(10L), any(), any()))
                    .thenReturn(List.of(log));

            Double result = recoveryIndexService.calculateAndRecord(1L, 10L, 7);

            assertThat(result).isEqualTo(100.0);

            ArgumentCaptor<HypothesisEvidence> captor = ArgumentCaptor.forClass(HypothesisEvidence.class);
            verify(hypothesisEvidenceRepository).save(captor.capture());

            HypothesisEvidence evidence = captor.getValue();
            assertThat(evidence.getHypothesis()).isEqualTo("RECOVERY_INDEX_HYPOTHESIS");
            assertThat(evidence.getHypothesisVersion()).isEqualTo("v1");
            assertThat(evidence.getSubjectType()).isEqualTo("FAMILY_MEMBER");
            assertThat(evidence.getSubjectId()).isEqualTo(10L);
            assertThat(evidence.getMeasurementType()).isEqualTo("RECOVERY_INDEX");
            assertThat(evidence.getMeasurementValue()).isEqualTo(100.0);
            assertThat(evidence.getInstrument()).isEqualTo("RECOVERY_INDEX_V1");
            assertThat(evidence.getSource()).isEqualTo(EvidenceSource.DERIVED);
            assertThat(evidence.getObservedAt()).isNotNull();
        }
    }

    // ═══════════════════════════════════════════════════════════════════
    //  semaphore()
    // ═══════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("semaphore()")
    class Semaphore {

        @Test
        @DisplayName("null → null")
        void shouldReturnNull_whenIndexNull() {
            assertThat(recoveryIndexService.semaphore(null)).isNull();
        }

        @Test
        @DisplayName(">=70 → GREEN")
        void shouldReturnGreen() {
            assertThat(recoveryIndexService.semaphore(70.0)).isEqualTo("GREEN");
            assertThat(recoveryIndexService.semaphore(95.0)).isEqualTo("GREEN");
        }

        @Test
        @DisplayName("40-69 → YELLOW")
        void shouldReturnYellow() {
            assertThat(recoveryIndexService.semaphore(40.0)).isEqualTo("YELLOW");
            assertThat(recoveryIndexService.semaphore(69.9)).isEqualTo("YELLOW");
        }

        @Test
        @DisplayName("<40 → RED")
        void shouldReturnRed() {
            assertThat(recoveryIndexService.semaphore(0.0)).isEqualTo("RED");
            assertThat(recoveryIndexService.semaphore(39.9)).isEqualTo("RED");
        }
    }
}
